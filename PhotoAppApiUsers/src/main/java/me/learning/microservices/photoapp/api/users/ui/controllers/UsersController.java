package me.learning.microservices.photoapp.api.users.ui.controllers;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import me.learning.microservices.photoapp.api.users.mapper.UserMapper;
import me.learning.microservices.photoapp.api.users.service.UsersService;
import me.learning.microservices.photoapp.api.users.service.exception.UserAlreadyExistsException;
import me.learning.microservices.photoapp.api.users.shared.UserDto;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserRequest;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserResponse;
import me.learning.microservices.photoapp.api.users.ui.model.UserResponse;

@RestController
@RequestMapping("/users")
@Slf4j
public class UsersController {

    @Autowired
    private Environment env;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserMapper userMapper;


    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "User not found")
    @ExceptionHandler(UsernameNotFoundException.class)
    public void userNotFound() {}

    @ResponseStatus(code = HttpStatus.CONFLICT, reason = "User already exists")
    @ExceptionHandler(UserAlreadyExistsException.class)
    public void userConflict() {}


    @GetMapping("/status/check")
    public String status() {
        log.debug("Working on port " + env.getProperty("local.server.port") + ".");
        return "Working on port " + env.getProperty("local.server.port") + ".";
    }

    @PostMapping(
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE },
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest userDetails) {
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
        @RequestParam(name = "withAlbums", required = false, defaultValue = "false") boolean withAlbums) {

        UserDto userDto = withAlbums ?
            usersService.findUserByUserIdWithAlbums(userId)
            : usersService.findUserByUserId(userId);
        UserResponse user = userMapper.mapToUserResponse(userDto);
        return ResponseEntity.status(HttpStatus.OK)
            .body(user);
    }
}
