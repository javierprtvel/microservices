package me.learning.microservices.photoapp.api.users.ui.controllers.integration;

import lombok.Data;

@Data
public class Album {

    private final String id;

    private final String albumId;

    private final String userId;

    private final String name;

    private final String description;
}
