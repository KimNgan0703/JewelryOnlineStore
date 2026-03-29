package com.jewelryonlinestore.dto.response;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về thông tin sản phẩm cho View (C03, C04).
 * Dùng cho cả danh sách (list) và chi tiết (detail).
 */
@Data
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;


    // Phân loại
    private String categoryName;
    private Long categoryId;
    private String brandName;
    private String collectionName;
    private String materialName;
    private String gender;

    // Giá
    private BigDecimal basePrice;
    private BigDecimal comparePrice;
    private boolean hasDiscount;
    private Integer discountPercent; // % giảm giá so với compare_price

    // Trạng thái
    private boolean isActive;
    private boolean isNew;
    private boolean isBestSeller;
    private boolean inStock; // true nếu ít nhất 1 variant còn hàng

    // Ảnh
    private String primaryImageUrl;          // ảnh chính (cho card danh sách)
    private List<String> allImageUrls;       // tất cả ảnh (cho trang chi tiết)

    // Đánh giá tổng hợp
    private Double averageRating;
    private Integer reviewCount;


    // Biến thể (chỉ load ở trang chi tiết)
    private List<VariantInfo> variants;

    // SEO
    private String metaTitle;
    private String metaDescription;

    private LocalDateTime createdAt;

    // ---- Inner class ----
    @Data
    @Builder
    public static class VariantInfo {
        private Long id;
        private String sku;
        private String size;
        private BigDecimal price;
        private Integer stockQuantity;
        private boolean isActive;
        private boolean lowStock; // true nếu stockQuantity <= lowStockThreshold
    }
}

