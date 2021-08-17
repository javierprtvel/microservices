package me.learning.microservices.photoapp.api.albums.ui.controllers.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.lang.reflect.Type;
import java.util.List;
import lombok.experimental.UtilityClass;
import me.learning.microservices.photoapp.api.albums.data.album.Album;
import me.learning.microservices.photoapp.api.albums.ui.model.AlbumResponse;
import org.springframework.core.ParameterizedTypeReference;

@UtilityClass
public class AlbumResponseUtils {

    public static boolean albumResponseEquals(Album album, AlbumResponse albumResponse) {
        return album == null && albumResponse == null
            || album != null && albumResponse != null
            && album.getAlbumId().equals(albumResponse.getAlbumId())
            && album.getName().equals(albumResponse.getName())
            && album.getId().equals(albumResponse.getId())
            && album.getUserId().equals(albumResponse.getUserId())
            && album.getDescription().equals(albumResponse.getDescription());
    }

    public static TypeReference albumResponseListTypeReference() {
        var albumResponseListParameterizedTypeRef = new ParameterizedTypeReference<List<AlbumResponse>>() {};
        return new TypeReference<Object>() {
            @Override
            public Type getType() {
                return albumResponseListParameterizedTypeRef.getType();
            }
        };
    }
}
