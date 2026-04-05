package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.Category;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
	List<Category> getRootCategories();
	List<Category> getAllCategories();
	List<Category> getAllCategoriesTree();
	Category getCategoryBySlug(String slug);

	// API hỗ trợ thêm nhanh Category từ form Admin
	Category createCategory(String name, MultipartFile image);
	void deleteCategory(Long id);
	Category updateCategory(Long id, String name, MultipartFile image);
}