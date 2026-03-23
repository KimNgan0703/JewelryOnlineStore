package com.jewelryonlinestore.repository;
import com.jewelryonlinestore.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Tất cả variant của sản phẩm (chi tiết sản phẩm - C04)
    List<ProductVariant> findByProductIdAndIsActiveTrueOrderBySizeAsc(Long productId);

    // Tìm variant theo product + size (kiểm tra trùng)
    Optional<ProductVariant> findByProductIdAndSize(Long productId, String size);

    // Kiểm tra còn hàng (cho badge "Hết hàng")
    @Query("SELECT COUNT(v) > 0 FROM ProductVariant v WHERE v.product.id = :productId AND v.stockQuantity > 0 AND v.isActive = true")
    boolean hasStock(@Param("productId") Long productId);

    // Danh sách variant sắp hết hàng (A04 - cảnh báo)
    @Query("""
        SELECT v FROM ProductVariant v
        WHERE v.stockQuantity <= v.lowStockThreshold
          AND v.stockQuantity > 0
          AND v.isActive = true
        ORDER BY v.stockQuantity ASC
    """)
    List<ProductVariant> findLowStockVariants();

    // Cập nhật tồn kho (sau khi đặt hàng / nhập kho)
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :delta WHERE v.id = :id")
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    // Kiểm tra đủ hàng trước khi đặt (C06)
    @Query("SELECT v.stockQuantity >= :quantity FROM ProductVariant v WHERE v.id = :id")
    boolean hasSufficientStock(@Param("id") Long id, @Param("quantity") int quantity);
}

