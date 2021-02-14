package me.learning.microservices.photoapp.api.albums.communication;

import feign.Response;
import feign.codec.ErrorDecoder;
import me.learning.microservices.photoapp.api.albums.service.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {

        switch (response.status()) {
            case 400:
                if (methodKey.contains("getUser")) {
                    return new Exception("Could not retrieve user due to bad request error: " + response.reason());
                }
                break;
            case 403:
                if (methodKey.contains("getUser")) {
                    return new Exception("Unauthorized to retrieve user: " + response.reason());
                }
                break;
            case 404:
                if (methodKey.contains("getUser")) {
                    return new UserNotFoundException("User not found: " + response.reason());
                }
                break;
            default:
                return new Exception(response.reason());
        }

        return null;
    }
}
