package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Đánh giá approved của sản phẩm (trang chi tiết C04)
    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId, Review.ReviewStatus status, Pageable pageable
    );

    // Kiểm tra đã review order_item này chưa (C08)
    boolean existsByOrderItemId(Long orderItemId);

    // Tìm review của customer cho sản phẩm
    Optional<Review> findByCustomerIdAndProductId(Long customerId, Long productId);

    // Rating trung bình của sản phẩm
    // ✅ Dùng enum thay vì string 'approved'
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = com.jewelryonlinestore.entity.Review.ReviewStatus.APPROVED")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    // Phân bố rating (1-5 sao)
    // ✅ Dùng enum thay vì string 'approved'
    @Query("""
        SELECT r.rating, COUNT(r) FROM Review r
        WHERE r.product.id = :productId
          AND r.status = com.jewelryonlinestore.entity.Review.ReviewStatus.APPROVED
        GROUP BY r.rating
    """)
    List<Object[]> findRatingDistribution(@Param("productId") Long productId);

    // Admin: lọc review (A08)
    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.customer c
        JOIN FETCH r.product p
        WHERE (:status    IS NULL OR r.status    = :status)
          AND (:productId IS NULL OR p.id        = :productId)
          AND (:rating    IS NULL OR r.rating    = :rating)
        ORDER BY r.createdAt DESC
    """)
    Page<Review> filterReviews(
            @Param("status")    Review.ReviewStatus status,
            @Param("productId") Long productId,
            @Param("rating")    Integer rating,
            Pageable pageable
    );

    // ✅ Đếm review theo enum (dashboard widget - A02)
    long countByStatus(Review.ReviewStatus status);
}