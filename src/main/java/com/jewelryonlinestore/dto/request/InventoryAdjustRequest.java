package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequest {

    @NotNull(message = "Thiếu ID phân loại sản phẩm")
    private Long variantId;

    @NotNull(message = "Thiếu số lượng điều chỉnh")
    private Integer quantityChange; // Bắt buộc phải tên là quantityChange

    private String reason;
    private String note;
}