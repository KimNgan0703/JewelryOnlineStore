package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.request.PromotionRequest;
import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public String promotionList(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(required = false) Boolean isActive,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        var promotions = promotionService.searchPromotions(keyword, isActive, page, 15);
        model.addAttribute("promotions", promotions.getContent());
        model.addAttribute("currentPage", promotions.getNumber());
        model.addAttribute("totalPages", promotions.getTotalPages());
        model.addAttribute("pageTitle", "Quản Lý Khuyến Mãi");
        return "admin/promotions";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("promotionRequest", new PromotionRequest());
        model.addAttribute("isEdit", false);
        model.addAttribute("pageTitle", "Tạo Mã Khuyến Mãi");
        // FIX: Đổi đường dẫn trả về đúng tên file HTML của form
        return "admin/promotion-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("promotionRequest") PromotionRequest req,
                         BindingResult result, Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Tạo Mã Khuyến Mãi");
            // FIX: Khi lỗi validate, trả lại đúng trang form để hiển thị thông báo lỗi đỏ
            return "admin/promotion-form";
        }
        promotionService.createPromotion(req);
        redirectAttr.addFlashAttribute("toast_success", "Tạo khuyến mãi thành công!");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("promotionRequest", promotionService.getPromotionForEdit(id));
        model.addAttribute("isEdit", true);
        // FIX: Thêm biến promoId để giao diện biết đang sửa mã nào
        model.addAttribute("promoId", id);
        model.addAttribute("pageTitle", "Sửa Khuyến Mãi");
        return "admin/promotion-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("promotionRequest") PromotionRequest req,
                         BindingResult result, Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            // FIX: Cấp lại promoId nếu form bị lỗi nhập liệu để không bị mất đường dẫn
            model.addAttribute("promoId", id);
            model.addAttribute("pageTitle", "Sửa Khuyến Mãi");
            return "admin/promotion-form";
        }
        promotionService.updatePromotion(id, req);
        redirectAttr.addFlashAttribute("toast_success", "Cập nhật thành công!");
        return "redirect:/admin/promotions";
    }

    @PatchMapping("/{id}/toggle")
    @ResponseBody
    public ResponseEntity<ApiResponse<Boolean>> toggle(@PathVariable Long id) {
        boolean active = promotionService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok(active ? "Đã bật" : "Đã tắt", active));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(ApiResponse.ok("Đã xóa khuyến mãi thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể xóa: " + e.getMessage()));
        }
    }
}