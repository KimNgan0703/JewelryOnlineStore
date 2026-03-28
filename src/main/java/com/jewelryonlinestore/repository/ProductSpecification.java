package com.jewelryonlinestore.repository;

import com.jewelryonlinestore.dto.request.ProductFilterRequest;
import com.jewelryonlinestore.entity.Category;
import com.jewelryonlinestore.entity.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> of(ProductFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy sản phẩm đang active
            predicates.add(cb.isTrue(root.get("isActive")));

            // Keyword (name / shortDescription)
            String kw = filter.getKeyword();
            if (kw != null && !kw.isBlank()) {
                String pattern = "%" + kw.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("shortDescription")), pattern)
                ));
            }

            // ── Category: lọc cả category cha lẫn các category con ──
            if (filter.getCategoryId() != null) {
                Subquery<Long> childSub = query.subquery(Long.class);
                Root<Category> catRoot = childSub.from(Category.class);
                childSub.select(catRoot.get("id"))
                        .where(cb.or(
                                cb.equal(catRoot.get("id"), filter.getCategoryId()),
                                cb.equal(catRoot.get("parent").get("id"), filter.getCategoryId())
                        ));
                predicates.add(root.get("category").get("id").in(childSub));
            }

            // Brand
            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
            }

            // Material
            if (filter.getMaterialId() != null) {
                predicates.add(cb.equal(root.get("material").get("id"), filter.getMaterialId()));
            }

            // Khoảng giá
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), filter.getMaxPrice()));
            }

            // Chỉ còn hàng
            if (Boolean.TRUE.equals(filter.getInStockOnly())) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<com.jewelryonlinestore.entity.ProductVariant> vRoot =
                        sub.from(com.jewelryonlinestore.entity.ProductVariant.class);
                sub.select(vRoot.get("product").get("id"))
                        .where(
                                cb.equal(vRoot.get("product"), root),
                                cb.isTrue(vRoot.get("isActive")),
                                cb.greaterThan(vRoot.get("stockQuantity"), 0)
                        );
                predicates.add(cb.exists(sub));
            }

            // Chỉ sản phẩm mới
            if (Boolean.TRUE.equals(filter.getIsNew())) {
                predicates.add(cb.isTrue(root.get("isNew")));
            }

            // Chỉ bán chạy
            if (Boolean.TRUE.equals(filter.getIsBestSeller())) {
                predicates.add(cb.isTrue(root.get("isBestSeller")));
            }

            // Tránh duplicate do JOIN
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}