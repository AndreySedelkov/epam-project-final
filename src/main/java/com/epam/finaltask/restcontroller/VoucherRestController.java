package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.ApiResponse;
import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.TourType;
import com.epam.finaltask.model.TransferType;
import com.epam.finaltask.model.HotelType;
import com.epam.finaltask.paged.PagedResponse;
import com.epam.finaltask.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherRestController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<VoucherDTO>>> getAllVouchers(
            @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String locale) {

        Page<VoucherDTO> page = voucherService.findAll(pageable, locale);

        PagedResponse<VoucherDTO> paged = new PagedResponse<>(page);

        ApiResponse<PagedResponse<VoucherDTO>> response =
                new ApiResponse<>(
                        "OK",
                        "Vouchers retrieved successfully",
                        paged
                );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getVouchersByUserId(
            @PathVariable("userId") String userId) {
        List<VoucherDTO> vouchers = voucherService.findAllByUserId(userId);
        ApiResponse<List<VoucherDTO>> response =
                new ApiResponse<>("OK", "Vouchers retrieved successfully", vouchers);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherDTO>> createVoucher(
            @RequestBody VoucherDTO voucherDTO) {
        VoucherDTO created = voucherService.create(voucherDTO);
        ApiResponse<VoucherDTO> response =
                new ApiResponse<>("OK", "Voucher is successfully created", created);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherDTO>> updateVoucher(
            @PathVariable("id") String id,
            @RequestBody VoucherDTO voucherDTO) {
        VoucherDTO updated = voucherService.update(id, voucherDTO);
        ApiResponse<VoucherDTO> response =
                new ApiResponse<>("OK", "Voucher is successfully updated", updated);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(
            @PathVariable("id") String id) {
        voucherService.delete(id);
        ApiResponse<Void> response =
                new ApiResponse<>("OK", String.format("Voucher with Id %s has been deleted", id), null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VoucherDTO>> changeVoucherHotStatus(
            @PathVariable("id") String id,
            @RequestBody Map<String, Boolean> payload,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String locale) {

        Boolean hot = payload.get("hot");
        if (hot == null) {
            ApiResponse<VoucherDTO> bad =
                    new ApiResponse<>("ERROR", "Field 'hot' is required", null);
            return ResponseEntity.badRequest().body(bad);
        }

        VoucherDTO updated = voucherService.changeHotStatus(id, hot, locale);
        ApiResponse<VoucherDTO> response =
                new ApiResponse<>("OK", "Voucher HOT status succesfully changed", updated);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/order")
    public ResponseEntity<ApiResponse<VoucherDTO>> orderVoucher(
            @PathVariable("id") String id,
            @RequestParam String userId) {

        VoucherDTO ordered = voucherService.order(id, userId);
        ApiResponse<VoucherDTO> response =
                new ApiResponse<>("OK", "Voucher ordered successfully", ordered);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tour-type")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getByTourType(
            @RequestParam TourType tourType) {

        List<VoucherDTO> vouchers = voucherService.findAllByTourType(tourType);
        ApiResponse<List<VoucherDTO>> response =
                new ApiResponse<>("OK", "Filtered by tour type", vouchers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transfer-type")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getByTransferType(
            @RequestParam TransferType transferType) {

        List<VoucherDTO> vouchers =
                voucherService.findAllByTransferType(transferType.name());
        ApiResponse<List<VoucherDTO>> response =
                new ApiResponse<>("OK", "Filtered by transfer type", vouchers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hotel-type")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getByHotelType(
            @RequestParam HotelType hotelType) {

        List<VoucherDTO> vouchers = voucherService.findAllByHotelType(hotelType);
        ApiResponse<List<VoucherDTO>> response =
                new ApiResponse<>("OK", "Filtered by hotel type", vouchers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/price")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getByPrice(
            @RequestParam Double price) {

        List<VoucherDTO> vouchers = voucherService.findAllByPrice(price);
        ApiResponse<List<VoucherDTO>> response =
                new ApiResponse<>("OK", "Filtered by price", vouchers);
        return ResponseEntity.ok(response);
    }
}