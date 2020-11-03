package me.learning.microservices.photoapp.api.albums.ui.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateAlbumRequest {

    @NotBlank
    private String userId;

    @NotBlank
    @Size(min = 1, max = 120)
    private String name;

    @Size(max = 240)
    private String description;
}
