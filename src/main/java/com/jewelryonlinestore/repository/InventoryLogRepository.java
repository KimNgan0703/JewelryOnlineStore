package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    // Lịch sử nhập/xuất của 1 variant (A04)
    Page<InventoryLog> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);

    // Lịch sử do 1 admin thực hiện
    Page<InventoryLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Tổng nhập kho trong khoảng thời gian
    @Query("""
        SELECT COALESCE(SUM(il.quantityChange), 0) FROM InventoryLog il
        WHERE il.variant.id = :variantId
          AND il.quantityChange > 0
          AND il.createdAt BETWEEN :from AND :to
    """)
    Integer sumImportedBetween(
            @Param("variantId") Long variantId,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to
    );
}

