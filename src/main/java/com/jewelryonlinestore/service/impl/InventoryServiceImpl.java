package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.dto.request.InventoryAdjustRequest;
import com.jewelryonlinestore.entity.InventoryLog;
import com.jewelryonlinestore.entity.ProductVariant;
import com.jewelryonlinestore.entity.User;
import com.jewelryonlinestore.repository.InventoryLogRepository;
import com.jewelryonlinestore.repository.ProductVariantRepository;
import com.jewelryonlinestore.repository.UserRepository;
import com.jewelryonlinestore.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<?> getInventoryList(String keyword, boolean lowStockOnly, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        // Gọi hàm đã JOIN FETCH, trả về kết quả mượt mà chỉ với 1 câu Query
        return productVariantRepository.searchInventory(kw, lowStockOnly, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    @Transactional
    public void adjustStock(InventoryAdjustRequest req, Authentication auth) {
        ProductVariant variant = productVariantRepository.findById(req.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + req.getVariantId()));

        int nextStock = variant.getStockQuantity() + req.getQuantityChange();
        if (nextStock < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không đủ để xuất!");
        }
        variant.setStockQuantity(nextStock);
        productVariantRepository.save(variant);

        InventoryLog log = InventoryLog.builder()
                .variant(variant)
                .user(resolveUser(auth))
                .quantityChange(req.getQuantityChange())
                .reason(req.getReason())
                .note(req.getNote())
                .build();
        inventoryLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<?> getVariantHistory(Long variantId, int page, int size) {
        return inventoryLogRepository.findByVariantIdOrderByCreatedAtDesc(variantId, PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public int getLowStockCount() {
        // Dùng hàm Count sẽ nhanh hơn gấp 10 lần so với việc tải ra 1 cái List rồi gọi .size()
        return productVariantRepository.countLowStockVariants();
    }

    private User resolveUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}