package com.epam.finaltask.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.epam.finaltask.dto.UserDTO;
import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.mapper.UserMapper;
import com.epam.finaltask.model.Role;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private final UUID userId = UUID.randomUUID();
    private User domainUser;
    private UserDTO userDto;

    @BeforeEach
    void setUp() {
        domainUser = new User();
        domainUser.setId(userId);
        domainUser.setUsername("bobRoss");
        domainUser.setPassword("encoded");
        domainUser.setName("Bob");
        domainUser.setSurname("Ross");
        domainUser.setEmail("bob.ross@example.com");
        domainUser.setPhoneNumber("555-1234");
        domainUser.setRole(Role.USER);
        domainUser.setActive(false);
        domainUser.setBalance(BigDecimal.ZERO);

        userDto = new UserDTO();
        userDto.setId(userId);
        userDto.setUsername("bobRoss");
        userDto.setName("Bob");
        userDto.setSurname("Ross");
        userDto.setEmail("bob.ross@example.com");
        userDto.setPhoneNumber("555-1234");
        userDto.setBalance(BigDecimal.ZERO);
        userDto.setActive(true);
    }

    @Test
    @DisplayName("getUserByUsername – existing user → returns DTO")
    void getUserByUsername_UserExists_Success() {
        when(userRepository.findUserByUsername("bobRoss"))
                .thenReturn(Optional.of(domainUser));
        when(userMapper.toUserDTO(domainUser)).thenReturn(userDto);

        UserDTO result = userService.getUserByUsername("bobRoss");

        assertNotNull(result);
        assertEquals("bobRoss", result.getUsername());

        verify(userRepository, times(1)).findUserByUsername("bobRoss");
        verify(userMapper, times(1)).toUserDTO(domainUser);
    }

    @Test
    @DisplayName("getUserByUsername – missing user → throws ResourceNotFoundException")
    void getUserByUsername_UserNotFound_Throws() {
        when(userRepository.findUserByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername("unknown"));

        verify(userRepository).findUserByUsername("unknown");
    }

    @Test
    @DisplayName("getUserById – existing user → returns DTO")
    void getUserById_UserExist_Success() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(domainUser));
        when(userMapper.toUserDTO(domainUser)).thenReturn(userDto);

        UserDTO result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());

        verify(userRepository).findById(userId);
        verify(userMapper).toUserDTO(domainUser);
    }

    @Test
    @DisplayName("getUserById – missing user → throws ResourceNotFoundException")
    void getUserById_UserNotFound_Throws() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(userId));

        verify(userRepository).findById(userId);
    }
}