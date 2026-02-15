package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.ApiResponse;
import com.epam.finaltask.dto.AuthRequestDTO;
import com.epam.finaltask.dto.AuthResponseDTO;
import com.epam.finaltask.dto.RefreshRequestDTO;
import com.epam.finaltask.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationRestController {

    private final AuthenticationService authenticationService;

    public AuthenticationRestController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @RequestBody @Valid AuthRequestDTO authRequestDTO) {
        AuthResponseDTO authResponse = authenticationService.authenticate(authRequestDTO);
        ApiResponse<AuthResponseDTO> response = new ApiResponse<>(
                "OK",
                "Authentication successful",
                authResponse
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(
            @RequestBody @Valid RefreshRequestDTO refreshRequestDTO) {
        AuthResponseDTO authResponse = authenticationService.refreshToken(refreshRequestDTO);
        ApiResponse<AuthResponseDTO> response = new ApiResponse<>(
                "OK",
                "Token refreshed successfully",
                authResponse
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "").trim();
        authenticationService.logout(token);
        ApiResponse<Void> response = new ApiResponse<>(
                "OK",
                "Logout successful",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}