package com.civicai.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    @Value("${security.api-key.header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${security.api-key.value:CivicAISecretKey2025}")
    private String apiKeyValue;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Bypass security for actuator endpoints
        if (path.startsWith("/actuator") || path.contains("/health")) {
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(apiKeyHeader)) {
            log.warn("Missing API key header in request to {}", path);
            return unauthorizedResponse(exchange, "Missing API Key");
        }

        String receivedApiKey = request.getHeaders().getFirst(apiKeyHeader);
        if (!apiKeyValue.equals(receivedApiKey)) {
            log.warn("Invalid API key provided in request to {}", path);
            return unauthorizedResponse(exchange, "Invalid API Key");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message).getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100; // Run early
    }
}
