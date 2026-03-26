package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Tìm customer theo user_id (dùng sau khi đăng nhập)
    Optional<Customer> findByUserId(Long userId);

    // Tìm theo số điện thoại
    Optional<Customer> findByPhone(String phone);

    // Kiểm tra customer tồn tại theo user
    boolean existsByUserId(Long userId);

    // Tìm kiếm customer (A06 - tìm theo tên, email, phone)
    @Query("""
        SELECT c FROM Customer c
        JOIN c.user u
        WHERE (:keyword IS NULL OR
               LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               c.phone           LIKE CONCAT('%', :keyword, '%'))
        AND (:status IS NULL OR u.status = :status)
    """)
    Page<Customer> searchCustomers(
            @Param("keyword") String keyword,
            @Param("status")  User.Status status, // Đã sửa String thành User.Status
            Pageable pageable
    );

    // Thống kê dashboard: số khách hàng mới trong tháng (A02)
    @Query("""
        SELECT COUNT(c) FROM Customer c
        WHERE MONTH(c.createdAt) = :month AND YEAR(c.createdAt) = :year
    """)
    Long countNewCustomersInMonth(@Param("month") int month, @Param("year") int year);

    boolean existsByPhone(String phone);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}