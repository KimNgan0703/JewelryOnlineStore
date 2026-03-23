package com.jewelryonlinestore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Tổng hợp đánh giá của 1 sản phẩm (hiển thị ở trang chi tiết C04).
 */
@Data
@Builder
public class ProductReviewSummary {

    private Double averageRating;
    private Integer totalReviews;
    private Map<Integer, Integer> ratingDistribution; // {5: 30, 4: 15, 3: 5, 2: 2, 1: 1}
    private List<ReviewResponse> reviews;
    private int page;
    private int totalPages;
}
