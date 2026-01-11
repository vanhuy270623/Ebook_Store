package stu.datn.ebook_store.service;

import stu.datn.ebook_store.dto.ProgressSyncRequest;
import stu.datn.ebook_store.dto.ProgressSyncResponse;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Book;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Reading Progress management
 * REFACTORED: Removed 13 unused methods (có thể filter/sort trong code)
 */
public interface ReadingProgressService {
    // Core methods - ĐANG DÙNG
    Optional<ReadingProgress> getReadingProgressByUserAndBook(User user, Book book);
    ReadingProgress saveReadingProgress(ReadingProgress readingProgress);
    List<ReadingProgress> getReadingProgressByUser(User user);
    List<ReadingProgress> getReadingProgressByUserWithBookDetails(User user);
    List<ReadingProgress> getFavoriteBooksByUser(User user);
    boolean toggleFavorite(User user, String bookId);

    // Bookmark management methods - ĐANG DÙNG
    void addBookmark(String progressId, String location, Integer pageNumber, Float percentage, String note);
    void removeBookmark(String progressId, String bookmarkId);
    List<ReadingProgress.BookmarkData> getBookmarks(String progressId);

    // Anti-Skimming & Progress Tracking - MỚI
    ProgressSyncResponse syncProgress(User user, ProgressSyncRequest request);
    boolean canUserReview(User user, String bookId);
}

