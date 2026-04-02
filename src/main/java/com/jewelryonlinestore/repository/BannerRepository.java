package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Chỉ lấy banner đang bật VÀ thỏa mãn điều kiện thời gian (Trang chủ)
    @Query("""
        SELECT b FROM Banner b 
        WHERE b.isActive = true 
          AND (b.startDate IS NULL OR b.startDate <= CURRENT_TIMESTAMP) 
          AND (b.endDate IS NULL OR b.endDate >= CURRENT_TIMESTAMP)
        ORDER BY b.sortOrder ASC
    """)
    List<Banner> findActiveAndValidBanners();

    // Admin: lấy tất cả banner kể cả ẩn/hết hạn để quản lý
    List<Banner> findAllByOrderBySortOrderAsc();
}