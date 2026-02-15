package com.epam.finaltask.service;

import com.epam.finaltask.model.Role;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername → returns UserDetails when user exists, active & unlocked")
    void loadUserByUsernameSuccess() {
        User u = new User();
        u.setUsername("viktorKovalenko");
        u.setPassword("encrypted");
        u.setRole(Role.USER);
        u.setActive(true);
        u.setLocked(false);

        given(userRepository.findUserByUsername("viktorKovalenko"))
                .willReturn(Optional.of(u));

        UserDetails ud = service.loadUserByUsername("viktorKovalenko");

        assertThat(ud.getUsername()).isEqualTo("viktorKovalenko");
        assertThat(ud.getPassword()).isEqualTo("encrypted");
        assertThat(ud.isEnabled()).isTrue();
        assertThat(ud.isAccountNonLocked()).isTrue();
        assertThat(ud.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        then(userRepository).should().findUserByUsername("viktorKovalenko");
    }

    @Test
    @DisplayName("loadUserByUsername → throws when user not found")
    void loadUserByUsernameNotFound() {
        given(userRepository.findUserByUsername("missing"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: missing");

        then(userRepository).should().findUserByUsername("missing");
    }

    @Test
    @DisplayName("loadUserByUsername → returns non-locked UserDetails even if domain locked")
    void loadUserByUsernameLocked() {
        User u = new User();
        u.setUsername("lockedUser");
        u.setPassword("pwd");
        u.setRole(Role.USER);
        u.setActive(true);
        u.setLocked(true);

        given(userRepository.findUserByUsername("lockedUser"))
                .willReturn(Optional.of(u));

        UserDetails ud = service.loadUserByUsername("lockedUser");

        assertThat(ud.isAccountNonLocked()).isFalse();
        assertThat(ud.isEnabled()).isTrue();

        then(userRepository).should().findUserByUsername("lockedUser");
    }
}