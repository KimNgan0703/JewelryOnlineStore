package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.service.EmailService;
import jakarta.mail.MessagingException;
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
    private final OrderRepository orderRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // URL gốc của website, thay đổi thành domain thật khi deploy
    private final String baseUrl = "http://localhost:8080";

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            Context context = new Context();
            context.setVariable("verifyUrl", baseUrl + "/auth/verify?token=" + token);

            sendMimeMessage(toEmail, "Xác minh tài khoản Jewelry Store", "email/verify-account", context);
            log.info("Đã gửi email xác minh tài khoản tới {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email xác minh tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            Context context = new Context();
            context.setVariable("resetUrl", baseUrl + "/auth/reset-password?token=" + token);

            sendMimeMessage(toEmail, "Đặt lại mật khẩu Jewelry Store", "email/reset-password", context);
            log.info("Đã gửi email đặt lại mật khẩu tới {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email đặt lại mật khẩu tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void sendOrderConfirmationEmail(String orderNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
            if (order == null || order.getCustomer() == null) return;

            // Ép Hibernate tải danh sách items để tránh lỗi LazyInit trong Template
            if (order.getItems() != null) order.getItems().size();

            Context context = new Context();
            context.setVariable("order", order);

            String subject = "Đặt hàng thành công! Mã đơn #" + order.getOrderNumber();
            sendMimeMessage(order.getCustomer().getUser().getEmail(), subject, "email/order-confirm", context);

            log.info("Đã gửi email xác nhận đơn hàng #{} thành công", orderNumber);
        } catch (Exception e) {
            log.error("Lỗi gửi email xác nhận đơn hàng {}: {}", orderNumber, e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void sendOrderStatusUpdateEmail(String orderNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
            if (order == null || order.getCustomer() == null) return;

            Context context = new Context();
            context.setVariable("order", order);

            String subject = "Cập nhật trạng thái đơn hàng #" + order.getOrderNumber();
            sendMimeMessage(order.getCustomer().getUser().getEmail(), subject, "email/order-status", context);

            log.info("Đã gửi email cập nhật trạng thái đơn hàng #{} thành công", orderNumber);
        } catch (Exception e) {
            log.error("Lỗi gửi email cập nhật trạng thái {}: {}", orderNumber, e.getMessage());
        }
    }

    /**
     * Hàm hỗ trợ gửi Email định dạng HTML
     */
    private void sendMimeMessage(String to, String subject, String templateName, Context context) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String htmlContent = templateEngine.process(templateName, context);

        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(subject + " - Jewelry Store");
        helper.setText(htmlContent, true);

        javaMailSender.send(message);
    }
}