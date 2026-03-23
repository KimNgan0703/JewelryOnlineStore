package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Tìm item trong giỏ theo variant (kiểm tra trùng trước khi thêm)
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    // Xóa toàn bộ item của một giỏ (sau khi đặt hàng thành công)
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);

    // Đếm số lượng item trong giỏ (hiển thị badge trên icon giỏ)
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.id = :cartId")
    int sumQuantityByCartId(@Param("cartId") Long cartId);
}
