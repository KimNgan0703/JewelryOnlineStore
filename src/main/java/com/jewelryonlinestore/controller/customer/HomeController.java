package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.entity.Collection;
import com.jewelryonlinestore.service.BannerService;
import com.jewelryonlinestore.service.CategoryService;
import com.jewelryonlinestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * C02 — Trang chủ: banner, danh mục, bộ sưu tập, SP bán chạy, SP mới.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService  productService;
    private final CategoryService categoryService;
    private final BannerService   bannerService;

    @GetMapping("/")
    public String home(Model model, Authentication auth) {

        // Lọc chỉ lấy các bộ sưu tập đang hiển thị
        List<Collection> activeCollections = productService.getAllCollections().stream()
                .filter(Collection::isActive)
                .toList();

        model.addAttribute("banners",       bannerService.getActiveBanners());
        model.addAttribute("categories",    categoryService.getRootCategories());
        model.addAttribute("collections",   activeCollections); // Truyền BST xuống view
        model.addAttribute("bestSellers",   productService.getBestSellers(8));
        model.addAttribute("newProducts",   productService.getNewProducts(8));
        model.addAttribute("pageTitle",     "Trang Chủ");
        model.addAttribute("authentication", auth);
        return "customer/home";
    }

    @GetMapping("/privacy-terms")
    public String privacyAndTermsPage(Model model) {
        model.addAttribute("pageTitle", "Chính sách & Điều khoản");
        return "privacy"; // Trỏ tới file giao diện
    }
}