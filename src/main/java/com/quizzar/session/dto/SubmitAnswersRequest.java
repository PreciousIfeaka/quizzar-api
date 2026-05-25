package com.quizzar.session.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmitAnswersRequest {
    private List<AnswerSubmission> answers;
}
