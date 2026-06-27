package com.quizzar.generation.service;

import com.quizzar.document.entity.UploadedDocument;
import com.quizzar.document.repository.UploadedDocumentRepository;
import com.quizzar.generation.client.dto.AiQuizGenerationResult;
import com.quizzar.generation.client.dto.AiQuizGenerationResult.AiQuestionDto;
import com.quizzar.generation.dto.*;
import com.quizzar.generation.prompt.ExtractionPromptBuilder;
import com.quizzar.generation.prompt.FormattingPromptBuilder;
import com.quizzar.generation.prompt.SpecsPromptBuilder;
import com.quizzar.question.entity.AnswerOption;
import com.quizzar.question.entity.Question;
import com.quizzar.question.entity.QuestionType;
import com.quizzar.question.entity.ShortAnswerKey;
import com.quizzar.question.repository.QuestionRepository;
import com.quizzar.quiz.entity.Quiz;
import com.quizzar.quiz.entity.QuizMode;
import com.quizzar.quiz.entity.TimingMode;
import com.quizzar.quiz.repository.QuizRepository;
import com.quizzar.quiz.service.QuizService;
import com.quizzar.storage.service.S3StorageService;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GenerationOrchestrationService {

    private final DocumentExtractionService extractionService;
    private final AiGenerationService aiGenerationService;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final S3StorageService s3StorageService;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final TeacherRepository teacherRepository;
    private final QuizService quizService;
    private final ExtractionPromptBuilder extractionPromptBuilder;
    private final FormattingPromptBuilder formattingPromptBuilder;
    private final SpecsPromptBuilder specsPromptBuilder;

    @Value("${quizzar.base-url}")
    private String baseUrl;

    public GenerationResponse generateFromUpload(GenerateFromUploadRequest request,
            MultipartFile file,
            UUID teacherId) {
        Teacher teacher = getTeacher(teacherId);

        Quiz quiz = createQuizShell(request.getQuizTitle(), request.getQuizDescription(), request.getQuizMode(), teacher);

        String s3Key = s3StorageService.uploadFile(file, teacherId, quiz.getId());
        saveDocumentRecord(quiz, s3Key, file.getOriginalFilename(), file.getContentType(), file.getSize());

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }
        String extractedText = extractionService.extractText(fileBytes, file.getOriginalFilename(), file.getContentType());

        String prompt = extractionPromptBuilder.build(extractedText);
        AiQuizGenerationResult aiResult = aiGenerationService.generateQuiz(prompt);

        persistQuestions(quiz, aiResult.getQuestions());

        applyTiming(quiz, request.getTimingPreference(), request.getManualTimerSeconds(), aiResult);
        quizRepository.save(quiz);

        return buildGenerationResponse(quiz, aiResult, baseUrl);
    }

    public GenerationResponse generateFromPaste(GenerateFromPasteRequest request, UUID teacherId) {
        Teacher teacher = getTeacher(teacherId);
        Quiz quiz = createQuizShell(request.getQuizTitle(), request.getQuizDescription(), request.getQuizMode(), teacher);

        String prompt = formattingPromptBuilder.build(request.getRawText());
        AiQuizGenerationResult aiResult = aiGenerationService.generateQuiz(prompt);

        persistQuestions(quiz, aiResult.getQuestions());
        applyTiming(quiz, request.getTimingPreference(), request.getManualTimerSeconds(), aiResult);
        quizRepository.save(quiz);

        return buildGenerationResponse(quiz, aiResult, baseUrl);
    }

    public GenerationResponse generateFromSpecs(GenerateFromSpecsRequest request,
            MultipartFile syllabusFile,
            UUID teacherId) {
        Teacher teacher = getTeacher(teacherId);
        Quiz quiz = createQuizShell(request.getQuizTitle(), request.getQuizDescription(), request.getQuizMode(), teacher);

        String syllabusContext = "";
        if (syllabusFile != null && !syllabusFile.isEmpty()) {
            String s3Key = s3StorageService.uploadFile(syllabusFile, teacherId, quiz.getId());
            saveDocumentRecord(quiz, s3Key, syllabusFile.getOriginalFilename(), syllabusFile.getContentType(), syllabusFile.getSize());
            byte[] fileBytes;
            try {
                fileBytes = syllabusFile.getBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read uploaded syllabus file bytes", e);
            }
            syllabusContext = extractionService.extractText(fileBytes, syllabusFile.getOriginalFilename(), syllabusFile.getContentType());
        } else if (StringUtils.hasText(request.getSyllabusS3Key())) {
            saveDocumentRecord(quiz, request.getSyllabusS3Key(), request.getSyllabusFilename(), request.getSyllabusContentType(), request.getSyllabusSizeBytes());
            byte[] fileBytes = s3StorageService.downloadFile(request.getSyllabusS3Key());
            syllabusContext = extractionService.extractText(fileBytes, request.getSyllabusFilename(), request.getSyllabusContentType());
        } else if (StringUtils.hasText(request.getSyllabusText())) {
            syllabusContext = request.getSyllabusText();
        }

        String prompt = specsPromptBuilder.build(request, syllabusContext);
        AiQuizGenerationResult aiResult = aiGenerationService.generateQuiz(prompt);

        persistQuestions(quiz, aiResult.getQuestions());
        applyTiming(quiz, request.getTimingPreference(), request.getManualTimerSeconds(), aiResult);
        quizRepository.save(quiz);

        return buildGenerationResponse(quiz, aiResult, baseUrl);
    }

    private void persistQuestions(Quiz quiz, List<AiQuestionDto> aiQuestions) {
        for (AiQuestionDto dto : aiQuestions) {
            Question question = Question.builder()
                    .quiz(quiz)
                    .questionText(dto.getQuestionText())
                    .questionType(QuestionType.valueOf(dto.getQuestionType()))
                    .orderIndex(dto.getOrderIndex())
                    .points(dto.getPoints() != null ? dto.getPoints() : 1)
                    .build();

            if (dto.getOptions() != null) {
                List<AnswerOption> options = dto.getOptions().stream()
                        .map(o -> AnswerOption.builder()
                                .question(question)
                                .optionText(o.getText())
                                .optionLabel(o.getLabel())
                                .isCorrect(o.isCorrect())
                                .build())
                        .toList();
                question.setAnswerOptions(new ArrayList<>(options));
            }

            if (dto.getAcceptedAnswers() != null) {
                List<ShortAnswerKey> keys = dto.getAcceptedAnswers().stream()
                        .map(a -> ShortAnswerKey.builder()
                                .question(question)
                                .acceptedAnswer(a)
                                .isCaseSensitive(false)
                                .build())
                        .toList();
                question.setShortAnswerKeys(new ArrayList<>(keys));
            }

            questionRepository.save(question);
        }
    }

    private void applyTiming(Quiz quiz, TimingPreference preference, Integer manualSeconds,
            AiQuizGenerationResult aiResult) {
        quiz.setAiSuggestedTimeSeconds(aiResult.getAiSuggestedTimeSeconds());
        quiz.setAiSuggestedTimingMode(aiResult.getAiSuggestedTimingMode());

        if (preference == null || preference == TimingPreference.AI_SUGGESTED) {
            quiz.setTimingMode(TimingMode.valueOf(aiResult.getAiSuggestedTimingMode()));
            quiz.setTimerValueSeconds(aiResult.getAiSuggestedTimeSeconds());
        } else if (preference == TimingPreference.NONE) {
            quiz.setTimingMode(TimingMode.NONE);
            quiz.setTimerValueSeconds(null);
        } else {
            quiz.setTimingMode(TimingMode.valueOf(preference.name()));
            quiz.setTimerValueSeconds(manualSeconds);
        }

        if (quiz.getQuizMode() == QuizMode.PER_QUESTION) {
            if (quiz.getTimerValueSeconds() != null && quiz.getTimerValueSeconds() > 0) {
                quiz.setTimingMode(TimingMode.PER_QUESTION);
            } else {
                quiz.setTimingMode(TimingMode.NONE);
            }
        }
    }

    private Quiz createQuizShell(String title, String description, QuizMode quizMode, Teacher teacher) {
        Quiz quiz = Quiz.builder()
                .teacher(teacher)
                .title(title)
                .description(description)
                .quizCode(quizService.generateQuizCode())
                .quizMode(quizMode != null ? quizMode : QuizMode.OVERALL)
                .timingMode(TimingMode.NONE)
                .build();
        return quizRepository.save(quiz);
    }

    private Teacher getTeacher(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
    }

    private void saveDocumentRecord(Quiz quiz, String s3Key, String filename, String contentType, Long sizeBytes) {
        UploadedDocument doc = UploadedDocument.builder()
                .quiz(quiz)
                .s3Key(s3Key)
                .originalFilename(filename)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
        uploadedDocumentRepository.save(doc);
    }

    private GenerationResponse buildGenerationResponse(Quiz quiz, AiQuizGenerationResult aiResult, String baseUrl) {
        return GenerationResponse.builder()
                .quizId(quiz.getId())
                .quizCode(quiz.getQuizCode())
                .shareUrl(baseUrl + "/public/quiz/" + quiz.getQuizCode())
                .aiTimingSuggestion(new AiTimingSuggestion(
                        aiResult.getAiSuggestedTimingMode(),
                        aiResult.getAiSuggestedTimeSeconds(),
                        aiResult.getAiTimingReasoning()))
                .build();
    }
}
