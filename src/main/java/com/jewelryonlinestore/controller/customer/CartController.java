package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.dto.response.*;
import com.jewelryonlinestore.service.CartService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * C05 — Quản lý giỏ hàng & mã khuyến mãi.
 * Kết hợp cả Server-Side render (trang giỏ) lẫn AJAX endpoints (thêm/xóa/cập nhật).
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ── Trang giỏ hàng ───────────────────────────────────
    @GetMapping
    public String cartPage(Authentication auth, HttpSession session, Model model) {
        CartResponse cart = cartService.getCart(auth, session);
        model.addAttribute("cart",      cart);
        model.addAttribute("pageTitle", "Giỏ Hàng");
        return "customer/cart";
    }

    // ── Thêm vào giỏ (AJAX) ──────────────────────────────
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest req,
            Authentication auth, HttpSession session) {
        CartResponse cart = cartService.addItem(req, auth, session);
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm vào giỏ hàng!", cart));
    }

    // ── Cập nhật số lượng (AJAX) ─────────────────────────
    @PutMapping("/items/{cartItemId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam int quantity,
            Authentication auth, HttpSession session) {
        CartResponse cart = cartService.updateQuantity(cartItemId, quantity, auth, session);
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    // ── Xóa sản phẩm khỏi giỏ (AJAX) ───────────────────
    @DeleteMapping("/items/{cartItemId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long cartItemId,
            Authentication auth, HttpSession session) {
        CartResponse cart = cartService.removeItem(cartItemId, auth, session);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa sản phẩm", cart));
    }

    // ── Áp dụng mã giảm giá (AJAX) ──────────────────────
    @PostMapping("/apply-coupon")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest req,
            Authentication auth, HttpSession session) {
        CartResponse cart = cartService.applyCoupon(req.getCode(), auth, session);
        return ResponseEntity.ok(ApiResponse.ok("Mã giảm giá hợp lệ!", cart));
    }

    // ── Hủy mã giảm giá (AJAX) ──────────────────────────
    @DeleteMapping("/coupon")
    @ResponseBody
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon(
            Authentication auth, HttpSession session) {
        CartResponse cart = cartService.removeCoupon(auth, session);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy mã giảm giá", cart));
    }

    // ── Đếm số item (AJAX — cập nhật badge icon giỏ) ────
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<ApiResponse<Integer>> cartCount(
            Authentication auth, HttpSession session) {
        int count = cartService.getCartItemCount(auth, session);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }
}
