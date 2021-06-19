package me.learning.microservices.photoapp.api.users.data.album;

import feign.Feign;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.context.annotation.Bean;

public class AlbumsServiceClientConfig {

    @Bean
    public Feign.Builder faultTolerantAlbumsServiceClient() {
        var circuitBreaker = CircuitBreaker.ofDefaults(AlbumsServiceClient.ALBUMS_SERVICE_NAME);
        var rateLimiter = RateLimiter.ofDefaults(AlbumsServiceClient.ALBUMS_SERVICE_NAME);
        var decorators = FeignDecorators.builder()
            .withRateLimiter(rateLimiter)
            .withCircuitBreaker(circuitBreaker)
            .withFallbackFactory(AlbumsServiceClientFallback::new)
            .build();

        return Resilience4jFeign.builder(decorators);
    }

    @Bean
    public AlbumsServiceClientErrorDecoder albumsServiceClientErrorDecoder() {
        return new AlbumsServiceClientErrorDecoder();
    }
}
