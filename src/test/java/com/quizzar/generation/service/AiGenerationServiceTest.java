package com.quizzar.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizzar.common.exception.AiGenerationException;
import com.quizzar.generation.client.GeminiClient;
import com.quizzar.generation.client.dto.AiQuizGenerationResult;
import com.quizzar.generation.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GeminiProperties geminiProperties;

    @InjectMocks
    private AiGenerationService aiGenerationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // Inject ObjectMapper manually since @InjectMocks won't find it
        ReflectionTestUtils.setField(aiGenerationService, "objectMapper", objectMapper);
    }

    @Test
    void generateQuiz_validResponse_parsesSuccessfully() {
        String validJson = """
            {
              "questions": [{
                "questionText": "What is 2+2?",
                "questionType": "MCQ",
                "orderIndex": 1,
                "points": 1,
                "options": [
                  {"label": "A", "text": "3", "isCorrect": false},
                  {"label": "B", "text": "4", "isCorrect": true},
                  {"label": "C", "text": "5", "isCorrect": false},
                  {"label": "D", "text": "6", "isCorrect": false}
                ],
                "acceptedAnswers": null
              }],
              "aiSuggestedTimingMode": "PER_QUESTION",
              "aiSuggestedTimeSeconds": 30,
              "aiTimingReasoning": "Simple question."
            }
            """;

        when(geminiProperties.getGenerationTemperature()).thenReturn(0.3);
        when(geminiProperties.getMaxOutputTokens()).thenReturn(8192);
        when(geminiClient.generate(any(), any(), anyDouble(), anyInt())).thenReturn(validJson);

        AiQuizGenerationResult result = aiGenerationService.generateQuiz("test prompt");

        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getQuestionType()).isEqualTo("MCQ");
        assertThat(result.getAiSuggestedTimingMode()).isEqualTo("PER_QUESTION");
    }

    @Test
    void generateQuiz_invalidJson_throwsAiGenerationException() {
        when(geminiProperties.getGenerationTemperature()).thenReturn(0.3);
        when(geminiProperties.getMaxOutputTokens()).thenReturn(8192);
        when(geminiClient.generate(any(), any(), anyDouble(), anyInt()))
            .thenReturn("not valid json at all");

        assertThatThrownBy(() -> aiGenerationService.generateQuiz("test prompt"))
            .isInstanceOf(AiGenerationException.class)
            .hasMessageContaining("could not be parsed");
    }

    @Test
    void generateQuiz_geminiClientThrows_propagatesException() {
        when(geminiProperties.getGenerationTemperature()).thenReturn(0.3);
        when(geminiProperties.getMaxOutputTokens()).thenReturn(8192);
        when(geminiClient.generate(any(), any(), anyDouble(), anyInt()))
            .thenThrow(new AiGenerationException("Gemini rate limit exceeded."));

        assertThatThrownBy(() -> aiGenerationService.generateQuiz("test prompt"))
            .isInstanceOf(AiGenerationException.class)
            .hasMessageContaining("rate limit");
    }

    @Test
    void gradeShortAnswer_correctAnswer_returnsTrue() {
        when(geminiProperties.getGradingTemperature()).thenReturn(0.1);
        when(geminiProperties.getGradingMaxOutputTokens()).thenReturn(10);
        when(geminiClient.generate(any(), any(), anyDouble(), anyInt())).thenReturn("true");

        boolean result = aiGenerationService.gradeShortAnswer(
            "What is the capital of France?",
            "Paris",
            List.of("Paris", "paris")
        );
        assertThat(result).isTrue();
    }

    @Test
    void gradeShortAnswer_geminiFailure_returnsFalseWithoutThrowing() {
        when(geminiProperties.getGradingTemperature()).thenReturn(0.1);
        when(geminiProperties.getGradingMaxOutputTokens()).thenReturn(10);
        when(geminiClient.generate(any(), any(), anyDouble(), anyInt()))
            .thenThrow(new AiGenerationException("Gemini unavailable"));

        // Must NOT throw — grading failure defaults to false
        boolean result = aiGenerationService.gradeShortAnswer(
            "What is 2+2?", "four", List.of("4", "four"));
        assertThat(result).isFalse();
    }
}
