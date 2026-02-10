package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusUpdateEvent {
    private Long orderId;
    private String orderNumber;
    private String status;
    private String message;
    private String userId;
    private String trackingNumber;
    private String carrier;
}
