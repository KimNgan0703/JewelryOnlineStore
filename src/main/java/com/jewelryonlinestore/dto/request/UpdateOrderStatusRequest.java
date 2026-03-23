package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO Admin cập nhật trạng thái đơn hàng (A05).
 */
@Data
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Trạng thái mới không được để trống")
    private String newStatus; // processing | preparing | shipping | delivered | cancelled | returned

    private String note;

    private String cancelledReason; // bắt buộc nếu newStatus = cancelled
}
