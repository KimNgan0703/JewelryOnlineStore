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

<<<<<<< Updated upstream
    @NotBlank(message = "Vui lòng chọn loại giảm giá")
    private String type;

    @NotNull(message = "Vui lòng nhập giá trị giảm")
=======
    @NotBlank(message = "Loại giảm giá không được để trống")
    private String type; // PERCENTAGE | FIXED

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá trị phải lớn hơn 0")
>>>>>>> Stashed changes
    private BigDecimal value;

    private BigDecimal minOrderValue;
    private Integer usageLimit;

<<<<<<< Updated upstream
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

=======
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
>>>>>>> Stashed changes
}