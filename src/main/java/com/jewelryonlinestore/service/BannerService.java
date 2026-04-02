package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerService {
	List<Banner> getActiveBanners();
	List<Banner> getAllBanners();

	// Đã thêm tham số startDate và endDate
	Banner createBanner(String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText, LocalDateTime startDate, LocalDateTime endDate);

	Banner updateBanner(Long id, String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText, LocalDateTime startDate, LocalDateTime endDate);

	boolean toggleActive(Long id);
	void deleteBanner(Long id);
}