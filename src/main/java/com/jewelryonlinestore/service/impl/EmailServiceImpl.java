package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final OrderRepository orderRepository; // Thêm công cụ lấy đơn hàng

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    @Async("asyncExecutor")
    public void sendVerificationEmail(String email, String token) {}

    @Override
    @Async("asyncExecutor")
    public void sendPasswordResetEmail(String email, String token) {}

    @Override
    @Async("asyncExecutor")
    public void sendOrderConfirmation(String email, String orderNumber) {}

    @Override
    @Async("asyncExecutor")
    public void sendOrderStatusUpdate(String email, String orderNumber, String newStatus) {}

    // =======================================================================
    // HÀM 1: GỬI MAIL KHI KHÁCH ĐẶT HÀNG THÀNH CÔNG
    // =======================================================================
    @Override
    @Async
    @Transactional(readOnly = true) // Kích hoạt kết nối Database mới cho luồng này
    public void sendOrderConfirmationEmail(String orderNumber) {
        try {
            // Tự động query lại dữ liệu mới nhất, miễn nhiễm lỗi Proxy
            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
            if (order == null || order.getCustomer() == null || order.getCustomer().getUser() == null) return;

            // Ép Hibernate tải danh sách sản phẩm để in ra mail
            if (order.getItems() != null) {
                order.getItems().size();
            }

            String toEmail = order.getCustomer().getUser().getEmail();
            if (toEmail == null || toEmail.isBlank()) return;

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("order", order);

            String htmlContent = templateEngine.process("email/order-confirm", context);

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đặt hàng thành công! Mã đơn #" + order.getOrderNumber() + " - Jewelry Store");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Đã gửi email XÁC NHẬN ĐẶT HÀNG thành công tới {}", toEmail);

        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận đặt hàng: {}", e.getMessage(), e);
        }
    }

    // =======================================================================
    // HÀM 2: GỬI MAIL KHI ADMIN CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    // =======================================================================
    @Override
    @Async
    @Transactional(readOnly = true)
    public void sendOrderStatusUpdateEmail(String orderNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
            if (order == null || order.getCustomer() == null || order.getCustomer().getUser() == null) return;

            String toEmail = order.getCustomer().getUser().getEmail();
            if (toEmail == null || toEmail.isBlank()) return;

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("order", order);

            String htmlContent = templateEngine.process("email/order-status", context);

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("Cập nhật trạng thái đơn hàng #" + order.getOrderNumber() + " - Jewelry Store");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Đã gửi email CẬP NHẬT TRẠNG THÁI thành công tới {}", toEmail);

        } catch (Exception e) {
            log.error("Lỗi khi gửi email cập nhật trạng thái: {}", e.getMessage(), e);
        }
    }
}