package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    Optional<Collection> findBySlugAndIsActiveTrue(String slug);

    // Kiểm tra trùng lặp khi thêm mới
    boolean existsBySlug(String slug);
    boolean existsByName(String name);

    // Kiểm tra trùng lặp slug cho trường hợp cập nhật (trừ chính nó)
    boolean existsBySlugAndIdNot(String slug, Long id);
}