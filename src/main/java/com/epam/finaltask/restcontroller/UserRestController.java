package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.ApiResponse;
import com.epam.finaltask.dto.SignUpRequestDTO;
import com.epam.finaltask.dto.UserDTO;
import com.epam.finaltask.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerUser(
            @RequestBody @Validated SignUpRequestDTO signUpRequest
    ) {
        UserDTO registeredUser = userService.register(signUpRequest);
        ApiResponse<UserDTO> response = new ApiResponse<>(
                "OK",
                "User registration successful",
                registeredUser
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        UserDTO userDTO = userService.getUserByUsername(username);
        ApiResponse<UserDTO> response = new ApiResponse<>(
                "OK",
                "User profile loaded successfully",
                userDTO
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserProfile(@RequestBody @Validated UserDTO userDTO,
                                                                  Authentication authentication) {
        String username = authentication.getName();
        UserDTO updatedUser = userService.updateUser(username, userDTO);
        ApiResponse<UserDTO> response = new ApiResponse<>(
                "OK",
                "User profile updated successfully",
                updatedUser
        );
        return ResponseEntity.ok(response);
    }
}