package me.learning.microservices.photoapp.api.users.communication;

import feign.Response;
import feign.codec.ErrorDecoder;
import me.learning.microservices.photoapp.api.users.service.exception.AlbumNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {

        switch (response.status()) {
            case 400:
                if (methodKey.contains("getAlbums")) {
                    return new Exception("Could not retrieve user album due to bad request error: " + response.reason());
                }
                break;
            case 403:
                if (methodKey.contains("getAlbums")) {
                    return new Exception("Unauthorized to retrieve user album: " + response.reason());
                }
                break;
            case 404:
                if (methodKey.contains("getAlbums")) {
                    return new AlbumNotFoundException("User album not found");
                }
                break;
            default:
                return new Exception(response.reason());
        }

        return null;
    }
}
