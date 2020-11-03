package me.learning.microservices.photoapp.api.users.ui.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "First name cannot be null")
    @Size(min = 2, message = "First name must not be less than two characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be null")
    @Size(min = 2, message = "Last name must not be less than two characters")
    private String lastName;

    @NotBlank(message = "Password cannot be null")
    @Size(min = 8, max = 16, message = "Password must contain between 8 and 16 characters")
    private String password;

    @NotBlank(message = "Email cannot be null")
    @Email
    private String email;
}
