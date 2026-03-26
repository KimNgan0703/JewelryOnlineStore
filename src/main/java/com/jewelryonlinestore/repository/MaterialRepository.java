package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByNameAsc();
    // Thêm dòng này
    boolean existsByNameIgnoreCase(String name);
}