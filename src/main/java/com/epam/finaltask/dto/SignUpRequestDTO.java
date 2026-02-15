package com.epam.finaltask.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class SignUpRequestDTO {

    @NotEmpty(message = "username.required")
    private String username;

    @NotEmpty(message = "firstName.required")
    private String firstName;

    @NotEmpty(message = "lastName.required")
    private String lastName;

    @NotEmpty(message = "password.required")
    @Size(min = 8, message = "password.size")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).+$",
            message = "password.policy"
    )
    private String password;

    @NotEmpty(message = "password.confirm.required")
    private String confirmPassword;

    @NotEmpty(message = "email.required")
    @Email(message = "email.invalid")
    private String email;

    @NotEmpty(message = "phone.required")
    @Pattern(
            regexp = "\\+?[0-9\\- ]{7,15}",
            message = "phone.invalid"
    )
    private String phoneNumber;
}
