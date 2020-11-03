package me.learning.microservices.photoapp.api.albums.ui.model;

import lombok.Data;

@Data
public class AlbumResponse {

    private String id;

    private String albumId;

    private String userId;

    private String name;

    private String description;
}
