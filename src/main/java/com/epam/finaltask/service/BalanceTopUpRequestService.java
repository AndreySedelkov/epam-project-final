package com.epam.finaltask.service;

import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.model.BalanceTopUpRequest;
import com.epam.finaltask.model.TopUpRequestStatus;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.BalanceTopUpRequestRepository;
import com.epam.finaltask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceTopUpRequestService {

    private static final BigDecimal MAX_TOPUP = BigDecimal.valueOf(10000);

    private final BalanceTopUpRequestRepository requestRepository;
    private final UserRepository userRepository;

    @Transactional
    public BalanceTopUpRequest createRequest(String username, double amount) {
        BigDecimal value = BigDecimal.valueOf(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(MAX_TOPUP) > 0) {
            throw new IllegalArgumentException("The amount must be between $0 and $10,000.");
        }

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        BalanceTopUpRequest request = new BalanceTopUpRequest();
        request.setUser(user);
        request.setAmount(value);
        request.setStatus(TopUpRequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Transactional
    public void approveRequest(UUID requestId, String processedBy) {
        BalanceTopUpRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found: " + requestId));

        if (request.getStatus() != TopUpRequestStatus.PENDING) {
            throw new IllegalStateException("Top-up request already processed");
        }

        User user = request.getUser();
        BigDecimal current = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(current.add(request.getAmount()));

        request.setStatus(TopUpRequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedBy);

        userRepository.save(user);
        requestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(UUID requestId, String processedBy) {
        BalanceTopUpRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found: " + requestId));

        if (request.getStatus() != TopUpRequestStatus.PENDING) {
            throw new IllegalStateException("Top-up request already processed");
        }

        request.setStatus(TopUpRequestStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedBy);
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<BalanceTopUpRequest> findAllRequests() {
        return requestRepository.findAllByOrderByRequestedAtDesc();
    }
}
