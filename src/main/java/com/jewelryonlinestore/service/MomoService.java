package com.jewelryonlinestore.service;

// ĐÃ XÓA 2 DÒNG IMPORT JACKSON BỊ LỖI MÀU ĐỎ

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MomoService {

    @Value("${momo.endpoint}")
    private String endpoint;
    @Value("${momo.refund-endpoint:https://payment.momo.vn/v2/gateway/api/refund}")
    private String refundEndpoint;
    @Value("${momo.partner-code}")
    private String partnerCode;
    @Value("${momo.access-key}")
    private String accessKey;
    @Value("${momo.secret-key}")
    private String secretKey;
    @Value("${momo.redirect-url}")
    private String redirectUrl;
    @Value("${momo.ipn-url}")
    private String ipnUrl;

    public String createMomoPaymentUrl(String orderId, String totalAmount, String orderInfo) throws Exception {
        String requestId = orderId + "_" + System.currentTimeMillis();
        // MoMo requires orderId to be unique per transaction attempt.
        // We append timestamp here so retries will have unique orderId.
        String uniqueOrderId = orderId + "_" + System.currentTimeMillis();
        String requestType = "payWithATM";
        String extraData = orderId; // save original orderId in extraData

        // 1. Tạo chuỗi raw data để ký (Đúng thứ tự alphabet theo tài liệu MoMo)
        String rawHash = "accessKey=" + accessKey +
                "&amount=" + totalAmount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + uniqueOrderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        // 2. Ký bằng thuật toán HMAC-SHA256
        String signature = MomoEncoderUtils.signHmacSHA256(rawHash, secretKey);

        // 3. Tạo body JSON gửi lên MoMo
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "Jewelry Store");
        requestBody.put("storeId", "MomoTestStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", totalAmount);
        requestBody.put("orderId", uniqueOrderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        // 4. Bắn API sang MoMo bằng RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        // ĐÃ SỬA: Ép kiểu thẳng kết quả trả về sang Map, KHÔNG CẦN DÙNG JACKSON
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);

        // 5. Lấy link payUrl
        if (response != null && response.containsKey("payUrl")) {
            return response.get("payUrl").toString();
        } else {
            throw new RuntimeException("Lỗi tạo thanh toán MoMo: " + response);
        }
    }

    public RefundResult refund(String orderId, long amount, String transId, String description) throws Exception {
        String requestId = orderId + "_refund_" + System.currentTimeMillis();
        String rawHash = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&description=" + description
                + "&orderId=" + orderId
                + "&partnerCode=" + partnerCode
                + "&requestId=" + requestId
                + "&transId=" + transId;

        String signature = MomoEncoderUtils.signHmacSHA256(rawHash, secretKey);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("orderId", orderId);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", String.valueOf(amount));
        requestBody.put("transId", transId);
        requestBody.put("lang", "vi");
        requestBody.put("description", description);
        requestBody.put("signature", signature);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(refundEndpoint, entity, Map.class);
        String resultCode = response == null ? null : String.valueOf(response.get("resultCode"));
        boolean success = "0".equals(resultCode);
        String message = response == null ? "Không nhận được phản hồi từ MoMo"
                : String.valueOf(response.get("message"));
        String refundTransId = response == null || response.get("transId") == null
                ? null
                : String.valueOf(response.get("transId"));

        return new RefundResult(success, resultCode, message, refundTransId, response);
    }

    public record RefundResult(
            boolean success,
            String resultCode,
            String message,
            String refundTransId,
            Map<String, Object> rawResponse) {
    }
}