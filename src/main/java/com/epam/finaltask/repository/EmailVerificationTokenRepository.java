package com.epam.finaltask.repository;

import com.epam.finaltask.model.EmailVerificationToken;
import com.epam.finaltask.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByUser(User user);

    Optional<EmailVerificationToken> findByUserAndToken(User user, String token);

    @Modifying
    @Transactional
    @Query("delete from EmailVerificationToken t where t.user = :user")
    void deleteByUser(@Param("user") User user);
}
