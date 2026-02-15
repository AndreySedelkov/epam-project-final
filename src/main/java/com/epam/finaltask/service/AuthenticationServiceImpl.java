package com.epam.finaltask.service;

import com.epam.finaltask.config.JwtProperties;
import com.epam.finaltask.dto.AuthRequestDTO;
import com.epam.finaltask.dto.AuthResponseDTO;
import com.epam.finaltask.dto.RefreshRequestDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    private Map<String, String> refreshTokenStore;
    private Set<String> tokenBlacklist;

    @PostConstruct
    public void init() {
        this.refreshTokenStore = new HashMap<>();
        this.tokenBlacklist = new HashSet<>();
    }

    @Override
    public AuthResponseDTO authenticate(AuthRequestDTO authRequestDTO) {
        String username = authRequestDTO.getUsername();
        String rawPassword = authRequestDTO.getPassword();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null || !passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String accessToken = generateToken(username, jwtProperties.getExpiration());
        String refreshToken = generateToken(username, jwtProperties.getRefreshTokenExpiration());

        refreshTokenStore.put(username, refreshToken);
        return new AuthResponseDTO(accessToken, refreshToken, username);
    }

    @Override
    public AuthResponseDTO refreshToken(RefreshRequestDTO refreshRequestDTO) {
        String providedRefreshToken = refreshRequestDTO.getRefreshToken();
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(providedRefreshToken)
                    .getBody();

            String username = claims.getSubject();
            String stored = refreshTokenStore.get(username);
            if (stored == null || !stored.equals(providedRefreshToken)) {
                throw new BadCredentialsException("Refresh token is invalid or expired");
            }

            String newAccess = generateToken(username, jwtProperties.getExpiration());
            String newRefresh = generateToken(username, jwtProperties.getRefreshTokenExpiration());

            refreshTokenStore.put(username, newRefresh);
            return new AuthResponseDTO(newAccess, newRefresh, username);
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid refresh token", ex);
        }
    }

    @Override
    public void logout(String token) {
        tokenBlacklist.add(token);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            refreshTokenStore.remove(claims.getSubject());
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    private String generateToken(String subject, long validityMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(
                        SignatureAlgorithm.HS256,
                        jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)
                )
                .compact();
    }
}