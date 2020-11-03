package me.learning.microservices.photoapp.api.albums.shared;

import java.io.Serializable;

import lombok.Data;

@Data
public class AlbumDto implements Serializable {

    private static final long serialVersionUID = 3002895756190119941L;

    private String id;

    private String albumId;

    private String userId;

    private String name;

    private String description;
}
