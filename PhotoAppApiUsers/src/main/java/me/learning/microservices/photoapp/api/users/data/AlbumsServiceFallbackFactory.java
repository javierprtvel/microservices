package me.learning.microservices.photoapp.api.users.data;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import feign.FeignException;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;

@Service
class AlbumsServiceFallbackFactory implements FallbackFactory<AlbumsServiceClient> {

    @Slf4j
    static class AlbumsServiceClientFallback implements AlbumsServiceClient {

        private final Throwable cause;

        public AlbumsServiceClientFallback(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public List<AlbumResponse> getAlbums(String authToken, String id) {

            // beware of the presence of any FeignErrorDecoder component
            if (this.cause instanceof FeignException && ((FeignException) this.cause).status() == 404) {
                log.error("404 error took place when 'getAlbums' was called with userID={}. Error message: {}", id, this.cause.getLocalizedMessage(), this.cause);
            } else {
                log.error("Another error took place: {}", this.cause.getLocalizedMessage());
            }

            return Collections.emptyList();
        }
    }

    @Override
    public AlbumsServiceClient create(Throwable cause) {
        return new AlbumsServiceClientFallback(cause);
    }
}
