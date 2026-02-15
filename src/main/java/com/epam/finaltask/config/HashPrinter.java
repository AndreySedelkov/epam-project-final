package com.epam.finaltask.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HashPrinter implements CommandLineRunner {

    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        System.out.println("bcrypt(121212) = " + encoder.encode("121212"));
    }
}
