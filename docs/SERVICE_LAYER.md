# Service Layer Documentation - Ebook Store

## Tổng Quan Service Layer

Service Layer là tầng **Business Logic** trong kiến trúc hệ thống, chịu trách nhiệm xử lý các nghiệp vụ, orchestrate giữa các repositories, và thực hiện data transformation.

---

## Cấu Trúc Service Layer

```
service/
├── impl/                        # Service implementations
│   ├── UserServiceImpl.java
│   ├── BookServiceImpl.java
│   └── ...
├── UserService.java             # Interface
├── BookService.java             # Interface
├── OrderService.java
├── CartService.java
├── CartItemService.java
├── AuthorService.java
├── CategoryService.java
├── ReviewService.java
├── CouponService.java
├── SubscriptionService.java
├── PostService.java
├── BannerService.java
├── BookAssetService.java
├── FileStorageService.java
├── ReadingProgressService.java
├── OrderItemService.java
└── UserDeviceService.java
```

**Pattern**: Interface + Implementation

**Benefit**: Loose coupling, testability, dependency injection

---

## Core Services

### 1. UserService

**Interface**: `UserService.java`  
**Implementation**: `UserServiceImpl.java`

**Responsibilities**:
- User registration and authentication
- User profile management
- User CRUD operations
- Password management
- User search and filtering

**Key Methods**:

```java
public interface UserService {
    // Authentication
    User findByUsername(String username);
    User findByEmail(String email);
    boolean authenticate(String username, String password);
    
    // Registration
    User register(RegisterDto registerDto);
    
    // User Management
    User findById(String userId);
    User save(User user);
    void delete(String userId);
    Page<User> findAll(Pageable pageable);
    Page<User> searchUsers(String keyword, Pageable pageable);
    
    // Profile
    User updateProfile(String userId, UserUpdateRequest request);
    void changePassword(String userId, String oldPassword, String newPassword);
    
    // Status
    void activateUser(String userId);
    void deactivateUser(String userId);
}
```

**Business Logic**:
- Validate unique username/email before registration
- Hash password using BCrypt
- Check user status before authentication
- Validate password strength
- Handle avatar upload

---

### 2. BookService

**Interface**: `BookService.java`  
**Implementation**: `BookServiceImpl.java`

**Responsibilities**:
- Book CRUD operations
- Book search and filtering
- Category management
- Author management
- Rating and review aggregation

**Key Methods**:

```java
public interface BookService {
    // CRUD
    Book findById(String bookId);
    Book save(Book book);
    void delete(String bookId);
    
    // Listing
    Page<Book> findAll(Pageable pageable);
    List<Book> findFeaturedBooks();
    List<Book> findNewReleases();
    List<Book> findBestSellers();
    
    // Search & Filter
    Page<Book> searchBooks(String keyword, Pageable pageable);
    Page<Book> findByCategory(String categoryId, Pageable pageable);
    Page<Book> findByAuthor(String authorId, Pageable pageable);
    Page<Book> filterBooks(BookFilterRequest filter, Pageable pageable);
    
    // Create/Update
    Book createBook(BookCreateRequest request);
    Book updateBook(String bookId, BookUpdateRequest request);
    void bulkDelete(List<String> bookIds);
    
    // Stats
    void incrementViewCount(String bookId);
    void updateRating(String bookId);
}
```

**Business Logic**:
- Validate book data (title, price, ISBN)
- Handle file uploads (cover, source)
- Manage many-to-many relationships (authors, categories)
- Update statistics (views, ratings)
- Check access permissions (FREE, PURCHASE, SUBSCRIPTION)

---

### 3. OrderService

**Interface**: `OrderService.java`  
**Implementation**: `OrderServiceImpl.java`

**Responsibilities**:
- Order creation and management
- Order status tracking
- Payment processing
- Order history

**Key Methods**:

```java
public interface OrderService {
    // Create Order
    Order createOrder(String userId, OrderCreateRequest request);
    Order createOrderFromCart(String userId, PaymentMethod method, String couponCode);
    
    // Order Management
    Order findById(String orderId);
    Page<Order> findByUser(String userId, Pageable pageable);
    Page<Order> findAll(Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    // Status Update
    void confirmOrder(String orderId);
    void completeOrder(String orderId);
    void cancelOrder(String orderId);
    void updatePaymentStatus(String orderId, PaymentStatus status);
    
    // Statistics
    Double getTotalRevenue();
    Long getTotalOrders();
    List<Order> getRecentOrders(int limit);
}
```

**Business Logic**:
- Validate cart items before checkout
- Apply coupon discounts
- Calculate total amount
- Handle payment integration (VNPAY, MOMO)
- Update book ownership after payment
- Send order confirmation emails
- Update coupon usage count

**Order Status Flow**:
```
PENDING → CONFIRMED → COMPLETED
   ↓
CANCELLED
```

---

### 4. CartService & CartItemService

**Interfaces**: `CartService.java`, `CartItemService.java`

**Responsibilities**:
- Shopping cart management
- Add/remove items
- Cart total calculation

**Key Methods**:

```java
public interface CartService {
    Cart getOrCreateCart(String userId);
    Cart findByUser(String userId);
    void clearCart(String userId);
}

public interface CartItemService {
    void addToCart(String userId, String bookId);
    void removeFromCart(String userId, String bookId);
    List<CartItem> getCartItems(String userId);
    Double calculateTotal(String userId);
    int getItemCount(String userId);
}
```

**Business Logic**:
- Create cart automatically for new users
- Validate book availability before adding
- Check duplicate items
- Calculate subtotal and total
- Handle book price changes

---

### 5. AuthorService

**Interface**: `AuthorService.java`

**Responsibilities**:
- Author CRUD operations
- Author search
- Books by author

**Key Methods**:

```java
public interface AuthorService {
    Author findById(String authorId);
    Author save(Author author);
    void delete(String authorId);
    List<Author> findAll();
    Page<Author> findAll(Pageable pageable);
    Author create(AuthorCreateRequest request);
    Author update(String authorId, AuthorUpdateRequest request);
    List<Book> getBooksByAuthor(String authorId);
}
```

**Business Logic**:
- Validate author name uniqueness
- Handle avatar upload
- Check for books before deletion
- Update author statistics

---

### 6. CategoryService

**Interface**: `CategoryService.java`

**Responsibilities**:
- Category CRUD operations
- Category hierarchy
- Books by category

**Key Methods**:

```java
public interface CategoryService {
    Category findById(String categoryId);
    Category save(Category category);
    void delete(String categoryId);
    List<Category> findAll();
    List<Category> findActiveCategories();
    Category create(CategoryCreateRequest request);
    Category update(String categoryId, CategoryUpdateRequest request);
    List<Book> getBooksByCategory(String categoryId);
}
```

**Business Logic**:
- Validate category name uniqueness
- Handle display order
- Check for books before deletion
- Toggle active status

---

### 7. ReviewService

**Interface**: `ReviewService.java`

**Responsibilities**:
- Review submission and moderation
- Rating calculation
- Review listing

**Key Methods**:

```java
public interface ReviewService {
    Review submitReview(String userId, String bookId, int rating, String comment);
    Review findById(String reviewId);
    List<Review> findByBook(String bookId);
    Page<Review> findByBook(String bookId, Pageable pageable);
    Page<Review> findPendingReviews(Pageable pageable);
    void approveReview(String reviewId);
    void rejectReview(String reviewId);
    void deleteReview(String reviewId);
    Double getAverageRating(String bookId);
}
```

**Business Logic**:
- Validate user purchased the book before reviewing
- Check one review per user per book
- Moderate reviews before approval
- Update book average rating
- Send notification to book owner

---

### 8. CouponService

**Interface**: `CouponService.java`

**Responsibilities**:
- Coupon management
- Coupon validation
- Discount calculation

**Key Methods**:

```java
public interface CouponService {
    Coupon findById(String couponId);
    Coupon findByCode(String code);
    Coupon create(CouponCreateRequest request);
    Coupon update(String couponId, CouponUpdateRequest request);
    void delete(String couponId);
    List<Coupon> findActiveCoupons();
    
    // Validation & Application
    boolean validateCoupon(String code, Double orderAmount);
    Double calculateDiscount(String code, Double orderAmount);
    void useCoupon(String code);
    boolean isExpired(String couponId);
}
```

**Business Logic**:
- Validate coupon code
- Check expiration date
- Check usage limit
- Check minimum order amount
- Calculate discount (percentage or fixed)
- Apply max discount limit
- Increment usage count

**Discount Types**:
- `PERCENTAGE`: e.g., 20% off
- `FIXED_AMOUNT`: e.g., 50,000 VND off

---

### 9. SubscriptionService

**Interface**: `SubscriptionService.java`

**Responsibilities**:
- Subscription plan management
- User subscription status
- Auto-renewal

**Key Methods**:

```java
public interface SubscriptionService {
    Subscription subscribe(String userId, String planName, int durationDays);
    Subscription findActiveSubscription(String userId);
    boolean hasActiveSubscription(String userId);
    void cancelSubscription(String subscriptionId);
    void renewSubscription(String subscriptionId);
    List<Subscription> findExpiringSubscriptions(int daysBeforeExpiry);
}
```

**Business Logic**:
- Validate plan name and price
- Check existing active subscription
- Calculate expiry date
- Handle auto-renewal
- Grant access to subscription books
- Send expiry notifications

**Plans**:
- **VIP**: 99,000 VND/month
- **PREMIUM**: 199,000 VND/month

---

### 10. FileStorageService

**Interface**: `FileStorageService.java`

**Responsibilities**:
- File upload handling
- File validation
- File path management

**Key Methods**:

```java
public interface FileStorageService {
    String storeFile(MultipartFile file, String directory);
    String storeCoverImage(MultipartFile file, String categorySlug);
    String storeBookSource(MultipartFile file, String categorySlug);
    String storeAvatar(MultipartFile file);
    void deleteFile(String filePath);
    boolean validateFileType(MultipartFile file, List<String> allowedTypes);
    boolean validateFileSize(MultipartFile file, long maxSize);
}
```

**Business Logic**:
- Validate file type (PDF, EPUB, JPG, PNG)
- Validate file size (max 50MB)
- Generate unique filename
- Create directory if not exists
- Return relative URL path
- Handle file deletion

**Upload Paths**:
```
F:/datn_uploads/book_asset/
├── image/
│   ├── covers/{category}/
│   ├── authors/
│   ├── avatars/
│   └── banners/
├── source/{category}/
└── preview/
```

---

### 11. BookAssetService

**Interface**: `BookAssetService.java`

**Responsibilities**:
- Book file (PDF/EPUB) management
- Preview generation
- Download tracking

**Key Methods**:

```java
public interface BookAssetService {
    BookAsset save(BookAsset asset);
    BookAsset findById(String assetId);
    BookAsset findByBook(String bookId);
    void delete(String assetId);
    String getDownloadUrl(String bookId, String userId);
    boolean canUserAccess(String bookId, String userId);
}
```

**Business Logic**:
- Validate user ownership or subscription
- Generate download URL
- Track download count
- Generate preview (first N pages)

---

### 12. ReadingProgressService

**Interface**: `ReadingProgressService.java`

**Responsibilities**:
- Track reading progress
- Save/load bookmark
- Calculate completion percentage

**Key Methods**:

```java
public interface ReadingProgressService {
    ReadingProgress save(String userId, String bookId, int currentPage, int totalPages);
    ReadingProgress findByUserAndBook(String userId, String bookId);
    List<ReadingProgress> findByUser(String userId);
    void updateProgress(String userId, String bookId, int currentPage);
    int getProgressPercentage(String userId, String bookId);
}
```

**Business Logic**:
- Calculate percentage: `(currentPage / totalPages) * 100`
- Update last read timestamp
- List recently read books

---

### 13. PostService

**Interface**: `PostService.java`

**Responsibilities**:
- Blog post management
- Post publishing
- View tracking

**Key Methods**:

```java
public interface PostService {
    Post create(PostCreateRequest request);
    Post update(String postId, PostUpdateRequest request);
    void delete(String postId);
    Post findById(String postId);
    Page<Post> findPublished(Pageable pageable);
    void publish(String postId);
    void unpublish(String postId);
    void incrementViewCount(String postId);
}
```

---

### 14. BannerService

**Interface**: `BannerService.java`

**Responsibilities**:
- Banner management
- Position management
- Active/inactive status

**Key Methods**:

```java
public interface BannerService {
    Banner create(BannerCreateRequest request);
    Banner update(String bannerId, BannerUpdateRequest request);
    void delete(String bannerId);
    List<Banner> findByPosition(BannerPosition position);
    List<Banner> findActiveBanners();
}
```

---

## Transaction Management

### @Transactional Annotation

**Usage**: Service layer methods

```java
@Service
@Transactional
public class BookServiceImpl implements BookService {
    
    @Transactional(readOnly = true)
    public Book findById(String bookId) {
        // Read-only transaction
    }
    
    @Transactional
    public Book save(Book book) {
        // Read-write transaction
    }
}
```

**Propagation**:
- Default: `REQUIRED`
- ReadOnly operations: `readOnly = true`

**Rollback**:
- Automatic rollback on `RuntimeException`
- Manual rollback with `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`

---

## Exception Handling

### Service Layer Exceptions

**Throw custom exceptions** từ service layer:

```java
public Book findById(String bookId) {
    return bookRepository.findById(bookId)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
}

public void deleteBook(String bookId) {
    if (hasActiveOrders(bookId)) {
        throw new BusinessException("Cannot delete book with active orders");
    }
    bookRepository.deleteById(bookId);
}
```

**Custom Exceptions**:
- `ResourceNotFoundException` - 404
- `BusinessException` - 400
- `DuplicateResourceException` - 409
- `UnauthorizedException` - 401
- `ForbiddenException` - 403

---

## Data Transformation (Entity ↔ DTO)

### Entity to DTO

```java
public BookResponse toResponse(Book book) {
    return BookResponse.builder()
        .bookId(book.getBookId())
        .title(book.getTitle())
        .price(book.getPrice())
        .coverImageUrl(book.getCoverImageUrl())
        .authors(book.getAuthors().stream()
            .map(Author::getName)
            .collect(Collectors.toList()))
        .build();
}
```

### DTO to Entity

```java
public Book toEntity(BookCreateRequest request) {
    return Book.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .price(request.getPrice())
        .publisher(request.getPublisher())
        .build();
}
```

---

## Service Dependencies

### Dependency Injection

**Constructor Injection** (recommended):

```java
@Service
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final BookService bookService;
    private final CartService cartService;
    private final CouponService couponService;
    
    @Autowired
    public OrderServiceImpl(
        OrderRepository orderRepository,
        UserService userService,
        BookService bookService,
        CartService cartService,
        CouponService couponService
    ) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.bookService = bookService;
        this.cartService = cartService;
        this.couponService = couponService;
    }
}
```

---

## Validation in Service Layer

### Business Logic Validation

```java
public User register(RegisterDto dto) {
    // Validate unique username
    if (userRepository.existsByUsername(dto.getUsername())) {
        throw new DuplicateResourceException("Username already exists");
    }
    
    // Validate unique email
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new DuplicateResourceException("Email already exists");
    }
    
    // Validate password strength
    if (!isPasswordStrong(dto.getPassword())) {
        throw new BusinessException("Password too weak");
    }
    
    // Create user
    User user = new User();
    user.setUsername(dto.getUsername());
    user.setEmail(dto.getEmail());
    user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
    
    return userRepository.save(user);
}
```

---

## Best Practices

### 1. Interface-Based Design
✅ Define interfaces for all services  
✅ Program to interfaces, not implementations

### 2. Single Responsibility
✅ Each service handles one domain entity  
✅ Avoid god services

### 3. Transaction Boundaries
✅ Service methods = transaction boundaries  
✅ Use `@Transactional` appropriately

### 4. Exception Handling
✅ Throw custom exceptions  
✅ Let controller handle exception responses

### 5. Dependency Management
✅ Use constructor injection  
✅ Avoid circular dependencies

### 6. Testing
✅ Write unit tests for service logic  
✅ Mock repository dependencies

---

**Tài liệu liên quan**:
- [Cấu trúc dự án](PROJECT_STRUCTURE.md)
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [API Documentation](API_DOCUMENTATION.md)

