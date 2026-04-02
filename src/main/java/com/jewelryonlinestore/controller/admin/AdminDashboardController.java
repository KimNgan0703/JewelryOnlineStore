package com.jewelryonlinestore.controller.admin;

<<<<<<< Updated upstream
import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.OrderRepository;
=======
import com.jewelryonlinestore.dto.response.DashboardResponse;
import com.jewelryonlinestore.service.DashboardService;
>>>>>>> Stashed changes
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping({"/admin", "/admin/dashboard"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

<<<<<<< Updated upstream
    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "month") String period, Model model) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end = now;
        String periodLabel = "";

        // Xác định mốc thời gian dựa trên Dropdown
        switch (period) {
            case "day":
                start = now.with(LocalTime.MIN);
                periodLabel = "Hôm nay";
                break;
            case "week":
                start = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).with(LocalTime.MIN);
                periodLabel = "Tuần này";
                break;
            case "year":
                start = now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
                periodLabel = "Năm nay";
                break;
            case "month":
            default:
                period = "month";
                start = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
                periodLabel = "Tháng này";
                break;
        }

        // 1. Thống kê tổng quan THEO KỲ
        BigDecimal totalSales = orderRepository.sumRevenueBetween(start, end);
        model.addAttribute("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        model.addAttribute("totalOrders", orderRepository.countByCreatedAtBetween(start, end));
        model.addAttribute("totalCustomers", customerRepository.countByCreatedAtBetween(start, end));
        model.addAttribute("pendingOrders", orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.PENDING, start, end));

        model.addAttribute("period", period);
        model.addAttribute("periodLabel", periodLabel);

        // 2. Lấy 5 đơn hàng mới nhất
        var recentOrders = orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        model.addAttribute("recentOrders", recentOrders);

        // 3. Biểu đồ tròn: Trạng thái đơn hàng TRONG KỲ
        long completed  = orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.DELIVERED, start, end);
        long processing = orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.PROCESSING, start, end) +
                orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.PREPARING, start, end);
        long pending    = orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.PENDING, start, end);
        long shipping   = orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.SHIPPING, start, end);
        long cancelled  = orderRepository.countByOrderStatusAndCreatedAtBetween(Order.OrderStatus.CANCELLED, start, end);

        model.addAttribute("chartStatusData", List.of(completed, processing, pending, shipping, cancelled));

        // 4. Biểu đồ cột: Doanh thu linh hoạt
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        if ("year".equals(period)) {
            // Nếu là Năm -> Hiện 12 tháng
            for (int i = 1; i <= 12; i++) {
                labels.add("T" + i);
                LocalDateTime s = now.withMonth(i).with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
                LocalDateTime e = now.withMonth(i).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
                if (s.isAfter(now)) {
                    data.add(BigDecimal.ZERO);
                } else {
                    BigDecimal rev = orderRepository.sumRevenueBetween(s, e);
                    data.add(rev != null ? rev : BigDecimal.ZERO);
                }
            }
            model.addAttribute("chartTitle", "Doanh thu 12 Tháng");
        } else {
            // Mặc định -> Hiện 7 ngày gần nhất
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                labels.add(date.format(DateTimeFormatter.ofPattern("dd/MM")));
                BigDecimal dailyRev = orderRepository.sumRevenueBetween(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()
                );
                data.add(dailyRev != null ? dailyRev : BigDecimal.ZERO);
            }
            model.addAttribute("chartTitle", "Doanh thu 7 ngày gần nhất");
        }

        model.addAttribute("chartSalesLabels", labels);
        model.addAttribute("chartSalesData", data);
=======
    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        DashboardResponse data = dashboardService.getDashboard(from, to);
        model.addAttribute("dashboard", data);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
>>>>>>> Stashed changes
        model.addAttribute("pageTitle", "Dashboard");
        return "admin/dashboard"; // Ánh xạ tới dashboard.html
    }
}