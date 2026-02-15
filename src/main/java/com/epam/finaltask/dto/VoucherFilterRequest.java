package com.epam.finaltask.dto;

import com.epam.finaltask.model.HotelType;
import com.epam.finaltask.model.TourType;
import com.epam.finaltask.model.TransferType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherFilterRequest {

    private TourType tourType;

    private HotelType hotelType;

    private TransferType transferType;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;
}

