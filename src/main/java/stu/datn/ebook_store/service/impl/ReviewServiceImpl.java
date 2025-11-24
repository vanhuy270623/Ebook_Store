package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Review;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.ReviewRepository;
import stu.datn.ebook_store.service.ReviewService;
import stu.datn.ebook_store.service.BookService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository, BookService bookService) {
        this.reviewRepository = reviewRepository;
        this.bookService = bookService;
    }

    @Override
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Override
    public Optional<Review> getReviewById(String reviewId) {
        return reviewRepository.findById(reviewId);
    }

    @Override
    public Review saveReview(Review review) {
        if (review.getReviewId() == null || review.getReviewId().isEmpty()) {
            review.setReviewId(generateReviewId());
        }
        Review savedReview = reviewRepository.save(review);
        // Update book rating after saving review
        if (savedReview.getBook() != null) {
            bookService.updateBookRating(savedReview.getBook().getBookId());
        }
        return savedReview;
    }

    @Override
    public void deleteReview(String reviewId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            String bookId = review.getBook().getBookId();
            reviewRepository.deleteById(reviewId);
            // Update book rating after deleting review
            bookService.updateBookRating(bookId);
        }
    }

    @Override
    public List<Review> getReviewsByBook(Book book) {
        return reviewRepository.findByBook(book);
    }

    @Override
    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUser(user);
    }

    @Override
    public Optional<Review> getReviewByUserAndBook(User user, Book book) {
        return reviewRepository.findByUserAndBook(user, book);
    }

    @Override
    public List<Review> getApprovedReviewsByBook(Book book) {
        return reviewRepository.findByBookAndIsApprovedTrue(book);
    }

    @Override
    public List<Review> getUnapprovedReviews() {
        return reviewRepository.findByIsApprovedFalse();
    }

    @Override
    public List<Review> getVerifiedPurchaseReviews() {
        return reviewRepository.findByIsVerifiedPurchaseTrue();
    }

    @Override
    public List<Review> getReviewsByBookAndRating(Book book, Integer rating) {
        return reviewRepository.findByBookAndRating(book, rating);
    }

    @Override
    public Double getAverageRatingForBook(Book book) {
        return reviewRepository.getAverageRatingForBook(book);
    }

    @Override
    public long countApprovedReviewsByBook(Book book) {
        return reviewRepository.countByBookAndIsApprovedTrue(book);
    }

    @Override
    public void approveReview(String reviewId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            review.setIsApproved(true);
            reviewRepository.save(review);
            // Update book rating after approval
            bookService.updateBookRating(review.getBook().getBookId());
        }
    }

    @Override
    public void rejectReview(String reviewId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            review.setIsApproved(false);
            reviewRepository.save(review);
            // Update book rating after rejection
            bookService.updateBookRating(review.getBook().getBookId());
        }
    }

    private String generateReviewId() {
        long count = reviewRepository.count();
        return "review_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

