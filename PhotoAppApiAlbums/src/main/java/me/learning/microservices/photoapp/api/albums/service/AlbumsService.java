package me.learning.microservices.photoapp.api.albums.service;

import me.learning.microservices.photoapp.api.albums.shared.AlbumDto;

import java.util.List;
import java.util.Optional;

public interface AlbumsService {

    AlbumDto createAlbum(AlbumDto details);

    List<AlbumDto> findAlbumsByUserId(String userId);

    List<AlbumDto> findAllAlbums();

    Optional<AlbumDto> findAlbumById(String id);
}
