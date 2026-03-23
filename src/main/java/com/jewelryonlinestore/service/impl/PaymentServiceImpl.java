package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Override
    public String createVnpayUrl(String orderNumber, HttpServletRequest request, Authentication auth) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String callback = baseUrl + "/payment/vnpay/callback";
        return callback + "?orderNumber=" + URLEncoder.encode(orderNumber, StandardCharsets.UTF_8);
    }

    @Override
    public String handleVnpayCallback(Map<String, String> params) {
        return extractOrderNumber(params);
    }

    @Override
    public String handleMomoCallback(Map<String, String> params) {
        return extractOrderNumber(params);
    }

    private String extractOrderNumber(Map<String, String> params) {
        String orderNumber = params.get("orderNumber");
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = params.get("vnp_TxnRef");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = params.get("orderId");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number not found in callback params");
        }
        return orderNumber;
    }
}

