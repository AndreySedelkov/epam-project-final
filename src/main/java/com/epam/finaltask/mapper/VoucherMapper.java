package com.epam.finaltask.mapper;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.*;
import com.epam.finaltask.model.VoucherTranslation;
import org.mapstruct.Mapper;
import org.mapstruct.Context;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    VoucherDTO toVoucherDTO(Voucher voucher);

    default Voucher toVoucher(VoucherDTO dto) {
        Voucher v = new Voucher();

        if (dto.getId() != null && !dto.getId().isBlank()) {
            v.setId(UUID.fromString(dto.getId()));
        }

        v.setTitle(dto.getTitle());
        v.setDescription(dto.getDescription());
        v.setPrice(dto.getPrice());
        v.setTourType(dto.getTourType() != null
                ? TourType.valueOf(dto.getTourType()) : null);
        v.setTransferType(dto.getTransferType() != null
                ? TransferType.valueOf(dto.getTransferType()) : null);
        v.setHotelType(dto.getHotelType() != null
                ? HotelType.valueOf(dto.getHotelType()) : null);
        v.setStatus(dto.getStatus() != null
                ? VoucherStatus.valueOf(dto.getStatus()) : VoucherStatus.REGISTERED);
        v.setArrivalDate(dto.getArrivalDate());
        v.setEvictionDate(dto.getEvictionDate());
        v.setHot(dto.isHot());
        v.setImageUrl(dto.getImageUrl());

        return v;
    }

    default VoucherDTO toVoucherDTO(Voucher voucher, @Context String locale) {
        VoucherDTO dto = toVoucherDTO(voucher);

        if (voucher.getTranslations() != null && !voucher.getTranslations().isEmpty()) {
            Optional<VoucherTranslation> trOpt = voucher.getTranslations().stream()
                    .filter(t -> t.getLocale().equalsIgnoreCase(locale))
                    .findFirst();
            if (trOpt.isEmpty()) {
                trOpt = voucher.getTranslations().stream()
                        .filter(t -> t.getLocale().equalsIgnoreCase("en"))
                        .findFirst();
            }
            trOpt.ifPresent(tr -> {
                dto.setTitle(tr.getTitle());
                dto.setDescription(tr.getDescription());
            });
        }

        if (dto.getTitle() == null) {
            dto.setTitle(voucher.getTitle());
        }
        if (dto.getDescription() == null) {
            dto.setDescription(voucher.getDescription());
        }

        boolean avail = voucher.isAvailableForPurchase();
        dto.setAvailable(avail);
        dto.setAvailableForPurchase(avail);

        return dto;
    }
}
