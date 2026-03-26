package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.request.AdminProductRequest;
import com.jewelryonlinestore.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Map;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @Value("${cloudinary.cloud-name}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.upload-preset}")
    private String cloudinaryUploadPreset;

    @GetMapping
    public String productList(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) Boolean isActive,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        var products = productService.adminSearchProducts(keyword, categoryId, isActive, page, 15);
        model.addAttribute("products", products.getContent());
        model.addAttribute("currentPage", products.getNumber());
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalItems", products.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Quản Lý Sản Phẩm");
        return "admin/products";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        if (!model.containsAttribute("productRequest")) {
            model.addAttribute("productRequest", new AdminProductRequest());
        }
        populateFormModel(model);
        model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
        model.addAttribute("isEdit", false);
        return "admin/product-form";
    }

    @PostMapping
    public String createProduct(@Valid @ModelAttribute("productRequest") AdminProductRequest req,
                                BindingResult result, Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            populateFormModel(model);
            model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
            model.addAttribute("isEdit", false);
            return "admin/product-form";
        }
        try {
            productService.createProduct(req);
            redirectAttr.addFlashAttribute("toast_success", "Đã thêm sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model);
            model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
            model.addAttribute("isEdit", false);
            return "admin/product-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("productRequest")) {
            model.addAttribute("productRequest", productService.getProductForEdit(id));
        }
        model.addAttribute("product", productService.getProductById(id));
        populateFormModel(model);
        model.addAttribute("pageTitle", "Sửa Sản Phẩm");
        model.addAttribute("isEdit", true);
        return "admin/product-form";
    }

    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productRequest") AdminProductRequest req,
                                BindingResult result, Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            model.addAttribute("product", productService.getProductById(id));
            populateFormModel(model);
            model.addAttribute("pageTitle", "Sửa Sản Phẩm");
            model.addAttribute("isEdit", true);
            return "admin/product-form";
        }
        try {
            productService.updateProduct(id, req);
            redirectAttr.addFlashAttribute("toast_success", "Đã cập nhật sản phẩm!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("product", productService.getProductById(id));
            populateFormModel(model);
            model.addAttribute("pageTitle", "Sửa Sản Phẩm");
            model.addAttribute("isEdit", true);
            return "admin/product-form";
        }
    }

    @PatchMapping("/{id}/toggle-active")
    @ResponseBody
    public ResponseEntity<com.jewelryonlinestore.dto.response.ApiResponse<Boolean>> toggleActive(@PathVariable Long id) {
        boolean active = productService.toggleActive(id);
        return ResponseEntity.ok(com.jewelryonlinestore.dto.response.ApiResponse.ok(active ? "Đã hiện sản phẩm" : "Đã ẩn sản phẩm", active));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<com.jewelryonlinestore.dto.response.ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(com.jewelryonlinestore.dto.response.ApiResponse.ok("Đã xóa sản phẩm", null));
    }

    // ── Trang Quản lý Danh mục ──────────────────────────────
    @GetMapping("/categories")
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Quản Lý Danh Mục");
        return "admin/categories"; // Trả về giao diện categories.html mới
    }

    // ── API Xóa Danh mục (AJAX) ─────────────────────────────
    @DeleteMapping("/categories/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa danh mục thành công!"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private void populateFormModel(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());
        model.addAttribute("collections", productService.getAllCollections());
        model.addAttribute("materials", productService.getAllMaterials());
        model.addAttribute("cloudinaryCloudName", cloudinaryCloudName);
        model.addAttribute("cloudinaryUploadPreset", cloudinaryUploadPreset);
    }

    // =================================================================
    // CÁC API THÊM NHANH (QUICK ADD) DÙNG CHO GIAO DIỆN AJAX
    // =================================================================

    @PostMapping("/quick-add/category")
    @ResponseBody
    public ResponseEntity<?> quickAddCategory(@RequestParam("name") String name) {
        try {
            var newCategory = categoryService.createCategory(name);
            return ResponseEntity.ok(Map.of("id", newCategory.getId(), "name", newCategory.getName()));
        } catch (Exception e) {
            // Đã bỏ chữ "Lỗi: " đi để giao diện hiện thông báo mượt hơn
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/quick-add/brand")
    @ResponseBody
    public ResponseEntity<?> quickAddBrand(@RequestParam("name") String name) {
        try {
            var newBrand = productService.createBrand(name);
            return ResponseEntity.ok(Map.of("id", newBrand.getId(), "name", newBrand.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/quick-add/material")
    @ResponseBody
    public ResponseEntity<?> quickAddMaterial(@RequestParam("name") String name) {
        try {
            var newMaterial = productService.createMaterial(name);
            return ResponseEntity.ok(Map.of("id", newMaterial.getId(), "name", newMaterial.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}