package com.jewelryonlinestore.controller.customer;

import com.jewelryonlinestore.dto.response.ApiResponse;
import com.jewelryonlinestore.dto.response.ProductCardResponse;
import com.jewelryonlinestore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

	private final WishlistService wishlistService;

	@GetMapping
	public String wishlistPage(Authentication auth, Model model) {
		List<ProductCardResponse> items = wishlistService.getWishlist(auth);
		model.addAttribute("items", items);
		model.addAttribute("pageTitle", "Wishlist");
		return "customer/wishlist";
	}

	@PostMapping("/toggle/{productId}")
	@ResponseBody
	public ResponseEntity<ApiResponse<Boolean>> toggle(@PathVariable Long productId,
													   Authentication auth) {
		boolean added = wishlistService.toggle(productId, auth);
		String message = added ? "Added to wishlist" : "Removed from wishlist";
		return ResponseEntity.ok(ApiResponse.ok(message, added));
	}

	@GetMapping("/check/{productId}")
	@ResponseBody
	public ResponseEntity<ApiResponse<Boolean>> check(@PathVariable Long productId,
													  Authentication auth) {
		boolean isWishlisted = wishlistService.isWishlisted(productId, auth);
		return ResponseEntity.ok(ApiResponse.ok(isWishlisted));
	}
}

