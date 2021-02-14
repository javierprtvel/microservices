package me.learning.microservices.photoapp.api.albums.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Album not found")
public class AlbumNotFoundException extends Exception {

    public AlbumNotFoundException() {
    }

    public AlbumNotFoundException(String message) {
        super(message);
    }
}
