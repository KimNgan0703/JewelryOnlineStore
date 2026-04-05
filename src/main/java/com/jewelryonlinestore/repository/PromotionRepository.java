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

    Optional<Promotion> findByCode(String code);
    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

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

    boolean existsByCodeAndIdNot(String code, Long id);

    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id")
    int incrementUsedCount(@Param("id") Long id);

    /**
     * Lọc theo keyword + status:
     *   ACTIVE   - isActive=true,  đang trong khoảng startDate–endDate
     *   INACTIVE - isActive=false, đang trong khoảng startDate–endDate
     *   EXPIRED  - endDate đã qua (bất kể isActive)
     *   UPCOMING - startDate chưa đến (bất kể isActive)
     *   null     - không lọc theo trạng thái
     */
    @Query("""
        SELECT p FROM Promotion p
        WHERE
            (:keyword IS NULL
                OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (
                :status IS NULL
                OR (:status = 'ACTIVE'   AND p.isActive = true  AND p.startDate <= :now AND p.endDate >= :now)
                OR (:status = 'INACTIVE' AND p.isActive = false AND p.startDate <= :now AND p.endDate >= :now)
                OR (:status = 'EXPIRED'  AND p.endDate < :now)
                OR (:status = 'UPCOMING' AND p.startDate > :now)
            )
        ORDER BY p.createdAt DESC
    """)
    Page<Promotion> searchPromotions(
            @Param("keyword") String keyword,
            @Param("status")  String status,
            @Param("now")     LocalDateTime now,
            Pageable pageable
    );

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
            "AND p.startDate <= CURRENT_TIMESTAMP " +
            "AND (p.endDate IS NULL OR p.endDate >= CURRENT_TIMESTAMP) " +
            "AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit) " +
            "ORDER BY p.createdAt DESC")
    List<Promotion> findAvailablePromotions();
}