package me.learning.microservices.photoapp.api.albums.data;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;

@FeignClient(name = UsersServiceClient.USERS_SERVICE_NAME, fallbackFactory = UsersServiceFallbackFactory.class)
public interface UsersServiceClient {

    String USERS_SERVICE_NAME = "users-ws";

    @GetMapping(path = "/users/{userId}")
    UserResponse getUser(@RequestHeader("Authorization") String authToken, @PathVariable String userId);
}
