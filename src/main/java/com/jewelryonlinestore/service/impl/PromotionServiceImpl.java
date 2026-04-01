package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.PromotionRequest;
import com.jewelryonlinestore.entity.*;
import com.jewelryonlinestore.repository.CategoryRepository;
import com.jewelryonlinestore.repository.ProductRepository;
import com.jewelryonlinestore.repository.PromotionConditionRepository;
import com.jewelryonlinestore.repository.PromotionRepository;
import com.jewelryonlinestore.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionConditionRepository conditionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> validateCoupon(String code, Cart cart) {
        Optional<Promotion> opt = promotionRepository.findByCodeAndIsActiveTrue(code.toUpperCase());
        if (opt.isEmpty()) return Optional.empty();

        Promotion promo = opt.get();
        LocalDateTime now = LocalDateTime.now();

        if (promo.getStartDate().isAfter(now) || (promo.getEndDate() != null && promo.getEndDate().isBefore(now))) {
            return Optional.empty();
        }
        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return Optional.empty();
        }

        BigDecimal cartTotal = cart.getItems().stream()
                .map(item -> item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (promo.getMinOrderValue() != null && cartTotal.compareTo(promo.getMinOrderValue()) < 0) {
            return Optional.empty();
        }

        BigDecimal discount = calculateDiscount(promo, cart);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        return Optional.of(promo);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Promotion promo, Cart cart) {
        List<PromotionCondition> conditions = conditionRepository.findByPromotionId(promo.getId());
        PromotionCondition cond = conditions.isEmpty() ? null : conditions.get(0);

        BigDecimal applicableAmount = BigDecimal.ZERO;
        int applicableQuantity = 0;

        for (CartItem item : cart.getItems()) {
            boolean isApplicable = false;

            if (cond == null || cond.getApplyTo().name().equals("ALL")) {
                isApplicable = true;
            } else if (cond.getApplyTo().name().equals("CATEGORY") && cond.getCategory() != null) {
                if (item.getVariant().getProduct().getCategory().getId().equals(cond.getCategory().getId())) {
                    isApplicable = true;
                }
            } else if (cond.getApplyTo().name().equals("PRODUCT") && cond.getProduct() != null) {
                if (item.getVariant().getProduct().getId().equals(cond.getProduct().getId())) {
                    isApplicable = true;
                }
            }

            if (isApplicable) {
                applicableAmount = applicableAmount.add(
                        item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                applicableQuantity += item.getQuantity();
            }
        }

        if (promo.getMinQuantity() != null && applicableQuantity < promo.getMinQuantity()) {
            return BigDecimal.ZERO;
        }
        if (applicableAmount.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        if (promo.getType().name().equals("PERCENTAGE")) {
            return applicableAmount.multiply(promo.getValue()).divide(BigDecimal.valueOf(100));
        } else {
            return promo.getValue().min(applicableAmount);
        }
    }

    /**
     * Tìm kiếm có lọc theo status:
     *   ACTIVE, INACTIVE, EXPIRED, UPCOMING hoặc null (tất cả)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Promotion> searchPromotions(String keyword, String status, int page, int size) {
        String normalizedStatus = blankToNull(status);
        return promotionRepository.searchPromotions(
                blankToNull(keyword),
                normalizedStatus,
                LocalDateTime.now(),
                PageRequest.of(page, size)
        );
    }

    @Override
    @Transactional
    public void createPromotion(PromotionRequest req) {
        Promotion saved = promotionRepository.save(toEntity(req, null));
        saveCondition(saved, req);
    }

    @Override
    @Transactional
    public void updatePromotion(Long id, PromotionRequest req) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
        Promotion saved = promotionRepository.save(toEntity(req, existing));
        conditionRepository.deleteByPromotionId(saved.getId());
        saveCondition(saved, req);
    }

    private void saveCondition(Promotion promo, PromotionRequest req) {
        if (req.getApplyTo() != null && !req.getApplyTo().equals("ALL")) {
            PromotionCondition cond = new PromotionCondition();
            cond.setPromotion(promo);
            cond.setApplyTo(PromotionCondition.ApplyTo.valueOf(req.getApplyTo()));

            if (req.getApplyTo().equals("CATEGORY") && req.getCategoryId() != null) {
                cond.setCategory(categoryRepository.findById(req.getCategoryId()).orElse(null));
            } else if (req.getApplyTo().equals("PRODUCT") && req.getProductId() != null) {
                cond.setProduct(productRepository.findById(req.getProductId()).orElse(null));
            }
            conditionRepository.save(cond);
        }
    }

    @Override
    @Transactional
    public boolean toggleActive(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
        promotion.setActive(!promotion.isActive());
        return promotionRepository.save(promotion).isActive();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionRequest getPromotionForEdit(Long id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
        PromotionRequest req = new PromotionRequest();
        req.setCode(p.getCode());
        req.setName(p.getName());
        req.setDescription(p.getDescription());
        req.setType(p.getType().name().toLowerCase());
        req.setValue(p.getValue());
        req.setMinOrderValue(p.getMinOrderValue());
        req.setUsageLimit(p.getUsageLimit());
        req.setStartDate(p.getStartDate());
        req.setEndDate(p.getEndDate());
        req.setActive(p.isActive());
        req.setMinQuantity(p.getMinQuantity());

        List<PromotionCondition> conds = conditionRepository.findByPromotionId(p.getId());
        if (!conds.isEmpty()) {
            PromotionCondition cond = conds.get(0);
            req.setApplyTo(cond.getApplyTo().name());
            if (cond.getCategory() != null) req.setCategoryId(cond.getCategory().getId());
            if (cond.getProduct() != null) req.setProductId(cond.getProduct().getId());
        } else {
            req.setApplyTo("ALL");
        }

        return req;
    }

    @Override
    @Transactional
    public void incrementUsedCount(Long promotionId) {
        promotionRepository.incrementUsedCount(promotionId);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá này!"));
        conditionRepository.deleteByPromotionId(id);
        promotionRepository.delete(promotion);
    }

    private Promotion toEntity(PromotionRequest req, Promotion existing) {
        Promotion target = existing == null ? new Promotion() : existing;
        target.setCode(req.getCode());
        target.setName(req.getName());
        target.setDescription(req.getDescription());
        target.setType(parseType(req.getType()));
        target.setValue(req.getValue());
        target.setMinOrderValue(req.getMinOrderValue());
        target.setUsageLimit(req.getUsageLimit());
        target.setStartDate(req.getStartDate());
        target.setEndDate(req.getEndDate());
        target.setActive(req.isActive());
        target.setMinQuantity(req.getMinQuantity());
        return target;
    }

    private Promotion.PromotionType parseType(String value) {
        if (value == null || value.isBlank()) return Promotion.PromotionType.PERCENTAGE;
        return Promotion.PromotionType.valueOf(value.trim().toUpperCase());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}