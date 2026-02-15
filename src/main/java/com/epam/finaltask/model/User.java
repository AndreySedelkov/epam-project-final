package com.epam.finaltask.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Voucher> vouchers = new ArrayList<>();

    @Column(precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean active = true;

    private int failedAttempts;

    private LocalDateTime lockTime;

    @Column(nullable = false)
    boolean locked;

    public void addVoucher(Voucher voucher) {
        vouchers.add(voucher);
        voucher.setUser(this);
    }

    public void removeVoucher(Voucher voucher) {
        vouchers.remove(voucher);
        voucher.setUser(null);
    }

    public boolean isAccountNonLocked() {
        if (lockTime == null) {
            return true;
        }
        if (LocalDateTime.now().isAfter(lockTime)) {
            this.failedAttempts = 0;
            this.lockTime = null;
            return true;
        }
        return false;
    }
}
