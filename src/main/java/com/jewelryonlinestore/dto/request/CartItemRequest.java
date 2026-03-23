package com.jewelryonlinestore.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotNull(message = "Variant ID không được để trống")
    private Long variantId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    @Max(value = 99, message = "Số lượng không được vượt quá 99")
    private Integer quantity;
}
