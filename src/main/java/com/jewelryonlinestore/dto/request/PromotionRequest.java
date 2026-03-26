package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String type; // PERCENTAGE | FIXED

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá trị phải lớn hơn 0")
    private BigDecimal value;

    private BigDecimal minOrderValue;

    private Integer usageLimit; // null = không giới hạn

    // FIX: Báo cho Spring Boot biết cách đọc định dạng ngày giờ từ giao diện HTML gửi lên
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    // FIX: Báo cho Spring Boot biết cách đọc định dạng ngày giờ
    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    private boolean isActive = true;
}