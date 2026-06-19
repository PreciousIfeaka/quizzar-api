package com.quizzar.generation.dto;

import com.quizzar.question.entity.QuestionType;
import com.quizzar.quiz.entity.QuizMode;
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
    private QuizMode quizMode = QuizMode.OVERALL;
    private String syllabusS3Key;
    private String syllabusFilename;
    private String syllabusContentType;
    private Long syllabusSizeBytes;
}
