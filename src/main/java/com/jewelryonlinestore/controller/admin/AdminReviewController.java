package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * A08 — Duyệt / ẩn đánh giá, phản hồi đánh giá.
 */
@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public String reviewList(@RequestParam(required = false)   String status,
                             @RequestParam(required = false)   Integer rating,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        var reviews = reviewService.adminFilterReviews(status, null, rating, page, 15);
        model.addAttribute("reviews",     reviews.getContent());
        model.addAttribute("currentPage", reviews.getNumber());
        model.addAttribute("totalPages",  reviews.getTotalPages());
        model.addAttribute("totalItems",  reviews.getTotalElements());
        model.addAttribute("activeStatus",status);
        model.addAttribute("pendingCount",reviewService.countPendingReviews());
        model.addAttribute("pageTitle",   "Quản Lý Đánh Giá");
        return "admin/reviews";
    }

    // ── Duyệt (AJAX) ─────────────────────────────────────
    @PatchMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        reviewService.approveReview(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt đánh giá", null));
    }

    // ── Ẩn (AJAX) ────────────────────────────────────────
    @PatchMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Long id) {
        reviewService.rejectReview(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ẩn đánh giá", null));
    }

    // ── Phản hồi (AJAX) ──────────────────────────────────
    @PostMapping("/{id}/respond")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> respond(@PathVariable Long id,
                                                     @RequestParam String response,
                                                     Authentication auth) {
        reviewService.addAdminResponse(id, response, auth);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi phản hồi", null));
    }
}
