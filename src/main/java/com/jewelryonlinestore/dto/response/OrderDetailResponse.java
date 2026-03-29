package com.jewelryonlinestore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chi tiết đơn hàng (C07 chi tiết / A05 chi tiết).
 */
@Data
@Builder
public class OrderDetailResponse {

    private Long id;
    private String orderNumber;

    // Thông tin khách hàng (snapshot)
    private String snapRecipientName;
    private String snapPhone;
    private String snapAddress;

    // Sản phẩm
    private List<OrderItemInfo> items;

    // Tiền
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String appliedCouponCode;
    private BigDecimal shippingFee;
    private BigDecimal total;

    // Thanh toán
    private String paymentMethod;
    private String paymentMethodLabel; // "Thanh toán khi nhận hàng"
    private String paymentStatus;
    private LocalDateTime paidAt;

    // Trạng thái
    private String orderStatus;
    private String orderStatusLabel;
    private String note;
    private String cancelledReason;

    // Lịch sử trạng thái
    private List<StatusHistoryInfo> statusHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean canCancel;
    private boolean canReview;

    // ---- Inner classes ----
    @Data
    @Builder
    public static class OrderItemInfo {
        private Long orderItemId;
        private Long variantId;
        private Long productId;
        private String productName;
        private String variantSize;
        private String imageUrl;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal total;
        private String productSlug;
        private boolean reviewed;       // đã đánh giá chưa
        private Long reviewId;          // nếu đã review
    }

    @Data
    @Builder
    public static class StatusHistoryInfo {
        private String oldStatus;
        private String newStatus;
        private String newStatusLabel;
        private String changedByName;   // tên admin hoặc "Hệ thống"
        private String note;
        private LocalDateTime createdAt;
    }
}
