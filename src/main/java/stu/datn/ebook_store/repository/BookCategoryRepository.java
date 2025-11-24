package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.BookCategory;

import java.util.List;
import java.util.Optional;

public interface BookCategoryRepository extends JpaRepository<BookCategory, String> {
    Optional<BookCategory> findByCategoryName(String categoryName);
    List<BookCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
}

