package com.epam.finaltask.repository;

import com.epam.finaltask.model.PasswordResetToken;
import com.epam.finaltask.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {
    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.user = :user")
    void deleteByUser(@Param("user") User user);

    Optional<PasswordResetToken> findByToken(String token);
}


