package com.jewelryonlinestore.entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items",
        indexes = { @Index(name = "idx_order", columnList = "order_id") }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    private ProductVariant variant;

    /** Snapshot — lưu lại để không bị ảnh hưởng khi sản phẩm thay đổi */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "variant_size", nullable = false, length = 50)
    private String variantSize;

    @Column(nullable = false)
    private int quantity;

    /** Giá tại thời điểm mua */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    /** price × quantity */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "orderItem", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Review review;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public boolean hasReview() { return review != null; }
}
