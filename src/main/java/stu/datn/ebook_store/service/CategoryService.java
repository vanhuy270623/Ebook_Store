package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories();
    Optional<Category> getCategoryById(String categoryId);
    Optional<Category> getCategoryByName(String categoryName);
    Category saveCategory(Category category);
    void deleteCategory(String categoryId);
}

