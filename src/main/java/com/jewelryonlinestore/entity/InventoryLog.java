package com.jewelryonlinestore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_log",
        indexes = {
                @Index(name = "idx_variant", columnList = "variant_id"),
                @Index(name = "idx_user",    columnList = "user_id")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    private ProductVariant variant;

    /** NULL nếu admin đã bị xóa khỏi hệ thống */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    /** Dương = nhập kho, âm = xuất kho */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public boolean isImport() { return quantityChange > 0; }
    public boolean isExport() { return quantityChange < 0; }
}
