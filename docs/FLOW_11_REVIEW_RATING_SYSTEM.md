# ⭐ FLOW 11: REVIEW & RATING SYSTEM (Hệ Thống Đánh Giá & Xếp Hạng)

## ⚠️ Implementation Status

**Backend:** ✅ 90% Complete
- ✅ Review entity và repository
- ✅ ReviewService với CRUD operations
- ✅ Admin moderation endpoints
- ⚠️ User review submission endpoint (needs testing)

**Frontend:** ⚠️ 40% Complete
- ❌ User review submission form (MISSING)
- ❌ Review display in book detail page (INCOMPLETE)
- ⚠️ Admin moderation UI (BASIC ONLY)
- ✅ Review entity structure

**Priority:** MEDIUM  
**Blocking:** No - System can function without reviews  
**Recommended:** Complete for better user engagement  

---

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 11.1: User - Submit Review](#flow-111-user---submit-review)
3. [Flow 11.2: User - View Reviews](#flow-112-user---view-reviews)
4. [Flow 11.3: User - Edit/Delete Review](#flow-113-user---editdelete-review)
5. [Flow 11.4: Admin - Review Moderation](#flow-114-admin---review-moderation)
6. [Flow 11.5: Review Analytics](#flow-115-review-analytics)
7. [Rating Calculation](#rating-calculation)

---

## Tổng Quan

### Review System Architecture
```
┌────────────────────────────────────────────────────────────┐
│                   REVIEW SYSTEM                            │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  USER FLOW                  │      ADMIN FLOW              │
│  ─────────                  │      ──────────              │
│                             │                              │
│  ✍️  Submit Review          │   📋 View All Reviews        │
│  ⭐ Rate 1-5 stars          │   ✅ Approve Review          │
│  📝 Write comment           │   ❌ Reject Review           │
│  📸 Upload images           │   🗑️  Delete Review          │
│  ✏️  Edit review            │   📊 Review Analytics        │
│  🗑️  Delete review          │   🔍 Filter & Search         │
│                             │                              │
└────────────────────────────────────────────────────────────┘
```

### Components

**Controllers**:
- `UserBookController.java` - User submits reviews
- `AdminReviewController.java` - Admin moderates reviews

**Services**:
- `ReviewService.java` - Review CRUD operations
- `BookService.java` - Update book ratings
- `OrderService.java` - Verify purchase for verified reviews

**Entities**:
- `Review.java` - Review data
- `Book.java` - Book with average rating
- `User.java` - Reviewer information

### URLs

**User Endpoints**:
- `POST /user/books/{bookId}/review` - Submit review
- `GET /user/books/{bookId}` - View book with reviews
- `POST /user/reviews/edit/{reviewId}` - Edit review
- `POST /user/reviews/delete/{reviewId}` - Delete review

**Admin Endpoints**:
- `GET /admin/reviews` - List all reviews
- `GET /admin/reviews?filter={status}` - Filter reviews
- `GET /admin/reviews/view/{id}` - View review details
- `POST /admin/reviews/approve/{id}` - Approve review
- `POST /admin/reviews/reject/{id}` - Reject review
- `POST /admin/reviews/delete/{id}` - Delete review
- `POST /admin/reviews/bulk-approve` - Bulk approve reviews
- `POST /admin/reviews/bulk-reject` - Bulk reject reviews

---

## Flow 11.1: User - Submit Review

### Sequence Diagram
```
User → Browser → UserBookController → ReviewService → OrderService → Database
  │       │              │                  │               │            │
  │ Click "Write Review"                                                 │
  │───────────────────►│                                                 │
  │       │            │ Check if logged in                             │
  │       │            │ Check if user owns book                        │
  │       │            ├──────────────────►│                            │
  │       │            │                   │ Check orders              │
  │       │            │                   ├────────────────►│         │
  │       │            │◄──────────────────┤                           │
  │       │            │                                                │
  │ Submit review form                                                  │
  │───────────────────►│                                                │
  │       │            │ Validate rating (1-5)                         │
  │       │            │ Validate comment (min length)                 │
  │       │            │ Check duplicate review                        │
  │       │            ├──────────────────►│                           │
  │       │            │                   │ existsByUserAndBook()     │
  │       │            │                   ├──────────────────────────►│
  │       │            │◄──────────────────┤                           │
  │       │            │ Create review                                 │
  │       │            ├──────────────────►│                           │
  │       │            │                   │ saveReview()              │
  │       │            │                   ├──────────────────────────►│
  │       │            │◄──────────────────┤                           │
  │       │            │ Update book rating                            │
  │◄───────────────────┤ Success message                               │
  │ Display success                                                     │
```

### Implementation Details

**Controller Method**:
```java
@PostMapping("/{bookId}/review")
public String submitReview(@PathVariable String bookId,
                          @RequestParam Integer rating,
                          @RequestParam String comment,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
    try {
        // 1. Validate user
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        // 2. Validate book exists
        Book book = bookService.getBookById(bookId);
        if (book == null) {
            redirectAttributes.addFlashAttribute("error", "Sách không tồn tại");
            return "redirect:/user/books";
        }

        // 3. Validate rating
        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Đánh giá phải từ 1-5 sao");
            return "redirect:/user/books/" + bookId;
        }

        // 4. Validate comment length
        if (comment == null || comment.trim().length() < 10) {
            redirectAttributes.addFlashAttribute("error", 
                "Nhận xét phải có ít nhất 10 ký tự");
            return "redirect:/user/books/" + bookId;
        }

        // 5. Check if user already reviewed this book
        if (reviewService.hasUserReviewedBook(currentUser.getUserId(), bookId)) {
            redirectAttributes.addFlashAttribute("error", 
                "Bạn đã đánh giá sách này rồi");
            return "redirect:/user/books/" + bookId;
        }

        // 6. Check if user owns the book (for verified purchase badge)
        boolean isVerifiedPurchase = orderService.hasUserPurchasedBook(
            currentUser.getUserId(), bookId
        );

        // 7. Create review
        Review review = new Review();
        review.setReviewId("rev_" + System.currentTimeMillis());
        review.setUser(currentUser);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setIsVerifiedPurchase(isVerifiedPurchase);
        review.setIsApproved(false); // Requires admin approval
        review.setCreatedAt(LocalDateTime.now());
        
        reviewService.saveReview(review);

        // 8. Update book's average rating
        bookService.updateBookRating(bookId);

        redirectAttributes.addFlashAttribute("success", 
            "Cảm ơn đánh giá của bạn! Đánh giá sẽ được hiển thị sau khi được duyệt.");
        
        return "redirect:/user/books/" + bookId;
        
    } catch (Exception e) {
        log.error("Error submitting review", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi gửi đánh giá. Vui lòng thử lại.");
        return "redirect:/user/books/" + bookId;
    }
}
```

**Service Methods**:
```java
// ReviewService.java
public boolean hasUserReviewedBook(String userId, String bookId) {
    return reviewRepository.existsByUserUserIdAndBookBookId(userId, bookId);
}

public Review saveReview(Review review) {
    return reviewRepository.save(review);
}

// OrderService.java
public boolean hasUserPurchasedBook(String userId, String bookId) {
    List<Order> orders = orderRepository.findCompletedOrdersByUserId(userId);
    
    return orders.stream()
        .flatMap(order -> order.getOrderItems().stream())
        .anyMatch(item -> item.getBook().getBookId().equals(bookId));
}
```

**Database Insert**:
```sql
INSERT INTO reviews (
    review_id, user_id, book_id, rating, comment,
    is_verified_purchase, is_approved, created_at
) VALUES (
    'rev_1733556789', 'user_001', 'book_001', 5, 
    'Sách rất hay, nội dung dễ hiểu và hữu ích!',
    true, false, NOW()
);
```

### Review Form Template

```html
<!-- Review submission form -->
<div class="review-form" th:if="${canReview}">
    <h3>Viết đánh giá</h3>
    
    <form th:action="@{/user/books/{id}/review(id=${book.bookId})}" method="post">
        
        <!-- Star Rating -->
        <div class="rating-input">
            <label>Đánh giá của bạn:</label>
            <div class="star-rating">
                <input type="radio" id="star5" name="rating" value="5" required>
                <label for="star5">⭐</label>
                
                <input type="radio" id="star4" name="rating" value="4">
                <label for="star4">⭐</label>
                
                <input type="radio" id="star3" name="rating" value="3">
                <label for="star3">⭐</label>
                
                <input type="radio" id="star2" name="rating" value="2">
                <label for="star2">⭐</label>
                
                <input type="radio" id="star1" name="rating" value="1">
                <label for="star1">⭐</label>
            </div>
        </div>
        
        <!-- Comment Textarea -->
        <div class="form-group">
            <label for="comment">Nhận xét:</label>
            <textarea id="comment" name="comment" 
                      class="form-control" rows="5" 
                      minlength="10" maxlength="1000"
                      placeholder="Chia sẻ cảm nhận của bạn về cuốn sách này (tối thiểu 10 ký tự)..."
                      required></textarea>
            <small class="form-text text-muted">
                <span id="charCount">0</span> / 1000 ký tự
            </small>
        </div>
        
        <!-- Submit Button -->
        <button type="submit" class="btn btn-primary">Gửi đánh giá</button>
    </form>
</div>

<!-- Character counter script -->
<script>
document.getElementById('comment').addEventListener('input', function() {
    const count = this.value.length;
    document.getElementById('charCount').textContent = count;
});
</script>
```

---

## Flow 11.2: User - View Reviews

### Sequence Diagram
```
User → Browser → UserBookController → ReviewService → Database
  │       │              │                  │             │
  │ View book detail page                                │
  │───────────────────►│                                  │
  │       │            │ getBookById()                    │
  │       │            │ getApprovedReviewsByBook()       │
  │       │            ├─────────────────────►│           │
  │       │            │                      │ SELECT    │
  │       │            │                      ├──────────►│
  │       │            │◄─────────────────────┤           │
  │       │            │ calculateAverageRating()         │
  │◄───────────────────┤ book-detail.html                │
  │ Display book with reviews                            │
```

### Implementation Details

**Controller Method**:
```java
@GetMapping("/{id}")
public String bookDetail(@PathVariable String id, Model model, 
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
    // Get book
    Book book = bookService.getBookById(id);
    if (book == null) {
        redirectAttributes.addFlashAttribute("error", "Sách không tồn tại");
        return "redirect:/user/books";
    }

    model.addAttribute("book", book);

    // Get approved reviews
    List<Review> reviews = reviewService.getApprovedReviewsByBook(id);
    model.addAttribute("reviews", reviews);
    model.addAttribute("reviewCount", reviews.size());

    // Calculate rating statistics
    Map<String, Object> ratingStats = calculateRatingStatistics(reviews);
    model.addAttribute("ratingStats", ratingStats);

    // Check if current user can review
    User currentUser = getCurrentUser(authentication);
    if (currentUser != null) {
        boolean hasReviewed = reviewService.hasUserReviewedBook(
            currentUser.getUserId(), id
        );
        boolean hasPurchased = orderService.hasUserPurchasedBook(
            currentUser.getUserId(), id
        );
        
        model.addAttribute("canReview", !hasReviewed);
        model.addAttribute("hasPurchased", hasPurchased);
    }

    return "user/book-detail";
}
```

**Rating Statistics Helper**:
```java
private Map<String, Object> calculateRatingStatistics(List<Review> reviews) {
    Map<String, Object> stats = new HashMap<>();
    
    if (reviews.isEmpty()) {
        stats.put("averageRating", 0.0);
        stats.put("ratingDistribution", new int[]{0, 0, 0, 0, 0});
        return stats;
    }

    // Calculate average
    double average = reviews.stream()
        .mapToInt(Review::getRating)
        .average()
        .orElse(0.0);
    
    stats.put("averageRating", Math.round(average * 10) / 10.0);

    // Calculate distribution
    int[] distribution = new int[5]; // Index 0 = 1 star, Index 4 = 5 stars
    for (Review review : reviews) {
        distribution[review.getRating() - 1]++;
    }
    
    stats.put("ratingDistribution", distribution);
    stats.put("totalReviews", reviews.size());

    return stats;
}
```

### Reviews Display Template

```html
<!-- Reviews section -->
<div class="reviews-section">
    <h2>Đánh giá từ độc giả</h2>
    
    <!-- Rating Summary -->
    <div class="rating-summary">
        <div class="average-rating">
            <h3 th:text="${ratingStats.averageRating}">4.5</h3>
            <div class="stars">
                <span th:each="i : ${#numbers.sequence(1, 5)}"
                      th:classappend="${i <= ratingStats.averageRating} ? 'star-filled' : 'star-empty'">
                    ⭐
                </span>
            </div>
            <p th:text="${reviewCount} + ' đánh giá'">120 đánh giá</p>
        </div>
        
        <!-- Rating Distribution -->
        <div class="rating-distribution">
            <div th:each="star, iterStat : ${#numbers.sequence(5, 1, -1)}" 
                 class="rating-bar">
                <span class="star-label" th:text="${star} + ' ⭐'">5 ⭐</span>
                <div class="progress">
                    <div class="progress-bar" 
                         th:style="'width: ' + (${ratingStats.ratingDistribution[star-1]} * 100.0 / ${reviewCount}) + '%'">
                    </div>
                </div>
                <span class="count" 
                      th:text="${ratingStats.ratingDistribution[star-1]}">50</span>
            </div>
        </div>
    </div>
    
    <!-- Individual Reviews -->
    <div class="reviews-list">
        <div th:each="review : ${reviews}" class="review-card">
            <div class="review-header">
                <img th:src="${review.user.avatar}" 
                     alt="Avatar" class="reviewer-avatar">
                <div class="reviewer-info">
                    <h4 th:text="${review.user.fullName}">John Doe</h4>
                    <div class="review-meta">
                        <span class="rating">
                            <span th:each="i : ${#numbers.sequence(1, review.rating)}">⭐</span>
                        </span>
                        <span class="date" 
                              th:text="${#temporals.format(review.createdAt, 'dd/MM/yyyy')}">
                            07/12/2025
                        </span>
                        <span th:if="${review.isVerifiedPurchase}" 
                              class="badge badge-success">
                            ✓ Đã mua sách
                        </span>
                    </div>
                </div>
            </div>
            
            <div class="review-content">
                <p th:text="${review.comment}">
                    Sách rất hay, nội dung dễ hiểu và hữu ích!
                </p>
            </div>
            
            <!-- Helpful buttons -->
            <div class="review-actions">
                <button class="btn-helpful" 
                        th:onclick="'markHelpful(\'' + ${review.reviewId} + '\')'">
                    👍 Hữu ích (<span th:text="${review.helpfulCount}">5</span>)
                </button>
            </div>
        </div>
    </div>
</div>
```

---

## Flow 11.3: User - Edit/Delete Review

### Edit Review

**Controller Method**:
```java
@PostMapping("/reviews/edit/{reviewId}")
public String editReview(@PathVariable String reviewId,
                        @RequestParam Integer rating,
                        @RequestParam String comment,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
    try {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Review review = reviewService.getReviewById(reviewId).orElse(null);
        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Đánh giá không tồn tại");
            return "redirect:/user/profile";
        }

        // Validate ownership
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Không có quyền chỉnh sửa");
            return "redirect:/user/profile";
        }

        // Update review
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setIsApproved(false); // Requires re-approval
        review.setUpdatedAt(LocalDateTime.now());
        
        reviewService.updateReview(review);

        // Update book rating
        bookService.updateBookRating(review.getBook().getBookId());

        redirectAttributes.addFlashAttribute("success", 
            "Cập nhật đánh giá thành công");
        
        return "redirect:/user/books/" + review.getBook().getBookId();
        
    } catch (Exception e) {
        log.error("Error editing review", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật đánh giá");
        return "redirect:/user/profile";
    }
}
```

### Delete Review

**Controller Method**:
```java
@PostMapping("/reviews/delete/{reviewId}")
public String deleteReview(@PathVariable String reviewId,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
    try {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Review review = reviewService.getReviewById(reviewId).orElse(null);
        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Đánh giá không tồn tại");
            return "redirect:/user/profile";
        }

        // Validate ownership
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Không có quyền xóa");
            return "redirect:/user/profile";
        }

        String bookId = review.getBook().getBookId();
        
        // Soft delete
        review.setDeletedAt(LocalDateTime.now());
        reviewService.updateReview(review);

        // Update book rating
        bookService.updateBookRating(bookId);

        redirectAttributes.addFlashAttribute("success", "Đã xóa đánh giá");
        
        return "redirect:/user/books/" + bookId;
        
    } catch (Exception e) {
        log.error("Error deleting review", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi xóa đánh giá");
        return "redirect:/user/profile";
    }
}
```

---

## Flow 11.4: Admin - Review Moderation

### List Reviews

**Controller Method**:
```java
@GetMapping
public String reviewsList(@RequestParam(required = false, defaultValue = "unapproved") String filter,
                         Model model) {
    List<Review> reviews;

    switch (filter.toLowerCase()) {
        case "approved":
            reviews = reviewService.getAllReviews().stream()
                .filter(Review::getIsApproved)
                .collect(Collectors.toList());
            break;
        case "rejected":
            reviews = reviewService.getAllReviews().stream()
                .filter(r -> !r.getIsApproved())
                .collect(Collectors.toList());
            break;
        case "verified":
            reviews = reviewService.getVerifiedPurchaseReviews();
            break;
        case "unapproved":
        default:
            reviews = reviewService.getUnapprovedReviews();
            break;
    }

    model.addAttribute("reviews", reviews);
    model.addAttribute("totalReviews", reviews.size());
    model.addAttribute("currentFilter", filter);

    // Quick statistics
    long totalUnapproved = reviewService.getUnapprovedReviews().size();
    long totalVerified = reviewService.getVerifiedPurchaseReviews().size();
    
    model.addAttribute("totalUnapproved", totalUnapproved);
    model.addAttribute("totalVerified", totalVerified);

    return "admin/reviews/list";
}
```

### Approve Review

**Controller Method**:
```java
@PostMapping("/approve/{id}")
public String approveReview(@PathVariable String id, 
                           RedirectAttributes redirectAttributes) {
    Review review = reviewService.getReviewById(id).orElse(null);

    if (review == null) {
        redirectAttributes.addFlashAttribute("error", "Không tìm thấy đánh giá!");
        return "redirect:/admin/reviews";
    }

    try {
        reviewService.approveReview(id);
        redirectAttributes.addFlashAttribute("success", "Duyệt đánh giá thành công!");
        return "redirect:/admin/reviews";
    } catch (Exception e) {
        log.error("Error approving review", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        return "redirect:/admin/reviews/view/" + id;
    }
}
```

**Service Method**:
```java
// ReviewService.java
@Transactional
public void approveReview(String reviewId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    
    review.setIsApproved(true);
    review.setApprovedAt(LocalDateTime.now());
    reviewRepository.save(review);
    
    // Update book's average rating
    updateBookAverageRating(review.getBook().getBookId());
}
```

### Reject Review

**Controller Method**:
```java
@PostMapping("/reject/{id}")
public String rejectReview(@PathVariable String id, 
                          RedirectAttributes redirectAttributes) {
    Review review = reviewService.getReviewById(id).orElse(null);

    if (review == null) {
        redirectAttributes.addFlashAttribute("error", "Không tìm thấy đánh giá!");
        return "redirect:/admin/reviews";
    }

    try {
        reviewService.rejectReview(id);
        redirectAttributes.addFlashAttribute("success", "Từ chối đánh giá thành công!");
        return "redirect:/admin/reviews";
    } catch (Exception e) {
        log.error("Error rejecting review", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        return "redirect:/admin/reviews/view/" + id;
    }
}
```

### Bulk Actions

**Controller Method**:
```java
@PostMapping("/bulk-approve")
@ResponseBody
public ResponseEntity<?> bulkApproveReviews(@RequestParam List<String> reviewIds) {
    try {
        int approvedCount = 0;
        
        for (String reviewId : reviewIds) {
            reviewService.approveReview(reviewId);
            approvedCount++;
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã duyệt " + approvedCount + " đánh giá",
            "count", approvedCount
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "Lỗi: " + e.getMessage()
        ));
    }
}

@PostMapping("/bulk-reject")
@ResponseBody
public ResponseEntity<?> bulkRejectReviews(@RequestParam List<String> reviewIds) {
    try {
        int rejectedCount = 0;
        
        for (String reviewId : reviewIds) {
            reviewService.rejectReview(reviewId);
            rejectedCount++;
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã từ chối " + rejectedCount + " đánh giá",
            "count", rejectedCount
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "Lỗi: " + e.getMessage()
        ));
    }
}
```

### Admin Template

```html
<!-- admin/reviews/list.html -->
<div class="reviews-management">
    <h1>Quản lý đánh giá</h1>
    
    <!-- Filter Tabs -->
    <ul class="nav nav-tabs">
        <li class="nav-item">
            <a class="nav-link" 
               th:classappend="${currentFilter == 'unapproved'} ? 'active' : ''"
               th:href="@{/admin/reviews(filter='unapproved')}">
                Chờ duyệt <span class="badge" th:text="${totalUnapproved}">5</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" 
               th:classappend="${currentFilter == 'approved'} ? 'active' : ''"
               th:href="@{/admin/reviews(filter='approved')}">
                Đã duyệt
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" 
               th:classappend="${currentFilter == 'verified'} ? 'active' : ''"
               th:href="@{/admin/reviews(filter='verified')}">
                Đã xác thực <span class="badge" th:text="${totalVerified}">10</span>
            </a>
        </li>
    </ul>
    
    <!-- Bulk Actions -->
    <div class="bulk-actions" th:if="${currentFilter == 'unapproved'}">
        <button onclick="bulkApprove()" class="btn btn-success">
            ✅ Duyệt đã chọn
        </button>
        <button onclick="bulkReject()" class="btn btn-danger">
            ❌ Từ chối đã chọn
        </button>
    </div>
    
    <!-- Reviews Table -->
    <table class="table">
        <thead>
            <tr>
                <th><input type="checkbox" id="selectAll"></th>
                <th>Sách</th>
                <th>Người đánh giá</th>
                <th>Đánh giá</th>
                <th>Nhận xét</th>
                <th>Ngày tạo</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="review : ${reviews}">
                <td><input type="checkbox" class="review-checkbox" th:value="${review.reviewId}"></td>
                <td th:text="${review.book.title}">Book Title</td>
                <td>
                    <span th:text="${review.user.fullName}">User Name</span>
                    <span th:if="${review.isVerifiedPurchase}" class="badge badge-success">✓</span>
                </td>
                <td>
                    <span th:each="i : ${#numbers.sequence(1, review.rating)}">⭐</span>
                    <span th:text="${review.rating}">5</span>
                </td>
                <td th:text="${#strings.abbreviate(review.comment, 50)}">Comment text...</td>
                <td th:text="${#temporals.format(review.createdAt, 'dd/MM/yyyy')}">07/12/2025</td>
                <td>
                    <span th:if="${review.isApproved}" class="badge badge-success">Đã duyệt</span>
                    <span th:unless="${review.isApproved}" class="badge badge-warning">Chờ duyệt</span>
                </td>
                <td>
                    <a th:href="@{/admin/reviews/view/{id}(id=${review.reviewId})}" 
                       class="btn btn-sm btn-info">Xem</a>
                    <form th:if="${!review.isApproved}" 
                          th:action="@{/admin/reviews/approve/{id}(id=${review.reviewId})}" 
                          method="post" style="display:inline;">
                        <button type="submit" class="btn btn-sm btn-success">Duyệt</button>
                    </form>
                    <form th:action="@{/admin/reviews/reject/{id}(id=${review.reviewId})}" 
                          method="post" style="display:inline;">
                        <button type="submit" class="btn btn-sm btn-danger">Từ chối</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

---

## Flow 11.5: Review Analytics

### Statistics Queries

**SQL Examples**:
```sql
-- Total reviews count
SELECT COUNT(*) FROM reviews WHERE deleted_at IS NULL;

-- Approved reviews
SELECT COUNT(*) FROM reviews 
WHERE is_approved = true AND deleted_at IS NULL;

-- Average rating by book
SELECT book_id, AVG(rating) as avg_rating, COUNT(*) as review_count
FROM reviews
WHERE is_approved = true AND deleted_at IS NULL
GROUP BY book_id;

-- Top reviewed books
SELECT b.book_id, b.title, COUNT(r.review_id) as review_count, AVG(r.rating) as avg_rating
FROM books b
LEFT JOIN reviews r ON b.book_id = r.book_id
WHERE r.is_approved = true AND r.deleted_at IS NULL
GROUP BY b.book_id, b.title
ORDER BY review_count DESC, avg_rating DESC
LIMIT 10;

-- Verified purchase reviews
SELECT COUNT(*) FROM reviews 
WHERE is_verified_purchase = true AND deleted_at IS NULL;

-- Rating distribution
SELECT rating, COUNT(*) as count
FROM reviews
WHERE is_approved = true AND deleted_at IS NULL
GROUP BY rating
ORDER BY rating DESC;

-- Reviews by month
SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count
FROM reviews
WHERE deleted_at IS NULL
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month DESC
LIMIT 12;
```

---

## Rating Calculation

### Update Book Average Rating

**Service Method**:
```java
// BookService.java
@Transactional
public void updateBookRating(String bookId) {
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    
    // Get approved reviews for this book
    List<Review> approvedReviews = reviewRepository
        .findByBookBookIdAndIsApprovedAndDeletedAtIsNull(bookId, true);
    
    if (approvedReviews.isEmpty()) {
        book.setAverageRating(0.0);
        book.setReviewCount(0);
    } else {
        // Calculate average
        double average = approvedReviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);
        
        book.setAverageRating(Math.round(average * 10) / 10.0);
        book.setReviewCount(approvedReviews.size());
    }
    
    bookRepository.save(book);
}
```

**Database Update**:
```sql
UPDATE books b
SET 
    average_rating = (
        SELECT AVG(r.rating)
        FROM reviews r
        WHERE r.book_id = b.book_id
        AND r.is_approved = true
        AND r.deleted_at IS NULL
    ),
    review_count = (
        SELECT COUNT(*)
        FROM reviews r
        WHERE r.book_id = b.book_id
        AND r.is_approved = true
        AND r.deleted_at IS NULL
    )
WHERE b.book_id = ?;
```

---

## Best Practices

### 1. **Review Quality**
- Minimum comment length (10-50 characters)
- Spam detection (duplicate reviews, keywords)
- Moderation queue for new reviews
- Verified purchase badge for authenticity

### 2. **User Experience**
- Show helpful/not helpful buttons
- Sort reviews (most helpful, newest, highest rating)
- Filter by rating
- Show reviewer's purchase history

### 3. **Performance**
- Cache average ratings
- Index on book_id, user_id, is_approved
- Paginate review lists
- Lazy load reviews (scroll to load more)

### 4. **Security**
- Validate review ownership before edit/delete
- Sanitize user input (XSS prevention)
- Rate limiting (max reviews per day)
- Report abuse functionality

---

## Related Flows

- **FLOW 02**: Admin Book Management - Books receive reviews
- **FLOW 03**: Shopping Cart & Checkout - Verified purchase badge
- **FLOW 04**: User Account Management - User's review history
- **FLOW 09**: Admin Dashboard - Review statistics

---

**Last Updated:** 07/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

