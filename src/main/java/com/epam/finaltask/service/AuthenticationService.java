package com.epam.finaltask.service;

import com.epam.finaltask.dto.AuthRequestDTO;
import com.epam.finaltask.dto.AuthResponseDTO;
import com.epam.finaltask.dto.RefreshRequestDTO;

public interface AuthenticationService {

    AuthResponseDTO authenticate(AuthRequestDTO authRequestDTO);

    AuthResponseDTO refreshToken(RefreshRequestDTO refreshRequestDTO);

    void logout(String token);

    boolean isTokenBlacklisted(String token);
}
