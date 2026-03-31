package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionRequest {
    @NotBlank(message = "Tên chương trình không được để trống")
    private String name;

    @NotBlank(message = "Mã code không được để trống")
    private String code;

    private String description;

    @NotBlank(message = "Vui lòng chọn loại giảm giá")
    private String type;

    @NotNull(message = "Vui lòng nhập giá trị giảm")
    private BigDecimal value;

    private BigDecimal minOrderValue;
    private Integer usageLimit;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    private boolean active;

    // =========================================================
    // 4 CỘT ĐIỀU KIỆN MỚI (Bắt buộc phải có để Spring Boot lưu)
    // =========================================================
    private String applyTo = "ALL";
    private Long categoryId;
    private Long productId;
    private Integer minQuantity;

}