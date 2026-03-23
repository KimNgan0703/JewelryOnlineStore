package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Lấy tất cả địa chỉ chưa xóa của khách (C01 - quản lý địa chỉ)
    List<Address> findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(Long customerId);

    // Lấy địa chỉ mặc định
    Optional<Address> findByCustomerIdAndIsDefaultTrueAndIsDeletedFalse(Long customerId);

    // Đếm số địa chỉ còn hoạt động (kiểm tra trước khi xóa địa chỉ mặc định)
    int countByCustomerIdAndIsDeletedFalse(Long customerId);

    // Bỏ mặc định tất cả địa chỉ của customer (trước khi set mặc định mới)
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customer.id = :customerId")
    int clearDefaultByCustomer(@Param("customerId") Long customerId);

    // Soft delete địa chỉ
    @Modifying
    @Query("UPDATE Address a SET a.isDeleted = true WHERE a.id = :id")
    int softDelete(@Param("id") Long id);

    // Kiểm tra địa chỉ thuộc về customer (bảo mật)
    boolean existsByIdAndCustomerId(Long id, Long customerId);
}
