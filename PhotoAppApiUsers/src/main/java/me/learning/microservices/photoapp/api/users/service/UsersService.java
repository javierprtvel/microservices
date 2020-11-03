package me.learning.microservices.photoapp.api.users.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import me.learning.microservices.photoapp.api.users.shared.UserDto;

public interface UsersService extends UserDetailsService {

    UserDto createUser(UserDto userDetails);

    UserDto findUserDetailsByEmail(String email);

    UserDto findUserByUserId(String userId);

    UserDto findUserByUserIdWithAlbums(String userId);
}
