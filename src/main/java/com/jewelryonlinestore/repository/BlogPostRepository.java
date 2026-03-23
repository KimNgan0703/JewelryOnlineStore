package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    // Bài viết đã publish, sắp xếp mới nhất (hiển thị website)
    Page<BlogPost> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    // Tìm theo slug (trang chi tiết bài viết)
    Optional<BlogPost> findBySlugAndIsPublishedTrue(String slug);

    // Kiểm tra slug trùng
    boolean existsBySlugAndIdNot(String slug, Long id);

    // Admin: tất cả bài viết (A09)
    Page<BlogPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
