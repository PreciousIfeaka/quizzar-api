package com.quizzar.session.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SubmitAnswersRequest {
    private List<AnswerSubmission> answers;
}
