package com.jewelryonlinestore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO Dashboard Admin (A02).
 */
@Data
@Builder
public class DashboardResponse {

    // KPI chính
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;

    private Integer ordersToday;
    private Integer ordersThisMonth;
    private Integer newCustomersThisMonth;
    private Integer totalProductsSold;

    // Đơn hàng theo trạng thái
    private Integer pendingOrders;
    private Integer processingOrders;
    private Integer shippingOrders;

    // Cảnh báo tồn kho thấp
    private Integer lowStockProductCount;

    // Đánh giá chờ duyệt
    private Integer pendingReviewCount;

    // Biểu đồ doanh thu (key = ngày "dd/MM", value = doanh thu)
    private Map<String, BigDecimal> revenueChart;

    // Top sản phẩm bán chạy
    private List<TopProductInfo> topProducts;

    // Top khách hàng
    private List<TopCustomerInfo> topCustomers;

    // ---- Inner classes ----
    @Data
    @Builder
    public static class TopProductInfo {
        private Long productId;
        private String productName;
        private String imageUrl;
        private Integer soldQuantity;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class TopCustomerInfo {
        private Long customerId;
        private String fullName;
        private String email;
        private Integer orderCount;
        private BigDecimal totalSpent;
    }
}
