package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.CartItemRequest;
import com.jewelryonlinestore.dto.response.CartResponse;
import com.jewelryonlinestore.entity.Cart;
import com.jewelryonlinestore.entity.CartItem;
import com.jewelryonlinestore.entity.Promotion;
import com.jewelryonlinestore.entity.ProductVariant;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.CartItemRepository;
import com.jewelryonlinestore.repository.CartRepository;
import com.jewelryonlinestore.repository.ProductVariantRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.service.CartService;
import com.jewelryonlinestore.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository            cartRepository;
    private final CartItemRepository        cartItemRepository;
    private final ProductVariantRepository  productVariantRepository;
    private final UserRepository            userRepository;
    private final PromotionService          promotionService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Authentication auth, HttpSession session) {
        Cart cart = findCart(auth, session, false);
        if (cart == null) {
            return emptyCart();
        }
        return toResponse(cart, null, null);
    }

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest req, Authentication auth, HttpSession session) {
        Cart cart = findCart(auth, session, true);
        ProductVariant variant = productVariantRepository.findById(req.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + req.getVariantId()));

        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), req.getVariantId())
                .orElseGet(() -> CartItem.builder().cart(cart).variant(variant).quantity(0).build());

        int qty = item.getQuantity() + req.getQuantity();
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        item.setQuantity(qty);
        cartItemRepository.save(item);
        return getCart(auth, session);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(Long cartItemId, int quantity, Authentication auth, HttpSession session) {
        Cart cart = findCart(auth, session, true);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to current cart");
        }
        if (quantity <= 0) {
            // ĐÃ SỬA: Xóa khỏi bộ nhớ đệm trước khi xóa dưới DB
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        return getCart(auth, session);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long cartItemId, Authentication auth, HttpSession session) {
        Cart cart = findCart(auth, session, true);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to current cart");
        }

        // ĐÃ SỬA: Xóa khỏi bộ nhớ đệm trước khi xóa dưới DB
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return getCart(auth, session);
    }

    @Override
    public CartResponse applyCoupon(String code, Authentication auth, HttpSession session) {
        CartResponse cart = getCart(auth, session);

        // Validate coupon thực sự
        Optional<Promotion> promotionOpt = promotionService.validateCoupon(code, cart.getSubtotal());
        if (promotionOpt.isEmpty()) {
            throw new IllegalArgumentException("Mã giảm giá không hợp lệ hoặc đã hết hạn");
        }

        Promotion promotion   = promotionOpt.get();
        BigDecimal discount   = promotionService.calculateDiscount(promotion, cart.getSubtotal());
        BigDecimal total      = cart.getSubtotal().add(cart.getShippingFee()).subtract(discount);

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(cart.getItems())
                .subtotal(cart.getSubtotal())
                .discountAmount(discount)
                .shippingFee(cart.getShippingFee())
                .total(total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total)
                .appliedCouponCode(code)
                .couponMessage(buildCouponMessage(promotion, discount))
                .totalItems(cart.getTotalItems())
                .build();
    }

    @Override
    public CartResponse removeCoupon(Authentication auth, HttpSession session) {
        return getCart(auth, session);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(Authentication auth, HttpSession session) {
        Cart cart = findCart(auth, session, false);
        if (cart == null) {
            return 0;
        }
        return cartItemRepository.sumQuantityByCartId(cart.getId());
    }

    @Override
    @Transactional
    public void clearCart(Authentication auth, HttpSession session) {
        Cart cart = findCart(auth, session, false);
        if (cart != null) {
            cartItemRepository.deleteAllByCartId(cart.getId());
        }
    }

    @Override
    @Transactional
    public void mergeGuestCartToUser(String sessionId, Long userId) {
        Cart guest = cartRepository.findBySessionIdWithItems(sessionId).orElse(null);
        if (guest == null) {
            return;
        }
        Cart userCart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(User.builder().id(userId).build()).build()));

        for (CartItem guestItem : guest.getItems()) {
            CartItem existing = cartItemRepository.findByCartIdAndVariantId(
                    userCart.getId(), guestItem.getVariant().getId()).orElse(null);
            if (existing == null) {
                guestItem.setCart(userCart);
                cartItemRepository.save(guestItem);
            } else {
                existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
                cartItemRepository.save(existing);
            }
        }
        cartItemRepository.deleteAllByCartId(guest.getId());
        cartRepository.delete(guest);
    }

    // ── Private helpers ──────────────────────────────────

    private Cart findCart(Authentication auth, HttpSession session, boolean createIfMissing) {
        User user = resolveUser(auth);
        if (user != null) {
            return cartRepository.findByUserIdWithItems(user.getId())
                    .orElseGet(() -> createIfMissing
                            ? cartRepository.save(Cart.builder().user(user).build()) : null);
        }
        String sessionId = session.getId();
        return cartRepository.findBySessionIdWithItems(sessionId)
                .orElseGet(() -> createIfMissing
                        ? cartRepository.save(Cart.builder().sessionId(sessionId).build()) : null);
    }

    private User resolveUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private String buildCouponMessage(Promotion promotion, BigDecimal discount) {
        if (promotion.getType() == Promotion.PromotionType.PERCENTAGE) {
            return "Giảm " + promotion.getValue().stripTrailingZeros().toPlainString()
                    + "% - Tiết kiệm " + String.format("%,.0f", discount) + "₫";
        }
        return "Giảm " + String.format("%,.0f", discount) + "₫";
    }

    private CartResponse emptyCart() {
        return CartResponse.builder()
                .cartId(null)
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .totalItems(0)
                .build();
    }

    private CartResponse toResponse(Cart cart, String coupon, String couponMessage) {
        List<CartResponse.CartItemInfo> items = cart.getItems().stream().map(ci ->
                CartResponse.CartItemInfo.builder()
                        .cartItemId(ci.getId())
                        .variantId(ci.getVariant().getId())
                        .productId(ci.getVariant().getProduct().getId())
                        .productName(ci.getVariant().getProduct().getName())
                        .productSlug(ci.getVariant().getProduct().getSlug())
                        .imageUrl(ci.getVariant().getProduct().getPrimaryImageUrl())
                        .size(ci.getVariant().getSize())
                        .unitPrice(ci.getVariant().getPrice())
                        .quantity(ci.getQuantity())
                        .lineTotal(ci.getLineTotal())
                        .maxQuantity(ci.getVariant().getStockQuantity())
                        .inStock(ci.getVariant().getStockQuantity() > 0)
                        .build()).toList();

        BigDecimal subtotal = items.stream()
                .map(CartResponse.CartItemInfo::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .total(subtotal)
                .appliedCouponCode(coupon)
                .couponMessage(couponMessage)
                .totalItems(items.stream().mapToInt(CartResponse.CartItemInfo::getQuantity).sum())
                .build();
    }
}