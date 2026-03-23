package com.jewelryonlinestore.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import java.util.Map;

public interface PaymentService {
    String createVnpayUrl(String orderNumber, HttpServletRequest request, Authentication auth);
    String handleVnpayCallback(Map<String, String> params);
    String handleMomoCallback(Map<String, String> params);
}
