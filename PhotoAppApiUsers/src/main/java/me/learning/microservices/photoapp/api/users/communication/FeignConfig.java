package me.learning.microservices.photoapp.api.users.communication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import feign.Logger;

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
