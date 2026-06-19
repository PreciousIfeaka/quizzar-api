package com.quizzar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvatarUrlRequest {
    @NotBlank(message = "Avatar URL is required")
    private String avatarUrl;
}
