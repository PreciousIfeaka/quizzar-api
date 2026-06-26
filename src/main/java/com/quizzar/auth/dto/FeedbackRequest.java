package com.quizzar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank(message = "text cannot be blank")
    @NotNull(message = "text is required")
    private String text;

    @NotBlank(message = "imageUrl cannot be blank")
    @NotNull(message = "imageUrl is required")
    private String imageUrl;
}
