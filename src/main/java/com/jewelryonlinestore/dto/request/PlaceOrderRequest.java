package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO nhận thông tin đặt hàng từ trang checkout (C06).
 */
@Data
public class PlaceOrderRequest {

    // Địa chỉ giao hàng (chọn từ sổ địa chỉ hoặc nhập mới)
    private Long addressId; // nếu chọn địa chỉ có sẵn

    // Nếu người dùng nhập địa chỉ mới tại trang checkout
    private AddressRequest newAddress;

    // Mã giảm giá (nếu có, đã apply trước)
    private String couponCode;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod; // cod | bank_transfer | vnpay | momo

    private String note; // Ghi chú đơn hàng
}
