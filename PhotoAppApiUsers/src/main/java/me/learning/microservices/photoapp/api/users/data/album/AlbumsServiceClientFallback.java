package me.learning.microservices.photoapp.api.users.data.album;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.service.exception.AlbumNotFoundException;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;

@RequiredArgsConstructor
@Slf4j
public class AlbumsServiceClientFallback implements AlbumsServiceClient {

    private final Throwable cause;
    
    @Override
    public List<AlbumResponse> getAlbums(String authToken, String id) {
        // beware of the presence of any FeignErrorDecoder component
        if (this.cause instanceof AlbumNotFoundException) {
            log.error("404 error took place when 'getAlbums' was called with userID={}. Error message: {}", id, this.cause.getLocalizedMessage());
        } else {
            log.error("Unexpected error took place when 'getAlbums' was called with userID={}. Error message: {}", id, this.cause.getLocalizedMessage());
        }

        return List.of();
    }
}
