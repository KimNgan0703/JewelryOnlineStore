package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    // Tìm theo slug (trang chi tiết - C04)
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    // Kiểm tra SKU trùng
    boolean existsBySkuAndIdNot(String sku, Long id);

    // Kiểm tra slug trùng
    boolean existsBySlugAndIdNot(String slug, Long id);

    // Sản phẩm bán chạy cho trang chủ (C02)
    List<Product> findTop8ByIsActiveTrueAndIsBestSellerTrueOrderByCreatedAtDesc();

    // Sản phẩm mới cho trang chủ (C02)
    List<Product> findTop8ByIsActiveTrueAndIsNewTrueOrderByCreatedAtDesc();

    // Sản phẩm cùng danh mục (gợi ý liên quan - C04)
    @Query("""
        SELECT p FROM Product p
        WHERE p.category.id = :categoryId
          AND p.id <> :excludeId
          AND p.isActive = true
        ORDER BY p.createdAt DESC
    """)
    List<Product> findRelatedProducts(
            @Param("categoryId") Long categoryId,
            @Param("excludeId")  Long excludeId,
            Pageable pageable
    );

    // Full-text search (C03)
    @Query(value = """
        SELECT * FROM products
        WHERE MATCH(name, short_description, description) AGAINST (:keyword IN BOOLEAN MODE)
          AND is_active = 1
    """, nativeQuery = true)
    List<Product> fullTextSearch(@Param("keyword") String keyword, Pageable pageable);

    // Lọc sản phẩm đầy đủ (C03) — keyword, category, brand, material, price, inStock
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.variants v
        LEFT JOIN p.material m
        WHERE p.isActive = true
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:materialId IS NULL OR m.id = :materialId)
          AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
          AND (:inStockOnly = false OR EXISTS (
                SELECT v2 FROM ProductVariant v2
                WHERE v2.product = p
                  AND v2.isActive = true
                  AND v2.stockQuantity > 0
              ))
    """)
    Page<Product> filterProducts(
            @Param("keyword")     String keyword,
            @Param("categoryId")  Long categoryId,
            @Param("brandId")     Long brandId,
            @Param("materialId")  Long materialId,
            @Param("minPrice")    BigDecimal minPrice,
            @Param("maxPrice")    BigDecimal maxPrice,
            @Param("inStockOnly") boolean inStockOnly,
            Pageable pageable
    );

    // Top bán chạy cho dashboard (A02)
    @Query("""
        SELECT p, SUM(oi.quantity) as sold
        FROM Product p
        JOIN p.variants v
        JOIN OrderItem oi ON oi.variant.id = v.id
        JOIN oi.order o
        WHERE o.orderStatus = 'delivered'
        GROUP BY p.id
        ORDER BY sold DESC
    """)
    List<Object[]> findTopSellingProducts(Pageable pageable);
    // Admin: Lọc và tìm kiếm sản phẩm
    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:isActive IS NULL OR p.isActive = :isActive)
        ORDER BY p.createdAt DESC
    """)
    Page<Product> adminSearchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}