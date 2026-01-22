package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    public String placeOrder(Order order) {
        order.setOrderNumber(UUID.randomUUID().toString());

        // Call Inventory Service, and place order if product is in stock
        Boolean allProductsInStock = order.getOrderLineItemsList().stream()
                .allMatch(lineItem -> {
                    Boolean result = webClientBuilder.build().get()
                            .uri("http://inventory-service:8083/api/inventory/" + lineItem.getSkuCode())
                            .retrieve()
                            .bodyToMono(Boolean.class)
                            .block();
                    return Boolean.TRUE.equals(result);
                });

        if (Boolean.TRUE.equals(allProductsInStock)) {
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", order.getOrderNumber());
            return "Order Placed Successfully";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    public String fallbackMethod(Order order, RuntimeException runtimeException) {
        return "Oops! Something went wrong, please order after some time!";
    }
}
