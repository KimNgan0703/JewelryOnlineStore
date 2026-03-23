package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ApplyCouponRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 100)
    private String code;
}
