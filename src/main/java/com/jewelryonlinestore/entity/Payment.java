    package com.jewelryonlinestore.entity;

    import jakarta.persistence.*;
    import lombok.*;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;

    @Entity
    @Table(name = "payments",
            indexes = {
                    @Index(name = "idx_order",       columnList = "order_id"),
                    @Index(name = "idx_transaction", columnList = "transaction_id")
            }
    )
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public class Payment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @EqualsAndHashCode.Include
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id", nullable = false)
        @ToString.Exclude
        private Order order;

        /** Mã giao dịch từ cổng thanh toán (VNPay / Momo) */
        @Column(name = "transaction_id", length = 255)
        private String transactionId;

        @Enumerated(EnumType.STRING)
        @Column(name = "payment_method", nullable = false, length = 20)
        private Order.PaymentMethod paymentMethod;

        @Column(nullable = false, precision = 15, scale = 0)
        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        @Builder.Default
        private PaymentStatus status = PaymentStatus.PENDING;

        /** Raw JSON response từ cổng thanh toán */
        @Column(columnDefinition = "JSON")
        private String payload;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
        @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

        public boolean isSuccess() { return status == PaymentStatus.SUCCESS; }

        public enum PaymentStatus { PENDING, SUCCESS, FAILED }
    }
