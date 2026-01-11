package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.Review;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * UserReviewController - Xử lý đánh giá sách từ phía người dùng
 * Chức năng:
 * - Submit review (với validation 20% reading progress)
 * - Update review
 * - Check eligibility
 * Pattern: RESTful API cho AJAX calls
 */
@Controller
@RequestMapping("/api/reviews")
public class UserReviewController extends BaseController {

    private static final double MINIMUM_READING_PROGRESS = 20.0;

    private final ReviewService reviewService;
    private final BookService bookService;
    private final OrderItemService orderItemService;
    private final ReadingProgressService readingProgressService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public UserReviewController(ReviewService reviewService,
                                BookService bookService,
                                OrderItemService orderItemService,
                                ReadingProgressService readingProgressService,
                                SubscriptionService subscriptionService) {
        this.reviewService = reviewService;
        this.bookService = bookService;
        this.orderItemService = orderItemService;
        this.readingProgressService = readingProgressService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Submit hoặc update review cho sách
     * POST /api/reviews
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitReview(
            @RequestParam String bookId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment) {

        Map<String, Object> response = new HashMap<>();
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để đánh giá sách.");
            return ResponseEntity.status(401).body(response);
        }

        // 1. Validate rating
        if (rating == null || rating < 1 || rating > 5) {
            response.put("success", false);
            response.put("message", "Đánh giá phải từ 1 đến 5 sao.");
            return ResponseEntity.badRequest().body(response);
        }

        // 2. Get book
        Optional<Book> bookOpt = bookService.getBookById(bookId);
        if (bookOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy sách.");
            return ResponseEntity.badRequest().body(response);
        }
        Book book = bookOpt.get();

        try {
            // 3. CHECK OWNERSHIP & ACCESS TYPE
            boolean hasAccess = false;
            boolean isVerifiedPurchase = false;
            String accessBadge = "";

            // 3a. Check if user purchased the book
            if (orderItemService.hasUserPurchasedBook(currentUser.getUserId(), bookId)) {
                hasAccess = true;
                isVerifiedPurchase = true;
                accessBadge = "✓ Đã mua sách";
            }
            // 3b. Check if user has active subscription
            else if (subscriptionService.hasActiveSubscription(currentUser.getUserId())) {
                // Check if book is available for subscription
                if (book.getAccessType() == Book.AccessType.SUBSCRIPTION ||
                    book.getAccessType() == Book.AccessType.BOTH) {
                    hasAccess = true;
                    accessBadge = "Thành viên VIP";
                }
            }
            // 3c. Check if book is free
            else if (book.getAccessType() == Book.AccessType.FREE) {
                hasAccess = true;
                accessBadge = "Sách miễn phí";
            }

            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "Bạn chưa sở hữu hoặc không có quyền truy cập sách này để đánh giá.");
                return ResponseEntity.status(403).body(response);
            }

            // 4. CHECK READING PROGRESS (Quy tắc 20% - áp dụng cho TẤT CẢ loại sách)
            // Sử dụng service method mới với Anti-Skimming validation
            if (!readingProgressService.canUserReview(currentUser, bookId)) {
                // Lấy thông tin chi tiết progress để hiển thị
                Optional<ReadingProgress> progressOpt = readingProgressService
                    .getReadingProgressByUserAndBook(currentUser, book);

                double percentRead = progressOpt
                    .map(p -> p.getProgressPercentage() != null ? p.getProgressPercentage().doubleValue() : 0.0)
                    .orElse(0.0);

                String message = progressOpt.isEmpty()
                    ? "Bạn chưa mở sách này lần nào. Hãy đọc ít nhất 20% nội dung để đánh giá."
                    : String.format(
                        "Bạn mới đọc %.1f%%. Hãy đọc ít nhất 20%% nội dung để có thể đưa ra đánh giá khách quan.",
                        percentRead
                    );

                response.put("success", false);
                response.put("message", message);
                response.put("currentProgress", percentRead);
                response.put("requiredProgress", MINIMUM_READING_PROGRESS);
                return ResponseEntity.status(403).body(response);
            }

            // 5. CHECK DUPLICATE (Update or Create)
            Optional<Review> existingReview = reviewService.getReviewByUserAndBook(currentUser, book);

            Review review;
            boolean isUpdate = false;

            if (existingReview.isPresent()) {
                // Mode: UPDATE
                review = existingReview.get();
                review.setRating(rating);
                review.setComment(comment);
                isUpdate = true;
            } else {
                // Mode: CREATE
                review = new Review();
                review.setUser(currentUser);
                review.setBook(book);
                review.setRating(rating);
                review.setComment(comment);
                review.setIsVerifiedPurchase(isVerifiedPurchase);
                review.setIsApproved(true); // Auto-approve by default (có thể thay đổi thành false nếu cần kiểm duyệt)
            }

            // 6. SAVE & RECALCULATE RATING
            Review savedReview = reviewService.saveReview(review);

            response.put("success", true);
            response.put("message", isUpdate
                ? "Cập nhật đánh giá thành công!"
                : "Cảm ơn bạn đã đánh giá!");
            response.put("review", Map.of(
                "reviewId", savedReview.getReviewId(),
                "rating", savedReview.getRating(),
                "comment", savedReview.getComment() != null ? savedReview.getComment() : "",
                "isVerifiedPurchase", savedReview.getIsVerifiedPurchase(),
                "accessBadge", accessBadge,
                "isUpdate", isUpdate
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Check if user can review a book
     * GET /api/reviews/can-review/{bookId}
     */
    @GetMapping("/can-review/{bookId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkCanReview(@PathVariable String bookId) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            response.put("canReview", false);
            response.put("reason", "Bạn cần đăng nhập để đánh giá sách.");
            return ResponseEntity.ok(response);
        }

        Optional<Book> bookOpt = bookService.getBookById(bookId);
        if (bookOpt.isEmpty()) {
            response.put("canReview", false);
            response.put("reason", "Không tìm thấy sách.");
            return ResponseEntity.ok(response);
        }
        Book book = bookOpt.get();

        // Check ownership
        boolean hasAccess = false;
        boolean isVerifiedPurchase = false;
        String accessBadge = "";

        if (orderItemService.hasUserPurchasedBook(currentUser.getUserId(), bookId)) {
            hasAccess = true;
            isVerifiedPurchase = true;
            accessBadge = "✓ Đã mua sách";
        } else if (subscriptionService.hasActiveSubscription(currentUser.getUserId())) {
            if (book.getAccessType() == Book.AccessType.SUBSCRIPTION ||
                book.getAccessType() == Book.AccessType.BOTH) {
                hasAccess = true;
                accessBadge = "Thành viên VIP";
            }
        } else if (book.getAccessType() == Book.AccessType.FREE) {
            hasAccess = true;
            accessBadge = "Sách miễn phí";
        }

        if (!hasAccess) {
            response.put("canReview", false);
            response.put("reason", "Bạn chưa sở hữu sách này.");
            return ResponseEntity.ok(response);
        }

        // Check reading progress (áp dụng cho TẤT CẢ loại sách)
        Optional<ReadingProgress> progressOpt = readingProgressService
            .getReadingProgressByUserAndBook(currentUser, book);

        if (progressOpt.isEmpty()) {
            response.put("canReview", false);
            response.put("reason", "Bạn chưa mở sách này. Hãy đọc ít nhất 20% để đánh giá.");
            response.put("currentProgress", 0.0);
            response.put("requiredProgress", MINIMUM_READING_PROGRESS);
            return ResponseEntity.ok(response);
        }

        ReadingProgress progress = progressOpt.get();
        double percentRead = progress.getProgressPercentage() != null
            ? progress.getProgressPercentage().doubleValue()
            : 0.0;

        if (percentRead < MINIMUM_READING_PROGRESS) {
            response.put("canReview", false);
            response.put("reason", String.format("Bạn mới đọc %.1f%%. Hãy đọc thêm để đạt 20%%.", percentRead));
            response.put("currentProgress", percentRead);
            response.put("requiredProgress", MINIMUM_READING_PROGRESS);
            return ResponseEntity.ok(response);
        }

        // Check if already reviewed
        Optional<Review> existingReview = reviewService.getReviewByUserAndBook(currentUser, book);

        response.put("canReview", true);
        response.put("hasExistingReview", existingReview.isPresent());
        response.put("accessBadge", accessBadge);
        response.put("isVerifiedPurchase", isVerifiedPurchase);
        response.put("currentProgress", percentRead);

        if (existingReview.isPresent()) {
            Review review = existingReview.get();
            response.put("existingReview", Map.of(
                "rating", review.getRating(),
                "comment", review.getComment() != null ? review.getComment() : ""
            ));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete user's own review
     * DELETE /api/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable String reviewId) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        Optional<Review> reviewOpt = reviewService.getReviewById(reviewId);
        if (reviewOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đánh giá.");
            return ResponseEntity.badRequest().body(response);
        }

        Review review = reviewOpt.get();

        // Check ownership
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền xóa đánh giá này.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            reviewService.deleteReview(reviewId);
            response.put("success", true);
            response.put("message", "Xóa đánh giá thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

