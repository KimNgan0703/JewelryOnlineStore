package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.AdminProductRequest;
import com.jewelryonlinestore.dto.request.ProductFilterRequest;
import com.jewelryonlinestore.dto.response.ProductCardResponse;
import com.jewelryonlinestore.dto.response.ProductResponse;
import com.jewelryonlinestore.entity.*;
import com.jewelryonlinestore.repository.CategoryRepository;
import com.jewelryonlinestore.repository.ProductRepository;
import com.jewelryonlinestore.repository.ProductVariantRepository;
import com.jewelryonlinestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCardResponse> filterProducts(ProductFilterRequest filter) {
        Page<Product> page = productRepository.filterProducts(
                filter.getCategoryId(),
                filter.getBrandId(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getGender(),
                PageRequest.of(filter.getPage(), filter.getSize()));
        return page.map(this::toCard);
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
        return productRepository.findTop8ByIsActiveTrueAndIsBestSellerTrueOrderByCreatedAtDesc().stream()
                .limit(limit)
                .map(this::toCard)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getNewProducts(int limit) {
        return productRepository.findTop8ByIsActiveTrueAndIsNewTrueOrderByCreatedAtDesc().stream()
                .limit(limit)
                .map(this::toCard)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getRelatedProducts(Long productId, Long categoryId, int limit) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return productRepository.findRelatedProducts(categoryId, productId, PageRequest.of(0, limit)).stream()
                .map(this::toCard)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return productRepository.findAll().stream()
                .map(Product::getBrand)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
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
        return productRepository.findAll().stream()
                .map(Product::getMaterial)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> adminSearchProducts(String keyword, Long categoryId, Boolean isActive, int page, int size) {
        Page<Product> products = productRepository.findAll(PageRequest.of(page, size));
        return products.map(this::toResponse);
    }

    @Override
    @Transactional
    public Long createProduct(AdminProductRequest req) {
        Product product = new Product();
        applyRequest(product, req);
        Product saved = productRepository.save(product);

        if (req.getVariants() != null) {
            for (AdminProductRequest.VariantRequest v : req.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(saved)
                        .sku(saved.getSku() + "-" + v.getSize())
                        .size(v.getSize())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity() == null ? 0 : v.getStockQuantity())
                        .lowStockThreshold(v.getLowStockThreshold() == null ? 5 : v.getLowStockThreshold())
                        .isActive(true)
                        .build();
                productVariantRepository.save(variant);
            }
        }

        // Ảnh đã được upload lên Cloudinary từ browser — chỉ cần lưu URL vào DB
        List<String> imageUrls = req.getImageUrlList();
        if (!imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(saved)
                        .imageUrl(imageUrls.get(i))
                        .isPrimary(i == 0)   // ảnh đầu tiên là primary
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
    public void updateProduct(Long id, AdminProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        applyRequest(product, req);

        // Cập nhật variant đầu tiên (biến thể mặc định)
        if (req.getVariants() != null && !req.getVariants().isEmpty()) {
            AdminProductRequest.VariantRequest vReq = req.getVariants().get(0);
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                // Sửa variant hiện có
                ProductVariant existing = product.getVariants().get(0);
                existing.setSize(vReq.getSize());
                existing.setPrice(vReq.getPrice());
                existing.setStockQuantity(vReq.getStockQuantity() == null ? 0 : vReq.getStockQuantity());
                existing.setLowStockThreshold(vReq.getLowStockThreshold() == null ? 5 : vReq.getLowStockThreshold());
                productVariantRepository.save(existing);
            } else {
                // Tạo mới nếu chưa có
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .sku(product.getSku() + "-" + vReq.getSize())
                        .size(vReq.getSize())
                        .price(vReq.getPrice())
                        .stockQuantity(vReq.getStockQuantity() == null ? 0 : vReq.getStockQuantity())
                        .lowStockThreshold(vReq.getLowStockThreshold() == null ? 5 : vReq.getLowStockThreshold())
                        .isActive(true)
                        .build();
                productVariantRepository.save(variant);
            }
        }

        // Thêm ảnh mới từ Cloudinary (nếu có)
        List<String> imageUrls = req.getImageUrlList();
        if (!imageUrls.isEmpty()) {
            int startOrder = product.getImages() == null ? 0 : product.getImages().size();
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrls.get(i))
                        .isPrimary(startOrder == 0 && i == 0)
                        .sortOrder(startOrder + i)
                        .build();
                product.getImages().add(image);
            }
        }

        productRepository.save(product);
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

        // FIX: map variants từ DB vào DTO — nếu thiếu thì @NotEmpty/@NotNull sẽ fail khi submit
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
            // Đảm bảo luôn có ít nhất 1 variant rỗng để form render được
            AdminProductRequest.VariantRequest emptyVariant = new AdminProductRequest.VariantRequest();
            req.setVariants(java.util.List.of(emptyVariant));
        }

        // Truyền URLs ảnh hiện tại vào hidden field để form edit hiển thị đúng
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
        String slug = toUniqueSlug(req.getName(), product.getId());
        product.setSlug(slug);
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
    }

    private String toUniqueSlug(String input, Long id) {
        String base = toSlug(input);
        String candidate = base;
        int i = 1;
        Long exclude = id == null ? -1L : id;
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
        if (gender == null || gender.isBlank()) {
            return null;
        }
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
                .averageRating(0.0)
                .reviewCount(0)
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
                .averageRating(0.0)
                .reviewCount(0)
                .variants(variants)
                .metaTitle(p.getMetaTitle())
                .metaDescription(p.getMetaDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}