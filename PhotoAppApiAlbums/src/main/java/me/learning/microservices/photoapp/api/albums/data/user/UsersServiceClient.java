package me.learning.microservices.photoapp.api.albums.data.user;

import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = UsersServiceClient.USERS_SERVICE_NAME, configuration = UsersServiceClientConfig.class)
public interface UsersServiceClient {

    String USERS_SERVICE_NAME = "users-ws";

    @GetMapping(path = "/users/{userId}")
    UserResponse getUser(@RequestHeader("Authorization") String authToken, @PathVariable String userId);
}
