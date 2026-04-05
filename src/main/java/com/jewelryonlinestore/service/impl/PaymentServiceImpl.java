package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.entity.Payment;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.repository.PaymentRepository;
import com.jewelryonlinestore.service.EmailService;
import com.jewelryonlinestore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
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
            updateOrderAsPaid(order, true);
            // Gửi email xác nhận sau khi VNPay thành công
            emailService.sendOrderConfirmationEmail(order.getOrderNumber());
        }
        return orderNumber;
    }

    // ── XỬ LÝ MOMO CALLBACK ────────────────────────────
    @Override
    @Transactional
    public String handleMomoCallback(Map<String, String> params) {
        String rawOrderId = params.get("orderId");
        String extraData = params.get("extraData");
        
        // extraData contains the original orderId, otherwise we split rawOrderId
        String orderNumber = (extraData != null && !extraData.isBlank()) 
                ? extraData 
                : (rawOrderId != null && rawOrderId.contains("_") ? rawOrderId.split("_")[0] : rawOrderId);
                
        String resultCode  = params.get("resultCode");

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderNumber));

        // resultCode = "0" nghĩa là thanh toán thành công
        if ("0".equals(resultCode)) {
            updateOrderAsPaid(order, false);
            try {
                saveMomoPayment(order, params, Payment.PaymentStatus.SUCCESS);
            } catch (Exception ex) {
                // Keep business state (order paid) even if payment audit row cannot be persisted.
                log.error("MoMo: lưu lịch sử giao dịch thất bại cho đơn {}: {}", orderNumber, ex.getMessage());
            }

            // KÍCH HOẠT GỬI EMAIL XÁC NHẬN ĐẶT HÀNG (MỚI BỔ SUNG)
            emailService.sendOrderConfirmationEmail(order.getOrderNumber());

            log.info("MoMo: Đơn hàng {} đã thanh toán và gửi mail thành công", orderNumber);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderRepository.save(order);
            try {
                saveMomoPayment(order, params, Payment.PaymentStatus.FAILED);
            } catch (Exception ex) {
                log.error("MoMo: lưu lịch sử giao dịch thất bại cho đơn {}: {}", orderNumber, ex.getMessage());
            }
            log.warn("MoMo: Thanh toán đơn hàng {} thất bại/bị hủy (Mã: {})", orderNumber, resultCode);
        }

        return orderNumber;
    }

    // Cập nhật trạng thái và thời gian thanh toán
    private void updateOrderAsPaid(Order order, boolean moveToProcessing) {
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        if (moveToProcessing) {
            order.setOrderStatus(Order.OrderStatus.PROCESSING);
        }
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private void saveMomoPayment(Order order, Map<String, String> params, Payment.PaymentStatus status) {
        String transactionId = params.get("transId");
        String payloadJson = toJson(params);
        Payment payment = Payment.builder()
                .order(order)
                .transactionId(transactionId)
                .paymentMethod(Order.PaymentMethod.MOMO)
                .amount(order.getTotal() == null ? BigDecimal.ZERO : order.getTotal())
                .status(status)
                .payload(payloadJson)
                .build();
        paymentRepository.saveAndFlush(payment);
    }

    private String toJson(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (index++ > 0) {
                sb.append(',');
            }
            sb.append('"').append(escapeJson(entry.getKey())).append('"').append(':')
                    .append('"').append(escapeJson(entry.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractOrderNumber(Map<String, String> params) {
        String orderNumber = params.get("orderNumber");
        if (orderNumber == null) orderNumber = params.get("vnp_TxnRef");
        if (orderNumber == null) orderNumber = params.get("orderId");
        return orderNumber;
    }
}