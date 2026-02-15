package com.epam.finaltask.specification;

import com.epam.finaltask.dto.VoucherFilterRequest;
import com.epam.finaltask.model.Voucher;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class VoucherSpecification {

    public static Specification<Voucher> byFilter(VoucherFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (f.getTourType() != null) {
                preds.add(cb.equal(root.get("tourType"), f.getTourType()));
            }
            if (f.getHotelType() != null) {
                preds.add(cb.equal(root.get("hotelType"), f.getHotelType()));
            }
            if (f.getTransferType() != null) {
                preds.add(cb.equal(root.get("transferType"), f.getTransferType()));
            }
            if (f.getMinPrice() != null) {
                preds.add(cb.ge(root.get("price"), f.getMinPrice()));
            }
            if (f.getMaxPrice() != null) {
                preds.add(cb.le(root.get("price"), f.getMaxPrice()));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}