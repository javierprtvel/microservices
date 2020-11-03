package me.learning.microservices.photoapp.api.albums.data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;
import org.springframework.data.couchbase.core.mapping.id.GeneratedValue;
import org.springframework.data.couchbase.core.mapping.id.GenerationStrategy;
import org.springframework.data.couchbase.core.mapping.id.IdAttribute;

import lombok.Data;

@Document
@Data
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationStrategy.USE_ATTRIBUTES)
    private String id;

    @Field
    @NotBlank
    @IdAttribute(order = 1)
    private String albumId;

    @Field
    @NotBlank
    @IdAttribute(order = 0)
    private String userId;

    @Field
    @NotBlank
    @Size(min = 1, max = 120)
    private String name;

    @Field
    @Size(max = 240)
    private String description;
}
