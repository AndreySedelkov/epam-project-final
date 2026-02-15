package com.epam.finaltask.service;

import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.model.*;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherOrderRepository;
import com.epam.finaltask.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class VoucherOrderServiceTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherOrderRepository voucherOrderRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private VoucherOrderService service;

    private String voucherId;
    private String userId;
    private UUID voucherUuid;
    private Voucher voucher;
    private User user;

    @BeforeEach
    void setUp() {
        voucherId   = UUID.randomUUID().toString();
        userId      = "bobRoss";
        voucherUuid = UUID.fromString(voucherId);

        voucher = new Voucher();
        voucher.setId(voucherUuid);
        voucher.setPrice(150.0);
        voucher.setArrivalDate(LocalDate.now().plusDays(1));
        voucher.setEvictionDate(LocalDate.now().plusDays(5));
        voucher.setStatus(VoucherStatus.REGISTERED);

        user = new User();
        user.setUsername(userId);
    }

    @Test
    @DisplayName("orderVoucher() → successful order")
    void orderVoucherSuccess() {
        given(voucherRepository.findById(voucherUuid))
                .willReturn(Optional.of(voucher));
        given(userRepository.findUserByUsername(userId))
                .willReturn(Optional.of(user));

        ArgumentCaptor<VoucherOrder> captor = ArgumentCaptor.forClass(VoucherOrder.class);
        VoucherOrder saved = new VoucherOrder();
        UUID orderId = UUID.randomUUID();
        saved.setId(orderId);
        saved.setUser(user);
        saved.setVoucher(voucher);
        saved.setOrderDate(LocalDateTime.now());
        saved.setOrderStatus(OrderStatus.CREATED);
        saved.setTotalPrice(voucher.getPrice());
        given(voucherOrderRepository.save(any(VoucherOrder.class)))
                .willReturn(saved);

        VoucherOrder result = service.orderVoucher(voucherId, userId);

        assertThat(result).isSameAs(saved);
        then(voucherRepository).should().findById(voucherUuid);
        then(userRepository).should().findUserByUsername(userId);
        then(voucherOrderRepository).should().save(captor.capture());

        VoucherOrder toSave = captor.getValue();
        assertThat(toSave.getVoucher()).isSameAs(voucher);
        assertThat(toSave.getUser()).isSameAs(user);
        assertThat(toSave.getTotalPrice()).isEqualTo(150.0);
        assertThat(toSave.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(toSave.getOrderDate()).isNotNull();
    }

    @Test
    @DisplayName("orderVoucher() → throws if voucher not found")
    void orderVoucherVoucherNotFound() {
        given(voucherRepository.findById(voucherUuid))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.orderVoucher(voucherId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Voucher not found with id: " + voucherId);

        then(voucherRepository).should().findById(voucherUuid);
        verifyNoMoreInteractions(userRepository, voucherOrderRepository);
    }

    @Test
    @DisplayName("orderVoucher() → throws if registration period ended")
    void orderVoucherExpired() {
        // arrivalDate in past → unavailable
        voucher.setArrivalDate(LocalDate.now());
        given(voucherRepository.findById(voucherUuid))
                .willReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.orderVoucher(voucherId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Registration period for this voucher has ended");

        then(voucherRepository).should().findById(voucherUuid);
        verifyNoMoreInteractions(userRepository, voucherOrderRepository);
    }

    @Test
    @DisplayName("orderVoucher() → throws if user not found")
    void orderVoucherUserNotFound() {
        given(voucherRepository.findById(voucherUuid))
                .willReturn(Optional.of(voucher));
        given(userRepository.findUserByUsername(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.orderVoucher(voucherId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + userId);

        then(voucherRepository).should().findById(voucherUuid);
        then(userRepository).should().findUserByUsername(userId);
        verifyNoMoreInteractions(voucherOrderRepository);
    }
}