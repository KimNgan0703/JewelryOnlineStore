package com.jewelryonlinestore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class ReviewRequest {

    @NotNull(message = "Order item ID không được để trống")
    private Long orderItemId;

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Integer rating;

    @NotBlank(message = "Nội dung đánh giá không được để trống")
    @Size(min = 10, max = 2000, message = "Đánh giá phải từ 10 đến 2000 ký tự")
    private String comment;

    // Upload ảnh kèm đánh giá (tối đa 5 ảnh)
    private List<MultipartFile> images;
}

