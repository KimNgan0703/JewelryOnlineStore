package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * C06 — Xử lý callback từ các cổng thanh toán (VNPay, Momo).
 */
@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── VNPay IPN (Instant Payment Notification) ──────────
    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam Map<String, String> params,
                                Authentication auth,
                                RedirectAttributes redirectAttr) {
        try {
            String orderNumber = paymentService.handleVnpayCallback(params);
            redirectAttr.addFlashAttribute("toast_success", "Thanh toán thành công!");
            return "redirect:/orders/" + orderNumber;
        } catch (Exception e) {
            log.error("VNPay callback error: {}", e.getMessage());
            redirectAttr.addFlashAttribute("toast_error",
                    "Thanh toán thất bại. Vui lòng thử lại hoặc chọn phương thức khác.");
            return "redirect:/orders";
        }
    }

    // ── Momo Redirect ────────────────────────────────────
    @GetMapping("/momo/callback")
    public String momoCallback(@RequestParam Map<String, String> params,
                               RedirectAttributes redirectAttr) {
        try {
            String orderNumber = paymentService.handleMomoCallback(params);
            redirectAttr.addFlashAttribute("toast_success", "Thanh toán MoMo thành công!");
            return "redirect:/orders/" + orderNumber;
        } catch (Exception e) {
            log.error("Momo callback error: {}", e.getMessage());
            redirectAttr.addFlashAttribute("toast_error", "Thanh toán MoMo thất bại.");
            return "redirect:/orders";
        }
    }

    // ── Tạo URL thanh toán VNPay (AJAX) ──────────────────
    @PostMapping("/vnpay/create")
    @ResponseBody
    public com.jewelryonlinestore.dto.response.ApiResponse<String> createVnpayUrl(
            @RequestParam String orderNumber,
            HttpServletRequest request,
            Authentication auth) {
        String url = paymentService.createVnpayUrl(orderNumber, request, auth);
        return com.jewelryonlinestore.dto.response.ApiResponse.ok(url);
    }
}

