package com.jewelryonlinestore.service.impl;

import com.jewelryonlinestore.entity.Category;
import com.jewelryonlinestore.repository.CategoryRepository;
import com.jewelryonlinestore.service.CategoryService;
import com.jewelryonlinestore.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    // Khai báo Regex dùng để lọc bỏ dấu và ký tự đặc biệt khi tạo Slug
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesTree() {
        return categoryRepository.findAllWithChildren();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + slug));
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, String name, MultipartFile image) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }

        category.setName(cleanName);
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileUploadService.upload(image, "categories");
            category.setImageUrl(imageUrl);
        }
        return categoryRepository.save(category);
    }

    // ── Hàm Thêm Nhanh (Đã sửa lỗi thiếu Slug) ─────────────────
    @Override
    @Transactional
    public Category createCategory(String name, MultipartFile image) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }

        // 1. Kiểm tra trùng lặp
        if (categoryRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Danh mục '" + cleanName + "' đã tồn tại!");
        }

        // 2. Nếu không trùng thì mới tạo mới
        Category category = new Category();
        category.setName(cleanName);
        category.setSlug(generateUniqueSlug(cleanName));
        category.setActive(true);

        if (image != null && !image.isEmpty()) {
            String imageUrl = fileUploadService.upload(image, "categories");
            category.setImageUrl(imageUrl);
        }

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));

        try {
            categoryRepository.delete(category);
            categoryRepository.flush(); // Ép Hibernate chạy lệnh DELETE ngay lập tức để bắt lỗi khóa ngoại
        } catch (DataIntegrityViolationException e) {
            // Bắt lỗi nếu danh mục này đang được dùng bởi một sản phẩm nào đó
            throw new IllegalStateException("Không thể xóa! Đang có sản phẩm thuộc danh mục này. Vui lòng chuyển sản phẩm sang danh mục khác trước khi xóa.");
        }
    }

    // ── Helper: Logic chuyển đổi chuỗi thành Slug ─────────────────
    private String generateUniqueSlug(String input) {
        // 1. Chuyển tiếng Việt có dấu thành không dấu
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 2. Thay khoảng trắng bằng dấu gạch ngang
        String slug = WHITESPACE.matcher(noAccent).replaceAll("-");

        // 3. Xóa các ký tự đặc biệt và đưa về viết thường
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ROOT);

        // 4. Kiểm tra trùng lặp trong DB, nếu trùng thì thêm -1, -2 vào đuôi
        String candidate = slug;
        int i = 1;
        while (categoryRepository.findBySlug(candidate).isPresent()) {
            candidate = slug + "-" + i++;
        }

        return candidate;
    }
}