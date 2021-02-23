package me.learning.microservices.photoapp.api.albums.ui.controllers.integration;

import lombok.Data;

@Data
public class UserCreationResponse {

    private String firstName;

    private String lastName;

    private String email;

    private String userId;
}

