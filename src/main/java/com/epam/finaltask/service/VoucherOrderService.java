package com.epam.finaltask.service;

import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.model.*;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherOrderRepository;
import com.epam.finaltask.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoucherOrderService {

    private static final Logger logger = LoggerFactory.getLogger(VoucherOrderService.class);

    private final VoucherRepository voucherRepository;
    private final VoucherOrderRepository voucherOrderRepository;
    private final UserRepository userRepository;

    @Transactional
    public VoucherOrder orderVoucher(String voucherId, String userId) {
        logger.info("Initiation of order processing for voucher {} by user {}", voucherId, userId);

        Voucher voucher = voucherRepository.findById(UUID.fromString(voucherId))
                .orElseThrow(() -> {
                    logger.error(
                            "Voucher with id {} not found", voucherId);
                    return new ResourceNotFoundException("Voucher not found with id: " + voucherId);
                });

        if (!voucher.isAvailableForPurchase()) {
            logger.error("Voucher {} is no longer available for purchase: registration period has expired", voucherId);
            throw new IllegalArgumentException("Registration period for this voucher has ended");
        }

        User user = userRepository.findUserByUsername(userId)
                .orElseThrow(() -> {
                    logger.error("User {} not found", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        logger.info(
                "Voucher status updated {}", voucherId);

        VoucherOrder order = new VoucherOrder();
        order.setUser(user);
        order.setVoucher(voucher);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setTotalPrice(voucher.getPrice());

        VoucherOrder savedOrder = voucherOrderRepository.save(order);
        logger.info(
                "Order created: id {}", savedOrder.getId());
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<VoucherOrder> findAllOrders() {
        return voucherOrderRepository.findAllWithUserAndVoucher();
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        VoucherOrder order = voucherOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot change status of confirmed order");
        }

        order.setOrderStatus(status);
        voucherOrderRepository.save(order);
    }
}
