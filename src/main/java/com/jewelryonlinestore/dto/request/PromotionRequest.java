package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tạo/sửa khuyến mãi (A07).
 */
@Data
public class PromotionRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã chỉ được chứa chữ hoa, số, dấu _ và -")
    private String code;

    @NotBlank(message = "Tên chương trình không được để trống")
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank(message = "Loại giảm giá không được để trống")
    private String type; // percentage | fixed

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal value;

    private BigDecimal minOrderValue;

    private Integer usageLimit; // null = không giới hạn

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    private boolean isActive = true;

    // Điều kiện áp dụng
    @NotBlank
    private String applyTo; // all | category | product

    private List<Long> categoryIds; // nếu applyTo = category
    private List<Long> productIds;  // nếu applyTo = product
}
