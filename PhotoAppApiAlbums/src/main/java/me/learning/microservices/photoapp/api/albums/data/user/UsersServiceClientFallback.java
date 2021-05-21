package me.learning.microservices.photoapp.api.albums.data.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.albums.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;

@RequiredArgsConstructor
@Slf4j
public class UsersServiceClientFallback implements UsersServiceClient {

    private final Throwable cause;

    @Override
    public UserResponse getUser(String authToken, String userId) {
        // beware of the presence of any FeignErrorDecoder component
        if (this.cause instanceof UserNotFoundException) {
            log.error("404 error took place when 'getUser' was called with userID={}. Error message: {}", userId, this.cause.getLocalizedMessage());
        } else {
            log.error("Unexpected error took place when 'getAlbums' was called with userID={}. Error message: {}", userId, this.cause.getLocalizedMessage());
        }

        return null;
    }
}
