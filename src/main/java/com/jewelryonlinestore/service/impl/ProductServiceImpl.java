package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.AdminProductRequest;
import com.jewelryonlinestore.dto.request.ProductFilterRequest;
import com.jewelryonlinestore.dto.response.ProductCardResponse;
import com.jewelryonlinestore.dto.response.ProductResponse;
import com.jewelryonlinestore.entity.*;
import com.jewelryonlinestore.repository.*;
import com.jewelryonlinestore.service.FileStorageService;
import com.jewelryonlinestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import com.jewelryonlinestore.repository.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Pattern NONLATIN   = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ProductRepository        productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository       categoryRepository;
    private final FileStorageService       fileStorageService;
    private final BrandRepository          brandRepository;
    private final MaterialRepository       materialRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCardResponse> filterProducts(ProductFilterRequest filter) {
        Sort sort = buildSort(filter.getSortBy());
        Specification<Product> spec = ProductSpecification.of(filter);
        Page<Product> page = productRepository.findAll(
                spec,
                PageRequest.of(filter.getPage(), filter.getSize(), sort)
        );
        return page.map(this::toCard);
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy) {
            case "price_asc"   -> Sort.by(Sort.Direction.ASC,  "basePrice");
            case "price_desc"  -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "best_seller" -> Sort.by(Sort.Direction.DESC, "isBestSeller")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "rating"      -> Sort.by(Sort.Direction.DESC, "averageRating") // Đã sửa để có thể sort theo Rating
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default            -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product p = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + slug));
        return toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getBestSellers(int limit) {
        // Xếp theo doanh số thực tế (tổng số lượng từ đơn hàng DELIVERED)
        return productRepository.findBestSellersByRevenue(PageRequest.of(0, limit))
                .stream().map(this::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getNewProducts(int limit) {
        return productRepository.findTop8ByIsActiveTrueAndIsNewTrueOrderByCreatedAtDesc()
                .stream().limit(limit).map(this::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getRelatedProducts(Long productId, Long categoryId, int limit) {
        if (categoryId == null) return Collections.emptyList();
        return productRepository.findRelatedProducts(categoryId, productId, PageRequest.of(0, limit))
                .stream().map(this::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.jewelryonlinestore.entity.Collection> getAllCollections() {
        return productRepository.findAll().stream()
                .map(Product::getCollection)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Material> getAllMaterials() {
        return materialRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> adminSearchProducts(String keyword, Long categoryId,
                                                     Boolean isActive, int page, int size) {
        return productRepository.adminSearchProducts(
                keyword == null || keyword.isBlank() ? null : keyword.trim(),
                categoryId,
                isActive,
                PageRequest.of(page, size)
        ).map(this::toResponse);
    }

    @Override
    @Transactional
    public Long createProduct(AdminProductRequest req) {
        Product product = new Product();
        applyRequest(product, req);
        Product saved = productRepository.save(product);

        if (req.getVariants() != null) {
            for (AdminProductRequest.VariantRequest vReq : req.getVariants()) {
                if (vReq.getSize() == null || vReq.getSize().isBlank()) continue; // bỏ qua dòng rỗng

                ProductVariant variant = new ProductVariant();
                variant.setProduct(saved);
                variant.setSize(vReq.getSize().trim());
                variant.setPrice(vReq.getPrice() != null ? vReq.getPrice() : req.getBasePrice());
                variant.setStockQuantity(vReq.getStockQuantity() == null ? 0 : vReq.getStockQuantity());
                variant.setLowStockThreshold(vReq.getLowStockThreshold() == null ? 5 : vReq.getLowStockThreshold());
                // Tự sinh SKU variant nếu bỏ trống để tránh Duplicate entry ''
                variant.setSku(generateVariantSku(vReq.getSku(), saved.getSku(), vReq.getSize()));
                variant.setActive(true);

                productVariantRepository.save(variant);
            }
        }

        List<String> imageUrls = req.getImageUrlList();
        if (!imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(saved)
                        .imageUrl(imageUrls.get(i))
                        .isPrimary(i == 0)
                        .sortOrder(i)
                        .build();
                saved.getImages().add(image);
            }
            productRepository.save(saved);
        }

        return saved.getId();
    }
    @Override
    @Transactional
    public void updateBrand(Long id, String name) {
        Brand brand = brandRepository.findById(id).orElseThrow();
        brand.setName(name);
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        // Kiểm tra xem thương hiệu có đang chứa sản phẩm nào không
        if (productRepository.existsByBrandId(id)) {
            throw new IllegalStateException("Không thể xóa! Thương hiệu này đang có sản phẩm.");
        }
        brandRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateMaterial(Long id, String name) {
        Material material = materialRepository.findById(id).orElseThrow();
        material.setName(name);
        materialRepository.save(material);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long id) {
        if (productRepository.existsByMaterialId(id)) {
            throw new IllegalStateException("Không thể xóa! Chất liệu này đang có sản phẩm.");
        }
        materialRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateProduct(Long id, AdminProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        applyRequest(product, req);

        if (req.getVariants() != null && !req.getVariants().isEmpty()) {
            // Xây map size→variant hiện có để tái sử dụng (update thay vì xóa/tạo mới)
            Map<String, ProductVariant> existingBySize = new java.util.LinkedHashMap<>();
            if (product.getVariants() != null) {
                for (ProductVariant pv : product.getVariants()) {
                    existingBySize.put(pv.getSize().trim().toLowerCase(), pv);
                }
            }

            Set<String> processedSizes = new java.util.HashSet<>();
            for (AdminProductRequest.VariantRequest vReq : req.getVariants()) {
                if (vReq.getSize() == null || vReq.getSize().isBlank()) continue;
                String sizeKey = vReq.getSize().trim().toLowerCase();
                processedSizes.add(sizeKey);

                ProductVariant pv = existingBySize.get(sizeKey);
                if (pv != null) {
                    // Cập nhật variant đã có
                    pv.setSize(vReq.getSize().trim());
                    pv.setPrice(vReq.getPrice() != null ? vReq.getPrice() : product.getBasePrice());
                    pv.setStockQuantity(vReq.getStockQuantity() == null ? 0 : vReq.getStockQuantity());
                    pv.setLowStockThreshold(vReq.getLowStockThreshold() == null ? 5 : vReq.getLowStockThreshold());
                    // Cập nhật SKU nếu người dùng nhập mới, giữ cũ nếu bỏ trống
                    if (vReq.getSku() != null && !vReq.getSku().isBlank()) {
                        pv.setSku(vReq.getSku().trim());
                    }
                    pv.setActive(true);
                    productVariantRepository.save(pv);
                } else {
                    // Tạo variant mới
                    ProductVariant newPv = ProductVariant.builder()
                            .product(product)
                            .size(vReq.getSize().trim())
                            .price(vReq.getPrice() != null ? vReq.getPrice() : product.getBasePrice())
                            .stockQuantity(vReq.getStockQuantity() == null ? 0 : vReq.getStockQuantity())
                            .lowStockThreshold(vReq.getLowStockThreshold() == null ? 5 : vReq.getLowStockThreshold())
                            .sku(generateVariantSku(vReq.getSku(), product.getSku(), vReq.getSize()))
                            .isActive(true)
                            .build();
                    productVariantRepository.save(newPv);
                }
            }

            // Vô hiệu hoá các variant bị xóa khỏi form (không xóa hẳn vì có thể có lịch sử đơn hàng)
            for (Map.Entry<String, ProductVariant> entry : existingBySize.entrySet()) {
                if (!processedSizes.contains(entry.getKey())) {
                    entry.getValue().setActive(false);
                    productVariantRepository.save(entry.getValue());
                }
            }
        }

        List<String> imageUrls = req.getImageUrlList();

        if (product.getImages() != null) {
            product.getImages().clear();
        }

        if (!imageUrls.isEmpty()) {
            if (product.getImages() == null) {
                product.setImages(new java.util.ArrayList<>());
            }
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrls.get(i))
                        .isPrimary(i == 0)
                        .sortOrder(i)
                        .build();
                product.getImages().add(image);
            }
        }

        productRepository.save(product);
    }

    @Override
    @Transactional
    public Brand createBrand(String name) {
        String cleanName = name.trim();
        if (brandRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Thương hiệu '" + cleanName + "' đã tồn tại!");
        }

        Brand brand = new Brand();
        brand.setName(cleanName);
        brand.setSlug(toSlug(cleanName));
        brand.setActive(true);
        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public Material createMaterial(String name) {
        String cleanName = name.trim();
        if (materialRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Chất liệu '" + cleanName + "' đã tồn tại!");
        }

        Material material = new Material();
        material.setName(cleanName);
        return materialRepository.save(material);
    }
    /**
     * Sinh SKU cho variant: dùng SKU do người dùng nhập nếu có,
     * ngược lại sinh tự động từ productSku + size để đảm bảo unique.
     */
    private String generateVariantSku(String inputSku, String productSku, String size) {
        if (inputSku != null && !inputSku.isBlank()) {
            return inputSku.trim();
        }
        // VD: SP-A1B2C3-16, SP-A1B2C3-FS
        String base = (productSku + "-" + size.trim()).toUpperCase()
                .replaceAll("[^A-Z0-9-]", "");
        String candidate = base;
        int i = 1;
        while (productVariantRepository.existsBySku(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateNextSku() {
        // Lấy 6 ký tự ngẫu nhiên từ UUID (gồm cả số và chữ cái)
        String randomSuffix = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Kết quả sinh ra sẽ có dạng: SP-A1B2C3
        return "SP-" + randomSuffix;
    }
    @Override
    @Transactional
    public boolean toggleActive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setActive(!product.isActive());
        return productRepository.save(product).isActive();
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductRequest getProductForEdit(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        AdminProductRequest req = new AdminProductRequest();
        req.setName(p.getName());
        req.setSku(p.getSku());
        req.setShortDescription(p.getShortDescription());
        req.setDescription(p.getDescription());
        req.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
        req.setBrandId(p.getBrand() != null ? p.getBrand().getId() : null);
        req.setCollectionId(p.getCollection() != null ? p.getCollection().getId() : null);
        req.setMaterialId(p.getMaterial() != null ? p.getMaterial().getId() : null);
        req.setGender(p.getGender() != null ? p.getGender().name().toLowerCase(java.util.Locale.ROOT) : null);
        req.setWeightGram(p.getWeightGram());
        req.setBasePrice(p.getBasePrice());
        req.setComparePrice(p.getComparePrice());
        req.setActive(p.isActive());
        req.setNew(p.isNew());
        req.setBestSeller(p.isBestSeller());
        req.setMetaTitle(p.getMetaTitle());
        req.setMetaDescription(p.getMetaDescription());

        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            java.util.List<AdminProductRequest.VariantRequest> variantRequests = p.getVariants().stream()
                    .filter(v -> v.isActive())
                    .map(v -> {
                        AdminProductRequest.VariantRequest vr = new AdminProductRequest.VariantRequest();
                        vr.setSize(v.getSize());
                        vr.setPrice(v.getPrice());
                        vr.setStockQuantity(v.getStockQuantity());
                        vr.setLowStockThreshold(v.getLowStockThreshold());
                        return vr;
                    })
                    .collect(java.util.stream.Collectors.toList());
            req.setVariants(variantRequests);
        } else {
            AdminProductRequest.VariantRequest emptyVariant = new AdminProductRequest.VariantRequest();
            req.setVariants(java.util.List.of(emptyVariant));
        }

        if (p.getImages() != null && !p.getImages().isEmpty()) {
            String urls = p.getImages().stream()
                    .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                    .map(ProductImage::getImageUrl)
                    .collect(java.util.stream.Collectors.joining(","));
            req.setImageUrls(urls);
        }
        return req;
    }

    private void applyRequest(Product product, AdminProductRequest req) {
        product.setName(req.getName());
        product.setSku(req.getSku());
        product.setSlug(toUniqueSlug(req.getName(), product.getId()));
        product.setShortDescription(req.getShortDescription());
        product.setDescription(req.getDescription());
        product.setBasePrice(req.getBasePrice());
        product.setComparePrice(req.getComparePrice());
        product.setActive(req.isActive());
        product.setNew(req.isNew());
        product.setBestSeller(req.isBestSeller());
        product.setMetaTitle(req.getMetaTitle());
        product.setMetaDescription(req.getMetaDescription());
        product.setGender(parseGender(req.getGender()));
        product.setWeightGram(req.getWeightGram());

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.getCategoryId()));
            product.setCategory(category);
        }
        if (req.getBrandId() != null) {
            Brand brand = brandRepository.findById(req.getBrandId())
                    .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + req.getBrandId()));
            product.setBrand(brand);
        }
        if (req.getMaterialId() != null) {
            Material material = materialRepository.findById(req.getMaterialId())
                    .orElseThrow(() -> new IllegalArgumentException("Material not found: " + req.getMaterialId()));
            product.setMaterial(material);
        }
    }

    private String toUniqueSlug(String input, Long id) {
        String base      = toSlug(input);
        String candidate = base;
        int    i         = 1;
        Long   exclude   = id == null ? -1L : id;
        while (productRepository.existsBySlugAndIdNot(candidate, exclude)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    private String toSlug(String input) {
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = WHITESPACE.matcher(noAccent).replaceAll("-");
        slug = NONLATIN.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ROOT);
    }

    private Product.Gender parseGender(String gender) {
        if (gender == null || gender.isBlank()) return null;
        return Product.Gender.valueOf(gender.trim().toUpperCase(Locale.ROOT));
    }

    private ProductCardResponse toCard(Product p) {
        return ProductCardResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .primaryImageUrl(p.getPrimaryImageUrl())
                .basePrice(p.getBasePrice())
                .comparePrice(p.getComparePrice())
                .hasDiscount(p.getDiscountPercent() != null)
                .discountPercent(p.getDiscountPercent())
                // ✅ Đã sửa: Lấy dữ liệu thật thay vì 0.0
                .averageRating(p.getAverageRating() != null ? p.getAverageRating() : 0.0)
                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                .isNew(p.isNew())
                .isBestSeller(p.isBestSeller())
                .inStock(p.hasStock())
                .build();
    }

    private ProductResponse toResponse(Product p) {
        List<String> allImages = p.getImages() == null ? Collections.emptyList()
                : p.getImages().stream().map(ProductImage::getImageUrl).toList();

        List<ProductResponse.VariantInfo> variants = p.getVariants() == null ? Collections.emptyList()
                : p.getVariants().stream().map(v -> ProductResponse.VariantInfo.builder()
                .id(v.getId())
                .sku(v.getSku())
                .size(v.getSize())
                .price(v.getPrice())
                .stockQuantity(v.getStockQuantity())
                .isActive(v.isActive())
                .lowStock(v.isLowStock())
                .build()).toList();

        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .collectionName(p.getCollection() != null ? p.getCollection().getName() : null)
                .materialName(p.getMaterial() != null ? p.getMaterial().getName() : null)
                .gender(p.getGender() != null ? p.getGender().name().toLowerCase(Locale.ROOT) : null)
                .basePrice(p.getBasePrice())
                .comparePrice(p.getComparePrice())
                .hasDiscount(p.getDiscountPercent() != null)
                .discountPercent(p.getDiscountPercent())
                .isActive(p.isActive())
                .isNew(p.isNew())
                .isBestSeller(p.isBestSeller())
                .inStock(p.hasStock())
                .primaryImageUrl(p.getPrimaryImageUrl())
                .allImageUrls(allImages)
                // ✅ Đã sửa: Lấy dữ liệu thật thay vì 0.0
                .averageRating(p.getAverageRating() != null ? p.getAverageRating() : 0.0)
                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                .variants(variants)
                .metaTitle(p.getMetaTitle())
                .metaDescription(p.getMetaDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrue();
    }


}