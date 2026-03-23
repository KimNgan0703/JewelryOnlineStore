package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.request.ProductFilterRequest;
import com.jewelryonlinestore.dto.response.ProductCardResponse;
import com.jewelryonlinestore.dto.response.ProductResponse;
import com.jewelryonlinestore.dto.response.ProductReviewSummary;
import com.jewelryonlinestore.service.CategoryService;
import com.jewelryonlinestore.service.ProductService;
import com.jewelryonlinestore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * C03 — Tìm kiếm & lọc sản phẩm
 * C04 — Xem chi tiết sản phẩm
 */
@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService  productService;
    private final CategoryService categoryService;
    private final ReviewService   reviewService;

    // ── Danh sách / Tìm kiếm / Lọc (C03) ───────────────
    @GetMapping("/products")
    public String productList(@ModelAttribute ProductFilterRequest filter, Model model) {
        Page<ProductCardResponse> page = productService.filterProducts(filter);
        model.addAttribute("products",   page.getContent());
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages",  page.getTotalPages());
        model.addAttribute("totalItems",  page.getTotalElements());
        model.addAttribute("filter",      filter);
        model.addAttribute("categories",  categoryService.getRootCategories());
        model.addAttribute("brands",      productService.getAllBrands());
        model.addAttribute("materials",   productService.getAllMaterials());
        model.addAttribute("pageTitle",   filter.getKeyword() != null
                ? "Kết quả: \"" + filter.getKeyword() + "\""
                : "Tất Cả Sản Phẩm");
        return "customer/product-list";
    }

    // ── Sản phẩm theo danh mục ───────────────────────────
    @GetMapping("/categories/{slug}")
    public String productsByCategory(@PathVariable String slug,
                                     @ModelAttribute ProductFilterRequest filter,
                                     Model model) {
        var category = categoryService.getCategoryBySlug(slug);
        filter.setCategoryId(category.getId());
        Page<ProductCardResponse> page = productService.filterProducts(filter);
        model.addAttribute("products",   page.getContent());
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages",  page.getTotalPages());
        model.addAttribute("totalItems",  page.getTotalElements());
        model.addAttribute("filter",      filter);
        model.addAttribute("category",    category);
        model.addAttribute("categories",  categoryService.getRootCategories());
        model.addAttribute("brands",      productService.getAllBrands());
        model.addAttribute("materials",   productService.getAllMaterials());
        model.addAttribute("pageTitle",   category.getName());
        return "customer/product-list";
    }

    // ── Chi tiết sản phẩm (C04) ──────────────────────────
    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        ProductResponse product   = productService.getProductBySlug(slug);
        ProductReviewSummary reviews = reviewService.getReviewSummary(product.getId(), 0, 5);
        model.addAttribute("product",         product);
        model.addAttribute("reviewSummary",   reviews);
        model.addAttribute("relatedProducts", productService.getRelatedProducts(product.getId(),
                product.getCategoryId(), 4));
        model.addAttribute("pageTitle",       product.getName());
        return "customer/product-detail";
    }
}
