package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.request.PromotionRequest;
import com.jewelryonlinestore.service.CategoryService;
import com.jewelryonlinestore.service.ProductService;
import com.jewelryonlinestore.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;

    // TIÊM 2 SERVICE NÀY VÀO ĐỂ LẤY DỮ LIỆU
    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Boolean active,
                                 Model model) {
        var promotions = promotionService.searchPromotions(keyword, active, page, 10);
        model.addAttribute("promotions", promotions.getContent());
        model.addAttribute("currentPage", promotions.getNumber());
        model.addAttribute("totalPages", promotions.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        model.addAttribute("pageTitle", "Quản Lý Khuyến Mãi");
        return "admin/promotions";
    }

    @GetMapping("/new")
    public String newPromotionForm(Model model) {
        model.addAttribute("promotionRequest", new PromotionRequest());
        model.addAttribute("isEdit", false);
        model.addAttribute("pageTitle", "Thêm Mã Khuyến Mãi");

        // NẠP DANH SÁCH CHO DROPDOWN MENU
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("products", productService.getAllProducts());
        return "admin/promotion-form";
    }

    @PostMapping
    public String createPromotion(@Valid @ModelAttribute PromotionRequest req,
                                  BindingResult result, Model model,
                                  RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Thêm Mã Khuyến Mãi");
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("products", productService.getAllProducts());
            return "admin/promotion-form";
        }
        try {
            promotionService.createPromotion(req);
            redirectAttr.addFlashAttribute("toast_success", "Đã tạo mã khuyến mãi thành công!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Thêm Mã Khuyến Mãi");
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("products", productService.getAllProducts());
            return "admin/promotion-form";
        }
        return "redirect:/admin/promotions";
    }

    @GetMapping("/{id}/edit")
    public String editPromotionForm(@PathVariable Long id, Model model) {
        model.addAttribute("promotionRequest", promotionService.getPromotionForEdit(id));
        model.addAttribute("id", id);
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Chỉnh Sửa Mã Khuyến Mãi");

        // NẠP DANH SÁCH CHO DROPDOWN MENU
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("products", productService.getAllProducts());
        return "admin/promotion-form";
    }

    @PostMapping("/{id}")
    public String updatePromotion(@PathVariable Long id,
                                  @Valid @ModelAttribute PromotionRequest req,
                                  BindingResult result, Model model,
                                  RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Chỉnh Sửa Mã Khuyến Mãi");
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("products", productService.getAllProducts());
            return "admin/promotion-form";
        }
        try {
            promotionService.updatePromotion(id, req);
            redirectAttr.addFlashAttribute("toast_success", "Đã cập nhật mã khuyến mãi thành công!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("id", id);
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Chỉnh Sửa Mã Khuyến Mãi");
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("products", productService.getAllProducts());
            return "admin/promotion-form";
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/toggle")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> togglePromotion(@PathVariable Long id) {
        boolean isActive = promotionService.toggleActive(id);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("isActive", isActive, "message", "Đã cập nhật trạng thái!"));
    }

    @PostMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttr) {
        try {
            promotionService.deletePromotion(id);
            redirectAttr.addFlashAttribute("toast_success", "Đã xóa mã khuyến mãi!");
        } catch (Exception e) {
            redirectAttr.addFlashAttribute("toast_error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}