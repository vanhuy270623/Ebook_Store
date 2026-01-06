package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.BookCategory;

import java.util.List;
import java.util.Optional;

public interface BookCategoryRepository extends JpaRepository<BookCategory, String> {
    Optional<BookCategory> findByCategoryName(String categoryName);
    List<BookCategory> findByIsActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT c FROM BookCategory c WHERE " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<BookCategory> searchCategories(@Param("keyword") String keyword);
}

