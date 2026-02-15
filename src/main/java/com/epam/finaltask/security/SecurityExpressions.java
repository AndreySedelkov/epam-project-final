package com.epam.finaltask.security;

import com.epam.finaltask.repository.VoucherOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("securityExpressions")
@RequiredArgsConstructor
public class SecurityExpressions {

    private final VoucherOrderRepository voucherOrderRepository;

    public boolean isOrderOwner(UUID orderId, String username) {
        return voucherOrderRepository.findById(orderId)
                .map(order -> order.getUser() != null
                        && order.getUser().getUsername().equals(username))
                .orElse(false);
    }
}
