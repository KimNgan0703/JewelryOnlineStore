package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Sử dụng JOIN FETCH để tải trước Product, giải quyết triệt để lỗi N+1 Query
    @Query(value = """
        SELECT pv FROM ProductVariant pv
        JOIN FETCH pv.product p
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:lowStockOnly = false
               OR (pv.stockQuantity <= pv.lowStockThreshold AND pv.stockQuantity > 0))
        """,
            countQuery = """
        SELECT COUNT(pv) FROM ProductVariant pv
        JOIN pv.product p
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:lowStockOnly = false
               OR (pv.stockQuantity <= pv.lowStockThreshold AND pv.stockQuantity > 0))
        """)
    Page<ProductVariant> searchInventory(
            @Param("keyword") String keyword,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable
    );

    // Đếm số lượng thay vì tải toàn bộ danh sách (Giảm tải RAM)
    @Query("""
        SELECT COUNT(pv) FROM ProductVariant pv
        WHERE pv.stockQuantity <= pv.lowStockThreshold
          AND pv.stockQuantity > 0
          AND pv.isActive = true
    """)
    int countLowStockVariants();
    // Thêm lại hàm này để DashboardServiceImpl hoạt động bình thường
    @Query("""
        SELECT pv FROM ProductVariant pv 
        WHERE pv.stockQuantity <= pv.lowStockThreshold 
          AND pv.stockQuantity > 0 
          AND pv.isActive = true 
        ORDER BY pv.stockQuantity ASC
    """)
    List<ProductVariant> findLowStockVariants();
}