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

/**
 * C06 — Đặt hàng & thanh toán.
 * C07 — Theo dõi & hủy đơn hàng.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService   orderService;
    private final CartService    cartService;
    private final AddressService addressService;

    // ── Trang Checkout (C06) ─────────────────────────────
    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, HttpSession session, Model model) {
        CartResponse cart = cartService.getCart(auth, session);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        placeOrderRequest.setNewAddress(new AddressRequest());
        placeOrderRequest.setPaymentMethod("cod");

        model.addAttribute("cart",              cart);
        model.addAttribute("addresses",         addressService.getMyAddresses(auth));
        model.addAttribute("placeOrderRequest", placeOrderRequest);
        model.addAttribute("pageTitle",         "Thanh Toán");
        return "customer/checkout";
    }

    // ── Đặt hàng (C06) ───────────────────────────────────
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
            CartResponse cart = cartService.getCart(auth, session);
            model.addAttribute("cart",              cart);
            model.addAttribute("addresses",         addressService.getMyAddresses(auth));
            model.addAttribute("placeOrderRequest", req);
            return "customer/checkout";
        }
        try {
            OrderDetailResponse order = orderService.placeOrder(req, auth, session);
            redirectAttr.addFlashAttribute("order",         order);
            redirectAttr.addFlashAttribute("toast_success", "Đặt hàng thành công!");
            return "redirect:/orders/success/" + order.getOrderNumber();
        } catch (Exception e) {
            redirectAttr.addFlashAttribute("toast_error", e.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    // ── Trang xác nhận đặt hàng thành công ──────────────
    @GetMapping("/success/{orderNumber}")
    public String orderSuccess(@PathVariable String orderNumber,
                               Authentication auth, Model model) {
        OrderDetailResponse order = orderService.getOrderDetail(orderNumber, auth);
        model.addAttribute("order",     order);
        model.addAttribute("pageTitle", "Đặt Hàng Thành Công");
        return "customer/order-success";
    }

    // ── Danh sách đơn hàng (C07) ─────────────────────────
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

    // ── Chi tiết đơn hàng (C07) ──────────────────────────
    @GetMapping("/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber,
                              Authentication auth, Model model) {
        OrderDetailResponse order = orderService.getOrderDetail(orderNumber, auth);
        model.addAttribute("order",     order);
        model.addAttribute("pageTitle", "Chi Tiết Đơn Hàng #" + orderNumber);
        return "customer/order-detail";
    }

    // ── Hủy đơn (C07 - AJAX) ─────────────────────────────
    @PostMapping("/{orderNumber}/cancel")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        try {
            orderService.cancelOrder(orderNumber, reason, auth);
            return ResponseEntity.ok(ApiResponse.ok("Đơn hàng đã được hủy", null));
        } catch (IllegalStateException e) {
            // canCancel() = false
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Mua lại (C07) ────────────────────────────────────
    @PostMapping("/{orderNumber}/reorder")
    public String reorder(@PathVariable String orderNumber,
                          Authentication auth, HttpSession session,
                          RedirectAttributes redirectAttr) {
        int added = orderService.reorder(orderNumber, auth, session);
        redirectAttr.addFlashAttribute("toast_success",
                "Đã thêm " + added + " sản phẩm vào giỏ hàng.");
        return "redirect:/cart";
    }
}