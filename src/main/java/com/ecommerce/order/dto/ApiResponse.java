package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private int httpCode;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    public ApiResponse(boolean success, int httpCode, String message, T data) {
        this.success = success;
        this.httpCode = httpCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }
}
