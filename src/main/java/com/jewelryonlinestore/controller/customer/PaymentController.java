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
 * C06 — Xử lý callback từ các cổng thanh toán (VNPay, Momo API thật).
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

    // =========================================================================
    // ── ĐẦU NHẬN KẾT QUẢ TỪ MOMO SANDBOX (REAL API) ──────────────────────────
    // =========================================================================

    @GetMapping("/momo/return")
    public String momoReturnCallback(@RequestParam Map<String, String> params,
                                     RedirectAttributes redirectAttr) {
        // Lấy mã đơn hàng từ URL trả về của MoMo để dự phòng trường hợp lỗi
        String orderId = params.getOrDefault("orderId", "");

        try {
            // Chuyển toàn bộ dữ liệu MoMo trả về cho PaymentService xử lý
            // Service này sẽ kiểm tra chữ ký (Signature) và cập nhật Database
            String orderNumber = paymentService.handleMomoCallback(params);

            redirectAttr.addFlashAttribute("toast_success", "Thanh toán MoMo thành công!");
            return "redirect:/orders/" + orderNumber;
        } catch (Exception e) {
            log.error("Momo callback error: {}", e.getMessage());

            // Nếu khách hàng bấm HỦY trên app MoMo, hoặc thanh toán lỗi
            redirectAttr.addFlashAttribute("toast_error", "Thanh toán MoMo thất bại hoặc đã bị hủy.");

            // Trả khách về lại trang chi tiết đơn hàng kèm thông báo lỗi
            return "redirect:/orders/" + orderId;
        }
    }
}