package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerService {
	List<Banner> getActiveBanners();
	List<Banner> getAllBanners();

	// Đã thêm Long collectionId ở cuối
	void createBanner(String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText, LocalDateTime startDate, LocalDateTime endDate, Long collectionId);

	// Đã thêm Long collectionId ở cuối
	void updateBanner(Long id, String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText, LocalDateTime startDate, LocalDateTime endDate, Long collectionId);

	boolean toggleActive(Long id);
	void deleteBanner(Long id);

	// Lấy banner thuộc một bộ sưu tập cụ thể (dùng cho trang collection-detail)
	java.util.List<com.jewelryonlinestore.entity.Banner> getBannersByCollection(Long collectionId);
}