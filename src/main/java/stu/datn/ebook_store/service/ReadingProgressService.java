package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Book;

import java.util.List;
import java.util.Optional;

public interface ReadingProgressService {
    List<ReadingProgress> getAllReadingProgress();
    Optional<ReadingProgress> getReadingProgressById(String progressId);
    Optional<ReadingProgress> getReadingProgressByUserAndBook(User user, Book book);
    ReadingProgress saveReadingProgress(ReadingProgress readingProgress);
    void deleteReadingProgress(String progressId);
    List<ReadingProgress> getReadingProgressByUser(User user);
    List<ReadingProgress> getReadingProgressByUserWithBookDetails(User user);
    List<ReadingProgress> getFavoriteBooksByUser(User user);
    List<ReadingProgress> getRecentReadingByUser(User user);
    List<ReadingProgress> getCompletedBooksByUser(User user);
    List<ReadingProgress> getReadingProgressByUserAndAccessType(User user, ReadingProgress.AccessType accessType);
    List<ReadingProgress> getReadingProgressByBook(Book book);
    long countCompletedBooksByUser(User user);
    List<ReadingProgress> getContinueReadingByUser(User user);
    void markAsFavorite(String progressId);
    void unmarkAsFavorite(String progressId);
    void markAsCompleted(String progressId);
    void updateProgress(String progressId, Float percentage, String location);
}

