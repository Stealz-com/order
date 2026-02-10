package com.ecommerce.order.client;

import com.ecommerce.order.dto.InventoryResponse;
import com.ecommerce.order.dto.StockRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {

    private final RestTemplate restTemplate;

    @Value("${inventory-service.url:http://inventory-service:8083}")
    private String inventoryServiceUrl;

    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        log.info("Calling Inventory Service for SKU codes: {}", skuCodes);
        String url = UriComponentsBuilder.fromHttpUrl(inventoryServiceUrl + "/api/inventory")
                .queryParam("skuCode", skuCodes)
                .toUriString();

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<InventoryResponse>>() {
                }).getBody();
    }

    public void deductStock(List<StockRequest> stockRequests) {
        log.info("Calling Inventory Service to deduct stock");
        String url = inventoryServiceUrl + "/api/inventory/deduct";

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(stockRequests),
                Void.class);
    }
}
