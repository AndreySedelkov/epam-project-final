package com.epam.finaltask.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VoucherOrderDTO {

    private String id;

    private String userId;

    private String voucherId;

    private LocalDateTime orderDate;

    private String orderStatus;

    private Double totalPrice;

    private String voucherTitle;

    private LocalDate voucherArrivalDate;

    private LocalDate voucherEvictionDate;

    private String userFullName;
}