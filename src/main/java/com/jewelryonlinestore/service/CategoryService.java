package com.jewelryonlinestore.service;

import com.jewelryonlinestore.entity.Category;

import java.util.List;

public interface CategoryService {
	List<Category> getRootCategories();
	List<Category> getAllCategories();
	List<Category> getAllCategoriesTree();
	Category getCategoryBySlug(String slug);
}

