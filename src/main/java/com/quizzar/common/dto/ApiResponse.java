package com.quizzar.common.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String timestamp;
    private T data;
    private String message;
    
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .timestamp(OffsetDateTime.now().toString())
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .timestamp(OffsetDateTime.now().toString())
            .data(data)
            .message(message)
            .build();
    }
}
