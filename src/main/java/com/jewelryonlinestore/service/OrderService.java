package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.PlaceOrderRequest;
import com.jewelryonlinestore.dto.request.UpdateOrderStatusRequest;
import com.jewelryonlinestore.dto.response.OrderDetailResponse;
import com.jewelryonlinestore.dto.response.OrderSummaryResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

public interface OrderService {
    OrderDetailResponse  placeOrder(PlaceOrderRequest req, Authentication auth, HttpSession session);
    OrderDetailResponse  getOrderDetail(String orderNumber, Authentication auth);
    OrderDetailResponse  getOrderDetailAdmin(String orderNumber);
    Page<OrderSummaryResponse> getMyOrders(Authentication auth, String status, int page, int size);
    boolean              cancelOrder(String orderNumber, String reason, Authentication auth);
    int                  reorder(String orderNumber, Authentication auth, HttpSession session);

    // Admin
    Page<OrderSummaryResponse> adminSearchOrders(String keyword, String status,
                                                 LocalDateTime from, LocalDateTime to,
                                                 int page, int size);
    OrderDetailResponse  updateOrderStatus(String orderNumber,
                                           UpdateOrderStatusRequest req, Authentication auth);

    // HÀM MỚI ĐƯỢC THÊM VÀO ĐỂ FIX LỖI DÒNG 261
    OrderDetailResponse  markAsPaid(String orderNumber, Authentication auth);

    long                 countByStatus(String status);

    // Khách hàng xác nhận đã nhận hàng
    OrderDetailResponse markAsDelivered(String orderNumber, Authentication auth);
}
