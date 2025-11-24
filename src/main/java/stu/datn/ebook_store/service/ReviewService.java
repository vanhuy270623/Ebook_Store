package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Review;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface ReviewService {
    List<Review> getAllReviews();
    Optional<Review> getReviewById(String reviewId);
    Review saveReview(Review review);
    void deleteReview(String reviewId);
    List<Review> getReviewsByBook(Book book);
    List<Review> getReviewsByUser(User user);
    Optional<Review> getReviewByUserAndBook(User user, Book book);
    List<Review> getApprovedReviewsByBook(Book book);
    List<Review> getUnapprovedReviews();
    List<Review> getVerifiedPurchaseReviews();
    List<Review> getReviewsByBookAndRating(Book book, Integer rating);
    Double getAverageRatingForBook(Book book);
    long countApprovedReviewsByBook(Book book);
    void approveReview(String reviewId);
    void rejectReview(String reviewId);
}

