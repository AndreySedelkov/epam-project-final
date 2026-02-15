package com.epam.finaltask.service;

import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.BalanceTopUpRequestRepository;
import com.epam.finaltask.repository.EmailVerificationTokenRepository;
import com.epam.finaltask.repository.PasswordResetTokenRepository;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepo;
    private final VoucherOrderRepository voucherOrderRepository;
    private final BalanceTopUpRequestRepository balanceTopUpRequestRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Transactional
    public void blockUser(UUID userId) {
        userRepo.findById(userId)
                .ifPresent(u -> u.setLocked(true));
    }


    @Transactional
    public void unblockUser(UUID userId) {
        userRepo.findById(userId)
                .ifPresent(u -> u.setLocked(false));
    }

    public List<User> findAllUsers() {
        return userRepo.findAll();
    }

    @Transactional
    public void deleteUser(UUID userId, String currentUsername) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("You cannot delete your own account");
        }

        voucherOrderRepository.deleteByUserId(userId);
        voucherOrderRepository.deleteByVoucherUserId(userId);
        balanceTopUpRequestRepository.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.deleteByUser(user);
        userRepo.delete(user);
    }
}
