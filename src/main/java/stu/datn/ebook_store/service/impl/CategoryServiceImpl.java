package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Category;
import stu.datn.ebook_store.repository.CategoryRepository;
import stu.datn.ebook_store.service.CategoryService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId);
    }

    @Override
    public Optional<Category> getCategoryByName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName);
    }

    @Override
    public Category saveCategory(Category category) {
        if (category.getCategoryId() == null || category.getCategoryId().isEmpty()) {
            category.setCategoryId(generateCategoryId());
        }
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(String categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    private String generateCategoryId() {
        long count = categoryRepository.count();
        return "category_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

