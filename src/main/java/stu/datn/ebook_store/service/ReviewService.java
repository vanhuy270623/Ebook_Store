package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Review;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Review Service Interface
 * REFACTORED: Removed 2 unused methods (DataTables filter)
 */
public interface ReviewService {
    List<Review> getAllReviews();
    Optional<Review> getReviewById(String reviewId);
    Review saveReview(Review review);
    void deleteReview(String reviewId);
    List<Review> getReviewsByBook(Book book); // GIỮ LẠI - Admin xem all reviews
    Optional<Review> getReviewByUserAndBook(User user, Book book);
    List<Review> getApprovedReviewsByBook(Book book);
    List<Review> getUnapprovedReviews();
    List<Review> getVerifiedPurchaseReviews();
    Double getAverageRatingForBook(Book book);
    long countApprovedReviewsByBook(Book book);
    void approveReview(String reviewId);
    void rejectReview(String reviewId);
    long getTotalReviewsCount();
}

