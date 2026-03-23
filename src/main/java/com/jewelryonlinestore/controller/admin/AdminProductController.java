package com.jewelryonlinestore.controller.admin;

import com.jewelryonlinestore.dto.request.AdminProductRequest;
import com.jewelryonlinestore.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A03 — Quản lý sản phẩm, danh mục, thương hiệu, chất liệu.
 */
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService  productService;
    private final CategoryService categoryService;

    // ── Danh sách sản phẩm ───────────────────────────────
    @GetMapping
    public String productList(@RequestParam(defaultValue = "")  String keyword,
                              @RequestParam(required = false)   Long categoryId,
                              @RequestParam(required = false)   Boolean isActive,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        var products = productService.adminSearchProducts(keyword, categoryId, isActive, page, 15);
        model.addAttribute("products",    products.getContent());
        model.addAttribute("currentPage", products.getNumber());
        model.addAttribute("totalPages",  products.getTotalPages());
        model.addAttribute("totalItems",  products.getTotalElements());
        model.addAttribute("keyword",     keyword);
        model.addAttribute("categories",  categoryService.getAllCategories());
        model.addAttribute("pageTitle",   "Quản Lý Sản Phẩm");
        return "admin/products";
    }

    // ── Form thêm mới ─────────────────────────────────────
    @GetMapping("/new")
    public String newProductForm(Model model) {
        model.addAttribute("productRequest", new AdminProductRequest());
        model.addAttribute("categories",     categoryService.getAllCategories());
        model.addAttribute("brands",         productService.getAllBrands());
        model.addAttribute("collections",    productService.getAllCollections());
        model.addAttribute("materials",      productService.getAllMaterials());
        model.addAttribute("pageTitle",      "Thêm Sản Phẩm Mới");
        model.addAttribute("isEdit",         false);
        return "admin/product-form";
    }

    // ── Tạo sản phẩm ─────────────────────────────────────
    @PostMapping
    public String createProduct(@Valid @ModelAttribute AdminProductRequest req,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            populateFormModel(model);
            model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
            return "admin/product-form";
        }
        try {
            Long id = productService.createProduct(req);
            redirectAttr.addFlashAttribute("toast_success", "Đã thêm sản phẩm thành công!");
            return "redirect:/admin/products/" + id + "/edit";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model);
            return "admin/product-form";
        }
    }

    // ── Form sửa ─────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productRequest", productService.getProductForEdit(id));
        model.addAttribute("product",        productService.getProductById(id));
        populateFormModel(model);
        model.addAttribute("pageTitle", "Sửa Sản Phẩm");
        model.addAttribute("isEdit",    true);
        return "admin/product-form";
    }

    // ── Cập nhật ─────────────────────────────────────────
    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute AdminProductRequest req,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            populateFormModel(model);
            model.addAttribute("isEdit", true);
            return "admin/product-form";
        }
        productService.updateProduct(id, req);
        redirectAttr.addFlashAttribute("toast_success", "Đã cập nhật sản phẩm!");
        return "redirect:/admin/products/" + id + "/edit";
    }

    // ── Toggle active (AJAX) ──────────────────────────────
    @PatchMapping("/{id}/toggle-active")
    @ResponseBody
    public ResponseEntity<com.jewelryonlinestore.dto.response.ApiResponse<Boolean>> toggleActive(
            @PathVariable Long id) {
        boolean active = productService.toggleActive(id);
        return ResponseEntity.ok(com.jewelryonlinestore.dto.response.ApiResponse.ok(
                active ? "Đã hiện sản phẩm" : "Đã ẩn sản phẩm", active));
    }

    // ── Xóa (soft delete / cứng) ─────────────────────────
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<com.jewelryonlinestore.dto.response.ApiResponse<Void>> deleteProduct(
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(com.jewelryonlinestore.dto.response.ApiResponse.ok("Đã xóa sản phẩm", null));
    }

    // ── Danh mục (tab) ────────────────────────────────────
    @GetMapping("/categories")
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.getAllCategoriesTree());
        model.addAttribute("pageTitle",  "Quản Lý Danh Mục");
        return "admin/products";
    }

    private void populateFormModel(Model model) {
        model.addAttribute("categories",  categoryService.getAllCategories());
        model.addAttribute("brands",      productService.getAllBrands());
        model.addAttribute("collections", productService.getAllCollections());
        model.addAttribute("materials",   productService.getAllMaterials());
    }
}
