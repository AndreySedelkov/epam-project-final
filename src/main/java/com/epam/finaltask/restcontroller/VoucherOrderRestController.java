package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.ApiResponse;
import com.epam.finaltask.dto.VoucherOrderDTO;
import com.epam.finaltask.model.VoucherOrder;
import com.epam.finaltask.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voucher-orders")
@RequiredArgsConstructor
public class VoucherOrderRestController {

    private final VoucherOrderService voucherOrderService;

    @PutMapping("/{voucherId}/order")
    public ResponseEntity<ApiResponse<VoucherOrderDTO>> orderVoucher(
            @PathVariable("voucherId") String voucherId,
            @RequestParam String userId) {

        VoucherOrder order = voucherOrderService.orderVoucher(voucherId, userId);

        VoucherOrderDTO dto = new VoucherOrderDTO();
        dto.setId(order.getId().toString());
        dto.setUserId(order.getUser().getUsername());
        dto.setVoucherId(order.getVoucher().getId().toString());
        dto.setOrderDate(order.getOrderDate());
        dto.setOrderStatus(order.getOrderStatus().name());
        dto.setTotalPrice(order.getTotalPrice());

        ApiResponse<VoucherOrderDTO> response = new ApiResponse<>(
                "OK",
                "Voucher ordered successfully",
                dto
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
