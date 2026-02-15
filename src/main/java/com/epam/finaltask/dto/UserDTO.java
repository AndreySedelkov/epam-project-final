package com.epam.finaltask.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UserDTO {

    private UUID id;

    private String username;

    private String name;

    private String surname;

    private String email;

    private String role;

    private String phoneNumber;

    private BigDecimal balance;

    private boolean active;

    private String authProvider;
}

