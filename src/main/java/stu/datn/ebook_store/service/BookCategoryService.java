package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.BookCategory;

import java.util.List;
import java.util.Optional;

public interface BookCategoryService {
    List<BookCategory> getAllCategories();
    Optional<BookCategory> getCategoryById(String categoryId);
    Optional<BookCategory> getCategoryByName(String categoryName);
    BookCategory saveCategory(BookCategory category);
    void deleteCategory(String categoryId);
    List<BookCategory> searchCategories(String keyword);
}

