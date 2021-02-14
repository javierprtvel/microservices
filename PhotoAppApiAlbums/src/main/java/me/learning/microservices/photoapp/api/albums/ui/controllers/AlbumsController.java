package me.learning.microservices.photoapp.api.albums.ui.controllers;

import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.albums.data.UsersServiceClient;
import me.learning.microservices.photoapp.api.albums.mapper.AlbumMapper;
import me.learning.microservices.photoapp.api.albums.security.authorization.AuthorizationHeaderParser;
import me.learning.microservices.photoapp.api.albums.service.AlbumsService;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumNotFoundException;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumServiceException;
import me.learning.microservices.photoapp.api.albums.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.albums.shared.AlbumDto;
import me.learning.microservices.photoapp.api.albums.ui.model.AlbumResponse;
import me.learning.microservices.photoapp.api.albums.ui.model.CreateAlbumRequest;
import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class AlbumsController {

    @Autowired
    private Environment env;

    @Autowired
    private AlbumsService albumsService;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private UsersServiceClient usersServiceClient;

    @Autowired
    private AuthorizationHeaderParser authorizationHeaderParser;

    @GetMapping("/albums/status/check")
    public String status() {
        log.debug("Working on port " + env.getProperty("local.server.port") + ".");
        return "Working on port " + env.getProperty("local.server.port") + ".";
    }

    @GetMapping(value = "/users/{userId}/albums",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public List<AlbumResponse> getUserAlbums(@PathVariable String userId) {
        List<AlbumDto> userAlbumDtos = albumsService.findAlbumsByUserId(userId);
        if (userAlbumDtos == null || userAlbumDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<AlbumResponse> userAlbums = albumMapper.mapToAlbumResponses(userAlbumDtos);

        log.info("Returning {} albums from user {}", userAlbums.size(), userId);
        return userAlbums;
    }

    @GetMapping(path = "/albums",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public List<AlbumResponse> getAllAlbums() {
        List<AlbumDto> albumDtos = albumsService.findAllAlbums();
        if (albumDtos == null) {
            return Collections.emptyList();
        }
        List<AlbumResponse> albums = albumMapper.mapToAlbumResponses(albumDtos);

        log.info("Returning all albums: {}", albums.size());
        return albums;
    }

    @GetMapping(path = "/albums/{id}",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<AlbumResponse> getAlbum(@PathVariable String id) throws AlbumNotFoundException {
        return ResponseEntity.ok(albumMapper.mapToAlbumResponse(albumsService.findAlbumById(id)));
    }

    @PostMapping(path = "/users/{userId}/albums",
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<AlbumResponse> createUserAlbum(@PathVariable String userId, @RequestBody @Valid CreateAlbumRequest createAlbumRequest) throws AlbumServiceException {

        try {
            log.info("Calling Users Microservice...");
            String authToken = authorizationHeaderParser.getAuthorizationHeaderFromContext();
            UserResponse userResponse = usersServiceClient.getUser(authToken, userId);
            log.info("Call for Albums Microservice ended.");
            if (userResponse == null) {
                throw new UserNotFoundException("User not found");
            }

            AlbumDto createdAlbum = albumsService.createAlbum(albumMapper.mapToAlbumDto(createAlbumRequest));
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(albumMapper.mapToAlbumResponse(createdAlbum));
        } catch (UserNotFoundException e) {
            throw e;
        } catch (AlbumServiceException e) {
            log.error("Error creating album {} for user {}: {}", createAlbumRequest.getName(), userId, e.getLocalizedMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating album {} for user {}: {}", createAlbumRequest.getName(), userId, e.getLocalizedMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        }
    }
}
