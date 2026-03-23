package com.jewelryonlinestore.controller.admin;


import com.jewelryonlinestore.dto.request.UpdateOrderStatusRequest;
import com.jewelryonlinestore.dto.response.*;
import com.jewelryonlinestore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

/**
 * A05 — Quản lý đơn hàng: xem, cập nhật trạng thái, hủy.
 */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String orderList(@RequestParam(defaultValue = "")  String keyword,
                            @RequestParam(required = false)   String status,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        var orders = orderService.adminSearchOrders(keyword, status, from, to, page, 15);
        model.addAttribute("orders",       orders.getContent());
        model.addAttribute("currentPage",  orders.getNumber());
        model.addAttribute("totalPages",   orders.getTotalPages());
        model.addAttribute("totalItems",   orders.getTotalElements());
        model.addAttribute("keyword",      keyword);
        model.addAttribute("activeStatus", status);
        // Đếm theo từng trạng thái để hiện badge
        model.addAttribute("countPending",    orderService.countByStatus("pending"));
        model.addAttribute("countProcessing", orderService.countByStatus("processing"));
        model.addAttribute("countShipping",   orderService.countByStatus("shipping"));
        model.addAttribute("pageTitle",    "Quản Lý Đơn Hàng");
        return "admin/orders";
    }

    @GetMapping("/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber, Model model) {
        model.addAttribute("order",                   orderService.getOrderDetailAdmin(orderNumber));
        model.addAttribute("updateOrderStatusRequest", new UpdateOrderStatusRequest());
        model.addAttribute("pageTitle",               "Chi Tiết Đơn #" + orderNumber);
        return "admin/order-detail";
    }

    // ── Cập nhật trạng thái (AJAX) ────────────────────────
    @PostMapping("/{orderNumber}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            Authentication auth) {
        OrderDetailResponse order = orderService.updateOrderStatus(orderNumber, req, auth);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công!", order));
    }
}

