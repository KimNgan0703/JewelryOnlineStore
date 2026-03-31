package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // Tìm theo mã giảm giá (validate coupon - C05)
    Optional<Promotion> findByCode(String code);
    Optional<Promotion> findByCodeAndIsActiveTrue(String code);
    // Kiểm tra mã hợp lệ: đang active, còn hạn, còn lượt dùng
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code = :code
          AND p.isActive = true
          AND p.startDate <= :now
          AND p.endDate   >= :now
          AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
    """)
    Optional<Promotion> findValidPromotion(
            @Param("code") String code,
            @Param("now")  LocalDateTime now
    );

    // Kiểm tra mã trùng (A07)
    boolean existsByCodeAndIdNot(String code, Long id);

    // Tăng used_count sau khi áp dụng thành công
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id")
    int incrementUsedCount(@Param("id") Long id);

    // Admin: lọc khuyến mãi theo trạng thái và thời gian
    @Query("""
        SELECT p FROM Promotion p
        WHERE (:keyword IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%',:keyword,'%'))
                                OR LOWER(p.name) LIKE LOWER(CONCAT('%',:keyword,'%')))
          AND (:isActive IS NULL OR p.isActive = :isActive)
        ORDER BY p.createdAt DESC
    """)
    Page<Promotion> searchPromotions(
            @Param("keyword")  String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
    // Lấy các mã đang Active, đã đến ngày bắt đầu, chưa hết hạn và chưa hết lượt sử dụng
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
            "AND p.startDate <= CURRENT_TIMESTAMP " +
            "AND (p.endDate IS NULL OR p.endDate >= CURRENT_TIMESTAMP) " +
            "AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit) " +
            "ORDER BY p.createdAt DESC")
    List<Promotion> findAvailablePromotions();

}

