package me.learning.microservices.photoapp.api.albums.communication;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import feign.Response;
import feign.codec.ErrorDecoder;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    // TODO: refactor this!
    @Override
    public Exception decode(String methodKey, Response response) {

        switch (response.status()) {
            case 400:
                if (methodKey.contains("getUser")) {
                    return new ResponseStatusException(HttpStatus.valueOf(response.status()), "Bad Request");
                }
                break;
            case 403:
                if (methodKey.contains("getUser")) {
                    return new ResponseStatusException(HttpStatus.valueOf(response.status()), "Forbidden");
                }
                break;
            case 404:
                if (methodKey.contains("getUser")) {
                    return new ResponseStatusException(HttpStatus.valueOf(response.status()), "User  not found");
                }
                break;
            default:
                return new Exception(response.reason());
        }

        return null;
    }
}
