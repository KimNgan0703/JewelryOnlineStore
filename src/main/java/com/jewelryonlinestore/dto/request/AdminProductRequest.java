package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DTO cho Admin thêm/sửa sản phẩm (A03).
 */
@Data
public class AdminProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 100)
    private String sku;

    private String shortDescription;
    private String description;

    private Long categoryId;
    private Long brandId;
    private Long collectionId;
    private Long materialId;

    private String gender; // male | female | unisex

    private BigDecimal weightGram;

    @NotNull(message = "Giá cơ sở không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal basePrice;

    private BigDecimal comparePrice;

    private boolean isActive = true;
    private boolean isNew = false;
    private boolean isBestSeller = false;

    // SEO
    private String metaTitle;
    private String metaDescription;

    // Ảnh sản phẩm — URL từ Cloudinary Widget (upload thẳng từ browser)
    // Dạng: "https://res.cloudinary.com/.../img1.jpg,https://res.cloudinary.com/.../img2.jpg"
    private String imageUrls;

    /**
     * Tách chuỗi imageUrls thành List để dùng trong Service.
     * Trả về empty list nếu chưa có ảnh nào.
     */
    public List<String> getImageUrlList() {
        if (imageUrls == null || imageUrls.isBlank()) return Collections.emptyList();
        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // Biến thể (size + giá)
    @NotEmpty(message = "Phải có ít nhất 1 biến thể sản phẩm")
    private List<VariantRequest> variants;

    @Data
    public static class VariantRequest {
        private Long id;
        @NotBlank(message = "Size không được để trống")
        private String size;

        @NotNull(message = "Giá biến thể không được để trống")
        @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0")
        private BigDecimal price;

        @NotNull(message = "Số lượng tồn kho không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        private Integer stockQuantity;

        @Min(1)
        private Integer lowStockThreshold = 5;

        private String sku;
    }
}