package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Review;

import java.time.LocalDateTime;

/**
 * DTO for Review response (Admin view)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private String reviewId;
    private String userId;
    private String username;
    private String userAvatar;
    private String bookId;
    private String bookTitle;
    private Integer rating;
    private String comment;
    private Boolean isVerifiedPurchase;
    private Boolean isApproved;
    private LocalDateTime createdAt;

    // Constructor from Entity
    public ReviewResponse(Review review) {
        this.reviewId = review.getReviewId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.isVerifiedPurchase = review.getIsVerifiedPurchase();
        this.isApproved = review.getIsApproved();
        this.createdAt = review.getCreatedAt();

        if (review.getUser() != null) {
            this.userId = review.getUser().getUserId();
            this.username = review.getUser().getUsername();
            this.userAvatar = review.getUser().getAvatarUrl();
        }

        if (review.getBook() != null) {
            this.bookId = review.getBook().getBookId();
            this.bookTitle = review.getBook().getTitle();
        }
    }
}

