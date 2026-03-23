package com.jewelryonlinestore.dto.response;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO gọn cho card sản phẩm trong danh sách / trang chủ.
 * Tránh load toàn bộ entity khi chỉ cần hiển thị card.
 */
@Data
@Builder
public class ProductCardResponse {

    private Long id;
    private String name;
    private String slug;
    private String primaryImageUrl;
    private BigDecimal basePrice;
    private BigDecimal comparePrice;
    private boolean hasDiscount;
    private Integer discountPercent;
    private Double averageRating;
    private Integer reviewCount;
    private boolean isNew;
    private boolean isBestSeller;
    private boolean inStock;
}
