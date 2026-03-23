package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Banner;
import com.jewelryonlinestore.repository.BannerRepository;
import com.jewelryonlinestore.service.BannerService;
import com.jewelryonlinestore.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;

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
    public Banner createBanner(String title, String linkUrl, int sortOrder, MultipartFile image) {
        String imageUrl = fileStorageService.store(image, "banners");
        return bannerRepository.save(Banner.builder()
                .title(title)
                .linkUrl(linkUrl)
                .sortOrder(sortOrder)
                .imageUrl(imageUrl)
                .isActive(true)
                .build());
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
        if (banner.getImageUrl() != null && !banner.getImageUrl().isBlank()) {
            fileStorageService.delete(banner.getImageUrl());
        }
        bannerRepository.delete(banner);
    }
}

