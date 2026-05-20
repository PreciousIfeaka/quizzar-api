package com.quizzar.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizLinkResponse {
    private String quizCode;
    private String shareUrl;
}
