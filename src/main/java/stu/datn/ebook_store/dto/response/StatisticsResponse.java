package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO for Dashboard Statistics (Admin)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersThisMonth;

    // Book statistics
    private Long totalBooks;
    private Long totalAuthors;
    private Long totalCategories;

    // Order statistics
    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueThisYear;

    // Review statistics
    private Long totalReviews;
    private Long pendingReviews;
    private Double averageRating;

    // Subscription statistics
    private Long totalSubscriptions;
    private Long activeSubscriptions;

    // Content statistics
    private Long totalPosts;
    private Long publishedPosts;
    private Long totalBanners;
    private Long activeBanners;

    // System statistics
    private Long totalViews;
    private Long viewsThisMonth;
}

