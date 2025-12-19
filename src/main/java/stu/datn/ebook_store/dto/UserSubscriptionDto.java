package stu.datn.ebook_store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stu.datn.ebook_store.entity.Book;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa thông tin về subscription đang active của user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionDto {

    private boolean hasActiveSubscription;
    private String subscriptionPackageName;
    private LocalDateTime subscriptionEndDate;
    private List<Book> subscriptionBooks;
    private int maxDevices;

    public UserSubscriptionDto(boolean hasActiveSubscription) {
        this.hasActiveSubscription = hasActiveSubscription;
        this.subscriptionBooks = List.of();
    }
}

