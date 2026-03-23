package com.jewelryonlinestore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_product_size", columnNames = {"product_id", "size"}
        ),
        indexes = {
                @Index(name = "idx_product", columnList = "product_id"),
                @Index(name = "idx_sku",     columnList = "sku")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    /** Ví dụ: "7", "8", "45cm", "50cm" */
    @Column(nullable = false, length = 50)
    private String size;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    /** Ngưỡng cảnh báo tồn kho thấp */
    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private int lowStockThreshold = 5;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean isLowStock()  { return stockQuantity <= lowStockThreshold && stockQuantity > 0; }
    public boolean isOutOfStock(){ return stockQuantity <= 0; }
    public boolean hasEnough(int qty) { return stockQuantity >= qty; }
}
