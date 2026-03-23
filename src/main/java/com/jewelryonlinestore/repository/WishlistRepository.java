package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

	@EntityGraph(attributePaths = {"product", "product.reviews"})
	List<Wishlist> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

	Optional<Wishlist> findByCustomerIdAndProductId(Long customerId, Long productId);

	boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

	void deleteByCustomerIdAndProductId(Long customerId, Long productId);
}

