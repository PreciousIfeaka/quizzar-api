package com.quizzar.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTimingSuggestion {
    private String mode;
    private Integer seconds;
    private String reasoning;
}
