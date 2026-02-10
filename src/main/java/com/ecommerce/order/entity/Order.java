package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "t_orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNumber;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderLineItems> orderLineItemsList;

    private String userId;
    private java.math.BigDecimal totalAmount;

    // Storing address as JSON/String or Embedded. Using Embedded fields for
    // simplicity with JPA
    private String shippingFullName;
    private String shippingAddressLine;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingPhone;

    private String status; // PLACED, PAID, SHIPPED, DELIVERED, etc.
    private String trackingNumber;
    private String carrier;

    @org.hibernate.annotations.CreationTimestamp
    private java.time.LocalDateTime createdAt;
}
