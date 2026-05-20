package com.quizzar.generation.dto;

import com.quizzar.question.entity.QuestionType;
import lombok.Data;
import java.util.List;

@Data
public class GenerateFromSpecsRequest {
    private String quizTitle;
    private String quizDescription;
    private List<QuestionType> questionTypes;
    private String gradeLevel;
    private Difficulty difficulty;
    private Integer numberOfQuestions;
    private String additionalNotes;
    private String syllabusText;
    private TimingPreference timingPreference;
    private Integer manualTimerSeconds;
}
