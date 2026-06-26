package com.quizzar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GoogleTokenRequest {
    @NotBlank(message = "token is required")
    @NotNull(message = "token is required")
    private String token;
}
