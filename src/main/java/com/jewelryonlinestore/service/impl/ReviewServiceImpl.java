package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.ReviewRequest;
import com.jewelryonlinestore.dto.response.ProductReviewSummary;
import com.jewelryonlinestore.dto.response.ReviewResponse;
import com.jewelryonlinestore.entity.*;
import com.jewelryonlinestore.repository.*;
import com.jewelryonlinestore.service.FileUploadService;
import com.jewelryonlinestore.service.ReviewService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final FileUploadService fileUploadService;

    // =========================
    // SUBMIT REVIEW (QUAN TRỌNG NHẤT)
    // =========================
    @Override
    @Transactional
    public ReviewResponse submitReview(ReviewRequest req, Authentication auth) {

        // 1. Lấy user + customer
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // 2. Lấy order item
        OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn"));

        // 3. Validate
        if (!orderItem.getOrder().getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Sản phẩm không thuộc đơn của bạn");
        }

        if (orderItem.getOrder().getOrderStatus() != Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Đơn chưa giao → không được review");
        }

        // ✅ đảm bảo mỗi order_item chỉ review 1 lần
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new IllegalStateException("Sản phẩm này đã được đánh giá rồi");
        }

        // 4. Tạo review
        Review review = new Review();
        review.setCustomer(customer);
        review.setProduct(orderItem.getVariant().getProduct());
        review.setOrderItem(orderItem);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setStatus(Review.ReviewStatus.PENDING);

        // 5. Upload ảnh
        List<ReviewImage> images = new ArrayList<>();
        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (!file.isEmpty()) {
                    String url = fileUploadService.upload(file, "reviews");

                    ReviewImage img = new ReviewImage();
                    img.setReview(review);
                    img.setImageUrl(url);

                    images.add(img);
                }
            }
        }
        review.setImages(images);

        // 6. Save review
        Review saved = reviewRepository.save(review);

        // 7. Link lại order_item
        orderItem.setReview(saved);
        orderItemRepository.save(orderItem);

        return toResponse(saved);
    }

    // =========================
    // LẤY REVIEW THEO PRODUCT
    // =========================
    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummary getReviewSummary(Long productId, int page, int size) {

        Page<Review> reviews = reviewRepository
                .findByProductIdAndStatusOrderByCreatedAtDesc(
                        productId,
                        Review.ReviewStatus.APPROVED,
                        PageRequest.of(page, size)
                );

        Double average = reviewRepository.findAverageRatingByProductId(productId);
        if (average == null) average = 0.0;

        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0);

        for (Object[] row : reviewRepository.findRatingDistribution(productId)) {
            distribution.put(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).intValue()
            );
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

    // =========================
    // ADMIN FILTER
    // =========================
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> adminFilterReviews(String status, Long productId,
                                                   Integer rating, int page, int size) {

        Review.ReviewStatus statusEnum = null;

        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Review.ReviewStatus.valueOf(status.toUpperCase());
            } catch (Exception ignored) {}
        }

        return reviewRepository
                .filterReviews(statusEnum, productId, rating, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    // =========================
    // APPROVE
    // =========================
    @Override
    @Transactional
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setStatus(Review.ReviewStatus.APPROVED);
        reviewRepository.save(review);

        updateProductRating(review.getProduct());
    }

    // =========================
    // REJECT
    // =========================
    @Override
    @Transactional
    public void rejectReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setStatus(Review.ReviewStatus.REJECTED);
        reviewRepository.save(review);

        updateProductRating(review.getProduct());
    }

    // =========================
    // ADMIN REPLY
    // =========================
    @Override
    @Transactional
    public void addAdminResponse(Long id, String response, Authentication auth) {

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setAdminResponse(response);
        reviewRepository.save(review);
    }

    // =========================
    // COUNT PENDING
    // =========================
    @Override
    @Transactional(readOnly = true)
    public long countPendingReviews() {
        return reviewRepository.countByStatus(Review.ReviewStatus.PENDING);
    }

    // =========================
    // UPDATE PRODUCT RATING
    // =========================
    private void updateProductRating(Product product) {
        if (product == null) return;

        Double avg = reviewRepository.findAverageRatingByProductId(product.getId());

        if (avg == null) avg = 0.0;
        else avg = Math.round(avg * 10.0) / 10.0;

        long count = reviewRepository.countByProductIdAndStatus(
                product.getId(),
                Review.ReviewStatus.APPROVED
        );

        product.setAverageRating(avg);
        product.setReviewCount((int) count);

        productRepository.save(product);
    }

    // =========================
    // MAPPER
    // =========================
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .customerName(review.getCustomer() != null ? review.getCustomer().getFullName() : null)
                .customerAvatarUrl(review.getCustomer() != null ? review.getCustomer().getAvatarUrl() : null)
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus().name().toLowerCase())
                .adminResponse(review.getAdminResponse())
                .imageUrls(Collections.emptyList())
                .createdAt(review.getCreatedAt())
                .isCurrentUserReview(false)
                .build();
    }
}