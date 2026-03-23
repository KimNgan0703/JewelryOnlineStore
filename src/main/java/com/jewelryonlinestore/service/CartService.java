package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.CartItemRequest;
import com.jewelryonlinestore.dto.response.CartResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;

public interface CartService {
    CartResponse getCart(Authentication auth, HttpSession session);
    CartResponse addItem(CartItemRequest req, Authentication auth, HttpSession session);
    CartResponse updateQuantity(Long cartItemId, int quantity,
                                Authentication auth, HttpSession session);
    CartResponse removeItem(Long cartItemId, Authentication auth, HttpSession session);
    CartResponse applyCoupon(String code, Authentication auth, HttpSession session);
    CartResponse removeCoupon(Authentication auth, HttpSession session);
    int          getCartItemCount(Authentication auth, HttpSession session);
    void         clearCart(Authentication auth, HttpSession session);
    void         mergeGuestCartToUser(String sessionId, Long userId);
}
