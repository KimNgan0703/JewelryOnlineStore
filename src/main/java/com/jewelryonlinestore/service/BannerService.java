package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
	List<Banner> getActiveBanners();
	List<Banner> getAllBanners();
	Banner createBanner(String title, String linkUrl, int sortOrder, MultipartFile image);
	boolean toggleActive(Long id);
	void deleteBanner(Long id);
}

