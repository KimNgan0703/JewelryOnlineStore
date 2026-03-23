package com.jewelryonlinestore.dto.response;


import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO đánh giá sản phẩm (C04 chi tiết / C08 / A08).
 */
@Data
@Builder
public class ReviewResponse {

    private Long id;
    private String customerName;
    private String customerAvatarUrl;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private String status;              // pending | approved | rejected
    private String adminResponse;
    private List<String> imageUrls;
    private LocalDateTime createdAt;

    // Dùng cho trang chi tiết sản phẩm
    private boolean isCurrentUserReview; // true nếu đây là review của user đang đăng nhập
}
