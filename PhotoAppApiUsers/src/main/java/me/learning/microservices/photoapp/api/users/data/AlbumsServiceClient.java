package me.learning.microservices.photoapp.api.users.data;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;

@FeignClient(name = AlbumsServiceClient.ALBUMS_SERVICE_NAME, fallbackFactory = AlbumsServiceFallbackFactory.class)
public interface AlbumsServiceClient {

    String ALBUMS_SERVICE_NAME = "albums-ws";

    @GetMapping(path = "/users/{id}/albums")
    List<AlbumResponse> getAlbums(@RequestHeader("Authorization") String authToken, @PathVariable String id);
}
