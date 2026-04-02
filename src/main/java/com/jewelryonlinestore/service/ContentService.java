package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service quản lý toàn bộ nội dung hiển thị trên website (A09):
 *  - Banner / Slideshow trang chủ
 *  - Bài viết / Tin tức (BlogPost)
 *
 * Tập trung logic nội dung vào 1 service thay vì tách BannerService + BlogService riêng lẻ,
 * giúp AdminContentController gọn hơn và dễ quản lý transaction.
 */
public interface ContentService {

    // ── Banner ────────────────────────────────────────────

    /** Lấy danh sách banner đang active (hiển thị trang chủ) */
    List<Banner> getActiveBanners();

    /** Lấy tất cả banner (admin quản lý) */
    List<Banner> getAllBanners();

    /**
     * Thêm banner mới.
     *
     * @param title     Tiêu đề banner
     * @param linkUrl   URL liên kết khi click (có thể null)
     * @param sortOrder Thứ tự hiển thị (số nhỏ hiện trước)
     * @param image     File ảnh banner
     * @return Banner vừa tạo
     */
    Banner createBanner(String title, String linkUrl, int sortOrder, MultipartFile image);

    /**
     * Cập nhật thông tin banner (không bao gồm ảnh).
     * Để thay ảnh, dùng updateBannerImage.
     */
    Banner updateBanner(Long id, String title, String linkUrl, int sortOrder);

    /**
     * Thay ảnh banner.
     *
     * @return URL ảnh mới
     */
    String updateBannerImage(Long id, MultipartFile image);

    /** Bật/tắt hiển thị banner. @return trạng thái active mới */
    boolean toggleBannerActive(Long id);

    /** Cập nhật thứ tự tất cả banner theo danh sách id truyền vào */
    void reorderBanners(List<Long> orderedIds);

    /** Xóa banner (xóa cả file ảnh) */
    void deleteBanner(Long id);

}
