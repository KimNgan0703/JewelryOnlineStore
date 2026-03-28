package com.jewelryonlinestore.service;

public interface EmailService {
    void sendVerificationEmail(String email, String token);
    void sendPasswordResetEmail(String email, String token);
    void sendOrderConfirmation(String email, String orderNumber);
    void sendOrderStatusUpdate(String email, String orderNumber, String newStatus);

    // Đã sửa tham số thành String orderNumber để tránh lỗi Hibernate Proxy
    void sendOrderConfirmationEmail(String orderNumber);
    void sendOrderStatusUpdateEmail(String orderNumber);
}