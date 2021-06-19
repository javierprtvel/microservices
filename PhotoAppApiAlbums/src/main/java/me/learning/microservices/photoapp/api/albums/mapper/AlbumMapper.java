package me.learning.microservices.photoapp.api.albums.mapper;

import java.util.List;

import me.learning.microservices.photoapp.api.albums.data.album.Album;
import me.learning.microservices.photoapp.api.albums.shared.AlbumDto;
import me.learning.microservices.photoapp.api.albums.ui.model.AlbumResponse;
import me.learning.microservices.photoapp.api.albums.ui.model.CreateAlbumRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlbumMapper {

    AlbumDto map(Album source);

    Album map(AlbumDto source);

    AlbumDto mapToAlbumDto(CreateAlbumRequest request);

    AlbumResponse mapToAlbumResponse(AlbumDto source);

    List<AlbumResponse> mapToAlbumResponses(List<AlbumDto> source);
}
