package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.service.PaymentService;
import com.jewelryonlinestore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    // ── XỬ LÝ VNPAY CALLBACK ────────────────────────────
    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam Map<String, String> params, RedirectAttributes redirectAttr) {
        try {
            String orderNumber = paymentService.handleVnpayCallback(params);
            redirectAttr.addFlashAttribute("toast_success", "Thanh toán VNPay thành công!");

            // ĐÃ SỬA: Chuyển từ ?orderNumber= sang đường dẫn /
            return "redirect:/orders/success/" + orderNumber;
        } catch (Exception e) {
            return "redirect:/orders?error=vnpay_failed";
        }
    }

    // ── XỬ LÝ MOMO RETURN ──────────────────────────────
    @GetMapping("/momo/return")
    public String momoReturnCallback(@RequestParam Map<String, String> params,
                                     RedirectAttributes redirectAttr) {
        String rawOrderId = params.getOrDefault("orderId", "");
        String extraData = params.get("extraData");
        String orderId = (extraData != null && !extraData.isBlank()) 
                ? extraData 
                : (rawOrderId.contains("_") ? rawOrderId.split("_")[0] : rawOrderId);
        
        String resultCode = params.get("resultCode");

        try {
            paymentService.handleMomoCallback(params);

            if ("0".equals(resultCode)) {
                // ĐÃ SỬA: Chuyển từ ?orderNumber= sang đường dẫn /
                return "redirect:/orders/success/" + orderId;
            } else {
                redirectAttr.addFlashAttribute("toast_error", "Thanh toán MoMo thất bại.");
                return "redirect:/orders/" + orderId;
            }
        } catch (Exception e) {
            return "redirect:/orders?error=system_error";
        }
    }
}