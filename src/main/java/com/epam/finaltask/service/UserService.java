package com.epam.finaltask.service;

import com.epam.finaltask.dto.UserDTO;
import com.epam.finaltask.dto.ProfileUpdateRequest;
import com.epam.finaltask.dto.SignUpRequestDTO;
import com.epam.finaltask.model.Role;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDTO register(SignUpRequestDTO dto);

    UserDTO getUserByUsername(String username);

    UserDTO updateUser(String username, UserDTO userDTO);

    UserDTO updateProfile(String username, ProfileUpdateRequest request);

    UserDTO changeAccountStatus(UserDTO userDTO);

    UserDTO getUserById(UUID id);

    void increaseFailedAttempts(String username);

    void resetFailedAttempts(String username);

    boolean isUserLocked(String username);

    void topUpBalance(String username, double amount);

    List<UserDTO> findAllUsers();

    void changeUserRole(UUID userId, Role newRole);
}
