package com.jewelryonlinestore.dto.request;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO nhận tham số lọc/tìm kiếm sản phẩm từ query string.
 * Dùng cho C03 (Tìm kiếm & lọc) và C02 (Trang chủ - danh mục).
 */
@Data
public class ProductFilterRequest {

    // Tìm kiếm full-text
    private String keyword;

    // Lọc theo danh mục (slug hoặc id)
    private Long categoryId;

    // Lọc theo thương hiệu
    private Long brandId;


    // Lọc theo chất liệu
    private Long materialId;

    // Lọc theo giới tính: male | female | unisex
    private String gender;

    // Khoảng giá
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Chỉ hiển thị sản phẩm còn hàng
    private Boolean inStockOnly = false;

    // Chỉ hiển thị sản phẩm mới
    private Boolean isNew;

    // Chỉ hiển thị sản phẩm bán chạy
    private Boolean isBestSeller;

    // Sắp xếp: price_asc | price_desc | newest | best_seller | rating
    private String sortBy = "newest";

    // Phân trang
    private int page = 0;
    private int size = 20;
}
