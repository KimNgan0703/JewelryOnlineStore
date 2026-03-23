package com.jewelryonlinestore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions",
        indexes = {
                @Index(name = "idx_code",   columnList = "code"),
                @Index(name = "idx_dates",  columnList = "start_date, end_date"),
                @Index(name = "idx_active", columnList = "is_active")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionType type;

    /** % hoặc số tiền cố định (DECIMAL 5,2 — tối đa 999.99) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_value", precision = 15, scale = 0)
    private BigDecimal minOrderValue;

    /** NULL = không giới hạn lượt dùng */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<PromotionCondition> conditions = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Business logic ───────────────────────────────────
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive
                && now.isAfter(startDate)
                && now.isBefore(endDate)
                && (usageLimit == null || usedCount < usageLimit);
    }

    /** Tính số tiền được giảm từ subtotal */
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (type == PromotionType.PERCENTAGE) {
            return subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        // FIXED: giá trị value lưu theo nghìn đồng nếu cần, hoặc đúng số tiền
        return value.min(subtotal); // không giảm hơn subtotal
    }

    public enum PromotionType { PERCENTAGE, FIXED }
}
