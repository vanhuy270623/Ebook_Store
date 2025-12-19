package stu.datn.ebook_store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stu.datn.ebook_store.entity.Book;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa thông tin về subscription và sách của user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLibraryDto {

    // Reading Progress
    private List<Object> readingProgresses;

    // Purchased Books
    private List<Book> purchasedBooks;

    // Subscription Books
    private List<Book> subscriptionBooks;

    // Favorites
    private List<Object> favoriteBooks;

    // Completed Books
    private List<Object> completedBooks;

    // Free Books
    private List<Book> freeBooks;

    // Statistics
    private long totalReading;
    private long totalPurchased;
    private long totalSubscription;
    private long totalFavorites;
    private long totalCompleted;
    private long totalFreeBooks;

    // Subscription Info
    private boolean hasActiveSubscription;
    private String subscriptionPackageName;
    private LocalDateTime subscriptionEndDate;
}

