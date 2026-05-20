package com.quizzar.generation.prompt;

import com.quizzar.generation.dto.GenerateFromSpecsRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Component
public class SpecsPromptBuilder {
    public String build(GenerateFromSpecsRequest request, String syllabusContext) {
        return String.format("""
            Generate %d quiz questions with the following specifications:
            - Quiz topic/title: %s
            - Question Types: %s
            - Grade/Educational Level: %s
            - Difficulty: %s
            - Additional Instructions: %s
            %s
            
            Ensure a balanced distribution of the requested question types.
            Each MCQ must have exactly 4 options with one correct answer.
            Each TRUE_FALSE must have exactly 2 options.
            """,
            request.getNumberOfQuestions(),
            request.getQuizTitle(),
            request.getQuestionTypes().stream().map(Enum::name).collect(Collectors.joining(", ")),
            request.getGradeLevel(),
            request.getDifficulty().name(),
            StringUtils.hasText(request.getAdditionalNotes()) ? request.getAdditionalNotes() : "None",
            StringUtils.hasText(syllabusContext) 
                ? "\nSyllabus/Curriculum Context:\n" + syllabusContext
                : ""
        );
    }
}
