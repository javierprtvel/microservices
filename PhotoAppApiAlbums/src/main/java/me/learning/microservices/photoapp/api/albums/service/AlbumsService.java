package me.learning.microservices.photoapp.api.albums.service;

import me.learning.microservices.photoapp.api.albums.service.exception.AlbumNotFoundException;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumServiceException;
import me.learning.microservices.photoapp.api.albums.shared.AlbumDto;

import java.util.List;

public interface AlbumsService {

    AlbumDto createAlbum(AlbumDto details) throws AlbumServiceException;

    List<AlbumDto> findAlbumsByUserId(String userId);

    List<AlbumDto> findAllAlbums();

    AlbumDto findAlbumById(String id) throws AlbumNotFoundException;
}
