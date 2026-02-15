package com.epam.finaltask.repository;

import com.epam.finaltask.model.BalanceTopUpRequest;
import com.epam.finaltask.model.TopUpRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceTopUpRequestRepository extends JpaRepository<BalanceTopUpRequest, UUID> {
    List<BalanceTopUpRequest> findAllByStatusOrderByRequestedAtDesc(TopUpRequestStatus status);
    List<BalanceTopUpRequest> findAllByUserUsernameOrderByRequestedAtDesc(String username);
    List<BalanceTopUpRequest> findAllByOrderByRequestedAtDesc();

    @Modifying
    void deleteByUserId(UUID userId);
}
