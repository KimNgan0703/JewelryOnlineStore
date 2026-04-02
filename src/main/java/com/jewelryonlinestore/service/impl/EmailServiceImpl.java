package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.NewsletterSubscriber;
import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.repository.NewsletterSubscriberRepository;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final OrderRepository orderRepository;
    private final NewsletterSubscriberRepository subscriberRepository;

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
    @Async // Chạy ngầm để không làm lag trang web khi gửi nhiều email
    public void sendNewCollectionNotification(String collectionName, String collectionSlug) {
        List<NewsletterSubscriber> subscribers = subscriberRepository.findAll();
        if (subscribers.isEmpty()) return;

        String subject = "✨ [Jewelry Store] Ra Mắt Bộ Sưu Tập Mới: " + collectionName;
        // Đổi localhost:8080 thành tên miền thật của bạn khi đưa lên mạng
        String collectionLink = "http://localhost:8080/collections/" + collectionSlug;

        String htmlContent = "<div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;'>" +
                "<h2 style='color: #d4af37; text-align: center;'>Ra Mắt Bộ Sưu Tập Mới!</h2>" +
                "<p>Chào bạn,</p>" +
                "<p>Chúng tôi vô cùng hào hứng thông báo bộ sưu tập mới nhất mang tên <strong>" + collectionName + "</strong> đã chính thức có mặt tại trang chủ.</p>" +
                "<p>Lấy cảm hứng từ sự thanh khiết và rực rỡ, mỗi sản phẩm là một kiệt tác giúp bạn tỏa sáng. Hãy là một trong những người đầu tiên khám phá những thiết kế độc bản này.</p>" +
                "<div style='text-align: center; margin: 35px 0;'>" +
                "<a href='" + collectionLink + "' style='background-color: #d4af37; color: #fff; padding: 14px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px;'>Khám Phá Ngay</a>" +
                "</div>" +
                "<p style='font-size: 0.9em; color: #7f8c8d; text-align: center;'>Cảm ơn bạn đã đồng hành cùng Jewelry Store.</p>" +
                "</div>";

        for (NewsletterSubscriber subscriber : subscribers) {
            try {
                // Sửa mailSender thành javaMailSender
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(senderEmail); // Thêm dòng này để tránh lỗi từ chối gửi từ server
                helper.setTo(subscriber.getEmail());
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                // Sửa mailSender thành javaMailSender
                javaMailSender.send(message);
            } catch (Exception e) {
                // In lỗi nhưng không làm dừng vòng lặp gửi cho người khác
                log.error("Lỗi khi gửi email cho: {} - Chi tiết: {}", subscriber.getEmail(), e.getMessage());
            }
        }
    }
}