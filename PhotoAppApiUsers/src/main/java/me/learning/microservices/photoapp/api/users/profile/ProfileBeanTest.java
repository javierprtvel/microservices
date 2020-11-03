package me.learning.microservices.photoapp.api.users.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ProfileBeanTest {

    @Autowired
    private Environment env;

    @Bean
    @Profile("production")
    public String createProductionBean() {
        log.debug("Production bean created. Active profiles are: {}. Default profiles are: {}. myapplication.environment is: {}", env.getActiveProfiles(), env.getDefaultProfiles(), env.getProperty("myapplication.environment"));
        return "Production bean";
    }

    @Bean
    @Profile("!production")
    public String createNonProductionBean() {
        log.debug("Non production bean created. Active profiles are: {}. Default profiles are: {}. myapplication.environment is: {}", env.getActiveProfiles(), env.getDefaultProfiles(), env.getProperty("myapplication.environment"));
        return "Non production bean";
    }

    @Bean
    @Profile("default")
    public String createDevelopmentBean() {
        log.debug("Development bean created. Active profiles are: {}. Default profiles are: {}. myapplication.environment is: {}", env.getActiveProfiles(), env.getDefaultProfiles(), env.getProperty("myapplication.environment"));
        return "Development bean";
    }
}
