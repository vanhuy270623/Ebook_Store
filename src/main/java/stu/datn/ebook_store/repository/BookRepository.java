package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, String> {
    Optional<Book> findByTitle(String title);

    List<Book> findByBookCategory(BookCategory category);

    List<Book> findByAccessType(Book.AccessType accessType);

    @Query("SELECT b FROM Book b WHERE b.title LIKE %:keyword% OR b.description LIKE %:keyword%")
    List<Book> searchByKeyword(@Param("keyword") String keyword);

    List<Book> findTop10ByOrderByViewCountDesc();

    List<Book> findTop10ByOrderByCreatedAtDesc();

    List<Book> findByIsDownloadableTrue();

    @Query("SELECT b FROM Book b JOIN b.authors a WHERE a.authorId = :authorId")
    List<Book> findByAuthor(@Param("authorId") String authorId);

    @Query("SELECT b FROM Book b WHERE b.averageRating >= :minRating ORDER BY b.averageRating DESC")
    List<Book> findByMinRating(@Param("minRating") Float minRating);

    long countByAccessType(Book.AccessType accessType);

    // Admin statistics methods
    @Query("SELECT b FROM Book b ORDER BY b.createdAt DESC")
    List<Book> findTopBooksOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.averageRating > 0 ORDER BY b.averageRating DESC, b.totalReviews DESC")
    List<Book> findTopBooksByRating(org.springframework.data.domain.Pageable pageable);
}

