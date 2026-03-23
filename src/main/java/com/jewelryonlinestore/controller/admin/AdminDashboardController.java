package com.jewelryonlinestore.controller.admin;


import com.jewelryonlinestore.dto.response.DashboardResponse;
import com.jewelryonlinestore.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/**
 * A02 — Dashboard & Báo cáo doanh thu, KPI.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        DashboardResponse data = dashboardService.getDashboard(from, to);
        model.addAttribute("dashboard", data);
        model.addAttribute("from",      from);
        model.addAttribute("to",        to);
        model.addAttribute("pageTitle", "Dashboard");
        return "admin/dashboard";
    }
}

