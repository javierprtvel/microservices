package me.learning.microservices.photoapp.api.albums.security.exception;

public class AuthorizationHeaderException extends Exception {

    private static final long serialVersionUID = 1980783804240031265L;

    public AuthorizationHeaderException() {
        super();
    }

    public AuthorizationHeaderException(String message) {
        super(message);
    }
}
