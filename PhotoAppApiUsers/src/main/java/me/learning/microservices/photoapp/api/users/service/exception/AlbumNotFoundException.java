package me.learning.microservices.photoapp.api.users.service.exception;

public class AlbumNotFoundException extends RuntimeException {

    public AlbumNotFoundException() {}

    public AlbumNotFoundException(String message) {
        super(message);
    }
}
