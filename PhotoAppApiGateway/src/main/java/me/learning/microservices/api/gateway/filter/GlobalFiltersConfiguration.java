package me.learning.microservices.api.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class GlobalFiltersConfiguration {

    @Bean
    @Order(1)
    public GlobalFilter secondPreFilter() {
        return (exchange, chain) -> {
            log.info("Second Global Pre-filter is being executed...");

            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> log.info("Second Global Post-filter is being executed")));
        };
    }

    @Bean
    @Order(2)
    public GlobalFilter thirdPreFilter() {
        return (exchange, chain) -> {
            log.info("Third Global Pre-filter is being executed...");

            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> log.info("First Global Post-filter is being executed")));
        };
    }
}
