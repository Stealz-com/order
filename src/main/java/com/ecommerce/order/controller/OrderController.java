package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/ping")
    public String ping() {
        return "Order Service is Up!";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        return orderService.placeOrder(orderRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public java.util.List<com.ecommerce.order.dto.OrderResponse> getOrders(
            @RequestHeader("X-User-Id") String userId) {
        return orderService.getOrders(userId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public com.ecommerce.order.dto.OrderResponse getOrderById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        return orderService.getOrderById(id, userId);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public void updateOrderStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String message) {
        orderService.updateOrderStatus(id, status, trackingNumber, carrier, message);
    }

    @GetMapping("/{id}/tracking")
    @ResponseStatus(HttpStatus.OK)
    public com.ecommerce.order.dto.OrderTrackingResponse getOrderTracking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        return orderService.getOrderTracking(id, userId);
    }

    public String fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
        return "Oops! Something went wrong, please order after some time!";
    }
}
