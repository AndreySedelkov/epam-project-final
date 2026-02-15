package com.epam.finaltask.service;

import com.epam.finaltask.model.AuthProvider;
import com.epam.finaltask.model.Role;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration()
                .getRegistrationId()
                .toLowerCase(Locale.ROOT);

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        String email = extractEmail(registrationId, attributes);
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email not found from OAuth2 provider");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(registrationId, attributes, email));

        if (!user.isAccountNonLocked() || user.isLocked() || !user.isActive()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_locked"),
                    "User account is locked or inactive");
        }

        Map<String, Object> principalAttributes = new HashMap<>(attributes);
        principalAttributes.put("username", user.getUsername());
        principalAttributes.put("email", user.getEmail());
        principalAttributes.put("name", user.getName());
        principalAttributes.put("surname", user.getSurname());

        List<GrantedAuthority> authorities = user.getRole().getAuthorities().stream()
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a.getAuthority()))
                .toList();

        return new DefaultOAuth2User(authorities, principalAttributes, "username");
    }

    private User createUser(String registrationId, Map<String, Object> attributes, String email) {
        String firstName = extractFirstName(registrationId, attributes);
        String lastName = extractLastName(registrationId, attributes);
        String providerId = extractProviderId(registrationId, attributes);
        String username = buildUniqueUsername(email, registrationId, providerId);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(firstName.isBlank() ? "User" : firstName);
        user.setSurname(lastName.isBlank() ? "OAuth" : lastName);
        user.setRole(Role.USER);
        user.setActive(true);
        user.setLocked(false);
        user.setFailedAttempts(0);
        user.setBalance(BigDecimal.ZERO);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhoneNumber(null);
        user.setAuthProvider(AuthProvider.GOOGLE);

        return userRepository.save(user);
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email instanceof String && !((String) email).isBlank()) {
            return (String) email;
        }
        return null;
    }

    private String extractFirstName(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return getString(attributes, "given_name");
        }
        return splitName(attributes).firstName;
    }

    private String extractLastName(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return getString(attributes, "family_name");
        }
        return splitName(attributes).lastName;
    }

    private String extractProviderId(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return getString(attributes, "sub");
        }
        return UUID.randomUUID().toString();
    }

    private String buildUniqueUsername(String email, String registrationId, String providerId) {
        String base = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = registrationId;
        }
        String candidate = base;
        if (userRepository.existsByUsername(candidate)) {
            String suffix = providerId.length() > 8 ? providerId.substring(0, 8) : providerId;
            candidate = base + "-" + registrationId + "-" + suffix;
        }
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return candidate;
    }

    private NameParts splitName(Map<String, Object> attributes) {
        String fullName = getString(attributes, "name");
        if (fullName.isBlank()) {
            return new NameParts("", "");
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        String first = parts.length > 0 ? parts[0] : "";
        String last = parts.length > 1 ? parts[1] : "";
        return new NameParts(first, last);
    }

    private String getString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String ? (String) value : "";
    }

    private static class NameParts {
        private final String firstName;
        private final String lastName;

        private NameParts(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
