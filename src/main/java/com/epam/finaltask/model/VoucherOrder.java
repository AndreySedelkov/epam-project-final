package com.epam.finaltask.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "voucher_orders")
public class VoucherOrder {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Double totalPrice;

    public String getVoucherTitle() {
        return voucher != null ? voucher.getTitle() : "";
    }

    public LocalDate getVoucherArrivalDate() {
        return voucher != null ? voucher.getArrivalDate() : null;
    }

    public LocalDate getVoucherEvictionDate() {
        return voucher != null ? voucher.getEvictionDate() : null;
    }

    public String getUserFullName() {
        if (user != null) {
            String firstName = user.getName() != null ? user.getName().trim() : "";
            String lastName  = user.getSurname() != null ? user.getSurname().trim() : "";
            return (firstName + " " + lastName).trim();
        }
        return "";
    }

    public String getFormattedOrderDate() {
        return orderDate != null ? orderDate.toString() : "";
    }
}