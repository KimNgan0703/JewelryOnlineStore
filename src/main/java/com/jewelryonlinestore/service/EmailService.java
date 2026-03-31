package com.jewelryonlinestore.service;

/**
 * Interface định nghĩa các dịch vụ gửi Email của hệ thống.
 */
public interface EmailService {

    // Gửi mail xác minh khi đăng ký tài khoản
    void sendVerificationEmail(String toEmail, String token);

    // Gửi mail hướng dẫn đặt lại mật khẩu
    void sendPasswordResetEmail(String toEmail, String token);

    // Gửi mail xác nhận ngay khi khách hàng đặt hàng thành công
    void sendOrderConfirmationEmail(String orderNumber);

    // Gửi mail thông báo khi trạng thái đơn hàng thay đổi (Đang giao, Đã giao, Hủy...)
    void sendOrderStatusUpdateEmail(String orderNumber);
}