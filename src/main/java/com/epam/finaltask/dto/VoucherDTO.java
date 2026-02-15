package com.epam.finaltask.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VoucherDTO {

    private String id;

    private String title;

    private String description;

    private Double price;

    private String tourType;

    private String transferType;

    private String hotelType;

    private String status;

    private boolean available;

    private LocalDate arrivalDate;

    private LocalDate evictionDate;

    private boolean availableForPurchase;

    private String userId;

    private boolean isHot;

    private String imageUrl;

    private String titleRu;
    private String descriptionRu;
    private String titleUz;
    private String descriptionUz;
    private String titleKa;
    private String descriptionKa;
}
