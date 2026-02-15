package com.epam.finaltask.repository;

import com.epam.finaltask.model.VoucherOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, UUID> {
    List<VoucherOrder> findByUserUsername(String username);

    @Modifying
    void deleteByUserId(UUID userId);

    @Modifying
    void deleteByVoucherUserId(UUID userId);

    @Query("select o from VoucherOrder o join fetch o.user join fetch o.voucher order by o.orderDate desc")
    List<VoucherOrder> findAllWithUserAndVoucher();
}
