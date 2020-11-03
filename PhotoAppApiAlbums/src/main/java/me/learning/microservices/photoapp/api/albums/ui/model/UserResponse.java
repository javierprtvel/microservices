package me.learning.microservices.photoapp.api.albums.ui.model;

import lombok.Data;

@Data
public class UserResponse {

    private String userId;

    private String firstName;

    private String lastName;

    private String email;
}
