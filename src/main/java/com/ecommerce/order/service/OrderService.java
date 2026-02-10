package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.dto.InventoryResponse;
import com.ecommerce.order.dto.OrderLineItemsDto;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.StockRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderLineItems;
import com.ecommerce.order.entity.OrderStatusHistory;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.ecommerce.order.dto.OrderTrackingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ecommerce.order.dto.OrderAddress;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public String placeOrder(OrderRequest orderRequest) {
        log.info("Initiating order placement process...");
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .collect(Collectors.toList());

        // 1. Check stock in bulk
        List<InventoryResponse> inventoryResponses = inventoryServiceClient.isInStock(skuCodes);

        boolean allProductsInStock = !inventoryResponses.isEmpty() && inventoryResponses.stream()
                .allMatch(InventoryResponse::isInStock);

        if (allProductsInStock) {
            // 2. Deduct stock
            List<StockRequest> stockRequests = order.getOrderLineItemsList().stream()
                    .map(item -> StockRequest.builder()
                            .skuCode(item.getSkuCode())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            inventoryServiceClient.deductStock(stockRequests);

            // 2.5 Map Address and other fields
            if (orderRequest.getShippingAddress() != null) {
                OrderAddress addr = orderRequest.getShippingAddress();
                order.setShippingFullName(addr.getFullName());
                order.setShippingAddressLine(addr.getAddress());
                order.setShippingCity(addr.getCity());
                order.setShippingState(addr.getState());
                order.setShippingZipCode(addr.getZipCode());
                order.setShippingPhone(addr.getPhone());
            }
            order.setUserId(orderRequest.getUserId());
            order.setTotalAmount(orderRequest.getTotalAmount());
            order.setStatus("PLACED");

            // 3. Save Order
            orderRepository.save(order);
            log.info("Order {} placed successfully", order.getOrderNumber());

            // 3.5 Log Initial Status History
            saveStatusHistory(order.getId(), "PLACED", "Order has been placed successfully.");

            // 4. Send Notification
            // Create Event
            // Note: Email logic is tricky. If OrderRequest has userId but no email, we
            // might need to fetch it.
            // Assumption: Frontend will pass email in userId field or we trust notification
            // service to find it?
            // Better: Frontend passes email in OrderRequest or Token.
            // For now, let's assume userId IS the email or we pass email separately.
            // BUT OrderRequest doesn't have email.
            // Let's assume the "userId" field in request might carry the email OR we rely
            // on a future "User Service" fetch.
            // PROPOSAL: Let's assume for now Notification Service listens to this event.
            // The event needs email.
            // Simple fix: Add 'email' to OrderRequest as well, or we fetch it.
            // Given I cannot easily change unrelated services right now, I will modify
            // OrderRequest to optionally take email if available.
            // Actually, I'll pass userId (which is often email in some systems) or just
            // rely on 'shippingFullName' splitting? No.
            // Let's assume userId is email for now (common in some tutorials) OR I'll add
            // 'email' field to OrderRequest.
            // I'll add 'email' field to OrderRequest in a separate tool call if needed, but
            // for now I'll use userId as email candidate.

            try {
                com.ecommerce.order.dto.OrderPlacedEvent event = new com.ecommerce.order.dto.OrderPlacedEvent();
                event.setOrderNumber(order.getOrderNumber());
                event.setTotalAmount(order.getTotalAmount());
                if (orderRequest.getShippingAddress() != null) {
                    event.setFirstName(orderRequest.getShippingAddress().getFullName());
                    event.setShippingAddress(orderRequest.getShippingAddress());
                }

                // Map items
                if (order.getOrderLineItemsList() != null) {
                    java.util.List<com.ecommerce.order.dto.OrderPlacedEvent.LineItem> eventItems = order
                            .getOrderLineItemsList().stream()
                            .map(item -> new com.ecommerce.order.dto.OrderPlacedEvent.LineItem(item.getSkuCode(),
                                    item.getPrice(), item.getQuantity()))
                            .collect(Collectors.toList());
                    event.setItems(eventItems);
                }

                // IMPORTANT: We need receiver email.
                String emailToSend = orderRequest.getEmail();
                if (emailToSend == null || emailToSend.isEmpty()) {
                    // Fallback as requested by user for testing
                    emailToSend = "aniket.ygosavi@gmail.com";
                }
                event.setEmail(emailToSend);

                String eventJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event);
                kafkaTemplate.send("notificationTopic", eventJson);
            } catch (Exception e) {
                log.error("Failed to send notification", e);
            }

            return "Order Placed Successfully";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setCustomImageUrl(orderLineItemsDto.getCustomImageUrl());
        return orderLineItems;
    }

    public String fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
        log.error("Fallback activated for order placement: {}", runtimeException.getMessage());
        return "Oops! Something went wrong, please order after some time!";
    }

    public List<com.ecommerce.order.dto.OrderResponse> getOrders(String userId) {
        log.info("Fetching all orders for user: {}", userId);
        try {
            List<Order> orders = orderRepository.findAllByUserId(userId);
            log.info("Found {} orders in DB", orders.size());
            return orders.stream()
                    .map(order -> {
                        try {
                            List<com.ecommerce.order.dto.OrderPlacedEvent.LineItem> responseItems = null;
                            if (order.getOrderLineItemsList() != null) {
                                responseItems = order.getOrderLineItemsList().stream()
                                        .map(item -> new com.ecommerce.order.dto.OrderPlacedEvent.LineItem(
                                                item.getSkuCode(), item.getPrice(), item.getQuantity()))
                                        .collect(Collectors.toList());
                            }

                            return com.ecommerce.order.dto.OrderResponse.builder()
                                    .id(order.getId())
                                    .orderNumber(order.getOrderNumber())
                                    .status(order.getStatus())
                                    .totalAmount(order.getTotalAmount())
                                    .createdAt(order.getCreatedAt())
                                    .items(responseItems)
                                    .build();
                        } catch (Exception e) {
                            log.error("Error mapping order {}", order.getOrderNumber(), e);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in getOrders", e);
            throw e;
        }
    }

    public com.ecommerce.order.dto.OrderResponse getOrderById(Long id, String userId) {
        log.info("Fetching order {} for user: {}", id, userId);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Verify the order belongs to the requesting user
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order: " + id);
        }

        List<com.ecommerce.order.dto.OrderPlacedEvent.LineItem> responseItems = null;
        if (order.getOrderLineItemsList() != null) {
            responseItems = order.getOrderLineItemsList().stream()
                    .map(item -> new com.ecommerce.order.dto.OrderPlacedEvent.LineItem(
                            item.getSkuCode(), item.getPrice(), item.getQuantity()))
                    .collect(Collectors.toList());
        }

        return com.ecommerce.order.dto.OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(responseItems)
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())
                .build();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status, String trackingNumber, String carrier, String message) {
        log.info("Updating status for order {}: to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (status != null) {
            order.setStatus(status);
        }
        if (trackingNumber != null) {
            order.setTrackingNumber(trackingNumber);
        }
        if (carrier != null) {
            order.setCarrier(carrier);
        }

        orderRepository.save(order);
        saveStatusHistory(orderId, status, message);

        // Send Real-time update event
        try {
            com.ecommerce.order.dto.OrderStatusUpdateEvent event = com.ecommerce.order.dto.OrderStatusUpdateEvent
                    .builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .status(order.getStatus())
                    .message(message != null ? message : "Order status updated to " + status)
                    .userId(order.getUserId())
                    .trackingNumber(order.getTrackingNumber())
                    .carrier(order.getCarrier())
                    .build();

            String eventJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event);
            kafkaTemplate.send("orderStatusTopic", eventJson);
            log.info("Sent status update event for order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send status update notification", e);
        }
    }

    public OrderTrackingResponse getOrderTracking(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order tracking: " + orderId);
        }

        List<OrderStatusHistory> history = orderStatusHistoryRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);

        List<OrderTrackingResponse.StatusHistoryDto> historyDtos = history.stream()
                .map(h -> OrderTrackingResponse.StatusHistoryDto.builder()
                        .status(h.getStatus())
                        .message(h.getMessage())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return OrderTrackingResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())
                .history(historyDtos)
                .build();
    }

    private void saveStatusHistory(Long orderId, String status, String message) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(orderId)
                .status(status)
                .message(message)
                .build();
        orderStatusHistoryRepository.save(history);
    }
}
