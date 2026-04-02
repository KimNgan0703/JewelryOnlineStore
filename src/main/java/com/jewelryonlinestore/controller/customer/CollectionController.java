package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.request.ProductFilterRequest;
import com.jewelryonlinestore.dto.response.ProductCardResponse;
import com.jewelryonlinestore.entity.Collection;
import com.jewelryonlinestore.service.BannerService;
import com.jewelryonlinestore.service.CategoryService;
import com.jewelryonlinestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang bộ sưu tập — hiển thị banner của BST + sản phẩm lọc theo BST (kiểu PNJ)
 */
@Controller
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final ProductService  productService;
    private final CategoryService categoryService;
    private final BannerService   bannerService;

    @GetMapping("/{slug}")
    public String collectionDetail(
            @PathVariable String slug,
            @ModelAttribute ProductFilterRequest filter,
            Model model) {

        // 1. Lấy thông tin bộ sưu tập
        Collection collection = productService.getCollectionBySlug(slug);

        // 2. Lấy banner riêng của bộ sưu tập này
        var collectionBanners = bannerService.getBannersByCollection(collection.getId());

        // 3. Lấy sản phẩm của BST (có filter + phân trang)
        Page<ProductCardResponse> productPage =
                productService.filterProductsByCollection(collection.getId(), filter);

        // 4. Đưa dữ liệu xuống view
        model.addAttribute("collection",       collection);
        model.addAttribute("banners",          collectionBanners);
        model.addAttribute("products",         productPage.getContent());
        model.addAttribute("currentPage",      productPage.getNumber());
        model.addAttribute("totalPages",       productPage.getTotalPages());
        model.addAttribute("totalItems",       productPage.getTotalElements());
        model.addAttribute("filter",           filter);
        model.addAttribute("categories",       categoryService.getRootCategories());
        model.addAttribute("brands",           productService.getAllBrands());
        model.addAttribute("materials",        productService.getAllMaterials());
        model.addAttribute("pageTitle",        collection.getName());
        return "customer/collection-detail";
    }
}