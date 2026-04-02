package com.jewelryonlinestore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // Constructor mặc định (không tham số)
    public ResourceNotFoundException() {
        super();
    }

    // Constructor nhận vào câu thông báo lỗi (tham số String) -> Sửa lỗi của bạn ở đây
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Thêm constructor hỗ trợ cả message và cause (nguyên nhân sâu xa) nếu sau này cần dùng
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}