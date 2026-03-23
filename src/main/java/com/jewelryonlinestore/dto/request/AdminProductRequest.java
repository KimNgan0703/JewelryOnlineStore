package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
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

    // Ảnh sản phẩm
    private List<MultipartFile> images;
    private Integer primaryImageIndex = 0; // index ảnh chính trong list

    // Biến thể (size + giá)
    @NotEmpty(message = "Phải có ít nhất 1 biến thể sản phẩm")
    private List<VariantRequest> variants;

    @Data
    public static class VariantRequest {
        @NotBlank(message = "Size không được để trống")
        private String size;

        @NotNull(message = "Giá biến thể không được để trống")
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal price;

        @NotNull
        @Min(0)
        private Integer stockQuantity;

        @Min(1)
        private Integer lowStockThreshold = 5;
    }
}
