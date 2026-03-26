package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.PromotionRequest;
import com.jewelryonlinestore.entity.Promotion;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.util.Optional;

public interface PromotionService {
    Optional<Promotion>  validateCoupon(String code, BigDecimal subtotal);
    BigDecimal           calculateDiscount(Promotion promotion, BigDecimal subtotal);
    Page<Promotion>      searchPromotions(String keyword, Boolean isActive, int page, int size);
    void                 createPromotion(PromotionRequest req);
    void                 updatePromotion(Long id, PromotionRequest req);
    boolean              toggleActive(Long id);
    PromotionRequest     getPromotionForEdit(Long id);
    void                 incrementUsedCount(Long promotionId);
    void deletePromotion(Long id);
}
