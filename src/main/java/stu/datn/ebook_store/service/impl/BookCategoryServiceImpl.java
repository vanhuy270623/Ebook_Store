package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.BookCategory;
import stu.datn.ebook_store.repository.BookCategoryRepository;
import stu.datn.ebook_store.service.BookCategoryService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookCategoryServiceImpl implements BookCategoryService {

    private final BookCategoryRepository bookCategoryRepository;

    @Autowired
    public BookCategoryServiceImpl(BookCategoryRepository bookCategoryRepository) {
        this.bookCategoryRepository = bookCategoryRepository;
    }

    @Override
    public List<BookCategory> getAllCategories() {
        return bookCategoryRepository.findAll();
    }

    @Override
    public Optional<BookCategory> getCategoryById(String categoryId) {
        return bookCategoryRepository.findById(categoryId);
    }

    @Override
    public Optional<BookCategory> getCategoryByName(String categoryName) {
        return bookCategoryRepository.findByCategoryName(categoryName);
    }

    @Override
    public BookCategory saveCategory(BookCategory category) {
        if (category.getBookCategoryId() == null || category.getBookCategoryId().isEmpty()) {
            category.setBookCategoryId(generateCategoryId());
        }
        return bookCategoryRepository.save(category);
    }

    @Override
    public void deleteCategory(String categoryId) {
        bookCategoryRepository.deleteById(categoryId);
    }

    private String generateCategoryId() {
        long count = bookCategoryRepository.count();
        return "category_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

