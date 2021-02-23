package me.learning.microservices.photoapp.api.albums.ui.controllers.utils;

import lombok.experimental.UtilityClass;
import me.learning.microservices.photoapp.api.albums.data.Album;
import me.learning.microservices.photoapp.api.albums.ui.model.AlbumResponse;

@UtilityClass
public class AlbumsControllerResponseUtils {

    public static boolean albumResponseEquals(Album album, AlbumResponse albumResponse) {
        return album == null && albumResponse == null
            || album != null && albumResponse != null
            && album.getAlbumId().equals(albumResponse.getAlbumId())
            && album.getName().equals(albumResponse.getName())
            && album.getId().equals(albumResponse.getId())
            && album.getUserId().equals(albumResponse.getUserId())
            && album.getDescription().equals(albumResponse.getDescription());
    }
}
