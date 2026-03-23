package com.jewelryonlinestore.controller.customer;


import com.jewelryonlinestore.dto.request.ReviewRequest;
import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.dto.response.ReviewResponse;
import com.jewelryonlinestore.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * C08 — Gửi đánh giá sản phẩm (sau khi đã nhận hàng).
 */
@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ── Gửi đánh giá (AJAX từ trang chi tiết đơn hàng) ──
    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @Valid @ModelAttribute ReviewRequest req,
            Authentication auth) {
        ReviewResponse review = reviewService.submitReview(req, auth);
        return ResponseEntity.ok(ApiResponse.ok("Cảm ơn bạn đã đánh giá sản phẩm!", review));
    }

    // ── Load thêm đánh giá (AJAX — infinite scroll / pagination) ──
    @GetMapping("/product/{productId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<com.jewelryonlinestore.dto.response.ProductReviewSummary>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        var summary = reviewService.getReviewSummary(productId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
