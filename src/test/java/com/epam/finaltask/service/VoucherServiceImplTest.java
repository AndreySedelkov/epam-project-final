package com.epam.finaltask.service;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.dto.VoucherFilterRequest;
import com.epam.finaltask.exception.ResourceNotFoundException;
import com.epam.finaltask.mapper.VoucherMapper;
import com.epam.finaltask.model.*;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.repository.VoucherTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private VoucherTranslationRepository voucherTranslationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private GoogleTranslateService googleTranslateService;

    @InjectMocks
    private VoucherServiceImpl service;

    private UUID id;
    private String idStr;
    private VoucherDTO dto;
    private Voucher voucher;
    private User user;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        idStr = id.toString();
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        lenient().when(googleTranslateService.translate(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dto = new VoucherDTO();
        dto.setId(idStr);
        dto.setUserId("bobRoss");
        dto.setTitle("Trip");
        dto.setDescription("Desc");
        dto.setPrice(100.0);
        dto.setTourType("ADVENTURE");
        dto.setTransferType("BUS");
        dto.setHotelType("FIVE_STARS");
        dto.setStatus("REGISTERED");
        dto.setArrivalDate(LocalDate.of(2025, 1, 1));
        dto.setEvictionDate(LocalDate.of(2025, 1, 5));
        dto.setHot(false);

        voucher = new Voucher();
        voucher.setId(id);
        voucher.setPrice(100.0);
        voucher.setTourType(TourType.ADVENTURE);
        voucher.setTransferType(TransferType.BUS);
        voucher.setHotelType(HotelType.FIVE_STARS);
        voucher.setStatus(VoucherStatus.REGISTERED);
        voucher.setArrivalDate(dto.getArrivalDate());
        voucher.setEvictionDate(dto.getEvictionDate());
        voucher.setHot(false);
        voucher.setTranslations(new ArrayList<>());

        user = new User();
        user.setUsername("bobRoss");
    }

    @Test
    @DisplayName("findAll(String locale) → uses unpaged(Pageable) & default sort")
    void findAll_DefaultUnpaged_Success() {
        Sort defaultSort = Sort.by(
                Sort.Order.desc("isHot"),
                Sort.Order.asc("arrivalDate")
        );
        Pageable unpaged = PageRequest.of(0, Integer.MAX_VALUE, defaultSort);

        Page<Voucher> pageEnt = new PageImpl<>(List.of(voucher), unpaged, 1);
        given(voucherRepository.findAll(unpaged)).willReturn(pageEnt);
        given(voucherMapper.toVoucherDTO(voucher, "en")).willReturn(dto);

        List<VoucherDTO> result = service.findAll("en");

        assertThat(result)
                .hasSize(1)
                .containsExactly(dto);

        then(voucherRepository).should().findAll(unpaged);
        then(voucherMapper).should().toVoucherDTO(voucher, "en");
    }


    @Test
    @DisplayName("findById() → returns DTO when found")
    void findById_Success() {
        given(voucherRepository.findById(id)).willReturn(Optional.of(voucher));
        given(voucherMapper.toVoucherDTO(voucher, "en")).willReturn(dto);

        VoucherDTO result = service.findById(idStr);

        assertThat(result).isSameAs(dto);
        then(voucherRepository).should().findById(id);
    }

    @Test
    @DisplayName("findById() → throws when missing")
    void findById_NotFound() {
        given(voucherRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(idStr))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Voucher not found with id: " + idStr);

        then(voucherRepository).should().findById(id);
    }

    @Test
    @DisplayName("create() → success with user and translation")
    void create_SuccessWithUser() {
        given(voucherMapper.toVoucher(dto)).willReturn(voucher);
        given(userRepository.findUserByUsername("bobRoss"))
                .willReturn(Optional.of(user));
        given(voucherRepository.save(voucher)).willReturn(voucher);
        given(voucherMapper.toVoucherDTO(voucher)).willReturn(dto);

        VoucherDTO result = service.create(dto);

        assertThat(result).isSameAs(dto);
        then(voucherTranslationRepository).should(atLeastOnce())
                .save(argThat(tr ->
                        tr.getLocale().equalsIgnoreCase("en")
                                && tr.getTitle().equals("Trip")
                                && tr.getDescription().equals("Desc")
                ));
    }


    @Test
    @DisplayName("create() → throws when user not found")
    void create_UserNotFound() {
        given(voucherMapper.toVoucher(dto)).willReturn(voucher);
        given(userRepository.findUserByUsername("bobRoss"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: bobRoss");

        then(userRepository).should().findUserByUsername("bobRoss");
        verifyNoMoreInteractions(voucherRepository, voucherTranslationRepository);
    }

    @Test
    @DisplayName("order() → sets status to PAID")
    void order_SetsPaid() {
        given(voucherRepository.findById(id)).willReturn(Optional.of(voucher));
        given(voucherRepository.save(voucher)).willReturn(voucher);
        given(voucherMapper.toVoucherDTO(voucher)).willReturn(dto);

        VoucherDTO result = service.order(idStr, "ignoredUser");

        assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.PAID);
        assertThat(result).isSameAs(dto);
    }

    @Test
    @DisplayName("order() → throws when missing")
    void order_NotFound() {
        given(voucherRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.order(idStr, "u"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Voucher not found with id: " + idStr);
    }

    @Test
    @DisplayName("update() → updates fields and translations")
    void update_Success() {
        given(voucherRepository.findById(id))
                .willReturn(Optional.of(voucher));
        VoucherDTO input = dto;
        given(voucherRepository.save(voucher)).willReturn(voucher);
        given(voucherMapper.toVoucherDTO(voucher, "en")).willReturn(dto);

        VoucherDTO result = service.update(idStr, input);

        assertThat(result).isSameAs(dto);
        assertThat(voucher.getHotelType()).isEqualTo(HotelType.FIVE_STARS);
        then(voucherRepository).should().save(voucher);
    }


    @Test
    @DisplayName("delete() → invokes repository")
    void delete_Invokes() {
        service.delete(idStr);
        then(voucherRepository).should().deleteById(id);
    }

    @Test
    @DisplayName("changeHotStatus() → toggles hot and returns DTO")
    void changeHotStatus_Success() {
        given(voucherRepository.findById(id)).willReturn(Optional.of(voucher));
        given(voucherRepository.save(voucher)).willReturn(voucher);
        given(voucherMapper.toVoucherDTO(voucher, "en")).willReturn(dto);

        VoucherDTO result = service.changeHotStatus(idStr, true, "en");

        assertThat(voucher.isHot()).isTrue();
        assertThat(result).isSameAs(dto);
    }

    @Test
    @DisplayName("changeHotStatus() → throws when missing")
    void changeHotStatus_NotFound() {
        given(voucherRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeHotStatus(idStr, true, "en"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Voucher not found: " + idStr);
    }

    @Test
    @DisplayName("findAllByUserId() → filters by user")
    void findAllByUserId() {
        Voucher v1 = new Voucher();
        v1.setUser(user);

        Voucher v2 = new Voucher();
        User other = new User();
        other.setUsername("someoneElse");
        v2.setUser(other);

        given(voucherRepository.findAll()).willReturn(List.of(v1, v2));
        VoucherDTO dto1 = new VoucherDTO();
        given(voucherMapper.toVoucherDTO(v1)).willReturn(dto1);

        List<VoucherDTO> list = service.findAllByUserId("bobRoss");

        assertThat(list).containsExactly(dto1);
    }


    @Test
    @DisplayName("findAllByPrice() → filters by price")
    void findAllByPrice() {
        Voucher cheap = new Voucher();
        cheap.setPrice(50.0);
        Voucher expensive = new Voucher();
        expensive.setPrice(200.0);
        given(voucherRepository.findAll()).willReturn(Arrays.asList(cheap, expensive));
        VoucherDTO cheapDto = new VoucherDTO();
        given(voucherMapper.toVoucherDTO(cheap)).willReturn(cheapDto);

        List<VoucherDTO> list = service.findAllByPrice(100.0);

        assertThat(list).containsExactly(cheapDto);
    }

    @Test
    @DisplayName("findAllByHotelType() → filters by hotel type FIVE_STARS")
    void findAllByHotelType() {
        Voucher v1 = new Voucher();
        v1.setHotelType(HotelType.FIVE_STARS);
        Voucher v2 = new Voucher();
        v2.setHotelType(HotelType.THREE_STARS);
        given(voucherRepository.findAll()).willReturn(List.of(v1, v2));

        VoucherDTO dto1 = new VoucherDTO();
        given(voucherMapper.toVoucherDTO(v1)).willReturn(dto1);

        List<VoucherDTO> list = service.findAllByHotelType(HotelType.FIVE_STARS);

        assertThat(list).containsExactly(dto1);
    }

    @Test
    @DisplayName("findAllByFilter(pageable) -> available vouchers first, ended registration last")
    void findAllByFilter_Pageable_AvailabilityFirst() {
        Voucher ended = new Voucher();
        ended.setStatus(VoucherStatus.REGISTERED);
        ended.setArrivalDate(LocalDate.now().minusDays(1));
        ended.setHot(false);
        ended.setTranslations(new ArrayList<>());

        Voucher available = new Voucher();
        available.setStatus(VoucherStatus.REGISTERED);
        available.setArrivalDate(LocalDate.now().plusDays(5));
        available.setHot(false);
        available.setTranslations(new ArrayList<>());

        VoucherDTO endedDto = new VoucherDTO();
        endedDto.setTitle("Ended");
        VoucherDTO availableDto = new VoucherDTO();
        availableDto.setTitle("Available");

        given(voucherRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Voucher>>any(), any(Sort.class)))
                .willReturn(List.of(ended, available));
        given(voucherMapper.toVoucherDTO(ended, "en")).willReturn(endedDto);
        given(voucherMapper.toVoucherDTO(available, "en")).willReturn(availableDto);

        Page<VoucherDTO> result = service.findAllByFilter(
                new VoucherFilterRequest(),
                PageRequest.of(0, 10),
                "en"
        );

        assertThat(result.getContent()).containsExactly(availableDto, endedDto);
        assertThat(result.getContent().get(0).isAvailable()).isTrue();
        assertThat(result.getContent().get(1).isAvailable()).isFalse();
    }
}
