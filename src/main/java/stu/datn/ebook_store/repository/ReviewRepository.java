package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Review;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByBook(Book book);

    List<Review> findByUser(User user);

    Optional<Review> findByUserAndBook(User user, Book book);

    List<Review> findByBookAndIsApprovedTrue(Book book);

    List<Review> findByIsApprovedFalse();

    List<Review> findByIsVerifiedPurchaseTrue();

    List<Review> findByBookAndRating(Book book, Integer rating);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book = :book AND r.isApproved = true")
    Double getAverageRatingForBook(@Param("book") Book book);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.book.bookId = :bookId AND r.isApproved = true")
    List<Object[]> calculateAverageRatingForBook(@Param("bookId") String bookId);

    long countByBookAndIsApprovedTrue(Book book);
}

