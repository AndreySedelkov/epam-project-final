package com.epam.finaltask.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "voucher_translations")
@Data
public class VoucherTranslation {

    @Id
    private String id;

    @Column(nullable = false, length = 5)
    private String locale;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1024)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;
}