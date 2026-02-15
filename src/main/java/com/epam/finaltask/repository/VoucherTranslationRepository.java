package com.epam.finaltask.repository;

import com.epam.finaltask.model.VoucherTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoucherTranslationRepository extends JpaRepository<VoucherTranslation, String> {
    Optional<VoucherTranslation> findByVoucherIdAndLocale(UUID voucherId, String locale);

}
