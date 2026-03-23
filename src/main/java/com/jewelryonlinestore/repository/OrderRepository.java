package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm theo mã đơn hàng (C07, A05)
    Optional<Order> findByOrderNumber(String orderNumber);

    // Đơn hàng của khách theo trạng thái (C07)
    Page<Order> findByCustomerIdAndOrderStatusInOrderByCreatedAtDesc(
            Long customerId, List<String> statuses, Pageable pageable
    );

    // Tất cả đơn hàng của khách (C07)
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    // Admin: tìm kiếm đơn hàng (A05)
    @Query("""
        SELECT o FROM Order o
        JOIN o.customer c
        WHERE (:keyword IS NULL OR
               LOWER(o.orderNumber)  LIKE LOWER(CONCAT('%',:keyword,'%')) OR
               LOWER(c.fullName)     LIKE LOWER(CONCAT('%',:keyword,'%')))
          AND (:status IS NULL OR o.orderStatus = :status)
          AND (:from IS NULL OR o.createdAt >= :from)
          AND (:to   IS NULL OR o.createdAt <= :to)
        ORDER BY o.createdAt DESC
    """)
    Page<Order> searchOrders(
            @Param("keyword") String keyword,
            @Param("status")  String status,
            @Param("from")    LocalDateTime from,
            @Param("to")      LocalDateTime to,
            Pageable pageable
    );

    // Dashboard: doanh thu theo khoảng thời gian (A02)
    @Query("""
        SELECT COALESCE(SUM(o.total), 0) FROM Order o
        WHERE o.orderStatus = 'delivered'
          AND o.createdAt BETWEEN :from AND :to
    """)
    BigDecimal sumRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    // Dashboard: doanh thu từng ngày trong tháng (cho biểu đồ A02)
    @Query(value = """
        SELECT DATE(created_at) as day, SUM(total) as revenue
        FROM orders
        WHERE order_status = 'delivered'
          AND MONTH(created_at) = :month
          AND YEAR(created_at)  = :year
        GROUP BY DATE(created_at)
        ORDER BY day
    """, nativeQuery = true)
    List<Object[]> findDailyRevenue(@Param("month") int month, @Param("year") int year);

    // Đếm đơn hàng theo trạng thái (dashboard widget)
    long countByOrderStatus(Order.OrderStatus status);

    // Kiểm tra customer có đơn hàng đã giao để cho phép review (C08)
    @Query("""
        SELECT COUNT(o) > 0 FROM Order o
        WHERE o.customer.id = :customerId
          AND o.orderStatus  = 'delivered'
    """)
    boolean hasDeliveredOrder(@Param("customerId") Long customerId);
}
