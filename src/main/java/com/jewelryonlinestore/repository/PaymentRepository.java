package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Order;
import com.jewelryonlinestore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
            Long orderId,
            Order.PaymentMethod paymentMethod,
            Payment.PaymentStatus status
    );
}

