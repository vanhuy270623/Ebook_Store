package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, String> {
    Optional<ReadingProgress> findByUserAndBook(User user, Book book);

    List<ReadingProgress> findByUser(User user);

    @Query("SELECT DISTINCT rp FROM ReadingProgress rp " +
           "LEFT JOIN FETCH rp.book b " +
           "LEFT JOIN FETCH b.authors " +
           "LEFT JOIN FETCH b.bookCategory " +
           "WHERE rp.user = :user")
    List<ReadingProgress> findByUserWithBookDetails(@Param("user") User user);

    List<ReadingProgress> findByUserAndIsFavoriteTrue(User user);

    List<ReadingProgress> findByUserOrderByLastReadAtDesc(User user);

    List<ReadingProgress> findByUserAndIsCompletedTrue(User user);

    List<ReadingProgress> findByUserAndAccessType(User user, ReadingProgress.AccessType accessType);

    List<ReadingProgress> findByBook(Book book);

    long countByUserAndIsCompletedTrue(User user);

    @Query("SELECT rp FROM ReadingProgress rp WHERE rp.user = :user AND rp.progressPercentage > 0 AND rp.isCompleted = false ORDER BY rp.lastReadAt DESC")
    List<ReadingProgress> findContinueReading(@Param("user") User user);
}

