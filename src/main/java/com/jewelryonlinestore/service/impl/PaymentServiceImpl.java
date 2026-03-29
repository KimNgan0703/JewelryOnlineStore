package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.repository.OrderRepository;
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

    @Override
    public String createVnpayUrl(String orderNumber, HttpServletRequest request, Authentication auth) {
        // Code tạo URL VNPay cũ của bạn (nếu có logic thật thì thay vào đây)
        return "vnpay_url_placeholder";
    }

    // ── XỬ LÝ VNPAY CALLBACK ────────────────────────────
    @Override
    @Transactional
    public String handleVnpayCallback(Map<String, String> params) {
        String orderNumber = extractOrderNumber(params);
        String responseCode = params.get("vnp_ResponseCode");

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderNumber));

        if ("00".equals(responseCode)) {
            updateOrderAsPaid(order);
            log.info("VNPay: Đơn hàng {} đã thanh toán thành công", orderNumber);
        }
        return orderNumber;
    }

    // ── XỬ LÝ MOMO CALLBACK (REAL API) ──────────────────
    @Override
    @Transactional
    public String handleMomoCallback(Map<String, String> params) {
        String orderNumber = extractOrderNumber(params);
        String resultCode = params.get("resultCode"); // "0" là thành công theo tài liệu MoMo

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderNumber));

        // CHỖ QUAN TRỌNG NHẤT: Cập nhật Database nếu MoMo báo thành công
        if ("0".equals(resultCode)) {
            updateOrderAsPaid(order);
            log.info("MoMo: Đơn hàng {} đã thanh toán thành công", orderNumber);
        } else {
            log.warn("MoMo: Thanh toán đơn hàng {} thất bại với mã: {}", orderNumber, resultCode);
        }

        return orderNumber;
    }

    // ── HÀM DÙNG CHUNG ĐỂ CẬP NHẬT TRẠNG THÁI ─────────────
    private void updateOrderAsPaid(Order order) {
        order.setPaymentStatus(Order.PaymentStatus.PAID);   // Chuyển sang ĐÃ THANH TOÁN
        order.setOrderStatus(Order.OrderStatus.PROCESSING); // Chuyển sang ĐANG XỬ LÝ
        order.setPaidAt(LocalDateTime.now());         // Lưu giờ thanh toán
        orderRepository.save(order);                  // Lệnh chốt xuống Database
    }

    // ── HÀM TRÍCH XUẤT MÃ ĐƠN HÀNG ───────────────────────
    private String extractOrderNumber(Map<String, String> params) {
        String orderNumber = params.get("orderNumber");
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = params.get("vnp_TxnRef"); // Dành cho VNPay
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = params.get("orderId");    // Dành cho MoMo
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Không tìm thấy mã đơn hàng trong tham số trả về");
        }
        return orderNumber;
    }
}