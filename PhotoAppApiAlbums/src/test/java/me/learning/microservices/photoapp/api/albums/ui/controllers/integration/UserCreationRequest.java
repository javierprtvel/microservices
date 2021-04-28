package me.learning.microservices.photoapp.api.albums.ui.controllers.integration;

import lombok.Data;

@Data
public class UserCreationRequest {

    private final String firstName;

    private final String lastName;

    private final String password;

    private final String email;
}
