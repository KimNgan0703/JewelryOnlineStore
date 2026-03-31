package com.jewelryonlinestore.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jewelryonlinestore.entity.Banner;
import com.jewelryonlinestore.repository.BannerRepository;
import com.jewelryonlinestore.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final Cloudinary cloudinary;

    @Override
    @Transactional(readOnly = true)
    public List<Banner> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banner> getAllBanners() {
        return bannerRepository.findAllByOrderBySortOrderAsc();
    }

    @Override
    @Transactional
    public Banner createBanner(String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText) {
        String finalImageUrl = "";

        // Ưu tiên 1: Tải file lên Cloudinary
        if (image != null && !image.isEmpty()) {
            try {
                Map uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.asMap("folder", "banners"));
                finalImageUrl = uploadResult.get("secure_url").toString();
            } catch (IOException e) {
                log.error("Lỗi khi upload ảnh banner lên Cloudinary: ", e);
                throw new RuntimeException("Không thể upload ảnh, vui lòng thử lại!");
            }
        }
        // Ưu tiên 2: Sử dụng link dán trực tiếp
        else if (imageUrlText != null && !imageUrlText.isBlank()) {
            finalImageUrl = imageUrlText.trim();
        }
        // Nếu không có cả 2 -> Báo lỗi
        else {
            throw new IllegalArgumentException("Vui lòng tải ảnh lên hoặc nhập link ảnh trực tiếp!");
        }

        return bannerRepository.save(Banner.builder()
                .title(title)
                .linkUrl(linkUrl)
                .sortOrder(sortOrder)
                .imageUrl(finalImageUrl)
                .isActive(true)
                .build());
    }

    @Override
    @Transactional
    public Banner updateBanner(Long id, String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));

        banner.setTitle(title);
        banner.setLinkUrl(linkUrl);
        banner.setSortOrder(sortOrder);

        // Đổi ảnh bằng cách tải file mới
        if (image != null && !image.isEmpty()) {
            try {
                Map uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.asMap("folder", "banners"));
                String newImageUrl = uploadResult.get("secure_url").toString();
                deleteImageFromCloudinary(banner.getImageUrl());
                banner.setImageUrl(newImageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Không thể cập nhật ảnh mới!");
            }
        }
        // Đổi ảnh bằng cách dán link mới (Nếu khác với link cũ)
        else if (imageUrlText != null && !imageUrlText.isBlank() && !imageUrlText.equals(banner.getImageUrl())) {
            deleteImageFromCloudinary(banner.getImageUrl());
            banner.setImageUrl(imageUrlText.trim());
        }

        return bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public boolean toggleActive(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));
        banner.setActive(!banner.isActive());
        return bannerRepository.save(banner).isActive();
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));
        deleteImageFromCloudinary(banner.getImageUrl());
        bannerRepository.delete(banner);
    }

    // Xóa ảnh cũ trên Cloudinary
    private void deleteImageFromCloudinary(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
            try {
                String[] parts = imageUrl.split("/");
                String fileWithExtension = parts[parts.length - 1];
                String folder = parts[parts.length - 2];
                String fileName = fileWithExtension.split("\\.")[0];
                String publicId = folder + "/" + fileName;
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Đã xóa ảnh cũ trên Cloudinary: {}", publicId);
            } catch (Exception e) {
                log.warn("Không thể tự động xóa ảnh cũ trên Cloudinary: {}", imageUrl, e);
            }
        }
    }
}