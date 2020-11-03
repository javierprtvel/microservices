package me.learning.microservices.photoapp.api.users.shared;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;

@Data
public class UserDto implements Serializable {

    private static final long serialVersionUID = 8379832196355882395L;

    private String firstName;

    private String lastName;

    private String password;

    private String email;

    private String userId;

    private String encryptedPassword;

    private List<AlbumResponse> albums;
}
