package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.ReviewRequest;
import com.jewelryonlinestore.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

public interface ReviewService {
    ReviewResponse       submitReview(ReviewRequest req, Authentication auth);
    ProductReviewSummary getReviewSummary(Long productId, int page, int size);
    Page<ReviewResponse> adminFilterReviews(String status, Long productId,
                                            Integer rating, int page, int size);
    void                 approveReview(Long id);
    void                 rejectReview(Long id);
    void                 addAdminResponse(Long id, String response, Authentication auth);
    long                 countPendingReviews();
}
