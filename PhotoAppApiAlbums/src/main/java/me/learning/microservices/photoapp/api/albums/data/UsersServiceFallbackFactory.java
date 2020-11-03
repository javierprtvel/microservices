package me.learning.microservices.photoapp.api.albums.data;

import org.springframework.stereotype.Service;

import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;
import feign.FeignException;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;

@Service
public class UsersServiceFallbackFactory implements FallbackFactory<UsersServiceClient> {

    @Slf4j
    static class UsersServiceClientFallback implements UsersServiceClient {

        private final Throwable cause;

        public UsersServiceClientFallback(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public UserResponse getUser(String authToken, String userId) {

            // beware of the presence of any FeignErrorDecoder component
            if (this.cause instanceof FeignException && ((FeignException) this.cause).status() == 404) {
                log.error(
                    "404 error took place when 'getUser' was called with userID={}. Error message: {}",
                    userId,
                    this.cause.getLocalizedMessage(),
                    this.cause);
            } else {
                log.error("Another error took place: {}", this.cause.getLocalizedMessage());
            }

            return null;
        }
    }

    @Override
    public UsersServiceClient create(Throwable throwable) {
        return new UsersServiceClientFallback(throwable);
    }
}
