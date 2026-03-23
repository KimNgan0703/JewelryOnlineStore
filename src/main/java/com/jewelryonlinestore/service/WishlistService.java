package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.response.ProductCardResponse;
import com.jewelryonlinestore.entity.Customer;
import com.jewelryonlinestore.entity.Product;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.entity.Wishlist;
import com.jewelryonlinestore.repository.CustomerRepository;
import com.jewelryonlinestore.repository.ProductRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

	private final WishlistRepository wishlistRepository;
	private final ProductRepository productRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<ProductCardResponse> getWishlist(Authentication auth) {
		Customer customer = resolveCustomer(auth);
		if (customer == null) {
			return Collections.emptyList();
		}

		return wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
				.stream()
				.map(Wishlist::getProduct)
				.map(this::toProductCard)
				.toList();
	}

	@Transactional
	public boolean toggle(Long productId, Authentication auth) {
		Customer customer = resolveCustomer(auth);
		if (customer == null) {
			return false;
		}

		return wishlistRepository.findByCustomerIdAndProductId(customer.getId(), productId)
				.map(existing -> {
					wishlistRepository.delete(existing);
					return false;
				})
				.orElseGet(() -> {
					Product product = productRepository.findById(productId)
							.orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

					Wishlist wishlist = Wishlist.builder()
							.customer(customer)
							.product(product)
							.build();
					wishlistRepository.save(wishlist);
					return true;
				});
	}

	@Transactional(readOnly = true)
	public boolean isWishlisted(Long productId, Authentication auth) {
		Customer customer = resolveCustomer(auth);
		return customer != null
				&& wishlistRepository.existsByCustomerIdAndProductId(customer.getId(), productId);
	}

	private ProductCardResponse toProductCard(Product product) {
		double averageRating = product.getReviews().stream()
				.filter(review -> review.getStatus() == com.jewelryonlinestore.entity.Review.ReviewStatus.APPROVED)
				.mapToInt(r -> r.getRating())
				.average()
				.orElse(0.0d);

		int reviewCount = (int) product.getReviews().stream()
				.filter(review -> review.getStatus() == com.jewelryonlinestore.entity.Review.ReviewStatus.APPROVED)
				.count();

		Integer discountPercent = product.getDiscountPercent();

		return ProductCardResponse.builder()
				.id(product.getId())
				.name(product.getName())
				.slug(product.getSlug())
				.primaryImageUrl(product.getPrimaryImageUrl())
				.basePrice(product.getBasePrice())
				.comparePrice(product.getComparePrice())
				.hasDiscount(discountPercent != null)
				.discountPercent(discountPercent)
				.averageRating(averageRating)
				.reviewCount(reviewCount)
				.isNew(product.isNew())
				.isBestSeller(product.isBestSeller())
				.inStock(product.hasStock())
				.build();
	}

	private Customer resolveCustomer(Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			return null;
		}

		Object principal = auth.getPrincipal();
		if (principal instanceof User user) {
			return customerRepository.findByUserId(user.getId()).orElse(null);
		}

		if (principal instanceof UserDetails userDetails) {
			return userRepository.findByEmail(userDetails.getUsername())
					.flatMap(user -> customerRepository.findByUserId(user.getId()))
					.orElse(null);
		}

		return userRepository.findByEmail(auth.getName())
				.flatMap(user -> customerRepository.findByUserId(user.getId()))
				.orElse(null);
	}
}

