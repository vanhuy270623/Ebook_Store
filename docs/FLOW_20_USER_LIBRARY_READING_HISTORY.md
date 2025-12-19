# FLOW 20: User Library & Reading History (Thư Viện & Lịch Sử Đọc)

**Dự án:** Ebook Store  
**Ngày tạo:** 20/12/2025  
**Người tạo:** Development Team  
**Phiên bản:** 1.0  

---

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Luồng Xử Lý](#luồng-xử-lý)
3. [Implementation Details](#implementation-details)
4. [Database Schema](#database-schema)
5. [Testing](#testing)

---

## Tổng Quan

### Mục Đích
Quản lý thư viện sách cá nhân và lịch sử đọc sách của user.

### Key Features
- ✅ Xem tất cả sách đã mua/đăng ký
- ✅ Lịch sử đọc sách với progress
- ✅ Sách yêu thích (favorites)
- ✅ Lọc theo access type
- ✅ Sắp xếp theo thời gian đọc gần nhất
- ✅ Tiếp tục đọc từ vị trí đã lưu

### Actors
- **User**: Xem và quản lý thư viện của mình

### Preconditions
- User đã đăng nhập
- User có ít nhất 1 sách đã mua hoặc có subscription

---

## Luồng Xử Lý

### Sequence Diagram: View Library

```
User          UserLibraryController    OrderService    BookService    ReadingProgressService
 │                    │                     │               │                   │
 │──GET /user/library>│                     │               │                   │
 │                    │                     │               │                   │
 │                    │──getCurrentUser()─>│               │                   │
 │                    │<─────User──────────│               │                   │
 │                    │                     │               │                   │
 │                    │──getPurchasedBooks(user)─>         │                   │
 │                    │                     │──getOrdersByUser()───────>        │
 │                    │                     │<─List<Order>──────────────        │
 │                    │                     │               │                   │
 │                    │                     │──Extract Books from Orders──────>│
 │                    │<─List<Book>─────────│               │                   │
 │                    │                     │               │                   │
 │                    │──getSubscriptionBooks(user)─────────────────>          │
 │                    │<─List<Book>────────────────────────────────            │
 │                    │                     │               │                   │
 │                    │──getReadingProgress(user)──────────────────────>       │
 │                    │<─List<Progress>────────────────────────────────        │
 │                    │                     │               │                   │
 │                    │──Merge & Enrich Books with Progress───>                │
 │                    │                     │               │                   │
 │<──library page─────│                     │               │                   │
 │                    │                     │               │                   │
```

### Sequence Diagram: Reading History

```
User          UserLibraryController    ReadingProgressService    BookService
 │                    │                          │                   │
 │──GET /user/reading-history>│                  │                   │
 │                    │                          │                   │
 │                    │──getCurrentUser()──>     │                   │
 │                    │<─────User──────────      │                   │
 │                    │                          │                   │
 │                    │──getReadingProgress(user)>                   │
 │                    │<─List<Progress>──────────│                   │
 │                    │                          │                   │
 │                    │──For each progress:      │                   │
 │                    │    getBook(bookId)───────────────────>       │
 │                    │<───Book──────────────────────────────        │
 │                    │                          │                   │
 │                    │──Sort by lastReadAt DESC─>                   │
 │                    │                          │                   │
 │<──reading history page│                       │                   │
 │                    │                          │                   │
```

---

## Implementation Details

### Controller: `UserLibraryController.java`

**Location:** `src/main/java/stu/datn/ebook_store/controller/user/UserLibraryController.java`

**Endpoints:**
```
GET /user/library          : Thư viện cá nhân
GET /user/reading-history  : Lịch sử đọc sách
```

#### 1. View Library
```java
@GetMapping("/library")
public String library(
        @RequestParam(required = false) String filter,
        @RequestParam(required = false) String sort,
        Authentication authentication,
        Model model) {

    User currentUser = (User) authentication.getPrincipal();

    // 1. Lấy sách đã mua (PAID books)
    List<Book> purchasedBooks = getPurchasedBooks(currentUser);

    // 2. Lấy sách từ subscription (SUBSCRIPTION books)
    List<Book> subscriptionBooks = getSubscriptionBooks(currentUser);

    // 3. Merge và loại bỏ duplicate
    Set<Book> allBooks = new HashSet<>();
    allBooks.addAll(purchasedBooks);
    allBooks.addAll(subscriptionBooks);

    // 4. Lấy reading progress
    Map<String, ReadingProgress> progressMap = 
        readingProgressService.getProgressMapByUser(currentUser.getUserId());

    // 5. Tạo DTOs với progress info
    List<LibraryBookDto> libraryBooks = allBooks.stream()
        .map(book -> {
            LibraryBookDto dto = new LibraryBookDto();
            dto.setBook(book);
            
            ReadingProgress progress = progressMap.get(book.getBookId());
            if (progress != null) {
                dto.setLastReadAt(progress.getLastReadAt());
                dto.setProgress(progress.getProgressPercentage());
                dto.setIsFavorite(progress.getIsFavorite());
            }
            
            return dto;
        })
        .collect(Collectors.toList());

    // 6. Apply filters
    if (filter != null) {
        libraryBooks = applyFilter(libraryBooks, filter);
    }

    // 7. Apply sorting
    if (sort != null) {
        libraryBooks = applySorting(libraryBooks, sort);
    }

    model.addAttribute("books", libraryBooks);
    model.addAttribute("currentFilter", filter);
    model.addAttribute("currentSort", sort);
    model.addAttribute("totalBooks", libraryBooks.size());

    return "user/library/index";
}
```

#### Helper: getPurchasedBooks()
```java
private List<Book> getPurchasedBooks(User user) {
    // Lấy tất cả orders đã thanh toán
    List<Order> paidOrders = orderService.getOrdersByUser(user.getUserId())
        .stream()
        .filter(order -> PAID_STATUSES.contains(order.getPaymentStatus()))
        .collect(Collectors.toList());

    // Extract books từ order items
    Set<Book> books = new HashSet<>();
    for (Order order : paidOrders) {
        List<OrderItem> items = orderItemService.getOrderItemsByOrder(order.getOrderId());
        for (OrderItem item : items) {
            books.add(item.getBook());
        }
    }

    return new ArrayList<>(books);
}
```

#### Helper: getSubscriptionBooks()
```java
private List<Book> getSubscriptionBooks(User user) {
    // Kiểm tra user có subscription active không
    boolean hasActiveSubscription = subscriptionService
        .hasActiveSubscription(user.getUserId());

    if (!hasActiveSubscription) {
        return Collections.emptyList();
    }

    // Lấy tất cả SUBSCRIPTION books
    return bookService.getBooksByAccessType(Book.AccessType.SUBSCRIPTION);
}
```

#### 2. Reading History
```java
@GetMapping("/reading-history")
public String readingHistory(
        Authentication authentication,
        Model model) {

    User currentUser = (User) authentication.getPrincipal();

    // Lấy reading progress
    List<ReadingProgress> progressList = readingProgressService
        .getReadingProgressByUser(currentUser);

    // Filter: Chỉ lấy những cuốn đã đọc (lastReadAt != null)
    List<ReadingProgress> readBooks = progressList.stream()
        .filter(p -> p.getLastReadAt() != null)
        .sorted((p1, p2) -> p2.getLastReadAt().compareTo(p1.getLastReadAt()))
        .collect(Collectors.toList());

    // Tạo DTOs
    List<ReadingHistoryDto> history = readBooks.stream()
        .map(progress -> {
            ReadingHistoryDto dto = new ReadingHistoryDto();
            dto.setBook(progress.getBook());
            dto.setLastReadAt(progress.getLastReadAt());
            dto.setProgress(progress.getProgressPercentage());
            dto.setLastPageRead(progress.getLastPageRead());
            dto.setTotalPages(progress.getTotalPages());
            return dto;
        })
        .collect(Collectors.toList());

    model.addAttribute("history", history);
    model.addAttribute("totalBooks", history.size());

    return "user/library/reading-history";
}
```

---

### Service: `ReadingProgressService.java`

**Location:** `src/main/java/stu/datn/ebook_store/service/ReadingProgressService.java`

#### getReadingProgressByUser()
```java
public List<ReadingProgress> getReadingProgressByUser(User user) {
    return readingProgressRepository.findByUser_UserId(user.getUserId());
}
```

#### getProgressMapByUser()
```java
public Map<String, ReadingProgress> getProgressMapByUser(String userId) {
    List<ReadingProgress> progressList = 
        readingProgressRepository.findByUser_UserId(userId);
    
    return progressList.stream()
        .collect(Collectors.toMap(
            p -> p.getBook().getBookId(),
            p -> p
        ));
}
```

#### getOrCreateProgress()
```java
public ReadingProgress getOrCreateProgress(String userId, String bookId) {
    Optional<ReadingProgress> existing = readingProgressRepository
        .findByUser_UserIdAndBook_BookId(userId, bookId);
    
    if (existing.isPresent()) {
        return existing.get();
    }
    
    // Create new
    User user = userService.getUserById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));
    
    Book book = bookService.getBookById(bookId)
        .orElseThrow(() -> new NotFoundException("Book not found"));
    
    ReadingProgress progress = new ReadingProgress();
    progress.setProgressId(generateProgressId());
    progress.setUser(user);
    progress.setBook(book);
    progress.setLastPageRead(0);
    progress.setIsFavorite(false);
    progress.setCreatedAt(LocalDateTime.now());
    
    return readingProgressRepository.save(progress);
}
```

---

### DTOs

#### LibraryBookDto.java
```java
public class LibraryBookDto {
    private Book book;
    private LocalDateTime lastReadAt;
    private Integer progress; // 0-100%
    private Boolean isFavorite;
    private Boolean isPurchased;
    private Boolean isFromSubscription;
    
    // Getters and Setters
}
```

#### ReadingHistoryDto.java
```java
public class ReadingHistoryDto {
    private Book book;
    private LocalDateTime lastReadAt;
    private Integer lastPageRead;
    private Integer totalPages;
    private Integer progress; // 0-100%
    
    // Getters and Setters
}
```

---

### Entity: `ReadingProgress.java`

**Location:** `src/main/java/stu/datn/ebook_store/entity/ReadingProgress.java`

```java
@Entity
@Table(name = "reading_progress")
public class ReadingProgress {
    
    @Id
    @Column(name = "progress_id")
    private String progressId;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(name = "last_page_read")
    private Integer lastPageRead;
    
    @Column(name = "total_pages")
    private Integer totalPages;
    
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
    
    @Column(name = "last_downloaded_at")
    private LocalDateTime lastDownloadedAt;
    
    @Column(name = "is_favorite")
    private Boolean isFavorite;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Computed field
    public Integer getProgressPercentage() {
        if (totalPages == null || totalPages == 0) {
            return 0;
        }
        return (int) ((lastPageRead * 100.0) / totalPages);
    }
    
    // Getters and Setters
}
```

---

## Database Schema

### Table: `reading_progress`

```sql
CREATE TABLE `reading_progress` (
  `progress_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `last_page_read` int DEFAULT NULL,
  `total_pages` int DEFAULT NULL,
  `last_read_at` datetime DEFAULT NULL,
  `last_downloaded_at` datetime DEFAULT NULL,
  `is_favorite` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`progress_id`),
  UNIQUE KEY `unique_user_book` (`user_id`, `book_id`),
  KEY `user_id` (`user_id`),
  KEY `book_id` (`book_id`),
  CONSTRAINT FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Query: Get User's Library

```sql
-- Get all books user has access to
SELECT DISTINCT 
    b.book_id,
    b.title,
    b.cover_image_url,
    b.access_type,
    rp.last_read_at,
    rp.last_page_read,
    rp.total_pages,
    rp.is_favorite,
    CASE 
        WHEN rp.total_pages > 0 
        THEN (rp.last_page_read * 100 / rp.total_pages)
        ELSE 0 
    END as progress_percentage
FROM books b
LEFT JOIN reading_progress rp 
    ON b.book_id = rp.book_id AND rp.user_id = ?
WHERE 
    -- FREE books
    b.access_type = 'FREE'
    OR
    -- PAID books that user purchased
    b.book_id IN (
        SELECT oi.book_id 
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.order_id
        WHERE o.user_id = ? 
        AND o.payment_status IN ('COMPLETED', 'PAID')
    )
    OR
    -- SUBSCRIPTION books (if user has active subscription)
    (b.access_type = 'SUBSCRIPTION' AND EXISTS (
        SELECT 1 FROM orders o
        JOIN subscriptions s ON o.subscription_id = s.subscription_id
        WHERE o.user_id = ?
        AND o.payment_status IN ('COMPLETED', 'PAID')
        AND o.subscription_end_date > NOW()
    ))
ORDER BY 
    COALESCE(rp.last_read_at, b.created_at) DESC;
```

---

## Frontend Implementation

### Library Page

**Template:** `user/library/index.html`

```html
<div class="container">
    <h2>Thư Viện Của Tôi</h2>
    
    <div class="library-stats">
        <div class="stat-card">
            <i class="fas fa-book"></i>
            <div>
                <h4>[[${totalBooks}]]</h4>
                <p>Tổng sách</p>
            </div>
        </div>
    </div>
    
    <!-- Filters -->
    <div class="filters">
        <select name="filter" onchange="applyFilter(this.value)">
            <option value="">Tất cả</option>
            <option value="favorites">Yêu thích</option>
            <option value="purchased">Đã mua</option>
            <option value="subscription">Từ gói VIP</option>
            <option value="reading">Đang đọc</option>
        </select>
        
        <select name="sort" onchange="applySort(this.value)">
            <option value="recent">Đọc gần đây</option>
            <option value="title">Tiêu đề A-Z</option>
            <option value="progress">Tiến độ đọc</option>
        </select>
    </div>
    
    <!-- Books Grid -->
    <div class="books-grid">
        <div th:each="item : ${books}" class="book-card">
            <div class="book-cover">
                <img th:src="${item.book.coverImageUrl}" 
                     th:alt="${item.book.title}">
                
                <span th:if="${item.isFavorite}" class="favorite-badge">
                    <i class="fas fa-heart"></i>
                </span>
                
                <div class="progress-bar" th:if="${item.progress > 0}">
                    <div class="progress-fill" 
                         th:style="'width: ' + ${item.progress} + '%'"></div>
                </div>
            </div>
            
            <h4>[[${item.book.title}]]</h4>
            
            <div class="book-actions">
                <a th:href="@{/reading/book/{id}(id=${item.book.bookId})}" 
                   class="btn btn-primary">
                    <i class="fas fa-book-open"></i>
                    <span th:text="${item.progress > 0 ? 'Tiếp tục đọc' : 'Bắt đầu đọc'}"></span>
                </a>
                
                <a th:href="@{/books/download/{id}(id=${item.book.bookId})}" 
                   class="btn btn-secondary">
                    <i class="fas fa-download"></i> Tải xuống
                </a>
            </div>
        </div>
    </div>
</div>
```

### Reading History Page

**Template:** `user/library/reading-history.html`

```html
<div class="container">
    <h2>Lịch Sử Đọc Sách</h2>
    
    <div class="reading-history-list">
        <div th:each="item : ${history}" class="history-item">
            <img th:src="${item.book.coverImageUrl}" 
                 th:alt="${item.book.title}"
                 class="history-cover">
            
            <div class="history-info">
                <h4>[[${item.book.title}]]</h4>
                <p class="text-muted">
                    Đọc lần cuối: [[${#temporals.format(item.lastReadAt, 'dd/MM/yyyy HH:mm')}]]
                </p>
                
                <div class="progress-info">
                    <div class="progress">
                        <div class="progress-bar" 
                             th:style="'width: ' + ${item.progress} + '%'">
                        </div>
                    </div>
                    <span class="progress-text">
                        [[${item.lastPageRead}]] / [[${item.totalPages}]] trang 
                        ([[${item.progress}]]%)
                    </span>
                </div>
            </div>
            
            <div class="history-actions">
                <a th:href="@{/reading/book/{id}(id=${item.book.bookId})}" 
                   class="btn btn-primary">
                    Tiếp tục đọc
                </a>
            </div>
        </div>
    </div>
</div>
```

---

## Testing

### Test Cases

#### TC-1: View Empty Library
**Precondition:** User chưa mua sách nào

**Steps:**
1. Login
2. Navigate to `/user/library`

**Expected Result:**
- Empty state shown
- Message: "Bạn chưa có sách nào trong thư viện"
- Link to browse books

---

#### TC-2: View Library with Purchased Books
**Precondition:** User đã mua 3 sách

**Steps:**
1. Navigate to `/user/library`
2. Verify books shown

**Expected Result:**
- 3 books displayed
- Each book has "Đã mua" badge
- "Đọc ngay" button available

---

#### TC-3: View Library with Subscription
**Precondition:** User có active subscription

**Steps:**
1. Navigate to `/user/library`
2. Verify all SUBSCRIPTION books shown

**Expected Result:**
- All SUBSCRIPTION books accessible
- Badge: "Từ gói VIP"

---

#### TC-4: Filter by Favorites
**Steps:**
1. Mark 2 books as favorite
2. Apply filter: "Yêu thích"

**Expected Result:**
- Only 2 favorite books shown

---

#### TC-5: Sort by Recent
**Steps:**
1. Read book A today
2. Read book B yesterday
3. Sort by "Đọc gần đây"

**Expected Result:**
- Book A shown first
- Book B shown second

---

#### TC-6: Reading History
**Steps:**
1. Read book A to page 50/100
2. Navigate to `/user/reading-history`

**Expected Result:**
- Book A shown with 50% progress
- Last read timestamp correct

---

#### TC-7: Continue Reading
**Steps:**
1. User read book to page 75
2. Close reader
3. Click "Tiếp tục đọc"

**Expected Result:**
- Reader opens at page 75 (not page 1)

---

## Related Flows

- **FLOW 03**: Shopping Cart (để mua sách)
- **FLOW 07**: Reading Interface (để đọc sách)
- **FLOW 10**: Subscription Management (SUBSCRIPTION books)
- **FLOW 18**: Secure Download (tải sách)
- **FLOW 22**: Favorites System

---

**Status:** ✅ COMPLETE  
**Implementation:** 100%  
**Last Updated:** 20/12/2025

