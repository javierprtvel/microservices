package me.learning.microservices.photoapp.api.users.ui.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.mapper.UserMapper;
import me.learning.microservices.photoapp.api.users.service.UsersService;
import me.learning.microservices.photoapp.api.users.service.exception.UserAlreadyExistsException;
import me.learning.microservices.photoapp.api.users.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.users.shared.UserDto;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserRequest;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserResponse;
import me.learning.microservices.photoapp.api.users.ui.model.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {

    @Autowired
    private Environment env;

    private final UsersService usersService;

    private final UserMapper userMapper;

    @GetMapping("/status/check")
    public String status() {
        log.debug("Working on port " + env.getProperty("local.server.port") + ".");
        return "Working on port " + env.getProperty("local.server.port") + ".";
    }

    @PostMapping(
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest userDetails) throws UserAlreadyExistsException {
        UserDto createdUser = usersService.createUser(userMapper.map(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userMapper.mapToCreateUserResponse(createdUser));
    }

    @GetMapping(
        value = "/{userId}",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    @PreAuthorize("principal == #userId")
    public ResponseEntity<UserResponse> getUser(
        @PathVariable("userId") String userId,
        @RequestParam(name = "withAlbums", required = false, defaultValue = "false") boolean withAlbums) throws UserNotFoundException {

        UserDto userDto = withAlbums ?
            usersService.findUserByUserIdWithAlbums(userId)
            : usersService.findUserByUserId(userId);
        UserResponse user = userMapper.mapToUserResponse(userDto);
        return ResponseEntity.status(HttpStatus.OK)
            .body(user);
    }
}
