package me.learning.microservices.photoapp.api.albums.communication;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FeignConfig {

    @Bean
    @Profile("!production")
    public Logger.Level feignDefaultLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    @Profile("production")
    public Logger.Level feignProductionLoggerLevel() {
        return Logger.Level.NONE;
    }
}
