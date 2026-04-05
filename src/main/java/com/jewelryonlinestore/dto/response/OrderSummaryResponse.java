package com.jewelryonlinestore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO gọn cho danh sách đơn hàng (C07 - Đơn hàng của tôi / A05 - Admin).
 */
@Data
@Builder
public class OrderSummaryResponse {

    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private BigDecimal total;
    private String orderStatus;         // pending | processing | preparing | shipping | delivered | cancelled | returned
    private String orderStatusLabel;    // "Chờ xác nhận" | "Đang giao" | ...
    private String paymentMethod;
    private String paymentStatus;       // pending | paid | failed | refunded
    private int itemCount;
    private String firstProductName;    // tên sản phẩm đầu tiên (preview)
    private String firstProductImage;
    private boolean canCancel;          // true nếu còn trong trạng thái pending/processing
    private boolean canReview;          // true nếu delivered và chưa review hết
    private Boolean canRetryMomoPayment;
}
