package com.quizzar.session.dto;

import com.quizzar.quiz.entity.TimingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartSessionResponse {
    private UUID sessionId;
    private TimingMode timingMode;
    private Integer timerValueSeconds;
}
