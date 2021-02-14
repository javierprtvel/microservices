package me.learning.microservices.photoapp.api.albums.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Unexpected error in album service")
public class AlbumServiceException extends Exception {

    public AlbumServiceException() {}

    public AlbumServiceException(String message) {
        super(message);
    }
}
