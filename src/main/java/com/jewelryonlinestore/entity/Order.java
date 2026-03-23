package com.jewelryonlinestore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders",
        indexes = {
                @Index(name = "idx_customer",     columnList = "customer_id"),
                @Index(name = "idx_order_number", columnList = "order_number"),
                @Index(name = "idx_status",       columnList = "order_status"),
                @Index(name = "idx_created",      columnList = "created_at")
        }
)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    @ToString.Exclude
    private Address address;

    // ── Snapshot địa chỉ tại thời điểm đặt hàng ─────────
    @Column(name = "snap_recipient_name", nullable = false, length = 255)
    private String snapRecipientName;

    @Column(name = "snap_phone", nullable = false, length = 20)
    private String snapPhone;

    @Column(name = "snap_address", nullable = false, columnDefinition = "TEXT")
    private String snapAddress;

    // ── Khuyến mãi ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    @ToString.Exclude
    private Promotion promotion;

    // ── Tiền ─────────────────────────────────────────────
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 0)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal total;

    // ── Thanh toán ────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ── Trạng thái đơn ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    @ToString.Exclude
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Payment> payments = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Business logic helpers ────────────────────────────
    public boolean canCancel() {
        return orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PROCESSING;
    }

    public boolean isDelivered() { return orderStatus == OrderStatus.DELIVERED; }
    public boolean isCancelled() { return orderStatus == OrderStatus.CANCELLED; }

    // ── Enums ─────────────────────────────────────────────
    public enum PaymentMethod  { COD, BANK_TRANSFER, VNPAY, MOMO }
    public enum PaymentStatus  { PENDING, PAID, FAILED, REFUNDED }
    public enum OrderStatus    {
        PENDING, PROCESSING, PREPARING, SHIPPING, DELIVERED, CANCELLED, RETURNED;

        public String getLabel() {
            return switch (this) {
                case PENDING    -> "Chờ xác nhận";
                case PROCESSING -> "Đang xử lý";
                case PREPARING  -> "Đang chuẩn bị";
                case SHIPPING   -> "Đang giao hàng";
                case DELIVERED  -> "Đã giao hàng";
                case CANCELLED  -> "Đã hủy";
                case RETURNED   -> "Đã trả hàng";
            };
        }

        public String getBadgeClass() {
            return switch (this) {
                case PENDING    -> "badge-warning";
                case PROCESSING -> "badge-info";
                case PREPARING  -> "badge-primary";
                case SHIPPING   -> "badge-indigo";
                case DELIVERED  -> "badge-success";
                case CANCELLED  -> "badge-danger";
                case RETURNED   -> "badge-secondary";
            };
        }
    }
}
