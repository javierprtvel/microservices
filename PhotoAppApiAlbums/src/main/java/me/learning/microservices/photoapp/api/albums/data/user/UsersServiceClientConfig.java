package me.learning.microservices.photoapp.api.albums.data.user;

import feign.Feign;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.context.annotation.Bean;

public class UsersServiceClientConfig {

    @Bean
    public Feign.Builder faultTolerantUsersServiceClient() {
        var circuitBreaker = CircuitBreaker.ofDefaults(UsersServiceClient.USERS_SERVICE_NAME);
        var rateLimiter = RateLimiter.ofDefaults(UsersServiceClient.USERS_SERVICE_NAME);
        var decorators = FeignDecorators.builder()
            .withRateLimiter(rateLimiter)
            .withCircuitBreaker(circuitBreaker)
            .withFallbackFactory(UsersServiceClientFallback::new)
            .build();

        return Resilience4jFeign.builder(decorators);
    }

    @Bean
    public UsersServiceClientErrorDecoder usersServiceClientErrorDecoder() {
        return new UsersServiceClientErrorDecoder();
    }
}
