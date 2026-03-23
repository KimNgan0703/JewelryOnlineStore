package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    @Async("asyncExecutor")
    public void sendVerificationEmail(String email, String token) {
        log.info("Send verification email to {} with token {}", email, token);
    }

    @Override
    @Async("asyncExecutor")
    public void sendPasswordResetEmail(String email, String token) {
        log.info("Send password reset email to {} with token {}", email, token);
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

