package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByIsActiveTrueOrderByNameAsc();
    List<Brand> findAllByOrderByNameAsc();
}