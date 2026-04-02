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

    // Admin Product
    Page<ProductResponse>     adminSearchProducts(String keyword, Long categoryId, Boolean isActive, int page, int size);
    Long                      createProduct(AdminProductRequest req);
    void                      updateProduct(Long id, AdminProductRequest req);
    boolean                   toggleActive(Long id);
    void                      deleteProduct(Long id);

    AdminProductRequest       getProductForEdit(Long id);

<<<<<<< Updated upstream
    // Helpers
    List<Product>             getAllProducts();
    String                    generateNextSku();

    // Brand Management
    Brand                     createBrand(String name);
    void                      updateBrand(Long id, String name);
    void                      deleteBrand(Long id);

    // Material Management
    Material                  createMaterial(String name);
    void                      updateMaterial(Long id, String name);
    void                      deleteMaterial(Long id);

    // Collection Management (Phong cách PNJ)
    Collection                createCollection(String name, String imageUrl);
    void                      updateCollection(Long id, String name, String imageUrl);
    void                      deleteCollection(Long id);
    Collection                getCollectionBySlug(String slug);
    Page<ProductCardResponse> filterProductsByCollection(Long collectionId, ProductFilterRequest filter);
}
=======
}
>>>>>>> Stashed changes
