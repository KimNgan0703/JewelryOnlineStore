package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Tìm giỏ hàng của user đã đăng nhập (C05)
    Optional<Cart> findByUserId(Long userId);

    // Tìm giỏ hàng của guest theo session (C05)
    Optional<Cart> findBySessionId(String sessionId);

    // Kiểm tra user đã có giỏ chưa
    boolean existsByUserId(Long userId);

    // Tìm giỏ + fetch items (tránh N+1 query)
    @Query("""
        SELECT c FROM Cart c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product p
        WHERE c.user.id = :userId
    """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("""
        SELECT c FROM Cart c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product p
        WHERE c.sessionId = :sessionId
    """)
    Optional<Cart> findBySessionIdWithItems(@Param("sessionId") String sessionId);
}

