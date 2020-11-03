package me.learning.microservices.photoapp.api.users.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.data.AlbumsServiceClient;
import me.learning.microservices.photoapp.api.users.data.User;
import me.learning.microservices.photoapp.api.users.data.UsersRepository;
import me.learning.microservices.photoapp.api.users.mapper.UserMapper;
import me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationHeaderParser;
import me.learning.microservices.photoapp.api.users.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.users.shared.UserDto;
import me.learning.microservices.photoapp.api.users.service.exception.UserAlreadyExistsException;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;

@Slf4j
@Service
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final AlbumsServiceClient albumsServiceClient;

    private final AuthorizationHeaderParser authorizationHeaderParser;

    @Autowired
    public UsersServiceImpl(UsersRepository usersRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
        AlbumsServiceClient albumsServiceClient, AuthorizationHeaderParser authorizationHeaderParser) {
        this.usersRepository = usersRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.albumsServiceClient = albumsServiceClient;
        this.authorizationHeaderParser = authorizationHeaderParser;
    }

    @Override
    public UserDto createUser(UserDto userDetails) {
        userDetails.setUserId(UUID.randomUUID().toString());

        usersRepository.findByEmail(userDetails.getEmail())
            .ifPresent(user -> { throw new UserAlreadyExistsException("User already exists"); });

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
    public UserDto findUserDetailsByEmail(String email) {
        return usersRepository.findByEmail(email)
            .map(userMapper::map)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UserDto findUserByUserId(String userId) {
        return usersRepository.findByUserId(userId)
            .map(userMapper::map)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserDto findUserByUserIdWithAlbums(String userId) {
        log.info("Calling Albums Microservice...");
        String authToken = authorizationHeaderParser.getAuthorizationHeaderFromContext();
        /* Feign with no fallback approach
        List<AlbumResponse> albums;
        try {
            albums = albumsServiceClient.getAlbums(userId);
        } catch (FeignException e) {
            log.error("Error getting albums for user '{}': {}", userId, e.getLocalizedMessage());
            albums = null;
        }
        */
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
