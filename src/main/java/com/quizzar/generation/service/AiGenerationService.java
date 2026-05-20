package com.quizzar.generation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizzar.common.exception.AiGenerationException;
import com.quizzar.generation.client.GeminiClient;
import com.quizzar.generation.client.dto.AiQuizGenerationResult;
import com.quizzar.generation.client.dto.AnswerGradingDto;
import com.quizzar.generation.client.dto.GeminiShortAnswerResponse;
import com.quizzar.generation.config.GeminiProperties;
import com.quizzar.session.dto.ShortAnswerSubmission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    // —————————————————————————————————————————————————————————————————————————
    // SYSTEM PROMPT — injected on every generateContent call as systemInstruction
    // —————————————————————————————————————————————————————————————————————————
    private static final String GENERATION_SYSTEM_PROMPT = """
        You are an expert educational quiz designer. Your ONLY output must be a single valid JSON object.
        Do not include any text, explanation, or markdown formatting before or after the JSON.
        Do not wrap the JSON in code blocks. Output raw JSON only.

        The JSON must match this exact schema:
        {
          "questions": [
            {
              "questionText": "string",
              "questionType": "MCQ | TRUE_FALSE | SHORT_ANSWER",
              "orderIndex": number (starts at 1),
              "points": number (default 1),
              "options": [
                { "label": "A|B|C|D|True|False", "text": "string", "isCorrect": boolean }
              ] or null,
              "acceptedAnswers": ["string"] or null
            }
          ],
          "aiSuggestedTimingMode": "NONE | PER_QUESTION | OVERALL",
          "aiSuggestedTimeSeconds": number,
          "aiTimingReasoning": "string"
        }

        Hard rules:
        - "options" is non-null ONLY for MCQ and TRUE_FALSE. It is null for SHORT_ANSWER.
        - "acceptedAnswers" is non-null ONLY for SHORT_ANSWER. It is null for MCQ and TRUE_FALSE.
        - MCQ must have EXACTLY 4 options. Exactly ONE must have isCorrect = true.
        - TRUE_FALSE must have EXACTLY 2 options labelled "True" and "False". Exactly ONE isCorrect = true.
        - SHORT_ANSWER acceptedAnswers must contain at least 1 accepted phrase or keyword.
        - orderIndex starts at 1 and increments sequentially.
        - aiSuggestedTimeSeconds is per-question seconds if mode is PER_QUESTION, or total quiz seconds if OVERALL.
        - If the content is simple enough to not need timing, use NONE and set aiSuggestedTimeSeconds to 0.
        - Output must be valid, parseable JSON. Do not truncate.
        """;

    private static final String GRADING_SYSTEM_PROMPT = """
        You are a strict but fair quiz grader. You will be given a question, a student's answer,
        and a list of accepted answers or keywords. Determine if the student's answer is semantically
        correct or meaningfully equivalent to any accepted answer.

        Reply with ONLY the single word: true
        Or ONLY the single word: false

        No punctuation. No explanation. No other output.
        """;

    private static final String MULTIPLE_GRADING_SYSTEM_PROMPT = """
            You are a strict but fair quiz grader. You will be given a list of questions, the student's answers,
            and the accepted answers or keywords. Determine if the student's answers are semantically correct or
            meaningfully equivalent to any accepted answer. Your ONLY output must be a single valid JSON object.
            Do not include any text, explanation, or markdown formatting before or after the JSON.
            Do not wrap the JSON in code blocks. Output raw JSON only.

            The JSON must match this exact schema:
            
            {
              "answers": [
                {
                  "submissionId": "string",
                  "isCorrect": "boolean" // true or false
                }
              ]
            }
            
            - Output must be valid, parseable JSON. Do not truncate.""";

    // —————————————————————————————————————————————————————————————————————————
    // QUIZ GENERATION — called by GenerationOrchestrationService for all 3 modes
    // —————————————————————————————————————————————————————————————————————————

    /**
     * Sends a prompt to Gemini and parses the JSON response into AiQuizGenerationResult.
     * Retried up to 3 times with exponential backoff on AiGenerationException.
     *
     * @param userPrompt  Built by one of the three PromptBuilder classes
     * @return            Parsed quiz generation result
     */
    @Retryable(
        retryFor = {AiGenerationException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 10000)
    )
    public AiQuizGenerationResult generateQuiz(String userPrompt) {
        log.info("Sending quiz generation prompt to Gemini");

        String rawJson = geminiClient.generate(
            GENERATION_SYSTEM_PROMPT,
            userPrompt,
            geminiProperties.getGenerationTemperature(),
            geminiProperties.getMaxOutputTokens()
        );

        return parseGenerationResponse(rawJson);
    }

    @Recover
    public AiQuizGenerationResult recoverGeneration(AiGenerationException e, String userPrompt) {
        log.error("All Gemini quiz generation retries exhausted. Last error: {}", e.getMessage());
        throw new AiGenerationException(
            "Quiz generation failed after multiple attempts. Please try again later.");
    }

    // —————————————————————————————————————————————————————————————————————————
    // SHORT ANSWER GRADING — called per SHORT_ANSWER question at submit time
    // Uses lower temperature and a tiny token budget (just "true" or "false")
    // —————————————————————————————————————————————————————————————————————————

    /**
     * Uses Gemini to semantically grade a student's short answer.
     * Not retried — on failure, defaults to false (conservative grading).
     *
     * @param questionText    The original question
     * @param studentAnswer   What the student typed
     * @param acceptedAnswers List of accepted answers/keywords from ShortAnswerKey
     * @return                true if Gemini considers the answer correct
     */
    public boolean gradeShortAnswer(String questionText, String studentAnswer,
                                     List<String> acceptedAnswers) {
        if (studentAnswer == null || studentAnswer.isBlank()) return false;

        String userPrompt = String.format("""
            Question: %s
            Student's Answer: %s
            Accepted Answers / Keywords: %s
            """,
            questionText,
            studentAnswer.trim(),
            String.join(", ", acceptedAnswers)
        );

        try {
            String result = geminiClient.generate(
                GRADING_SYSTEM_PROMPT,
                userPrompt,
                geminiProperties.getGradingTemperature(),
                geminiProperties.getGradingMaxOutputTokens()
            );
            return "true".equalsIgnoreCase(result.trim());
        } catch (AiGenerationException e) {
            // Grading failure must not crash the submission — default to false
            log.warn("Short answer grading failed for question [{}], defaulting to false. Reason: {}",
                questionText.length() > 60 ? questionText.substring(0, 60) + "..." : questionText,
                e.getMessage());
            return false;
        }
    }

    @Retryable(
            retryFor = {AiGenerationException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 10000)
    )
    public Map<UUID, Boolean> gradeMultipleShortAnswers(
            String submissions
    ) {
        String userPrompt = String.format("""
                Submission Answers: %s""",
                submissions);

        String rawJson = geminiClient.generate(
                MULTIPLE_GRADING_SYSTEM_PROMPT,
                userPrompt,
                geminiProperties.getGenerationTemperature(),
                geminiProperties.getMaxOutputTokens()
        );

        return parseShortAnswersResponse(rawJson);
    }

    @Recover
    public Map<UUID, Boolean> recoverGradeMultipleShortAnswers(AiGenerationException e, String submissions) {
        log.error("All Gemini short answers response retries exhausted. Last error: {}", e.getMessage());
        throw new AiGenerationException(
                "Short answers response failed after multiple attempts. Please try again later.");
    }


    // —————————————————————————————————————————————————————————————————————————
    // PRIVATE HELPERS
    // —————————————————————————————————————————————————————————————————————————

    private AiQuizGenerationResult parseGenerationResponse(String rawJson) {
        try {
            // Defensively strip any accidental markdown fences Gemini may still add
            String cleaned = rawJson
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

            AiQuizGenerationResult result = objectMapper.readValue(cleaned, AiQuizGenerationResult.class);

            validateGenerationResult(result);
            return result;

        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON response. Raw response length: {}", rawJson.length());
            throw new AiGenerationException(
                "AI returned a response that could not be parsed. Please try again.");
        }
    }

    private Map<UUID, Boolean> parseShortAnswersResponse(String rawJson) {
        try {
            String cleaned = rawJson.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            GeminiShortAnswerResponse gradingPayload = objectMapper.readValue(
                    cleaned,
                    GeminiShortAnswerResponse.class
            );

            return gradingPayload.answers().stream()
                    .collect(Collectors.toMap(
                            answer -> UUID.fromString(answer.submissionId()),
                            AnswerGradingDto::isCorrect
                    ));
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini Short Answer JSON response. Raw response length: {}", rawJson.length());
            throw new AiGenerationException(
                    "AI returned a response that could not be parsed. Please try again.");
        }
    }

    private void validateGenerationResult(AiQuizGenerationResult result) {
        if (result == null) {
            throw new AiGenerationException("Parsed AI result is null.");
        }
        if (result.getQuestions() == null || result.getQuestions().isEmpty()) {
            throw new AiGenerationException(
                "AI returned no questions. The input may be too short or unclear.");
        }
        // Validate each question's structural integrity
        for (int i = 0; i < result.getQuestions().size(); i++) {
            var q = result.getQuestions().get(i);
            String ref = "Question at index " + i;

            if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
                throw new AiGenerationException(ref + " has no question text.");
            }
            if (q.getQuestionType() == null) {
                throw new AiGenerationException(ref + " has no question type.");
            }

            switch (q.getQuestionType()) {
                case "MCQ" -> {
                    if (q.getOptions() == null || q.getOptions().size() != 4) {
                        throw new AiGenerationException(ref + " (MCQ) must have exactly 4 options.");
                    }
                    long correctCount = q.getOptions().stream().filter(AiQuizGenerationResult.AiOptionDto::isCorrect).count();
                    if (correctCount != 1L) {
                        throw new AiGenerationException(ref + " (MCQ) must have exactly 1 correct option.");
                    }
                }
                case "TRUE_FALSE" -> {
                    if (q.getOptions() == null || q.getOptions().size() != 2) {
                        throw new AiGenerationException(ref + " (TRUE_FALSE) must have exactly 2 options.");
                    }
                }
                case "SHORT_ANSWER" -> {
                    if (q.getAcceptedAnswers() == null || q.getAcceptedAnswers().isEmpty()) {
                        throw new AiGenerationException(ref + " (SHORT_ANSWER) must have at least 1 accepted answer.");      
                    }
                }
                default -> throw new AiGenerationException(ref + " has unrecognised type: " + q.getQuestionType());
            }
        }
    }
}
