package me.learning.microservices.photoapp.api.users.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "User not found")
public class UserNotFoundException extends Exception {

    public UserNotFoundException() {}

    public UserNotFoundException(String message) {
        super(message);
    }
}
