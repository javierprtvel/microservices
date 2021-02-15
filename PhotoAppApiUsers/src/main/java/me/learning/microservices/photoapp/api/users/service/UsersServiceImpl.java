package me.learning.microservices.photoapp.api.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.data.AlbumsServiceClient;
import me.learning.microservices.photoapp.api.users.data.User;
import me.learning.microservices.photoapp.api.users.data.UsersRepository;
import me.learning.microservices.photoapp.api.users.mapper.UserMapper;
import me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationHeaderParser;
import me.learning.microservices.photoapp.api.users.service.exception.UserAlreadyExistsException;
import me.learning.microservices.photoapp.api.users.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.users.shared.UserDto;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final AlbumsServiceClient albumsServiceClient;

    private final AuthorizationHeaderParser authorizationHeaderParser;

    @Override
    public UserDto createUser(UserDto userDetails) throws UserAlreadyExistsException {

        if (usersRepository.findByEmail(userDetails.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        userDetails.setUserId(UUID.randomUUID().toString());
        User user = userMapper.map(userDetails);
        user.setEncryptedPassword(passwordEncoder.encode(userDetails.getPassword()));
        User createdUser = usersRepository.save(user);

        return userMapper.map(createdUser);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = usersRepository.findByEmail(username); // email field is used as username
        return user
            .map(u -> new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getEncryptedPassword(),
                true,
                true,
                true,
                true,
                Collections.emptyList()
            ))
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Override
    public UserDto findUserDetailsByEmail(String email) throws UserNotFoundException {
        return usersRepository.findByEmail(email)
            .map(userMapper::map)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UserDto findUserByUserId(String userId) throws UserNotFoundException {
        return usersRepository.findByUserId(userId)
            .map(userMapper::map)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserDto findUserByUserIdWithAlbums(String userId) throws UserNotFoundException {
        log.info("Calling Albums Microservice...");
        String authToken = authorizationHeaderParser.getAuthorizationHeaderFromContext();
        List<AlbumResponse> albums = albumsServiceClient.getAlbums(authToken, userId);
        log.info("Call for Albums Microservice ended.");

        return usersRepository.findByUserId(userId)
            .map(user -> {
                UserDto u = userMapper.map(user);
                u.setAlbums(albums);
                return u;
            })
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
