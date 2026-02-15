package com.epam.finaltask.service;

import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("blockUser() should lock the user when found")
    void blockUserShouldSetLockedTrue() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setLocked(false);

        given(userRepo.findById(userId)).willReturn(Optional.of(user));

        adminService.blockUser(userId);

        assertThat(user.isLocked()).isTrue();
        then(userRepo).should().findById(userId);
    }

    @Test
    @DisplayName("blockUser() should do nothing when user not found")
    void blockUserShouldHandleMissingUser() {
        UUID userId = UUID.randomUUID();
        given(userRepo.findById(userId)).willReturn(Optional.empty());

        adminService.blockUser(userId);
        then(userRepo).should().findById(userId);
    }

    @Test
    @DisplayName("unblockUser() should unlock the user when found")
    void unblockUserShouldSetLockedFalse() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setLocked(true);

        given(userRepo.findById(userId)).willReturn(Optional.of(user));

        adminService.unblockUser(userId);

        assertThat(user.isLocked()).isFalse();
        then(userRepo).should().findById(userId);
    }

    @Test
    @DisplayName("findAllUsers() should return all users from repository")
    void findAllUsersShouldReturnList() {
        User u1 = new User();
        User u2 = new User();
        given(userRepo.findAll()).willReturn(List.of(u1, u2));

        List<User> result = adminService.findAllUsers();

        assertThat(result).containsExactly(u1, u2);
        then(userRepo).should().findAll();
    }
}