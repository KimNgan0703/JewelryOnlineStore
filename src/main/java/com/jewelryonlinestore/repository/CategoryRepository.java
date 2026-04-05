package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tất cả danh mục đang active (cho menu / filter)
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();

    // Danh mục gốc (không có parent)
    List<Category> findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();

    // Danh mục con của một danh mục cha
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);

    // Tìm theo slug (URL friendly)
    Optional<Category> findBySlug(String slug);

    // Kiểm tra slug trùng
    boolean existsBySlugAndIdNot(String slug, Long id);

    // Toàn bộ cây danh mục (admin - A03)
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children ORDER BY c.sortOrder ASC")
    List<Category> findAllWithChildren();

    // Kiểm tra danh mục có sản phẩm không (trước khi xóa)
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.category.id = :categoryId")
    boolean hasProducts(Long categoryId);
    // Thêm dòng này để kiểm tra trùng tên (không phân biệt hoa/thường)
    boolean existsByNameIgnoreCase(String name);
}

