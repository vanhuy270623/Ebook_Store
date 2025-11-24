# 🔄 TÀI LIỆU LUỒNG XỬ LÝ HỆ THỐNG - EBOOK STORE

**Ngày cập nhật:** 23/11/2025  
**Phiên bản:** 1.0

---

## 📋 MỤC LỤC

1. [Kiến trúc tổng quan](#1-kiến-trúc-tổng-quan)
2. [Luồng Authentication](#2-luồng-authentication)
3. [Luồng Book Management (CRUD)](#3-luồng-book-management-crud)
4. [Luồng File Upload](#4-luồng-file-upload)
5. [Luồng Order & Payment](#5-luồng-order--payment)
6. [Luồng Review & Rating](#6-luồng-review--rating)

---

## 1. KIẾN TRÚC TỔNG QUAN

### 📐 Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  (Controllers + Thymeleaf Templates + Static Resources)  │
└─────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────┐
│                     BUSINESS LAYER                       │
│              (Services + DTOs + Validators)              │
└─────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────┐
│                  DATA ACCESS LAYER                       │
│              (Repositories + JPA Entities)               │
└─────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────┐
│                    DATABASE (MySQL)                      │
│                   (18 Tables + Relations)                │
└─────────────────────────────────────────────────────────┘
```

### 🔑 Core Components

| Layer | Components | Trách nhiệm |
|-------|-----------|-------------|
| **Controller** | `*Controller.java` | Xử lý HTTP requests, routing |
| **Service** | `*Service.java`, `*ServiceImpl.java` | Business logic, validation |
| **Repository** | `*Repository.java` | Data access, CRUD operations |
| **Entity** | `*.java` (entities) | Map với database tables |
| **DTO** | `*DTO.java` | Data transfer giữa layers |
| **Config** | `SecurityConfig`, `WebConfig` | Cấu hình hệ thống |

---

## 2. LUỒNG AUTHENTICATION

### 2.1. Luồng Đăng Ký (Register)

```
┌────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────┐
│ User   │───>│ AuthController│───>│ UserService │───>│   DB     │
│        │    │   /register   │    │             │    │  users   │
└────────┘    └──────────────┘    └─────────────┘    └──────────┘
                                                              │
    1. GET /auth/register                                     │
       → Show form                                            │
                                                              │
    2. POST /auth/register                                    │
       → Validate input                                       │
       → Check username exists? ─────────────────────────────┘
       → Check email exists?
       → Hash password (BCrypt)
       → Assign ROLE_USER
       → Save to DB
       → Redirect to login
```

**Chi tiết từng bước:**

1. **User truy cập form đăng ký:**
   - URL: `GET /auth/register`
   - Controller: `AuthController.showRegisterPage()`
   - Template: `templates/auth/register.html`

2. **User submit form:**
   - URL: `POST /auth/register`
   - Input: `RegisterDto` (username, email, password, fullName, phone)
   - Validation:
     - Username chưa tồn tại
     - Email chưa tồn tại
     - Password strength
   - Process:
     - Hash password với BCrypt
     - Tạo User entity
     - Gán Role = "USER"
     - Save vào database
   - Redirect: `/auth/login?registered=true`

### 2.2. Luồng Đăng Nhập (Login)

```
┌────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────┐
│ User   │───>│ AuthController│───>│  Security   │───>│   DB     │
│        │    │   /login      │    │   Config    │    │  users   │
└────────┘    └──────────────┘    └─────────────┘    └──────────┘
                                          │
    1. GET /auth/login                    │
       → Show login form                  │
                                          │
    2. POST /auth/login                   │
       → Spring Security intercept        │
       → Load user from DB ───────────────┘
       → Verify password (BCrypt)
       → Create session
       → Redirect based on role:
          - ADMIN → /admin/dashboard
          - USER → /user/index
```

**Chi tiết từng bước:**

1. **User truy cập form đăng nhập:**
   - URL: `GET /auth/login`
   - Template: `templates/auth/login.html`

2. **User submit credentials:**
   - URL: `POST /auth/login`
   - Input: username, password
   - Spring Security xử lý:
     - `UserDetailsService` load user từ DB
     - So sánh password đã hash
     - Tạo `Authentication` object
     - Lưu vào `SecurityContext`
   - Success handler redirect theo role
   - Failure: Quay lại login với error message

### 2.3. Luồng Đăng Xuất (Logout)

```
User clicks "Logout" 
    ↓
POST /logout
    ↓
Spring Security:
    - Invalidate session
    - Clear SecurityContext
    - Delete cookies
    ↓
Redirect to /auth/login?logout=true
```

### 2.4. Session Management

```
┌─────────────────────────────────────────────┐
│          Spring Security Session            │
├─────────────────────────────────────────────┤
│ - Username                                  │
│ - Authorities (Roles)                       │
│ - Authentication status                     │
│ - CSRF Token                                │
└─────────────────────────────────────────────┘
        ↓ Stored in
┌─────────────────────────────────────────────┐
│        Server Memory / Redis                │
│        Expire: 30 minutes (default)         │
└─────────────────────────────────────────────┘
```

---

## 3. LUỒNG BOOK MANAGEMENT (CRUD)

### 3.1. Luồng Xem Danh Sách Sách (List Books)

```
Admin clicks "Books"
    ↓
GET /admin/books
    ↓
AdminBookController.listBooks()
    ↓
BookService.getAllBooks()
    ↓
BookRepository.findAll()
    ↓
Return List<Book>
    ↓
Model + View: templates/admin/books/list.html
    ↓
DataTables render with:
    - Book info
    - Authors
    - Category
    - Price
    - Actions (View, Edit, Delete)
```

**Các bước chi tiết:**

1. **Request:** `GET /admin/books`
2. **Controller:** `AdminBookController.listBooks(Model model)`
3. **Service:** `BookServiceImpl.getAllBooks()`
4. **Repository:** `BookRepository.findAll()` với JOIN FETCH authors, category
5. **View:** Render DataTables với JavaScript
6. **Features:**
   - Search, sort, pagination
   - Filter by category, access type
   - Quick actions

### 3.2. Luồng Thêm Sách Mới (Create Book)

```
┌─────────────────────────────────────────────────────────┐
│                  1. SHOW ADD FORM                        │
└─────────────────────────────────────────────────────────┘
GET /admin/books/add
    ↓
Load data:
    - All categories
    - All authors
    - Access types (enum)
    ↓
Render form: add.html
    ↓

┌─────────────────────────────────────────────────────────┐
│                  2. SUBMIT FORM                          │
└─────────────────────────────────────────────────────────┘
POST /admin/books/add
    ↓
Validate BookDTO:
    - Title required
    - ISBN format
    - Price >= 0
    ↓
Process:
    1. Upload cover image (if provided)
       → Save to F:/datn_uploads/book_asset/image/covers/
       → Return URL: /Book_Asset/image/covers/filename.jpg
    
    2. Create Book entity:
       - Set basic info (title, description, price...)
       - Set cover_image_url from upload
       - Link to category
       - Link to authors (many-to-many)
    
    3. Save to database:
       - BookRepository.save(book)
       - Auto create book_authors records
    ↓
Redirect: /admin/books?success=true
```

**Code flow:**

```java
// Controller
@PostMapping("/add")
public String addBook(@Valid @ModelAttribute BookDTO bookDTO,
                      @RequestParam Set<String> authorIds,
                      @RequestParam(required=false) MultipartFile coverImage) {
    
    // 1. Upload image
    if (coverImage != null && !coverImage.isEmpty()) {
        String imageUrl = bookService.uploadCoverImage(coverImage);
        bookDTO.setCoverImageUrl(imageUrl);
    }
    
    // 2. Create book
    Book savedBook = bookService.createBook(bookDTO, authorIds);
    
    // 3. Redirect
    return "redirect:/admin/books?success=true";
}

// Service
public Book createBook(BookDTO dto, Set<String> authorIds) {
    // Convert DTO to Entity
    Book book = new Book();
    book.setTitle(dto.getTitle());
    // ... set other fields
    
    // Link category
    BookCategory category = categoryRepo.findById(dto.getCategoryId())
        .orElseThrow();
    book.setCategory(category);
    
    // Link authors
    Set<Author> authors = authorRepo.findAllByIdIn(authorIds);
    book.setAuthors(authors);
    
    // Save
    return bookRepository.save(book);
}
```

### 3.3. Luồng Cập Nhật Sách (Update Book)

```
┌─────────────────────────────────────────────────────────┐
│                  1. SHOW EDIT FORM                       │
└─────────────────────────────────────────────────────────┘
GET /admin/books/edit/{id}
    ↓
Load existing book:
    - BookService.getBookById(id)
    - Join fetch authors, category
    ↓
Load form data:
    - All categories
    - All authors (mark selected)
    - Access types
    ↓
Render form: edit.html (pre-filled)
    ↓

┌─────────────────────────────────────────────────────────┐
│                  2. SUBMIT UPDATE                        │
└─────────────────────────────────────────────────────────┘
POST /admin/books/edit/{id}
    ↓
Validate BookDTO
    ↓
Process:
    1. Find existing book by ID
    
    2. Upload new cover (if provided):
       → Delete old file (optional)
       → Upload new file
       → Update cover_image_url
    
    3. Update fields:
       - title, description, price...
       - category
       - authors (clear old + add new)
    
    4. Save changes:
       - BookRepository.save(book)
       - JPA auto update book_authors
    ↓
Redirect: /admin/books/view/{id}?updated=true
```

### 3.4. Luồng Xóa Sách (Delete Book)

```
Admin clicks "Delete" button
    ↓
JavaScript confirmation dialog
    ↓
User confirms
    ↓
AJAX DELETE /admin/books/delete/{id}
    ↓
BookService.deleteBook(id)
    ↓
Check constraints:
    - Has active orders? → Throw exception
    - Has reading progress? → Set flag "deleted"
    ↓
If safe to delete:
    1. Delete book_authors records (cascade)
    2. Delete book_assets records (cascade)
    3. Delete reviews (cascade)
    4. Delete from books table
    5. Delete physical files (optional)
    ↓
Return JSON: { "success": true, "message": "Deleted" }
    ↓
JavaScript removes row from table
```

---

## 4. LUỒNG FILE UPLOAD

### 4.1. Cấu trúc Upload Directory

```
F:/datn_uploads/
└── book_asset/
    ├── image/
    │   ├── covers/           # Ảnh bìa sách
    │   ├── authors/          # Avatar tác giả
    │   ├── banners/          # Banner trang chủ
    │   └── icons/            # Icon category
    ├── files/
    │   └── ebooks/           # File PDF, EPUB
    └── preview/
        └── samples/          # File preview (chương đầu)
```

### 4.2. Mapping URL → Physical Path

```
Database URL                          Browser URL                           Physical Path
─────────────────────────────────────────────────────────────────────────────────────────────
/Book_Asset/image/covers/book1.jpg → localhost:8080/Book_Asset/image/covers/book1.jpg → F:/datn_uploads/book_asset/image/covers/book1.jpg
/Book_Asset/files/ebooks/book1.pdf → localhost:8080/Book_Asset/files/ebooks/book1.pdf → F:/datn_uploads/book_asset/files/ebooks/book1.pdf
```

### 4.3. Luồng Upload Cover Image

```
┌─────────────────────────────────────────────────────────┐
│              UPLOAD BOOK COVER IMAGE                     │
└─────────────────────────────────────────────────────────┘

User selects image file
    ↓
Form submit with MultipartFile
    ↓
Controller receives file
    ↓
BookService.uploadCoverImage(file)
    ↓
Validate:
    - File not empty?
    - Is image? (jpg, png, jpeg, webp)
    - Size <= 5MB?
    ↓
Generate unique filename:
    - Pattern: cover_{bookId}_{timestamp}.{ext}
    - Example: cover_BOOK001_1700000000.jpg
    ↓
Save to physical path:
    - Target: F:/datn_uploads/book_asset/image/covers/
    - Files.copy(inputStream, targetPath)
    ↓
Return database URL:
    - "/Book_Asset/image/covers/cover_BOOK001_1700000000.jpg"
    ↓
Save URL to database (book.cover_image_url)
```

**Code implementation:**

```java
@Service
public class BookServiceImpl implements BookService {
    
    @Value("${file.upload-dir}")
    private String uploadDir; // F:/datn_uploads/
    
    @Override
    public String uploadCoverImage(MultipartFile file) {
        // 1. Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
        
        // 2. Generate filename
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String newFilename = "cover_" + System.currentTimeMillis() + extension;
        
        // 3. Save to disk
        Path uploadPath = Paths.get(uploadDir, "book_asset/image/covers/");
        Files.createDirectories(uploadPath);
        
        Path targetPath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath);
        
        // 4. Return database URL
        return "/Book_Asset/image/covers/" + newFilename;
    }
}
```

### 4.4. Resource Handler Configuration

```java
@Configuration
public class WebConfig implements WebResourceRegistry {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /Book_Asset/image/** to physical directory
        registry.addResourceHandler("/Book_Asset/image/**")
                .addResourceLocations("file:" + uploadDir + "book_asset/image/");
        
        registry.addResourceHandler("/Book_Asset/files/**")
                .addResourceLocations("file:" + uploadDir + "book_asset/files/");
    }
    
    @PostConstruct
    public void init() {
        // Auto create directories on startup
        try {
            Files.createDirectories(Paths.get(uploadDir, "book_asset/image/covers"));
            Files.createDirectories(Paths.get(uploadDir, "book_asset/files/ebooks"));
            // ... create other directories
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directories", e);
        }
    }
}
```

---

## 5. LUỒNG ORDER & PAYMENT

### 5.1. Luồng Thêm Sách Vào Giỏ Hàng (Add to Cart)

```
User clicks "Add to Cart" on book detail page
    ↓
POST /user/cart/add
    ↓
CartService.addToCart(userId, bookId, quantity)
    ↓
Check:
    1. User has cart?
       - No: Create new cart
       - Yes: Use existing cart
    
    2. Book already in cart?
       - Yes: Update quantity
       - No: Add new cart item
    ↓
Save cart_items record
    ↓
Return cart count
    ↓
Update cart badge in UI
```

### 5.2. Luồng Checkout (Đặt Hàng)

```
┌─────────────────────────────────────────────────────────┐
│              1. VIEW CART & CHECKOUT                     │
└─────────────────────────────────────────────────────────┘
User goes to /user/cart
    ↓
Display:
    - All cart items
    - Total price
    - Checkout button
    ↓
User clicks "Checkout"
    ↓

┌─────────────────────────────────────────────────────────┐
│              2. REVIEW ORDER INFO                        │
└─────────────────────────────────────────────────────────┘
GET /user/checkout
    ↓
Display:
    - Delivery address (from user profile)
    - Payment method selection
    - Order summary
    ↓
User confirms
    ↓

┌─────────────────────────────────────────────────────────┐
│              3. PROCESS ORDER                            │
└─────────────────────────────────────────────────────────┘
POST /user/checkout
    ↓
OrderService.createOrder(userId, cartId, paymentMethod)
    ↓
Transaction:
    1. Create Order entity:
       - order_id (auto generate)
       - user_id
       - total_amount
       - status = PENDING
       - payment_status = PENDING
    
    2. Copy cart items to order_items:
       - For each cart_item:
         * Create order_item
         * Copy book_id, quantity, price
    
    3. Update book stock (if applicable)
    
    4. Clear cart:
       - Delete all cart_items
    ↓
Redirect based on payment method:
    - COD: /user/orders/{id}?success=true
    - VNPay: /payment/vnpay?orderId={id}
    - Momo: /payment/momo?orderId={id}
```

### 5.3. Luồng Thanh Toán VNPay

```
┌─────────────────────────────────────────────────────────┐
│              1. REDIRECT TO VNPAY                        │
└─────────────────────────────────────────────────────────┘
User selects VNPay payment
    ↓
PaymentService.createVNPayUrl(orderId, amount)
    ↓
Generate VNPay parameters:
    - vnp_TmnCode (merchant code)
    - vnp_Amount (amount * 100)
    - vnp_OrderInfo
    - vnp_ReturnUrl
    - vnp_SecureHash (HMAC SHA512)
    ↓
Redirect to VNPay payment gateway
    ↓

┌─────────────────────────────────────────────────────────┐
│              2. USER PAYS ON VNPAY                       │
└─────────────────────────────────────────────────────────┘
User enters card info on VNPay site
    ↓
VNPay processes payment
    ↓
VNPay redirects back to:
    ↓
GET /payment/vnpay/callback?vnp_ResponseCode=00&...
    ↓

┌─────────────────────────────────────────────────────────┐
│              3. HANDLE PAYMENT RESULT                    │
└─────────────────────────────────────────────────────────┘
PaymentController.vnpayCallback(params)
    ↓
Verify secure hash
    ↓
Check response code:
    - 00: Success
        * Update order: payment_status = PAID
        * Update order: status = CONFIRMED
        * Send email confirmation
        * Redirect: /user/orders/{id}?payment=success
    
    - Others: Failed
        * Update order: payment_status = FAILED
        * Keep order: status = PENDING
        * Redirect: /user/orders/{id}?payment=failed
```

---

## 6. LUỒNG REVIEW & RATING

### 6.1. Luồng Viết Review

```
User goes to book detail page
    ↓
Clicks "Write Review"
    ↓
Check:
    - User logged in? → Yes, continue | No, redirect to login
    - User purchased this book? → Yes, continue | No, show error
    - User already reviewed? → Yes, show edit form | No, show new form
    ↓
Display review form:
    - Rating (1-5 stars)
    - Review text
    ↓
POST /user/reviews/add
    ↓
ReviewService.addReview(userId, bookId, rating, comment)
    ↓
Validate:
    - Rating in range 1-5
    - Comment not empty
    ↓
Save review to database
    ↓
Update book statistics:
    1. Recalculate average_rating:
       - AVG(rating) from all reviews
    2. Update total_reviews count
    ↓
Redirect to book detail page
```

### 6.2. Luồng Update Book Rating (Auto)

```
┌─────────────────────────────────────────────────────────┐
│         TRIGGER: After any review CRUD operation         │
└─────────────────────────────────────────────────────────┘

ReviewService calls:
    bookService.updateBookRating(bookId)
    ↓
Query all reviews for this book:
    SELECT AVG(rating), COUNT(*) FROM reviews WHERE book_id = ?
    ↓
Update book record:
    UPDATE books SET
        average_rating = ?,
        total_reviews = ?
    WHERE book_id = ?
    ↓
Return updated book
```

---

## 7. LUỒNG BẢO MẬT (SECURITY FLOWS)

### 7.1. Authorization Flow

```
┌─────────────────────────────────────────────────────────┐
│              REQUEST COMES IN                            │
└─────────────────────────────────────────────────────────┘
    ↓
Spring Security Filter Chain:
    ↓
┌─────────────────────────────────────────────────────────┐
│         1. Check if authenticated                        │
└─────────────────────────────────────────────────────────┘
    ↓
    No → Redirect to /auth/login
    ↓
    Yes → Continue
    ↓
┌─────────────────────────────────────────────────────────┐
│         2. Check URL authorization                       │
└─────────────────────────────────────────────────────────┘
    ↓
/admin/** → Requires ROLE_ADMIN
    ↓
    Has ROLE_ADMIN? → Yes: Allow | No: 403 Forbidden
    ↓
/user/** → Requires ROLE_USER or ROLE_ADMIN
    ↓
    Has any role? → Yes: Allow | No: 403 Forbidden
    ↓
┌─────────────────────────────────────────────────────────┐
│         3. Check CSRF Token (for POST/PUT/DELETE)        │
└─────────────────────────────────────────────────────────┘
    ↓
    Token valid? → Yes: Allow | No: 403 Invalid CSRF
    ↓
┌─────────────────────────────────────────────────────────┐
│         4. Execute controller method                     │
└─────────────────────────────────────────────────────────┘
```

### 7.2. SecurityConfig Rules

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                // Public URLs
                .requestMatchers("/", "/home", "/auth/**").permitAll()
                .requestMatchers("/Book_Asset/**", "/shared/**").permitAll()
                
                // Admin URLs
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // User URLs
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                
                // All other URLs require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .successHandler(customSuccessHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
            );
        
        return http.build();
    }
}
```

---

## 8. LUỒNG ĐỌC SÁCH (READING FLOW)

### 8.1. Luồng Mở Sách Để Đọc

```
User clicks "Read Book" on book detail
    ↓
Check access rights:
    ↓
┌─────────────────────────────────────────────────────────┐
│              1. FREE BOOK                                │
└─────────────────────────────────────────────────────────┘
    - Anyone can read
    - No payment required
    ↓
┌─────────────────────────────────────────────────────────┐
│              2. PURCHASED BOOK                           │
└─────────────────────────────────────────────────────────┘
    - Check: User has order for this book?
    - Order status = COMPLETED?
    - Payment status = PAID?
    → Yes: Allow | No: Show "Buy Now" button
    ↓
┌─────────────────────────────────────────────────────────┐
│              3. SUBSCRIPTION BOOK                        │
└─────────────────────────────────────────────────────────┘
    - Check: User has active subscription?
    - Subscription end_date > now?
    → Yes: Allow | No: Show "Subscribe" button
    ↓
┌─────────────────────────────────────────────────────────┐
│              4. LOAD EBOOK FILE                          │
└─────────────────────────────────────────────────────────┘
If access granted:
    ↓
Get book asset:
    - Query bookassets table by book_id
    - Get file_path: /Book_Asset/files/ebooks/book.pdf
    ↓
Load reading progress:
    - Query reading_progress table
    - Get last_page, last_position
    ↓
Render reader:
    - PDF: Use PDF.js viewer
    - EPUB: Use EPUB.js viewer
    - Open at saved position
    ↓
Track reading:
    - Update view_count (once per session)
    - Save reading progress (every 30 seconds)
```

### 8.2. Luồng Lưu Tiến Độ Đọc

```
User reads book
    ↓
JavaScript tracks:
    - Current page number
    - Current scroll position
    - Reading time
    ↓
Auto save every 30 seconds:
    ↓
POST /user/reading-progress/save
    ↓
ReadingProgressService.updateProgress(
    userId, bookId, lastPage, lastPosition, readingTime
)
    ↓
Check existing progress:
    ↓
    Found → Update record
    Not found → Create new record
    ↓
Save to database:
    - last_page
    - last_position
    - updated_at
    ↓
Calculate completion percentage:
    completion = (last_page / total_pages) * 100
    ↓
If completion >= 100%:
    - Mark as completed
    - Update finish_date
    - Add to "Completed Books" list
```

---

## 📊 TỔNG KẾT CÁC LUỒNG CHÍNH

| Luồng | Endpoints | Roles Required | Status |
|-------|-----------|---------------|---------|
| **Register** | GET/POST /auth/register | Public | ✅ Completed |
| **Login** | GET/POST /auth/login | Public | ✅ Completed |
| **List Books (Admin)** | GET /admin/books | ADMIN | ✅ Completed |
| **Create Book** | GET/POST /admin/books/add | ADMIN | ✅ Completed |
| **Update Book** | GET/POST /admin/books/edit/{id} | ADMIN | ✅ Completed |
| **Delete Book** | DELETE /admin/books/delete/{id} | ADMIN | ✅ Completed |
| **Upload Cover** | POST /admin/books/upload-cover | ADMIN | ✅ Completed |
| **View Book Detail** | GET /user/books/{id} | USER, ADMIN | 🔄 In Progress |
| **Add to Cart** | POST /user/cart/add | USER | 🔄 In Progress |
| **Checkout** | POST /user/checkout | USER | ❌ Not Started |
| **Payment (VNPay)** | POST /payment/vnpay | USER | ❌ Not Started |
| **Write Review** | POST /user/reviews/add | USER | 🔄 In Progress |
| **Read Book** | GET /user/books/read/{id} | USER | ❌ Not Started |
| **Save Progress** | POST /user/reading-progress/save | USER | ❌ Not Started |

---

## 🔐 BẢO MẬT & AUTHORIZATION

### Role-Based Access Control (RBAC)

| URL Pattern | Allowed Roles | Description |
|-------------|---------------|-------------|
| `/` | ALL | Trang chủ |
| `/home` | ALL | Trang chủ |
| `/auth/**` | PUBLIC | Login, Register |
| `/Book_Asset/**` | PUBLIC | Static resources |
| `/admin/**` | ADMIN | Admin panel |
| `/user/**` | USER, ADMIN | User features |

### CSRF Protection

- Enabled cho POST, PUT, DELETE
- Token auto-generated trong forms
- Thymeleaf: `th:action` tự động thêm token

---

## 📁 TÀI LIỆU THAM KHẢO

- **Entity Design:** `DB/ebook_store.sql`
- **Repository Methods:** Xem các `*Repository.java`
- **Service Implementation:** Xem các `*ServiceImpl.java`
- **Controller Endpoints:** Xem các `*Controller.java`

---

**Cập nhật lần cuối:** 23/11/2025  
**Người thực hiện:** Development Team

