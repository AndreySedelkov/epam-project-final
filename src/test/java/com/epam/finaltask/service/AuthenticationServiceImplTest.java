package com.epam.finaltask.service;

import com.epam.finaltask.config.JwtProperties;
import com.epam.finaltask.dto.AuthRequestDTO;
import com.epam.finaltask.dto.AuthResponseDTO;
import com.epam.finaltask.dto.RefreshRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl authService;

    private final String secretKey = "01234567890123456789012345678901";
    private final long accessExp = 1_000L;
    private final long refreshExp = 2_000L;

    @BeforeEach
    void setUp() {
        authService.init();

        lenient().when(jwtProperties.getSecretKey())
                .thenReturn(secretKey);
        lenient().when(jwtProperties.getExpiration())
                .thenReturn(accessExp);
        lenient().when(jwtProperties.getRefreshTokenExpiration())
                .thenReturn(refreshExp);
    }

    @Test
    @DisplayName("authenticate() returns tokens and saves refresh")
    void authenticateSuccess() {
        String username = "roman";
        String rawPass = "pass";
        String encPass = "encodedPass";

        UserDetails ud = User.withUsername(username)
                .password(encPass)
                .authorities("ROLE_USER")
                .build();

        given(userDetailsService.loadUserByUsername(username))
                .willReturn(ud);
        given(passwordEncoder.matches(rawPass, encPass))
                .willReturn(true);

        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername(username);
        req.setPassword(rawPass);

        AuthResponseDTO resp = authService.authenticate(req);

        assertThat(resp.getUsername()).isEqualTo(username);
        assertThat(resp.getAccessToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();

        RefreshRequestDTO refreshReq = new RefreshRequestDTO();
        refreshReq.setRefreshToken(resp.getRefreshToken());
        assertThatNoException()
                .isThrownBy(() -> authService.refreshToken(refreshReq));

        then(userDetailsService).should().loadUserByUsername(username);
        then(passwordEncoder).should().matches(rawPass, encPass);
    }

    @Test
    @DisplayName("authenticate() throws with invalid accounts")
    void authenticateBadCredentials() {
        String username = "bob";
        given(userDetailsService.loadUserByUsername(username))
                .willReturn(null);

        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername(username);
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.authenticate(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid username or password");

        then(userDetailsService).should().loadUserByUsername(username);
    }

    @Test
    @DisplayName("refreshToken() throws on an invalid token")
    void refreshTokenInvalid() {
        RefreshRequestDTO bad = new RefreshRequestDTO();
        bad.setRefreshToken("bad.token.value");

        assertThatThrownBy(() -> authService.refreshToken(bad))
                .isInstanceOf(BadCredentialsException.class);
        then(jwtProperties).should().getSecretKey();
    }

    @Test
    @DisplayName("logout() adds to the blacklist and removes refresh")
    void logoutAndBlacklist() {
        String username = "alex";
        String raw = "pw";
        String enc = "enc";

        UserDetails ud = User.withUsername(username)
                .password(enc)
                .authorities("ROLE_USER")
                .build();
        given(userDetailsService.loadUserByUsername(username))
                .willReturn(ud);
        given(passwordEncoder.matches(raw, enc))
                .willReturn(true);

        AuthRequestDTO authReq = new AuthRequestDTO();
        authReq.setUsername(username);
        authReq.setPassword(raw);
        AuthResponseDTO loginResp = authService.authenticate(authReq);

        String refreshToken = loginResp.getRefreshToken();

        authService.logout(refreshToken);

        assertThat(authService.isTokenBlacklisted(refreshToken)).isTrue();

        RefreshRequestDTO refReq = new RefreshRequestDTO();
        refReq.setRefreshToken(refreshToken);
        assertThatThrownBy(() -> authService.refreshToken(refReq))
                .isInstanceOf(BadCredentialsException.class);
    }
}