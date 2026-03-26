package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.PromotionRequest;
import com.jewelryonlinestore.entity.Promotion;
import com.jewelryonlinestore.repository.PromotionRepository;
import com.jewelryonlinestore.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> validateCoupon(String code, BigDecimal subtotal) {
        Optional<Promotion> promotion = promotionRepository.findValidPromotion(code, LocalDateTime.now());
        if (promotion.isEmpty()) {
            return Optional.empty();
        }
        Promotion p = promotion.get();
        if (p.getMinOrderValue() != null && subtotal != null && subtotal.compareTo(p.getMinOrderValue()) < 0) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal subtotal) {
        if (promotion == null || subtotal == null) {
            return BigDecimal.ZERO;
        }
        return promotion.calculateDiscount(subtotal);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Promotion> searchPromotions(String keyword, Boolean isActive, int page, int size) {
        return promotionRepository.searchPromotions(blankToNull(keyword), isActive, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public void createPromotion(PromotionRequest req) {
        promotionRepository.save(toEntity(req, null));
    }

    @Override
    @Transactional
    public void updatePromotion(Long id, PromotionRequest req) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
        promotionRepository.save(toEntity(req, existing));
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
        return req;
    }


    @Override
    @Transactional
    public void incrementUsedCount(Long promotionId) {
        promotionRepository.incrementUsedCount(promotionId);
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
        return target;
    }
    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá này!"));
        promotionRepository.delete(promotion);
    }
    private Promotion.PromotionType parseType(String value) {
        if (value == null || value.isBlank()) {
            return Promotion.PromotionType.PERCENTAGE;
        }
        return Promotion.PromotionType.valueOf(value.trim().toUpperCase());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}



