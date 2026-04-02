package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.*;
import com.jewelryonlinestore.dto.response.*;
import com.jewelryonlinestore.entity.*;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductService {
    Page<ProductCardResponse> filterProducts(ProductFilterRequest filter);
    ProductResponse           getProductBySlug(String slug);
    ProductResponse           getProductById(Long id);
    List<ProductCardResponse> getBestSellers(int limit);
    List<ProductCardResponse> getNewProducts(int limit);
    List<ProductCardResponse> getRelatedProducts(Long productId, Long categoryId, int limit);
    List<Brand>               getAllBrands();
    List<Collection>          getAllCollections();
    List<Material>            getAllMaterials();

    // Admin
    Page<ProductResponse>     adminSearchProducts(String keyword, Long categoryId,
                                                  Boolean isActive, int page, int size);
    Long                      createProduct(AdminProductRequest req);
    void                      updateProduct(Long id, AdminProductRequest req);
    boolean                   toggleActive(Long id);
    void                      deleteProduct(Long id);
    // Bổ sung CRUD cho Brand & Material
    void updateBrand(Long id, String name);
    void deleteBrand(Long id);
    void updateMaterial(Long id, String name);
    void deleteMaterial(Long id);
    AdminProductRequest       getProductForEdit(Long id);

    // API hỗ trợ thêm nhanh Brand & Material từ form Admin
    Brand                     createBrand(String name);
    Material                  createMaterial(String name);
    // Thêm hàm này để lấy danh sách sản phẩm cho Dropdown Khuyến mãi
    List<Product> getAllProducts();
    // Hàm tự động sinh mã SKU theo thứ tự
    String generateNextSku();
}