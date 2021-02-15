package me.learning.microservices.photoapp.api.albums.data;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.albums.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;
import org.springframework.stereotype.Service;

@Service
class UsersServiceFallbackFactory implements FallbackFactory<UsersServiceClient> {

    @Slf4j
    static class UsersServiceClientFallback implements UsersServiceClient {

        private final Throwable cause;

        UsersServiceClientFallback(Throwable cause) {
            this.cause = cause;
        }

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

    @Override
    public UsersServiceClient create(Throwable throwable) {
        return new UsersServiceClientFallback(throwable);
    }
}
