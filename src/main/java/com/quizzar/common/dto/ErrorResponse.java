package com.quizzar.common.dto;

import lombok.*;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
}
