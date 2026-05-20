package com.quizzar.teacher.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class TeacherProfileResponse {
    private UUID id;
    private String keycloakSubject;
    private String email;
    private String name;
}
