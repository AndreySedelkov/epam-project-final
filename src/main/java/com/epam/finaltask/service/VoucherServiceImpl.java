package com.epam.finaltask.service;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.dto.VoucherFilterRequest;
import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.mapper.VoucherMapper;
import com.epam.finaltask.model.*;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.repository.VoucherTranslationRepository;
import com.epam.finaltask.specification.VoucherSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private static final Logger logger = LoggerFactory.getLogger(VoucherServiceImpl.class);

    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;
    private final VoucherTranslationRepository voucherTranslationRepository;
    private final UserRepository userRepository;
    private final GoogleTranslateService googleTranslateService;

    public VoucherDTO findById(String id) {
        Voucher voucher = voucherRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Voucher not found with id: " + id));
        String locale = LocaleContextHolder.getLocale().getLanguage();
        VoucherDTO dto = voucherMapper.toVoucherDTO(voucher, locale);
        hydrateTranslations(dto, voucher);
        return dto;
    }

    @Override
    @Transactional
    public VoucherDTO create(VoucherDTO dto) {
        Voucher voucher = voucherMapper.toVoucher(dto);

        if (dto.getUserId() != null && !dto.getUserId().isBlank()) {
            User user = userRepository.findUserByUsername(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found: " + dto.getUserId()));
            voucher.setUser(user);
        }

        voucher = voucherRepository.save(voucher);
        String locale = LocaleContextHolder.getLocale().getLanguage();
        upsertTranslation(voucher, locale, dto.getTitle(), dto.getDescription());

        String titleEn = "en".equalsIgnoreCase(locale)
                ? dto.getTitle()
                : resolveTranslation(null, dto.getTitle(), "en", null);
        String descEn = "en".equalsIgnoreCase(locale)
                ? dto.getDescription()
                : resolveTranslation(null, dto.getDescription(), "en", null);
        upsertTranslation(voucher, "en", titleEn, descEn);

        upsertTranslation(voucher, "ru",
                locale.equalsIgnoreCase("ru") ? dto.getTitle() : resolveTranslation(dto.getTitleRu(), dto.getTitle(), "ru", null),
                locale.equalsIgnoreCase("ru") ? dto.getDescription() : resolveTranslation(dto.getDescriptionRu(), dto.getDescription(), "ru", null));
        upsertTranslation(voucher, "uz",
                locale.equalsIgnoreCase("uz") ? dto.getTitle() : resolveTranslation(dto.getTitleUz(), dto.getTitle(), "uz", null),
                locale.equalsIgnoreCase("uz") ? dto.getDescription() : resolveTranslation(dto.getDescriptionUz(), dto.getDescription(), "uz", null));
        upsertTranslation(voucher, "ka",
                locale.equalsIgnoreCase("ka") ? dto.getTitle() : resolveTranslation(dto.getTitleKa(), dto.getTitle(), "ka", null),
                locale.equalsIgnoreCase("ka") ? dto.getDescription() : resolveTranslation(dto.getDescriptionKa(), dto.getDescription(), "ka", null));

        return voucherMapper.toVoucherDTO(voucher);
    }


    @Override
    public VoucherDTO order(String id, String userId) {
        Voucher voucher = voucherRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Voucher not found with id: " + id));
        voucher.setStatus(com.epam.finaltask.model.VoucherStatus.PAID);
        Voucher updated = voucherRepository.save(voucher);
        return voucherMapper.toVoucherDTO(updated);
    }

    @Override
    @Transactional
    public VoucherDTO update(String id, VoucherDTO dto) {
        log.info("Starting update for voucher ID={}, DTO={}", id, dto);

        Voucher existing = voucherRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.error("Voucher not found with id={}", id);
                    return new RuntimeException("Voucher not found with id: " + id);
                });

        if (dto.getTourType() != null) {
            existing.setTourType(TourType.valueOf(dto.getTourType()));
        } else {
            log.warn("TourType is null in update");
        }

        if (dto.getTransferType() != null) {
            existing.setTransferType(TransferType.valueOf(dto.getTransferType()));
        } else {
            log.warn("TransferType is null in update");
        }

        if (dto.getHotelType() != null) {
            existing.setHotelType(HotelType.valueOf(dto.getHotelType()));
        } else {
            log.warn("HotelType is null in update");
        }

        if (dto.getStatus() != null) {
            existing.setStatus(VoucherStatus.valueOf(dto.getStatus()));
        } else {
            log.warn("Status is null in update");
        }

        existing.setPrice(dto.getPrice());
        existing.setArrivalDate(dto.getArrivalDate());
        existing.setEvictionDate(dto.getEvictionDate());
        existing.setHot(dto.isHot());
        if (dto.getImageUrl() != null) {
            existing.setImageUrl(dto.getImageUrl());
        }

        String locale = LocaleContextHolder.getLocale().getLanguage();

        VoucherTranslation translation = existing.getTranslations().stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(locale))
                .findFirst()
                .orElse(null);

        if (translation == null) {
            translation = new VoucherTranslation();
            translation.setId(UUID.randomUUID().toString());
            translation.setLocale(locale);
            translation.setVoucher(existing);
            existing.getTranslations().add(translation);
        }

        translation.setTitle(dto.getTitle());
        translation.setDescription(dto.getDescription());

        Voucher saved = voucherRepository.save(existing);

        upsertTranslation(saved, "ru",
                resolveTranslation(dto.getTitleRu(), dto.getTitle(), "ru", getExistingTitle(saved, "ru")),
                resolveTranslation(dto.getDescriptionRu(), dto.getDescription(), "ru", getExistingDescription(saved, "ru")));
        upsertTranslation(saved, "uz",
                resolveTranslation(dto.getTitleUz(), dto.getTitle(), "uz", getExistingTitle(saved, "uz")),
                resolveTranslation(dto.getDescriptionUz(), dto.getDescription(), "uz", getExistingDescription(saved, "uz")));
        upsertTranslation(saved, "ka",
                resolveTranslation(dto.getTitleKa(), dto.getTitle(), "ka", getExistingTitle(saved, "ka")),
                resolveTranslation(dto.getDescriptionKa(), dto.getDescription(), "ka", getExistingDescription(saved, "ka")));

        VoucherDTO result = voucherMapper.toVoucherDTO(saved, locale);

        log.info("Updated voucher DTO: {}", result);

        return result;
    }


    @Override
    public void delete(String voucherId) {
        voucherRepository.deleteById(UUID.fromString(voucherId));
    }

    @Override
    @Transactional
    public VoucherDTO changeHotStatus(String id, boolean hot, String locale) {
        Voucher v = voucherRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));
        v.setHot(hot);
        voucherRepository.save(v);
        return voucherMapper.toVoucherDTO(v, locale);
    }

    @Override
    @Transactional
    public VoucherDTO changeStatus(String id, VoucherStatus newStatus, String locale) {
        Voucher v = voucherRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));
        v.setStatus(newStatus);
        voucherRepository.save(v);
        return voucherMapper.toVoucherDTO(v, locale);
    }


    @Override
    public List<VoucherDTO> findAllByUserId(String userId) {
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> v.getUser().getUsername().equals(userId))
                .collect(Collectors.toList());
        return vouchers.stream()
                .map(voucherMapper::toVoucherDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherDTO> findAllByTourType(TourType tourType) {
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> v.getTourType() == tourType)
                .collect(Collectors.toList());
        return vouchers.stream()
                .map(voucherMapper::toVoucherDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherDTO> findAllByTransferType(String transferType) {
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> v.getTransferType().name().equalsIgnoreCase(transferType))
                .collect(Collectors.toList());
        return vouchers.stream()
                .map(voucherMapper::toVoucherDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherDTO> findAllByHotelType(HotelType hotelType) {
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> v.getHotelType() == hotelType)
                .collect(Collectors.toList());
        return vouchers.stream()
                .map(voucherMapper::toVoucherDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherDTO> findAllByPrice(Double price) {
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> v.getPrice() <= price)
                .collect(Collectors.toList());
        return vouchers.stream()
                .map(voucherMapper::toVoucherDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherDTO> findAll(String locale) {
        Page<VoucherDTO> page = findAll(Pageable.unpaged(), locale);
        return page.getContent();
    }

    @Override
    public Page<VoucherDTO> findAll(Pageable pageable, String locale) {
        Sort defaultSort = Sort.by(
                Sort.Order.desc("isHot"),
                Sort.Order.asc("arrivalDate")
        );

        if (pageable.isUnpaged()) {
            pageable = PageRequest.of(
                    0,
                    Integer.MAX_VALUE,
                    defaultSort
            );
        } else if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    defaultSort
            );
        }

        Page<Voucher> page = voucherRepository.findAll(pageable);
        return page.map(v -> {
            VoucherDTO dto = voucherMapper.toVoucherDTO(v, locale);
            boolean avail = v.isAvailableForPurchase();
            dto.setAvailable(avail);
            dto.setAvailableForPurchase(avail);
            hydrateTranslations(dto, v);
            return dto;
        });
    }


    @Override
    public List<VoucherDTO> findAllByFilter(VoucherFilterRequest filter, String locale) {
        Specification<Voucher> spec = VoucherSpecification.byFilter(filter);

        Sort sort = Sort.by("isHot").descending()
                .and(Sort.by("arrivalDate").ascending());

        return voucherRepository.findAll(spec, sort)
                .stream()
                .map(v -> toVoucherDTO(v, locale))
                .sorted(voucherCatalogComparator())
                .collect(Collectors.toList());
    }

    @Override
    public Page<VoucherDTO> findAllByFilter(VoucherFilterRequest filter,
                                            Pageable pageable,
                                            String locale) {
        Specification<Voucher> spec = VoucherSpecification.byFilter(filter);
        Sort defaultSort = Sort.by("isHot").descending()
                .and(Sort.by("arrivalDate").ascending());

        if (pageable.getSort().isSorted()) {
            Page<Voucher> page = voucherRepository.findAll(spec, pageable);
            return page.map(v -> toVoucherDTO(v, locale));
        }

        List<VoucherDTO> ordered = voucherRepository.findAll(spec, defaultSort)
                .stream()
                .map(v -> toVoucherDTO(v, locale))
                .sorted(voucherCatalogComparator())
                .collect(Collectors.toList());

        if (pageable.isUnpaged()) {
            return new PageImpl<>(ordered, pageable, ordered.size());
        }

        int start = (int) pageable.getOffset();
        if (start >= ordered.size()) {
            return new PageImpl<>(List.of(), pageable, ordered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), ordered.size());
        return new PageImpl<>(ordered.subList(start, end), pageable, ordered.size());
    }

    private VoucherDTO toVoucherDTO(Voucher voucher, String locale) {
        VoucherDTO dto = voucherMapper.toVoucherDTO(voucher, locale);
        boolean available = voucher.isAvailableForPurchase();
        dto.setAvailable(available);
        dto.setAvailableForPurchase(available);
        hydrateTranslations(dto, voucher);
        return dto;
    }

    private Comparator<VoucherDTO> voucherCatalogComparator() {
        return Comparator.comparing(VoucherDTO::isAvailable, Comparator.reverseOrder())
                .thenComparing(VoucherDTO::isHot, Comparator.reverseOrder())
                .thenComparing(VoucherDTO::getArrivalDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private void upsertTranslation(Voucher voucher, String locale, String title, String description) {
        if ((title == null || title.isBlank()) && (description == null || description.isBlank())) {
            return;
        }
        VoucherTranslation translation = voucherTranslationRepository
                .findByVoucherIdAndLocale(voucher.getId(), locale)
                .orElseGet(() -> {
                    VoucherTranslation tr = new VoucherTranslation();
                    tr.setId(UUID.randomUUID().toString());
                    tr.setVoucher(voucher);
                    tr.setLocale(locale);
                    return tr;
                });
        if (title != null && !title.isBlank()) {
            translation.setTitle(title);
        }
        if (description != null && !description.isBlank()) {
            translation.setDescription(description);
        }
        voucherTranslationRepository.save(translation);
    }

    private String resolveTranslation(String manual, String source, String targetLocale, String existing) {
        if (manual != null && !manual.isBlank()) {
            return manual;
        }
        if (existing != null && !existing.isBlank()) {
            return null;
        }
        return googleTranslateService.translate(source, targetLocale);
    }

    private void hydrateTranslations(VoucherDTO dto, Voucher voucher) {
        if (voucher.getTranslations() == null) {
            return;
        }
        voucher.getTranslations().forEach(tr -> {
            switch (tr.getLocale().toLowerCase()) {
                case "ru" -> {
                    dto.setTitleRu(tr.getTitle());
                    dto.setDescriptionRu(tr.getDescription());
                }
                case "uz" -> {
                    dto.setTitleUz(tr.getTitle());
                    dto.setDescriptionUz(tr.getDescription());
                }
                case "ka" -> {
                    dto.setTitleKa(tr.getTitle());
                    dto.setDescriptionKa(tr.getDescription());
                }
                default -> {
                }
            }
        });
    }

    private String getExistingTitle(Voucher voucher, String locale) {
        return voucherTranslationRepository.findByVoucherIdAndLocale(voucher.getId(), locale)
                .map(VoucherTranslation::getTitle)
                .orElse(null);
    }

    private String getExistingDescription(Voucher voucher, String locale) {
        return voucherTranslationRepository.findByVoucherIdAndLocale(voucher.getId(), locale)
                .map(VoucherTranslation::getDescription)
                .orElse(null);
    }
}


