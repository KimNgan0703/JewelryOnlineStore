package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Banner;
import com.jewelryonlinestore.entity.Collection;
import com.jewelryonlinestore.exception.ResourceNotFoundException;
import com.jewelryonlinestore.repository.BannerRepository;
import com.jewelryonlinestore.repository.CollectionRepository;
import com.jewelryonlinestore.service.BannerService;
import com.jewelryonlinestore.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final FileUploadService fileUploadService;
    private final CollectionRepository collectionRepository; // THÊM DÒNG NÀY

    @Override
    public List<Banner> getActiveBanners() {
        return bannerRepository.findActiveAndValidBanners();
    }

    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAllByOrderBySortOrderAsc();
    }

    @Override
    @Transactional
    public void createBanner(String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText, LocalDateTime startDate, LocalDateTime endDate, Long collectionId) {
        String finalImageUrl = determineImageUrl(image, imageUrlText);

        if (finalImageUrl == null || finalImageUrl.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp hình ảnh cho banner (tải lên hoặc nhập link).");
        }

        Banner banner = Banner.builder()
                .title(title)
                .linkUrl(linkUrl)
                .sortOrder(sortOrder)
                .imageUrl(finalImageUrl)
                .startDate(startDate)
                .endDate(endDate)
                .isActive(true)
                .build();

        // XỬ LÝ GẮN BỘ SƯU TẬP
        if (collectionId != null) {
            Collection collection = collectionRepository.findById(collectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ sưu tập"));
            banner.setCollection(collection);
        }

        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void updateBanner(Long id, String title, String linkUrl, int sortOrder, MultipartFile image, String imageUrlText, LocalDateTime startDate, LocalDateTime endDate, Long collectionId) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Banner"));

        String finalImageUrl = determineImageUrl(image, imageUrlText);

        banner.setTitle(title);
        banner.setLinkUrl(linkUrl);
        banner.setSortOrder(sortOrder);
        banner.setStartDate(startDate);
        banner.setEndDate(endDate);

        if (finalImageUrl != null && !finalImageUrl.isBlank()) {
            banner.setImageUrl(finalImageUrl);
        }

        // XỬ LÝ CẬP NHẬT BỘ SƯU TẬP
        if (collectionId != null) {
            Collection collection = collectionRepository.findById(collectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Bộ sưu tập"));
            banner.setCollection(collection);
        } else {
            // Nếu admin bỏ chọn bộ sưu tập (chọn "-- Không thuộc BST nào --")
            banner.setCollection(null);
        }

        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public boolean toggleActive(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Banner"));
        banner.setActive(!banner.isActive());
        bannerRepository.save(banner);
        return banner.isActive();
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Banner");
        }
        bannerRepository.deleteById(id);
    }

    // Hàm phụ trợ xử lý logic lấy link ảnh
    @Override
    public java.util.List<com.jewelryonlinestore.entity.Banner> getBannersByCollection(Long collectionId) {
        return bannerRepository.findByCollectionIdAndIsActiveTrueOrderBySortOrderAsc(collectionId);
    }

    private String determineImageUrl(MultipartFile image, String imageUrlText) {
        if (image != null && !image.isEmpty()) {
            // SỬA LẠI ĐÚNG TÊN HÀM "upload" VÀ TRUYỀN 2 THAM SỐ NHƯ NÀY:
            return fileUploadService.upload(image, "banners");
        } else if (imageUrlText != null && !imageUrlText.isBlank()) {
            return imageUrlText;
        }
        return null;
    }
}