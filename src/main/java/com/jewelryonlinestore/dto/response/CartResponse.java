package com.jewelryonlinestore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về toàn bộ thông tin giỏ hàng (C05).
 */
@Data
@Builder
public class CartResponse {

    private Long cartId;
    private List<CartItemInfo> items;

    // Tổng tiền
    private BigDecimal subtotal;         // tổng trước giảm giá
    private BigDecimal discountAmount;   // số tiền được giảm
    private BigDecimal shippingFee;
    private BigDecimal total;            // tổng thanh toán

    // Mã giảm giá đang áp dụng
    private String appliedCouponCode;
    private String couponMessage;        // "Giảm 10% - tiết kiệm 50,000đ"

    private int totalItems;              // tổng số mặt hàng

    // ---- Inner class ----
    @Data
    @Builder
    public static class CartItemInfo {
        private Long cartItemId;
        private Long variantId;
        private Long productId;
        private String productName;
        private String productSlug;
        private String imageUrl;
        private String size;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;    // unitPrice * quantity
        private Integer maxQuantity;     // stockQuantity còn lại
        private boolean inStock;
    }
}
