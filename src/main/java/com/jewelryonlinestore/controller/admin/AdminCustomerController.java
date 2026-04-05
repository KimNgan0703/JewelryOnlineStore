package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final CustomerService customerService;

    @GetMapping
    public String customerList(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        var customers = customerService.searchCustomers(keyword, status, page, 15);
        model.addAttribute("customers", customers.getContent());
        model.addAttribute("currentPage", customers.getNumber());
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("totalItems", customers.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeStatus", status);
        model.addAttribute("pageTitle", "Quản Lý Khách Hàng");
        return "admin/customers"; // Ánh xạ tới customers.html
    }

    @GetMapping("/{id}")
    public String customerDetail(@PathVariable Long id, Model model) {
        model.addAttribute("customers", customerService.searchCustomers("", null, 0, 15).getContent());
        model.addAttribute("customer", customerService.getCustomerDetail(id));
        model.addAttribute("orders", customerService.getCustomerOrders(id, 0, 10));
        model.addAttribute("isDetail", true);
        model.addAttribute("pageTitle", "Chi Tiết Khách Hàng");
        return "admin/customers"; // Cùng file customers.html
    }

    @PatchMapping("/{id}/toggle-lock")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> toggleLock(@PathVariable Long id) {
        String newStatus = customerService.toggleLock(id);
        String msg = newStatus.equals("LOCKED") ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
        return ResponseEntity.ok(ApiResponse.ok(msg, newStatus));
    }
}