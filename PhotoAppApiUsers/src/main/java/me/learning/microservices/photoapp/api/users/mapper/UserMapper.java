package me.learning.microservices.photoapp.api.users.mapper;

import me.learning.microservices.photoapp.api.users.data.User;
import me.learning.microservices.photoapp.api.users.shared.UserDto;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserRequest;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserResponse;
import me.learning.microservices.photoapp.api.users.ui.model.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto map(User source);

    User map(UserDto source);

    UserDto map(CreateUserRequest source);

    CreateUserResponse mapToCreateUserResponse(UserDto source);

    UserResponse mapToUserResponse(UserDto source);
}
