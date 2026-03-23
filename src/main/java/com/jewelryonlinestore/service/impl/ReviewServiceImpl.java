package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.ReviewRequest;
import com.jewelryonlinestore.dto.response.ProductReviewSummary;
import com.jewelryonlinestore.dto.response.ReviewResponse;
import com.jewelryonlinestore.entity.Review;
import com.jewelryonlinestore.repository.ReviewRepository;
import com.jewelryonlinestore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ReviewResponse submitReview(ReviewRequest req, Authentication auth) {
        throw new UnsupportedOperationException("Review submission needs order-item validation flow");
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummary getReviewSummary(Long productId, int page, int size) {
        Page<Review> reviews = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, Review.ReviewStatus.APPROVED, PageRequest.of(page, size));

        Double average = reviewRepository.findAverageRatingByProductId(productId);
        if (average == null) average = 0.0;

        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0);
        for (Object[] row : reviewRepository.findRatingDistribution(productId)) {
            distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }

        return ProductReviewSummary.builder()
                .averageRating(average)
                .totalReviews((int) reviews.getTotalElements())
                .ratingDistribution(distribution)
                .reviews(reviews.map(this::toResponse).getContent())
                .page(reviews.getNumber())
                .totalPages(reviews.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> adminFilterReviews(String status, Long productId,
                                                   Integer rating, int page, int size) {
        // ✅ Chuyển String → enum, null nếu không truyền status
        Review.ReviewStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Review.ReviewStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // status không hợp lệ → bỏ qua filter
            }
        }
        return reviewRepository.filterReviews(statusEnum, productId, rating, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        review.setStatus(Review.ReviewStatus.APPROVED);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void rejectReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        review.setStatus(Review.ReviewStatus.REJECTED);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void addAdminResponse(Long id, String response, Authentication auth) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        review.setAdminResponse(response);
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReviews() {
        // ✅ Truyền enum thay vì String
        return reviewRepository.countByStatus(Review.ReviewStatus.PENDING);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .customerName(review.getCustomer() != null ? review.getCustomer().getFullName() : null)
                .customerAvatarUrl(review.getCustomer() != null ? review.getCustomer().getAvatarUrl() : null)
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus().name().toLowerCase(Locale.ROOT))
                .adminResponse(review.getAdminResponse())
                .imageUrls(Collections.emptyList())
                .createdAt(review.getCreatedAt())
                .isCurrentUserReview(false)
                .build();
    }
}