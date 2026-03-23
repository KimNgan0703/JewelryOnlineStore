package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.request.InventoryAdjustRequest;
import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * A04 — Quản lý kho: điều chỉnh tồn, xem lịch sử, cảnh báo.
 */
@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public String inventoryList(@RequestParam(defaultValue = "")  String keyword,
                                @RequestParam(defaultValue = "false") boolean lowStockOnly,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        model.addAttribute("variants",    inventoryService.getInventoryList(keyword, lowStockOnly, page, 20));
        model.addAttribute("lowStockCount", inventoryService.getLowStockCount());
        model.addAttribute("keyword",     keyword);
        model.addAttribute("lowStockOnly",lowStockOnly);
        model.addAttribute("pageTitle",   "Quản Lý Kho");
        return "admin/inventory";
    }

    // ── Điều chỉnh tồn kho (AJAX) ────────────────────────
    @PostMapping("/adjust")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> adjust(
            @Valid @RequestBody InventoryAdjustRequest req,
            Authentication auth) {
        inventoryService.adjustStock(req, auth);
        String msg = req.getQuantityChange() > 0
                ? "Nhập kho thành công +" + req.getQuantityChange()
                : "Xuất kho thành công " + req.getQuantityChange();
        return ResponseEntity.ok(ApiResponse.ok(msg, null));
    }

    // ── Lịch sử nhập/xuất của 1 variant (AJAX) ───────────
    @GetMapping("/history/{variantId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> history(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "0") int page) {
        var logs = inventoryService.getVariantHistory(variantId, page, 10);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
