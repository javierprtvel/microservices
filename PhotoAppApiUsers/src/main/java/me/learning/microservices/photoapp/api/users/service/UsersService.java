package me.learning.microservices.photoapp.api.users.service;

import me.learning.microservices.photoapp.api.users.service.exception.UserAlreadyExistsException;
import me.learning.microservices.photoapp.api.users.service.exception.UserNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;

import me.learning.microservices.photoapp.api.users.shared.UserDto;

public interface UsersService extends UserDetailsService {

    UserDto createUser(UserDto userDetails) throws UserAlreadyExistsException;

    UserDto findUserDetailsByEmail(String email) throws UserNotFoundException;

    UserDto findUserByUserId(String userId) throws UserNotFoundException;

    UserDto findUserByUserIdWithAlbums(String userId) throws UserNotFoundException;
}
