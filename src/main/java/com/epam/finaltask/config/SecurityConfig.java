package com.epam.finaltask.config;

import com.epam.finaltask.handler.CustomAuthenticationFailureHandler;
import com.epam.finaltask.handler.CustomAuthenticationSuccessHandler;
import com.epam.finaltask.service.CustomOidcUserService;
import com.epam.finaltask.service.CustomOAuth2UserService;
import com.epam.finaltask.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService               userDetailsService;
    private final PasswordEncoder                        passwordEncoder;
    private final CustomAuthenticationFailureHandler     failureHandler;
    private final CustomAuthenticationSuccessHandler     successHandler;
    private final CustomOidcUserService                  oidcUserService;
    private final CustomOAuth2UserService                oAuth2UserService;

    @Value("${app.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider authProvider)
            throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/api/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/swagger-ui.html"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/webjars/**"),
                                new AntPathRequestMatcher("/auth/telegram")

                        )
                )
                .headers(headers -> headers
                        .frameOptions(frameOpts -> frameOpts.sameOrigin())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/auth/sign-in",
                                "/auth/sign-up",
                                "/auth/send-verification-code",
                                "/auth/verify-email-code",
                                "/auth/forgot-password",
                                "/auth/forgot-password/**",
                                "/auth/reset-password",
                                "/auth/reset-password/**",
                                "/auth/telegram",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/uploads/**",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/h2-console/**",
                                "/error",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/sign-in")
                        .loginProcessingUrl("/auth/login")
                        .failureHandler(failureHandler)
                        .successHandler(successHandler)
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .authenticationProvider(authProvider);

        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/sign-in")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(oAuth2UserService)
                            .oidcUserService(oidcUserService)
                    )
                    .failureHandler(failureHandler)
                    .successHandler(successHandler)
            );
        }

        return http.build();
    }
}
