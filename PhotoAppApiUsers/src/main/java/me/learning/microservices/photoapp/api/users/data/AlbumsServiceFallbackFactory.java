package me.learning.microservices.photoapp.api.users.data;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.service.exception.AlbumNotFoundException;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
class AlbumsServiceFallbackFactory implements FallbackFactory<AlbumsServiceClient> {

    @Slf4j
    static class AlbumsServiceClientFallback implements AlbumsServiceClient {

        private final Throwable cause;

        AlbumsServiceClientFallback(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public List<AlbumResponse> getAlbums(String authToken, String id) {

            // beware of the presence of any FeignErrorDecoder component
            if (this.cause instanceof AlbumNotFoundException) {
                log.error("404 error took place when 'getAlbums' was called with userID={}. Error message: {}", id, this.cause.getLocalizedMessage());
            } else {
                log.error("Unexpected error took place when 'getAlbums' was called with userID={}. Error message: {}", id, this.cause.getLocalizedMessage());
            }

            return Collections.emptyList();
        }
    }

    @Override
    public AlbumsServiceClient create(Throwable cause) {
        return new AlbumsServiceClientFallback(cause);
    }
}
