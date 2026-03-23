package com.jewelryonlinestore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Generic wrapper cho kết quả có phân trang.
 */
@Data
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;           // trang hiện tại (0-based)
    private int size;           // số item mỗi trang
    private long totalElements; // tổng số item
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
}
