package me.learning.microservices.photoapp.api.users.ui.controllers.utils;

import java.util.Objects;
import lombok.experimental.UtilityClass;
import me.learning.microservices.photoapp.api.users.data.user.User;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserResponse;
import me.learning.microservices.photoapp.api.users.ui.model.UserResponse;

@UtilityClass
public class UsersControllerResponseUtils {

    public static boolean userResponseEqualsWithoutAlbums(User user, UserResponse userResponse) {
        return user == null && userResponse == null
            || user != null && userResponse != null
            && Objects.equals(user.getEmail(), userResponse.getEmail())
            && Objects.equals(user.getUserId(), userResponse.getUserId())
            && Objects.equals(user.getFirstName(), userResponse.getFirstName())
            && Objects.equals(user.getLastName(), userResponse.getLastName());
    }

    public static boolean createdUserResponseEqualsWithoutUserId(CreateUserResponse user, CreateUserResponse createUserResponse) {
        return user == null && createUserResponse == null
            || user != null && createUserResponse != null
            && Objects.equals(user.getEmail(), createUserResponse.getEmail())
            && Objects.equals(user.getFirstName(), createUserResponse.getFirstName())
            && Objects.equals(user.getLastName(), createUserResponse.getLastName());
    }
}
