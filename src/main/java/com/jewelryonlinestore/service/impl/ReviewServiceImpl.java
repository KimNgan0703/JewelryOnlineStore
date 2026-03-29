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
    private final com.jewelryonlinestore.repository.ProductRepository productRepository;
    private final com.jewelryonlinestore.repository.OrderItemRepository orderItemRepository;
    private final com.jewelryonlinestore.repository.UserRepository userRepository;
    private final com.jewelryonlinestore.repository.CustomerRepository customerRepository;
    private final com.jewelryonlinestore.service.FileUploadService fileUploadService;
    @Override
    @Transactional
    public ReviewResponse submitReview(ReviewRequest req, Authentication auth) {
        // 1. Lấy thông tin khách hàng
        com.jewelryonlinestore.entity.User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        com.jewelryonlinestore.entity.Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // 2. Tìm OrderItem được đánh giá
        com.jewelryonlinestore.entity.OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn hàng"));

        // 3. Kiểm tra điều kiện (Đúng chủ, Đã giao hàng, Chưa đánh giá)
        if (!orderItem.getOrder().getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Sản phẩm không thuộc đơn hàng của bạn");
        }
        if (orderItem.getOrder().getOrderStatus() != com.jewelryonlinestore.entity.Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Đơn hàng chưa hoàn thành, không thể đánh giá");
        }
        if (orderItem.hasReview()) {
            throw new IllegalStateException("Sản phẩm này đã được đánh giá rồi");
        }

        // 4. Tạo Entity Review
        Review review = new Review();
        review.setCustomer(customer);
        review.setProduct(orderItem.getVariant().getProduct());
        review.setOrderItem(orderItem);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setStatus(Review.ReviewStatus.PENDING); // Chờ Admin duyệt

        // 5. Xử lý Upload Ảnh (Nếu có)
        java.util.List<com.jewelryonlinestore.entity.ReviewImage> reviewImages = new java.util.ArrayList<>();
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            for (org.springframework.web.multipart.MultipartFile file : req.getImages()) {
                if (!file.isEmpty()) {
                    // Upload lên thư mục 'reviews' trên Cloudinary
                    String imageUrl = fileUploadService.upload(file, "reviews");

                    com.jewelryonlinestore.entity.ReviewImage reviewImage = new com.jewelryonlinestore.entity.ReviewImage();
                    reviewImage.setReview(review);
                    reviewImage.setImageUrl(imageUrl);
                    reviewImages.add(reviewImage);
                }
            }
        }
        review.setImages(reviewImages);

        // 6. Lưu Review xuống Database
        Review savedReview = reviewRepository.save(review);

        // 7. Cập nhật OrderItem là đã được review
        orderItem.setReview(savedReview);
        orderItemRepository.save(orderItem);

        return toResponse(savedReview);
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

        // Tự động tính toán lại sao cho Sản phẩm
        updateProductRating(review.getProduct());
    }

    @Override
    @Transactional
    public void rejectReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        review.setStatus(Review.ReviewStatus.REJECTED);
        reviewRepository.save(review);

        // Tính toán lại sao (nhỡ đâu bài này trước đó đã duyệt rồi giờ bị ẩn đi)
        updateProductRating(review.getProduct());
    }

    // HÀM TỰ ĐỘNG TÍNH TOÁN LẠI ĐIỂM TRUNG BÌNH CỦA SẢN PHẨM
    private void updateProductRating(com.jewelryonlinestore.entity.Product product) {
        if (product == null) return;

        // 1. Tính điểm trung bình mới
        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        if (avgRating == null) {
            avgRating = 0.0;
        } else {
            // Làm tròn 1 chữ số thập phân (Ví dụ: 4.6666 -> 4.7)
            avgRating = (double) Math.round(avgRating * 10) / 10;
        }

        // 2. Đếm lại tổng số bài đánh giá đã duyệt
        long reviewCount = reviewRepository.countByProductIdAndStatus(product.getId(), Review.ReviewStatus.APPROVED);

        // 3. Cập nhật vào thực thể Product
        product.setAverageRating(avgRating);
        product.setReviewCount((int) reviewCount);

        // 4. Lưu lại
        productRepository.save(product);
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