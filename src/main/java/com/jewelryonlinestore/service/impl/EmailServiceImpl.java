package com.jewelryonlinestore.service.impl;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    // Công cụ thực sự làm nhiệm vụ gửi mail của Spring Boot
    private final JavaMailSender javaMailSender;

    // Lấy email gửi đi từ file application.properties
    @Value("${spring.mail.username}")
    private String senderEmail;

    // Thay đổi domain này khi bạn đưa web lên mạng (deploy)
    private final String baseUrl = "http://localhost:8080";

    @Override
    @Async("asyncExecutor")
    public void sendVerificationEmail(String email, String token) {
        log.info("Send verification email to {} with token {}", email, token);
        // Tương tự, bạn có thể viết code gửi mail xác thực ở đây
    }

    @Override
    @Async("asyncExecutor")
    public void sendPasswordResetEmail(String email, String token) {
        log.info("Đang tiến hành gửi thư đặt lại mật khẩu cho {}...", email);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // Tham số 'true' cho phép gửi mail dạng HTML (có hỗ trợ UTF-8)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - Jewelry Online Store");

            // Tạo đường link đặt lại mật khẩu
            String resetUrl = baseUrl + "/auth/reset-password?token=" + token;

            // Nội dung thư bằng HTML
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto;'>"
                    + "<h2 style='color: #d4af37;'>Jewelry Online Store</h2>"
                    + "<h3>Xin chào!</h3>"
                    + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này.</p>"
                    + "<p>Vui lòng nhấn vào nút bên dưới để tạo mật khẩu mới:</p>"
                    + "<a href='" + resetUrl + "' style='display: inline-block; padding: 10px 20px; color: white; background-color: #2c3e50; text-decoration: none; border-radius: 5px;'>Đặt Lại Mật Khẩu</a>"
                    + "<p><br>Hoặc sao chép đường dẫn sau dán vào trình duyệt: <br><i>" + resetUrl + "</i></p>"
                    + "<p style='color: red;'>Lưu ý: Đường dẫn này chỉ có hiệu lực trong vòng 2 giờ.</p>"
                    + "<p>Nếu bạn không yêu cầu thay đổi mật khẩu, vui lòng bỏ qua email này để đảm bảo an toàn.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // true = sử dụng HTML

            // Lệnh gửi mail
            javaMailSender.send(message);
            log.info("Gửi mail thành công tới {}", email);

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email cho {}: {}", email, e.getMessage());
        }
    }

    @Override
    @Async("asyncExecutor")
    public void sendOrderConfirmation(String email, String orderNumber) {
        log.info("Send order confirmation to {} for order {}", email, orderNumber);
    }

    @Override
    @Async("asyncExecutor")
    public void sendOrderStatusUpdate(String email, String orderNumber, String newStatus) {
        log.info("Send order status update to {} for order {} => {}", email, orderNumber, newStatus);
    }
}