package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private boolean success;
    private int httpCode;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;

    public ErrorResponse(int httpCode, String message, List<String> errors) {
        this.success = false;
        this.httpCode = httpCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }
}
