package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private String orderNumber;
    private String email;
    private String firstName;
    private String lastName;
    private BigDecimal totalAmount;
    // We can include address if needed in email, but usually order number + total
    // is enough.
    // Let's include address just in case for email details.
    private OrderAddress shippingAddress;
    private java.util.List<LineItem> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LineItem {
        private String skuCode;
        private BigDecimal price;
        private Integer quantity;
        private String customImageUrl;
        private String originalImageUrl;
        private String designInstructions;
    }
}
