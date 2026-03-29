package com.jewelryonlinestore.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products",
        indexes = {
                @Index(name = "idx_slug",       columnList = "slug"),
                @Index(name = "idx_category",   columnList = "category_id"),
                @Index(name = "idx_brand",      columnList = "brand_id"),
                @Index(name = "idx_collection", columnList = "collection_id"),
                @Index(name = "idx_material",   columnList = "material_id"),
                @Index(name = "idx_active",     columnList = "is_active"),
                @Index(name = "idx_created",    columnList = "created_at")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;
    // ── Phân loại ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @ToString.Exclude
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    @ToString.Exclude
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    @ToString.Exclude
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    /** Trọng lượng tính bằng gram */
    @Column(name = "weight_gram", precision = 10, scale = 2)
    private BigDecimal weightGram;

    // ── Giá ──────────────────────────────────────────────
    @Column(name = "base_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal basePrice;

    /** Giá gốc (để hiển thị gạch ngang khi có sale) */
    @Column(name = "compare_price", precision = 15, scale = 0)
    private BigDecimal comparePrice;

    // ── Trạng thái & nhãn ────────────────────────────────
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private boolean isNew = false;

    @Column(name = "is_best_seller", nullable = false)
    @Builder.Default
    private boolean isBestSeller = false;

    // ── SEO ──────────────────────────────────────────────
    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ────────────────────────────────────
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    @ToString.Exclude
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Helper ───────────────────────────────────────────
    /** Trả về URL ảnh chính, hoặc ảnh đầu tiên nếu không có ảnh chính */
    public String getPrimaryImageUrl() {
        return images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> images.isEmpty() ? null : images.get(0).getImageUrl());
    }

    /** Kiểm tra sản phẩm có ít nhất 1 variant còn hàng */
    public boolean hasStock() {
        return variants.stream()
                .anyMatch(v -> v.isActive() && v.getStockQuantity() > 0);
    }

    /** Phần trăm giảm giá so với compare_price */
    public Integer getDiscountPercent() {
        if (comparePrice == null || comparePrice.compareTo(BigDecimal.ZERO) == 0) return null;
        if (basePrice.compareTo(comparePrice) >= 0) return null;
        return comparePrice.subtract(basePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(comparePrice, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public enum Gender { MALE, FEMALE, UNISEX }
}
