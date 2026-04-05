package com.jewelryonlinestore.controller;

import com.jewelryonlinestore.entity.Promotion;
import com.jewelryonlinestore.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Objects;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {

    private final PromotionRepository promotionRepository;

    @ModelAttribute("currentUser")
    public Object getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getPrincipal(), "anonymousUser")) {
            return null;
        }

        return auth.getPrincipal();
    }

    // THÊM MỚI: Tự động bơm danh sách mã giảm giá hợp lệ lên mọi trang của Khách hàng
    @ModelAttribute("availablePromotions")
    public List<Promotion> availablePromotions() {
        return promotionRepository.findAvailablePromotions();
    }
}