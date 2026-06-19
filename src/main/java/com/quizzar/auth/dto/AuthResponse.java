package com.quizzar.auth.dto;

import com.quizzar.teacher.dto.TeacherProfileResponse;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private TeacherProfileResponse profile;
}
