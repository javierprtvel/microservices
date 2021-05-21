package me.learning.microservices.photoapp.api.users.data.user;

import java.util.Optional;

import me.learning.microservices.photoapp.api.users.data.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends CrudRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);
}
