package me.learning.microservices.photoapp.api.users.data.album;

import java.util.List;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = AlbumsServiceClient.ALBUMS_SERVICE_NAME, configuration = AlbumsServiceClientConfig.class)
public interface AlbumsServiceClient {

    String ALBUMS_SERVICE_NAME = "albums-ws";

    @GetMapping(path = "/users/{id}/albums")
    List<AlbumResponse> getAlbums(@RequestHeader("Authorization") String authToken, @PathVariable String id);
}
