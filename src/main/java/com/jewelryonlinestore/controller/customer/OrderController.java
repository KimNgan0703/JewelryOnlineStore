package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.request.PlaceOrderRequest;
import com.jewelryonlinestore.dto.request.AddressRequest;
import com.jewelryonlinestore.dto.response.*;
import com.jewelryonlinestore.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * C06 — Đặt hàng & thanh toán.
 * C07 — Theo dõi & hủy đơn hàng.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService     orderService;
    private final CartService      cartService;
    private final AddressService   addressService;
    private final PromotionService promotionService;
    private final MomoService      momoService;

    // ── Nhận selectedItemIds từ cart, lưu session, redirect checkout ──────
    @PostMapping("/select-items")
    public String selectItems(@RequestParam(value = "selectedItemIds", required = false) List<Long> selectedItemIds,
                              HttpSession session) {
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            return "redirect:/cart";
        }
        session.setAttribute("selectedCartItemIds", selectedItemIds);
        return "redirect:/orders/checkout";
    }

    // ── Trang Checkout (C06) ──────────────────────────────
    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, HttpSession session, Model model) {
        CartResponse fullCart = cartService.getCart(auth, session);
        if (fullCart.getItems() == null || fullCart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        // Lọc chỉ các item được chọn
        @SuppressWarnings("unchecked")
        List<Long> selectedIds = (List<Long>) session.getAttribute("selectedCartItemIds");

        CartResponse cart;
        if (selectedIds != null && !selectedIds.isEmpty()) {
            // Tạo CartResponse chỉ chứa items được chọn
            cart = filterCartBySelectedIds(fullCart, selectedIds);
        } else {
            cart = fullCart;
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        placeOrderRequest.setNewAddress(new AddressRequest());
        placeOrderRequest.setPaymentMethod("COD");

        model.addAttribute("cart",              cart);
        model.addAttribute("addresses",         addressService.getMyAddresses(auth));
        model.addAttribute("placeOrderRequest", placeOrderRequest);
        model.addAttribute("pageTitle",         "Thanh Toán");
        return "customer/checkout";
    }

    // ── Helper: lọc CartResponse theo danh sách itemId ───
    private CartResponse filterCartBySelectedIds(CartResponse fullCart, List<Long> selectedIds) {
        List<CartResponse.CartItemInfo> filteredItems = fullCart.getItems().stream()
                .filter(item -> selectedIds.contains(item.getCartItemId()))
                .toList();

        java.math.BigDecimal subtotal = filteredItems.stream()
                .map(CartResponse.CartItemInfo::getLineTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return CartResponse.builder()
                .cartId(fullCart.getCartId())
                .items(filteredItems)
                .subtotal(subtotal)
                .discountAmount(java.math.BigDecimal.ZERO)
                .shippingFee(java.math.BigDecimal.ZERO)
                .total(subtotal)
                .totalItems(filteredItems.stream().mapToInt(CartResponse.CartItemInfo::getQuantity).sum())
                .build();
    }

    // ── Đặt hàng (C06) ────────────────────────────────────
    @PostMapping("/place")
    public String placeOrder(@Valid @ModelAttribute PlaceOrderRequest req,
                             BindingResult result,
                             Authentication auth,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            if (req.getNewAddress() == null) {
                req.setNewAddress(new AddressRequest());
            }
            CartResponse fullCart = cartService.getCart(auth, session);
            @SuppressWarnings("unchecked")
            List<Long> selectedIds = (List<Long>) session.getAttribute("selectedCartItemIds");
            CartResponse cart = (selectedIds != null && !selectedIds.isEmpty())
                    ? filterCartBySelectedIds(fullCart, selectedIds)
                    : fullCart;

            model.addAttribute("cart",              cart);
            model.addAttribute("addresses",         addressService.getMyAddresses(auth));
            model.addAttribute("placeOrderRequest", req);
            return "customer/checkout";
        }

        try {
            // Lấy selectedItemIds từ session để placeOrder chỉ xử lý items được chọn
            @SuppressWarnings("unchecked")
            List<Long> selectedIds = (List<Long>) session.getAttribute("selectedCartItemIds");
            req.setSelectedCartItemIds(selectedIds);

            OrderDetailResponse order = orderService.placeOrder(req, auth, session);

            // Xóa selectedItemIds khỏi session sau khi đặt hàng xong
            session.removeAttribute("selectedCartItemIds");

            if ("MOMO".equalsIgnoreCase(req.getPaymentMethod())) {
                try {
                    String payUrl = momoService.createMomoPaymentUrl(
                            order.getOrderNumber(),
                            String.valueOf(order.getTotal().longValue()),
                            "Thanh toan don hang " + order.getOrderNumber()
                    );
                    return "redirect:" + payUrl;
                } catch (Exception e) {
                    redirectAttr.addFlashAttribute("toast_error", "Không thể tạo liên kết MoMo lúc này.");
                    return "redirect:/orders/checkout";
                }
            }

            redirectAttr.addFlashAttribute("order",         order);
            redirectAttr.addFlashAttribute("toast_success", "Đặt hàng thành công!");
            return "redirect:/orders/success/" + order.getOrderNumber();

        } catch (Exception e) {
            redirectAttr.addFlashAttribute("toast_error", e.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    // ── Trang xác nhận đặt hàng thành công ───────────────
    @GetMapping("/success/{orderNumber}")
    public String orderSuccess(@PathVariable String orderNumber,
                               Authentication auth, Model model) {
        OrderDetailResponse order = orderService.getOrderDetail(orderNumber, auth);
        model.addAttribute("order",     order);
        model.addAttribute("pageTitle", "Đặt Hàng Thành Công");
        return "customer/order-success";
    }

    // ── Danh sách đơn hàng (C07) ──────────────────────────
    @GetMapping
    public String myOrders(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false)   String status,
                           Authentication auth, Model model) {
        Page<OrderSummaryResponse> orders = orderService.getMyOrders(auth, status, page, 10);
        model.addAttribute("orders",       orders.getContent());
        model.addAttribute("currentPage",  orders.getNumber());
        model.addAttribute("totalPages",   orders.getTotalPages());
        model.addAttribute("activeStatus", status);
        model.addAttribute("pageTitle",    "Đơn Hàng Của Tôi");
        return "customer/order-list";
    }

    // ── Chi tiết đơn hàng (C07) ───────────────────────────
    @GetMapping("/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber,
                              Authentication auth, Model model) {
        OrderDetailResponse order = orderService.getOrderDetail(orderNumber, auth);
        model.addAttribute("order",     order);
        model.addAttribute("pageTitle", "Chi Tiết Đơn Hàng #" + orderNumber);
        return "customer/order-detail";
    }

    @PostMapping("/{orderNumber}/retry-momo")
    public String retryMomoPayment(@PathVariable String orderNumber,
                                   Authentication auth,
                                   RedirectAttributes redirectAttr) {
        try {
            OrderDetailResponse order = orderService.getOrderDetail(orderNumber, auth);
            if (!Boolean.TRUE.equals(order.getCanRetryMomoPayment())) {
                redirectAttr.addFlashAttribute("toast_error", "Đơn hàng này không thể thanh toán lại bằng MoMo.");
                return "redirect:/orders/" + orderNumber;
            }
            String payUrl = momoService.createMomoPaymentUrl(
                    order.getOrderNumber(),
                    String.valueOf(order.getTotal().longValue()),
                    "Thanh toan lai don hang " + order.getOrderNumber()
            );
            return "redirect:" + payUrl;
        } catch (Exception e) {
            redirectAttr.addFlashAttribute("toast_error", "Không thể tạo liên kết thanh toán MoMo lúc này.");
            return "redirect:/orders/" + orderNumber;
        }
    }

    // ── Hủy đơn (C07 - AJAX) ──────────────────────────────
    @PostMapping("/{orderNumber}/cancel")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        try {
            boolean refunded = orderService.cancelOrder(orderNumber, reason, auth);
            String message = refunded ? "Đã hoàn tiền thành công" : "Đơn hàng đã được hủy";
            return ResponseEntity.ok(ApiResponse.ok(message, null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Khách xác nhận Đã nhận hàng ──────────────────────
    @PostMapping("/{orderNumber}/receive")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> receiveOrder(
            @PathVariable String orderNumber,
            Authentication auth) {
        try {
            orderService.markAsDelivered(orderNumber, auth);
            return ResponseEntity.ok(ApiResponse.ok("Cảm ơn bạn đã mua sắm! Đơn hàng đã hoàn thành.", null));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    // ── Mua lại (C07) ─────────────────────────────────────
    @PostMapping("/{orderNumber}/reorder")
    public String reorder(@PathVariable String orderNumber,
                          Authentication auth, HttpSession session,
                          RedirectAttributes redirectAttr) {
        int added = orderService.reorder(orderNumber, auth, session);
        redirectAttr.addFlashAttribute("toast_success", "Đã thêm " + added + " sản phẩm vào giỏ hàng.");
        return "redirect:/cart";
    }

    // ── API: Áp dụng mã giảm giá ──────────────────────────
    @PostMapping("/coupon/apply")
    @ResponseBody
    public ResponseEntity<?> applyCoupon(@RequestParam("code") String code,
                                         Authentication auth, HttpSession session) {
        try {
            CartResponse updatedCart = cartService.applyCoupon(code, auth, session);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi xử lý: " + e.getMessage()));
        }
    }

    // ── API: Gỡ mã giảm giá ──────────────────────────────
    @PostMapping("/coupon/remove")
    @ResponseBody
    public ResponseEntity<?> removeCoupon(Authentication auth, HttpSession session) {
        cartService.removeCoupon(auth, session);
        return ResponseEntity.ok(Map.of("message", "Đã gỡ mã giảm giá"));
    }
}