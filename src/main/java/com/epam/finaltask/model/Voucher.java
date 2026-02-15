package com.epam.finaltask.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@ToString(exclude = {"user", "translations"})
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TourType tourType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HotelType hotelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoucherTranslation> translations;

    @Column(nullable = false)
    private boolean isHot = false;


    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "eviction_date", nullable = false)
    private LocalDate evictionDate;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Transient
    public boolean isAvailableForPurchase() {
        LocalDate today = LocalDate.now();
        LocalDate registrationEnd = arrivalDate.minusDays(1);
        if (status == VoucherStatus.CANCELED) {
            return false;
        }
        return !today.isAfter(registrationEnd);
    }
}
