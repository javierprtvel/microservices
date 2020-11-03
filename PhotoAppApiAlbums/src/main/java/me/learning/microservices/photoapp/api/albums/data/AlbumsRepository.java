package me.learning.microservices.photoapp.api.albums.data;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumsRepository extends CrudRepository<Album, String> {

    List<Album> findAll();

    List<Album> findByUserId(String userId);
}
