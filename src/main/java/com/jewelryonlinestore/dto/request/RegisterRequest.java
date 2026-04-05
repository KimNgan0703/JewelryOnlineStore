package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9]).*$",
            message = "Mật khẩu phải chứa ít nhất 1 chữ hoa và 1 chữ số"
    )
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    // --- Thông tin cá nhân ---
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255)
    private String fullName;

    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String gender;
    private LocalDate birthDate;

    // --- Địa chỉ mặc định (ĐÃ XÓA @NotBlank ĐỂ FORM KHÔNG BỊ LỖI KẸT) ---
    private String recipientName;
    private String recipientPhone;
    private String province;
    private String district;
    private String ward;
    private String streetAddress;
}