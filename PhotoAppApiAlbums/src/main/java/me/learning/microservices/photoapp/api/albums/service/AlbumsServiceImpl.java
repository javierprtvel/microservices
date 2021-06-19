package me.learning.microservices.photoapp.api.albums.service;

import lombok.RequiredArgsConstructor;
import me.learning.microservices.photoapp.api.albums.data.album.Album;
import me.learning.microservices.photoapp.api.albums.data.album.AlbumsRepository;
import me.learning.microservices.photoapp.api.albums.mapper.AlbumMapper;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumNotFoundException;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumServiceException;
import me.learning.microservices.photoapp.api.albums.shared.AlbumDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumsServiceImpl implements AlbumsService {

    private final AlbumsRepository albumsRepository;

    private final AlbumMapper albumMapper;

    @Override
    public AlbumDto createAlbum(AlbumDto details) throws AlbumServiceException {
        details.setAlbumId(UUID.randomUUID().toString());

        Album album = albumMapper.map(details);
        Album createdAlbum;
        try {
            createdAlbum = albumsRepository.save(album);
        } catch (Exception e) {
            throw new AlbumServiceException("Album creation failed: " + e.getMessage());
        }

        return albumMapper.map(createdAlbum);
    }

    @Override
    public List<AlbumDto> findAlbumsByUserId(String userId) {
        return albumsRepository.findByUserId(userId).stream()
            .map(albumMapper::map)
            .collect(Collectors.toList());
    }

    @Override
    public List<AlbumDto> findAllAlbums() {
        return albumsRepository.findAll().stream()
            .map(albumMapper::map)
            .collect(Collectors.toList());
    }

    @Override
    public AlbumDto findAlbumById(String id) throws AlbumNotFoundException {
        return albumsRepository.findById(id)
            .map(albumMapper::map)
            .orElseThrow(() -> new AlbumNotFoundException("Album not found"));
    }
}
