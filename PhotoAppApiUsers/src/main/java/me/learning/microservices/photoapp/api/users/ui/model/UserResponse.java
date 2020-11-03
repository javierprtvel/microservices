package me.learning.microservices.photoapp.api.users.ui.model;

import java.util.List;

import lombok.Data;

@Data
public class UserResponse {

    private String userId;

    private String firstName;

    private String lastName;

    private String email;

    private List<AlbumResponse> albums;
}
