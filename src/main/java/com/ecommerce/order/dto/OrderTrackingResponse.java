package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderTrackingResponse {
    private Long orderId;
    private String orderNumber;
    private String status;
    private String trackingNumber;
    private String carrier;
    private List<StatusHistoryDto> history;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatusHistoryDto {
        private String status;
        private String message;
        private LocalDateTime createdAt;
    }
}
