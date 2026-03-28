package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO điều chỉnh tồn kho (A04).
 * quantityChange > 0: nhập kho | quantityChange < 0: xuất kho
 */
@Data
public class InventoryAdjustRequest {

    @NotNull(message = "Variant ID không được để trống")
    private Long variantId;

    @NotNull(message = "Số lượng không được để trống")
    // Đã xóa @NotZero gây lỗi hệ thống
    private Integer quantityChange;

    @NotBlank(message = "Lý do không được để trống")
    @Size(max = 255)
    private String reason; // Kiểm kê | Nhập hàng | Trả hàng | Bảo hành | Khác

    private String note;
}