package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.service.EmailService;
import com.jewelryonlinestore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final EmailService    emailService; // Inject EmailService để gửi mail

    @Override
    public String createVnpayUrl(String orderNumber, HttpServletRequest request, Authentication auth) {
        return "vnpay_url_placeholder";
    }

    @Override
    @Transactional
    public String handleVnpayCallback(Map<String, String> params) {
        String orderNumber = extractOrderNumber(params);
        String responseCode = params.get("vnp_ResponseCode");

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderNumber));

        if ("00".equals(responseCode)) {
            updateOrderAsPaid(order);
            // Gửi email xác nhận sau khi VNPay thành công
            emailService.sendOrderConfirmationEmail(order.getOrderNumber());
        }
        return orderNumber;
    }

    // ── XỬ LÝ MOMO CALLBACK ────────────────────────────
    @Override
    @Transactional
    public String handleMomoCallback(Map<String, String> params) {
        String orderNumber = params.get("orderId");
        String resultCode  = params.get("resultCode");

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderNumber));

        // resultCode = "0" nghĩa là thanh toán thành công
        if ("0".equals(resultCode)) {
            updateOrderAsPaid(order);

            // KÍCH HOẠT GỬI EMAIL XÁC NHẬN ĐẶT HÀNG (MỚI BỔ SUNG)
            emailService.sendOrderConfirmationEmail(order.getOrderNumber());

            log.info("MoMo: Đơn hàng {} đã thanh toán và gửi mail thành công", orderNumber);
        } else {
            log.warn("MoMo: Thanh toán đơn hàng {} thất bại/bị hủy (Mã: {})", orderNumber, resultCode);
        }

        return orderNumber;
    }

    // Cập nhật trạng thái và thời gian thanh toán
    private void updateOrderAsPaid(Order order) {
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setOrderStatus(Order.OrderStatus.PROCESSING); // Chuyển sang "Đang xử lý"
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private String extractOrderNumber(Map<String, String> params) {
        String orderNumber = params.get("orderNumber");
        if (orderNumber == null) orderNumber = params.get("vnp_TxnRef");
        if (orderNumber == null) orderNumber = params.get("orderId");
        return orderNumber;
    }
}