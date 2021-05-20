package me.learning.microservices.api.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(0)
@Slf4j
public class MyPreFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("First Global Pre-filter is being executed...");
        String requestPath = exchange.getRequest().getPath().toString();
        log.info("Request path = {}", requestPath);

        HttpHeaders headers = exchange.getRequest().getHeaders();
        headers.keySet()
            .forEach(headerName -> log.info("{} {}", headerName, headers.getFirst(headerName)));

        return chain.filter(exchange);
    }
}
