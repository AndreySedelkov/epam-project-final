package com.epam.finaltask.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {

    private String accessToken;

    private String refreshToken;

    private String username;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String accessToken, String refreshToken, String username) {

        this.accessToken = accessToken;

        this.refreshToken = refreshToken;

        this.username = username;
    }
}