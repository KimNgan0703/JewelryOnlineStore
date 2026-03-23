package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.request.InventoryAdjustRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

public interface InventoryService {
    Page<?>  getInventoryList(String keyword, boolean lowStockOnly, int page, int size);
    void     adjustStock(InventoryAdjustRequest req, Authentication auth);
    Page<?>  getVariantHistory(Long variantId, int page, int size);
    int      getLowStockCount();
}
