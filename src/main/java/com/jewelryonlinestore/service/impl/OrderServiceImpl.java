package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.PlaceOrderRequest;
import com.jewelryonlinestore.dto.request.UpdateOrderStatusRequest;
import com.jewelryonlinestore.dto.response.OrderDetailResponse;
import com.jewelryonlinestore.dto.response.OrderSummaryResponse;
import com.jewelryonlinestore.entity.Address;
import com.jewelryonlinestore.entity.Cart;
import com.jewelryonlinestore.entity.CartItem;
import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.entity.OrderItem;
import com.jewelryonlinestore.entity.Promotion;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.entity.ProductVariant;
import com.jewelryonlinestore.repository.AddressRepository;
import com.jewelryonlinestore.repository.CartItemRepository;
import com.jewelryonlinestore.repository.CartRepository;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.OrderRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.repository.ProductVariantRepository;
import com.jewelryonlinestore.service.EmailService;
import com.jewelryonlinestore.service.OrderService;
import com.jewelryonlinestore.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository     orderRepository;
    private final UserRepository      userRepository;
    private final CustomerRepository  customerRepository;
    private final AddressRepository   addressRepository;
    private final CartRepository      cartRepository;
    private final CartItemRepository  cartItemRepository;
    private final PromotionService    promotionService;

    // Inject EmailService và ProductVariantRepository
    private final EmailService             emailService;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public OrderDetailResponse placeOrder(PlaceOrderRequest req, Authentication auth, HttpSession session) {
        Customer customer = requireCustomer(auth);
        Address  address  = resolveAddress(req, customer);
        Cart     cart     = getCheckoutCart(auth, session);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<OrderItem> orderItems = buildOrderItems(cart.getItems());
        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = BigDecimal.ZERO;

        BigDecimal discountAmount  = BigDecimal.ZERO;
        Promotion  appliedPromotion = null;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            Optional<Promotion> promoOpt = promotionService.validateCoupon(req.getCouponCode(), subtotal);
            if (promoOpt.isPresent()) {
                appliedPromotion = promoOpt.get();
                discountAmount   = promotionService.calculateDiscount(appliedPromotion, subtotal);
            }
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discountAmount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber("ORD" + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 10).toUpperCase(Locale.ROOT))
                .customer(customer)
                .address(address)
                .snapRecipientName(address.getRecipientName())
                .snapPhone(address.getPhone())
                .snapAddress(address.getFullAddress())
                .promotion(appliedPromotion)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .total(total)
                .paymentMethod(parsePaymentMethod(req.getPaymentMethod()))
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PENDING)
                .note(req.getNote())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);

        if (emailService != null) {
            emailService.sendOrderConfirmationEmail(saved.getOrderNumber());
        }

        List<ProductVariant> variantsToUpdate = cart.getItems().stream()
                .map(CartItem::getVariant)
                .filter(Objects::nonNull)
                .toList();
        productVariantRepository.saveAll(variantsToUpdate);

        if (appliedPromotion != null) {
            promotionService.incrementUsedCount(appliedPromotion.getId());
        }

        cartItemRepository.deleteAllByCartId(cart.getId());
        return toDetail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderNumber, Authentication auth) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        Customer customer = requireCustomer(auth);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Order does not belong to current customer");
        }
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailAdmin(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Authentication auth, String status, int page, int size) {
        Customer customer = requireCustomer(auth);
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders;

        // 1. Nếu không có status hoặc status là "all" -> Lấy tất cả
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
        }
        else {
            try {
                // 2. Chuyển String từ URL (pending) -> Enum (PENDING)
                Order.OrderStatus statusEnum = Order.OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));

                // 3. Gọi hàm repository đã sửa ở Bước 1
                orders = orderRepository.findByCustomerIdAndOrderStatusOrderByCreatedAtDesc(
                        customer.getId(), statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                // Nếu người dùng nhập status bậy bạ trên URL -> Mặc định lấy tất cả
                orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
            }
        }

        return orders.map(this::toSummary);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber, String reason, Authentication auth) {
        Customer customer = requireCustomer(auth);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Order does not belong to current customer");
        }
        if (!order.canCancel()) {
            throw new IllegalStateException("Order cannot be cancelled at current status");
        }

        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        Order savedOrder = orderRepository.save(order);

        if (emailService != null) {
            emailService.sendOrderStatusUpdateEmail(savedOrder.getOrderNumber());
        }

        List<ProductVariant> variantsToRestore = order.getItems().stream().map(item -> {
            ProductVariant variant = item.getVariant();
            if (variant != null) {
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            }
            return variant;
        }).filter(Objects::nonNull).toList();
        productVariantRepository.saveAll(variantsToRestore);
    }

    @Override
    @Transactional
    public int reorder(String orderNumber, Authentication auth, HttpSession session) {
        return 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> adminSearchOrders(String keyword, String status,
                                                        LocalDateTime from, LocalDateTime to,
                                                        int page, int size) {
        Order.OrderStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = Order.OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
            }
        }

        return orderRepository
                .searchOrders(blankToNull(keyword), statusEnum, from, to, PageRequest.of(page, size))
                .map(this::toSummary);
    }

    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest req, Authentication auth) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        Order.OrderStatus oldStatus = order.getOrderStatus();
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(req.getNewStatus().toUpperCase(Locale.ROOT));

        order.setOrderStatus(newStatus);

        if (newStatus == Order.OrderStatus.CANCELLED) {
            order.setCancelledReason(req.getCancelledReason());

            // Cộng lại số lượng Tồn kho khi ADMIN hủy đơn
            if (oldStatus != Order.OrderStatus.CANCELLED) {
                List<ProductVariant> variantsToRestore = order.getItems().stream().map(item -> {
                    ProductVariant variant = item.getVariant();
                    if (variant != null) {
                        variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    }
                    return variant;
                }).filter(Objects::nonNull).toList();
                productVariantRepository.saveAll(variantsToRestore);
            }
        }
        // BỔ SUNG LẠI: Tự động chuyển Đã thanh toán khi Đã giao hàng
        else if (newStatus == Order.OrderStatus.DELIVERED) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        if (emailService != null) {
            emailService.sendOrderStatusUpdateEmail(savedOrder.getOrderNumber());
        }

        return toDetail(savedOrder);
    }

    @Override
    @Transactional
    public OrderDetailResponse markAsPaid(String orderNumber, Authentication auth) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());

        return toDetail(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDetailResponse markAsDelivered(String orderNumber, Authentication auth) {
        Customer customer = requireCustomer(auth);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderNumber));

        // Kiểm tra quyền sở hữu
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc về bạn");
        }

        // Kiểm tra trạng thái phải là SHIPPING mới được nhận
        if (order.getOrderStatus() != Order.OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng chưa được giao, không thể xác nhận nhận hàng");
        }

        // Cập nhật trạng thái
        order.setOrderStatus(Order.OrderStatus.DELIVERED);
        order.setPaymentStatus(Order.PaymentStatus.PAID); // Nhận hàng xong coi như đã thanh toán (dành cho COD)
        order.setUpdatedAt(LocalDateTime.now());

        return toDetail(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
        return orderRepository.countByOrderStatus(orderStatus);
    }

    // ── Private helpers ──────────────────────────────────

    private Customer requireCustomer(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private Address resolveAddress(PlaceOrderRequest req, Customer customer) {
        if (req.getAddressId() != null) {
            Address address = addressRepository.findById(req.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("Address not found: " + req.getAddressId()));
            if (!address.getCustomer().getId().equals(customer.getId())) {
                throw new IllegalArgumentException("Address does not belong to current customer");
            }
            return address;
        }

        if (req.getNewAddress() != null
                && req.getNewAddress().getRecipientName() != null
                && !req.getNewAddress().getRecipientName().isBlank()
                && req.getNewAddress().getPhone() != null
                && !req.getNewAddress().getPhone().isBlank()
                && req.getNewAddress().getProvince() != null
                && !req.getNewAddress().getProvince().isBlank()
                && req.getNewAddress().getDistrict() != null
                && !req.getNewAddress().getDistrict().isBlank()
                && req.getNewAddress().getWard() != null
                && !req.getNewAddress().getWard().isBlank()
                && req.getNewAddress().getStreetAddress() != null
                && !req.getNewAddress().getStreetAddress().isBlank()) {
            if (req.getNewAddress().isDefault()) {
                addressRepository.clearDefaultByCustomer(customer.getId());
            }
            return addressRepository.save(Address.builder()
                    .customer(customer)
                    .recipientName(req.getNewAddress().getRecipientName())
                    .phone(req.getNewAddress().getPhone())
                    .province(req.getNewAddress().getProvince())
                    .district(req.getNewAddress().getDistrict())
                    .ward(req.getNewAddress().getWard())
                    .streetAddress(req.getNewAddress().getStreetAddress())
                    .isDefault(req.getNewAddress().isDefault())
                    .build());
        }

        return addressRepository.findByCustomerIdAndIsDefaultTrueAndIsDeletedFalse(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("No address selected for order"));
    }

    private Cart getCheckoutCart(Authentication auth, HttpSession session) {
        if (auth != null && auth.getName() != null) {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return cartRepository.findByUserIdWithItems(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
        }
        return cartRepository.findBySessionIdWithItems(session.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
    }

    private List<OrderItem> buildOrderItems(List<CartItem> cartItems) {
        return cartItems.stream().map(cartItem -> {
            if (cartItem.getVariant() == null || cartItem.getVariant().getProduct() == null) {
                throw new IllegalArgumentException("Cart item is invalid");
            }
            if (cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (cartItem.getVariant().getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Product is out of stock: "
                        + cartItem.getVariant().getProduct().getName());
            }

            cartItem.getVariant().setStockQuantity(
                    cartItem.getVariant().getStockQuantity() - cartItem.getQuantity());

            BigDecimal price     = cartItem.getVariant().getPrice();
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            return OrderItem.builder()
                    .variant(cartItem.getVariant())
                    .productName(cartItem.getVariant().getProduct().getName())
                    .variantSize(cartItem.getVariant().getSize())
                    .quantity(cartItem.getQuantity())
                    .price(price)
                    .total(itemTotal)
                    .build();
        }).toList();
    }

    private Order.PaymentMethod parsePaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return Order.PaymentMethod.COD;
        }
        return Order.PaymentMethod.valueOf(method.toUpperCase(Locale.ROOT));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private OrderSummaryResponse toSummary(Order order) {
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .createdAt(order.getCreatedAt())
                .total(order.getTotal())
                .orderStatus(order.getOrderStatus().name().toLowerCase(Locale.ROOT))
                .orderStatusLabel(order.getOrderStatus().getLabel())
                .paymentMethod(order.getPaymentMethod().name().toLowerCase(Locale.ROOT))
                .paymentStatus(order.getPaymentStatus().name().toLowerCase(Locale.ROOT))
                .itemCount(order.getItems() == null ? 0 : order.getItems().size())
                .firstProductName(order.getItems() == null || order.getItems().isEmpty()
                        ? null : order.getItems().get(0).getProductName())
                .firstProductImage(null)
                .canCancel(order.canCancel())
                .canReview(order.isDelivered())
                .build();
    }

    private OrderDetailResponse toDetail(Order order) {
        List<OrderDetailResponse.OrderItemInfo> itemInfos = order.getItems() == null
                ? Collections.emptyList()
                : order.getItems().stream().map(item -> OrderDetailResponse.OrderItemInfo.builder()
                        .orderItemId(item.getId())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .productId(item.getVariant() != null && item.getVariant().getProduct() != null
                                ? item.getVariant().getProduct().getId() : null)

                        // THÊM ĐOẠN NÀY ĐỂ LẤY SLUG TỪ DATABASE LÊN
                        .productSlug(item.getVariant() != null && item.getVariant().getProduct() != null
                                ? item.getVariant().getProduct().getSlug() : null)

                        .productName(item.getProductName())
                        .variantSize(item.getVariantSize())
                        .imageUrl(item.getVariant() != null && item.getVariant().getProduct() != null
                                ? item.getVariant().getProduct().getPrimaryImageUrl() : null)
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .total(item.getTotal())
                        .reviewed(item.hasReview())
                        .reviewId(item.getReview() != null ? item.getReview().getId() : null)
                        .build())
                .toList();

        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .snapRecipientName(order.getSnapRecipientName())
                .snapPhone(order.getSnapPhone())
                .snapAddress(order.getSnapAddress())
                .items(itemInfos)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .appliedCouponCode(order.getPromotion() != null ? order.getPromotion().getCode() : null)
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod().name().toLowerCase(Locale.ROOT))
                .paymentMethodLabel(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name().toLowerCase(Locale.ROOT))
                .paidAt(null)
                .orderStatus(order.getOrderStatus().name().toLowerCase(Locale.ROOT))
                .orderStatusLabel(order.getOrderStatus().getLabel())
                .note(order.getNote())
                .cancelledReason(order.getCancelledReason())
                .statusHistory(Collections.emptyList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .canCancel(order.canCancel())
                .canReview(order.isDelivered())
                .build();
    }
}