package com.epam.finaltask.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String username;

    private String firstName;

    private String lastName;

    private String phoneNumber;
}
