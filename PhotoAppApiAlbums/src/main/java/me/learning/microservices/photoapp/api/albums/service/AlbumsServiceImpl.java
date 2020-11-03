package me.learning.microservices.photoapp.api.albums.service;

import me.learning.microservices.photoapp.api.albums.data.Album;
import me.learning.microservices.photoapp.api.albums.data.AlbumsRepository;
import me.learning.microservices.photoapp.api.albums.mapper.AlbumMapper;
import me.learning.microservices.photoapp.api.albums.shared.AlbumDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlbumsServiceImpl implements AlbumsService {

    @Autowired
    private AlbumsRepository albumsRepository;

    @Autowired
    private AlbumMapper albumMapper;

    @Override
    public AlbumDto createAlbum(AlbumDto details) {
        details.setAlbumId(UUID.randomUUID().toString());

        Album album = albumMapper.map(details);
        Album createdAlbum = albumsRepository.save(album);

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
    public Optional<AlbumDto> findAlbumById(String id) {
        return albumsRepository.findById(id)
            .map(albumMapper::map);
    }
}
