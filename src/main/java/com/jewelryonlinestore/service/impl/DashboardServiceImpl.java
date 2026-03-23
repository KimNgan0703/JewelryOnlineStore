package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.response.DashboardResponse;
import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.entity.Review;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.repository.ProductVariantRepository;
import com.jewelryonlinestore.repository.ReviewRepository;
import com.jewelryonlinestore.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek  = now.toLocalDate()
                .minusDays(now.getDayOfWeek().getValue() - 1L)
                .atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        BigDecimal today = orderRepository.sumRevenueBetween(startOfToday, now);
        BigDecimal week  = orderRepository.sumRevenueBetween(startOfWeek,  now);
        BigDecimal month = orderRepository.sumRevenueBetween(startOfMonth, now);

        return DashboardResponse.builder()
                .revenueToday(today)
                .revenueThisWeek(week)
                .revenueThisMonth(month)
                .ordersToday(0)
                .ordersThisMonth(0)
                .newCustomersThisMonth(0)
                .totalProductsSold(0)
                // ✅ Truyền enum thay vì String
                .pendingOrders(   (int) orderRepository.countByOrderStatus(Order.OrderStatus.PENDING))
                .processingOrders((int) orderRepository.countByOrderStatus(Order.OrderStatus.PROCESSING))
                .shippingOrders(  (int) orderRepository.countByOrderStatus(Order.OrderStatus.SHIPPING))
                .lowStockProductCount(productVariantRepository.findLowStockVariants().size())
                // ✅ Truyền enum thay vì String
                .pendingReviewCount((int) reviewRepository.countByStatus(Review.ReviewStatus.PENDING))
                .revenueChart(Collections.emptyMap())
                .topProducts(Collections.emptyList())
                .topCustomers(Collections.emptyList())
                .build();
    }
}