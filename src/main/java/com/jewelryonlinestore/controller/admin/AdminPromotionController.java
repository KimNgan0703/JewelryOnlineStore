package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.request.PromotionRequest;
import com.jewelryonlinestore.service.CategoryService;
import com.jewelryonlinestore.service.ProductService;
import com.jewelryonlinestore.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

<<<<<<< Updated upstream
import java.util.Map;

=======
>>>>>>> Stashed changes
@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
<<<<<<< Updated upstream
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(required = false) String keyword,
                                 // Đổi Boolean isActive → String status
                                 // Các giá trị hợp lệ: ACTIVE, INACTIVE, EXPIRED, UPCOMING (hoặc null = tất cả)
                                 @RequestParam(required = false) String status,
                                 Model model) {
        var promotions = promotionService.searchPromotions(keyword, status, page, 10);
        model.addAttribute("promotions", promotions.getContent());
        model.addAttribute("currentPage", promotions.getNumber());
        model.addAttribute("totalPages", promotions.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
=======
    public String promotionList(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(required = false) Boolean isActive,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        var promotions = promotionService.searchPromotions(keyword, isActive, page, 15);
        model.addAttribute("promotions", promotions.getContent());
        model.addAttribute("currentPage", promotions.getNumber());
        model.addAttribute("totalPages", promotions.getTotalPages());
>>>>>>> Stashed changes
        model.addAttribute("pageTitle", "Quản Lý Khuyến Mãi");
        return "admin/promotions";
    }

    @GetMapping("/new")
    public String newPromotionForm(Model model) {
        model.addAttribute("promotionRequest", new PromotionRequest());
        model.addAttribute("isEdit", false);
<<<<<<< Updated upstream
        model.addAttribute("pageTitle", "Thêm Mã Khuyến Mãi");
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("products", productService.getAllProducts());
=======
        model.addAttribute("pageTitle", "Tạo Mã Khuyến Mãi");
        // FIX: Đổi đường dẫn trả về đúng tên file HTML của form
>>>>>>> Stashed changes
        return "admin/promotion-form";
    }

    @PostMapping
<<<<<<< Updated upstream
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
=======
    public String create(@Valid @ModelAttribute("promotionRequest") PromotionRequest req,
                         BindingResult result, Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Tạo Mã Khuyến Mãi");
            // FIX: Khi lỗi validate, trả lại đúng trang form để hiển thị thông báo lỗi đỏ
>>>>>>> Stashed changes
            return "admin/promotion-form";
        }
        return "redirect:/admin/promotions";
    }

    @GetMapping("/{id}/edit")
<<<<<<< Updated upstream
    public String editPromotionForm(@PathVariable Long id, Model model) {
=======
    public String editForm(@PathVariable Long id, Model model) {
>>>>>>> Stashed changes
        model.addAttribute("promotionRequest", promotionService.getPromotionForEdit(id));
        model.addAttribute("id", id);
        model.addAttribute("isEdit", true);
<<<<<<< Updated upstream
        model.addAttribute("pageTitle", "Chỉnh Sửa Mã Khuyến Mãi");
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("products", productService.getAllProducts());
=======
        // FIX: Thêm biến promoId để giao diện biết đang sửa mã nào
        model.addAttribute("promoId", id);
        model.addAttribute("pageTitle", "Sửa Khuyến Mãi");
>>>>>>> Stashed changes
        return "admin/promotion-form";
    }

    @PostMapping("/{id}")
<<<<<<< Updated upstream
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
=======
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("promotionRequest") PromotionRequest req,
                         BindingResult result, Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            // FIX: Cấp lại promoId nếu form bị lỗi nhập liệu để không bị mất đường dẫn
            model.addAttribute("promoId", id);
            model.addAttribute("pageTitle", "Sửa Khuyến Mãi");
>>>>>>> Stashed changes
            return "admin/promotion-form";
        }
        return "redirect:/admin/promotions";
    }

    @PatchMapping("/{id}/toggle")
    @ResponseBody
    public ResponseEntity<?> togglePromotion(@PathVariable Long id) {
        boolean activeStatus = promotionService.toggleActive(id);
        return ResponseEntity.ok(Map.of("isActive", activeStatus, "message", "Đã cập nhật trạng thái!"));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
<<<<<<< Updated upstream
    public ResponseEntity<?> deletePromotion(@PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa mã khuyến mãi thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
=======
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(ApiResponse.ok("Đã xóa khuyến mãi thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể xóa: " + e.getMessage()));
>>>>>>> Stashed changes
        }
    }
}