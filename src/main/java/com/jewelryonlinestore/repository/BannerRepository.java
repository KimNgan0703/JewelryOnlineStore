package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Lấy banner đang active theo thứ tự sort (C02 - Trang chủ)
    List<Banner> findByIsActiveTrueOrderBySortOrderAsc();

    // Admin: tất cả banner kể cả ẩn (A09)
    List<Banner> findAllByOrderBySortOrderAsc();
}
