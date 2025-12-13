# 🔍 QUY TRÌNH TÌM VÀ SỬA LỖI THYMELEAF

**Ngày tạo:** 10/12/2025  
**Mục đích:** Hướng dẫn chi tiết cách kiểm tra và so sánh dữ liệu khi gặp lỗi Thymeleaf

---

## 📋 MỤC LỤC

1. [Các Nguồn Dữ Liệu Cần So Sánh](#1-các-nguồn-dữ-liệu-cần-so-sánh)
2. [Quy Trình Debug 5 Bước](#2-quy-trình-debug-5-bước)
3. [Ví Dụ Thực Tế: Lỗi book.getId()](#3-ví-dụ-thực-tế-lỗi-bookgetid)
4. [Tools Debug](#4-tools-debug)
5. [Checklist Nhanh](#5-checklist-nhanh)

---

## 1. CÁC NGUỒN DỮ LIỆU CẦN SO SÁNH

### 🗄️ A. DATABASE (Nguồn gốc)

**File:** `DB/ebook_store.sql`

**Cách xem:**
```sql
-- Xem cấu trúc bảng
DESCRIBE books;

-- Hoặc
SHOW CREATE TABLE books;

-- Xem tên cột
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'books';
```

**Ví dụ với bảng `books`:**
```sql
CREATE TABLE books (
    book_id VARCHAR(50) PRIMARY KEY,        -- ← Tên cột
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15,2) NOT NULL,
    book_category_id VARCHAR(50),
    access_type ENUM('FREE', 'PURCHASE', 'SUBSCRIPTION', 'BOTH'),
    view_count INT DEFAULT 0,
    average_rating FLOAT DEFAULT 0.0,
    created_at DATETIME,
    updated_at DATETIME
);
```

**Quy tắc đặt tên Database:**
- Sử dụng **snake_case**: `book_id`, `access_type`, `view_count`
- Chữ thường toàn bộ
- Dùng dấu gạch dưới `_` để ngăn cách

---

### 🏗️ B. ENTITY CLASS (JPA Mapping)

**File:** `src/main/java/stu/datn/ebook_store/entity/Book.java`

**Cách xem:**
```java
@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @Column(name = "book_id")      // ← Database column name
    private String bookId;          // ← Java field name (camelCase)
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "price")
    private BigDecimal price;       // ← Kiểu dữ liệu quan trọng!
    
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private AccessType accessType;  // ← Enum type
    
    @Column(name = "view_count")
    private Integer viewCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt; // ← LocalDateTime, không phải Date
    
    // Getters/Setters
    public String getBookId() { return bookId; }  // ← Method name
    public void setBookId(String bookId) { this.bookId = bookId; }
}
```

**Quy tắc đặt tên Entity:**
- Field name: **camelCase** (`bookId`, `accessType`, `viewCount`)
- Getter/Setter: `getBookId()`, `setBookId()`
- Annotation `@Column(name = "...")` map đến database column

**Mapping Table:**

| Database Column | Entity Field | Getter Method | Setter Method |
|----------------|--------------|---------------|---------------|
| `book_id` | `bookId` | `getBookId()` | `setBookId(String)` |
| `title` | `title` | `getTitle()` | `setTitle(String)` |
| `price` | `price` | `getPrice()` | `setPrice(BigDecimal)` |
| `access_type` | `accessType` | `getAccessType()` | `setAccessType(AccessType)` |
| `view_count` | `viewCount` | `getViewCount()` | `setViewCount(Integer)` |
| `created_at` | `createdAt` | `getCreatedAt()` | `setCreatedAt(LocalDateTime)` |

---

### 🎯 C. CONTROLLER (Data Preparation)

**File:** `src/main/java/stu/datn/ebook_store/controller/...Controller.java`

**Cách kiểm tra:**
```java
@GetMapping("/books")
public String listBooks(Model model) {
    log.debug("=== listBooks START ===");
    
    // 1. Lấy dữ liệu từ Service
    List<Book> books = bookService.getAllBooks();
    log.debug("Loaded {} books", books.size());
    
    // 2. Add vào Model - CHỖ NÀY QUAN TRỌNG!
    model.addAttribute("books", books);        // ← Key name: "books"
    model.addAttribute("totalBooks", books.size());
    
    // 3. In ra để kiểm tra
    if (!books.isEmpty()) {
        Book firstBook = books.get(0);
        log.debug("First book ID: {}", firstBook.getBookId());
        log.debug("First book title: {}", firstBook.getTitle());
    }
    
    log.debug("=== listBooks END ===");
    return "admin/books/list";  // ← Template path
}
```

**Những gì cần check:**
1. **Attribute key name:** `model.addAttribute("books", ...)` → Dùng `${books}` trong template
2. **Data type:** List<Book>, Book, String, Integer, etc.
3. **Null check:** Object có thể null không?
4. **Log output:** Xem console để biết data có được load không

---

### 🎨 D. THYMELEAF TEMPLATE (Display)

**File:** `src/main/resources/templates/.../*.html`

**Cách sử dụng:**
```html
<!-- 1. Truy cập biến từ Model -->
<div th:if="${books != null}">
    <!-- books = key trong model.addAttribute("books", ...) -->
</div>

<!-- 2. Truy cập property của object -->
<span th:text="${book.bookId}">ID</span>
<!-- book.bookId = gọi getter getBookId() -->

<!-- 3. Method call -->
<span th:text="${book.getBookId()}">ID</span>
<!-- Tương đương với trên -->

<!-- 4. Safe navigation -->
<span th:text="${book?.bookCategory?.categoryName ?: 'N/A'}">Category</span>
<!-- ?. = null-safe, ?: = default value -->
```

**Quy tắc Thymeleaf:**
- `${variableName}` = Lấy từ Model
- `${object.property}` = Gọi `object.getProperty()`
- `${object.method()}` = Gọi method trực tiếp
- `${object?.property}` = Null-safe access
- `${expression ?: default}` = Elvis operator (default value)

---

## 2. QUY TRÌNH DEBUG 5 BƯỚC

### 🔴 Khi gặp lỗi Thymeleaf

**Error message mẫu:**
```
org.thymeleaf.exceptions.TemplateProcessingException: 
Exception evaluating SpringEL expression: "book.getId()" 
(template: "debug/thymeleaf-test" - line 220, col 32)
```

---

### BƯỚC 1: Đọc Error Message

**Thông tin quan trọng:**
- ❗ **Expression:** `book.getId()` ← Cái gì đang gây lỗi
- ❗ **Template:** `debug/thymeleaf-test` ← File nào
- ❗ **Line:** 220, col 32 ← Vị trí chính xác

**Mở file:**
```
src/main/resources/templates/debug/thymeleaf-test.html
Dòng 220
```

---

### BƯỚC 2: Kiểm tra Entity Class

**Mở file Entity:**
```
src/main/java/stu/datn/ebook_store/entity/Book.java
```

**Tìm kiếm:**
1. Có field `id` không? → **KHÔNG**
2. Có method `getId()` không? → **KHÔNG**
3. Field thực tế là gì? → `bookId` (String)
4. Method getter là gì? → `getBookId()`

**So sánh:**
```java
// ❌ KHÔNG TỒN TẠI
private Long id;
public Long getId() { ... }

// ✅ THỰC TẾ
private String bookId;
public String getBookId() { ... }
```

---

### BƯỚC 3: Kiểm tra Controller

**Tìm Controller tương ứng:**
```java
// Từ error message: template "debug/thymeleaf-test"
// → Tìm controller return "debug/thymeleaf-test"

@GetMapping("/debug/thymeleaf-test")
public String testThymeleaf(Model model) {
    // Kiểm tra book được add như thế nào
    Book testBook = new Book();
    testBook.setBookId("test-book-1");  // ← setBookId, NOT setId
    testBook.setTitle("Test Book");
    
    model.addAttribute("book", testBook);  // ← Key: "book"
    return "debug/thymeleaf-test";
}
```

**Verify:**
- Key trong Model: `"book"` → Template dùng `${book}`
- Object type: `Book` → Có methods nào?
- Data đã được set chưa?

---

### BƯỚC 4: Kiểm tra Database (Optional)

**Nếu data từ database:**
```sql
-- Xem structure
DESCRIBE books;

-- Xem data thực tế
SELECT * FROM books LIMIT 1;
```

**Kết quả:**
```
book_id: "550e8400-e29b-41d4-a716-446655440000"
title: "Học Java từ A-Z"
price: 299000.00
```

**Confirm:**
- Column name: `book_id` (snake_case)
- Entity field: `bookId` (camelCase)
- Template: `${book.bookId}` hoặc `${book.getBookId()}`

---

### BƯỚC 5: So Sánh và Sửa

**Bảng so sánh:**

| Nguồn | Tên | Type | Cách truy cập |
|-------|-----|------|---------------|
| **Database** | `book_id` | VARCHAR(50) | SQL: `book_id` |
| **Entity** | `bookId` | String | Java: `book.bookId` hoặc `book.getBookId()` |
| **Template (SAI)** | `book.getId()` | ❌ | Thymeleaf: `${book.getId()}` |
| **Template (ĐÚNG)** | `book.bookId` hoặc `book.getBookId()` | ✅ | Thymeleaf: `${book.bookId}` |

**Fix:**
```html
<!-- ❌ TRƯỚC (SAI) -->
<span th:text="${book.getId()}">ID</span>

<!-- ✅ SAU (ĐÚNG) - Cách 1 -->
<span th:text="${book.bookId}">ID</span>

<!-- ✅ SAU (ĐÚNG) - Cách 2 -->
<span th:text="${book.getBookId()}">ID</span>
```

---

## 3. VÍ DỤ THỰC TẾ: LỖI book.getId()

### 🐛 Lỗi đã gặp

```
Error: Exception evaluating SpringEL expression: "book.getId()"
Template: debug/thymeleaf-test.html, line 220
```

### 🔍 Debug Process

#### Bước 1: Đọc error
- Expression: `book.getId()`
- File: `debug/thymeleaf-test.html`
- Line: 220

#### Bước 2: Check Entity
```java
// src/main/java/stu/datn/ebook_store/entity/Book.java

@Entity
public class Book {
    @Id
    @Column(name = "book_id")
    private String bookId;  // ← Tên field: bookId
    
    // Getter
    public String getBookId() { ... }  // ← Method: getBookId()
    
    // ❌ KHÔNG CÓ
    // public Long getId() { ... }
}
```

#### Bước 3: Check Database
```sql
-- DB/ebook_store.sql

CREATE TABLE books (
    book_id VARCHAR(50) PRIMARY KEY,  -- ← Column: book_id
    -- ...
);
```

#### Bước 4: So sánh

| Nguồn | Tên | Tồn tại? |
|-------|-----|---------|
| Database | `book_id` | ✅ |
| Entity Field | `bookId` | ✅ |
| Entity Getter | `getBookId()` | ✅ |
| Template (sai) | `getId()` | ❌ NOT FOUND |

#### Bước 5: Fix
```html
<!-- ❌ SAI -->
<span th:text="${book.getId()}">placeholder</span>

<!-- ✅ ĐÚNG -->
<span th:text="${book.getBookId()}">placeholder</span>
```

---

## 4. TOOLS DEBUG

### 🛠️ Tool 1: Debug Dashboard

**URL:** `http://localhost:2706/debug`

**Chức năng:**
- Test database connection
- View Model attributes
- Check authentication
- View all books

### 🛠️ Tool 2: Model Viewer

**URL:** `http://localhost:2706/debug/model-viewer`

**Hiển thị:**
- Tất cả attributes trong Model
- Data types
- Null values

**Cách dùng:**
```java
// Controller
@GetMapping("/debug/model-viewer")
public String viewModel(Model model) {
    model.addAttribute("book", testBook);
    
    // Xem tất cả attributes
    Map<String, Object> allAttrs = model.asMap();
    allAttrs.forEach((key, value) -> {
        log.debug("Model[{}] = {} ({})", 
            key, 
            value, 
            value != null ? value.getClass().getSimpleName() : "null"
        );
    });
    
    return "debug/model-viewer";
}
```

**Console output:**
```
Model[book] = Book@123456 (Book)
Model[books] = [Book@111, Book@222] (ArrayList)
Model[totalBooks] = 10 (Integer)
```

### 🛠️ Tool 3: IntelliJ Debugger

**Set breakpoint tại Controller:**
```java
@GetMapping("/books")
public String listBooks(Model model) {
    List<Book> books = bookService.getAllBooks();
    // ← BREAKPOINT HERE
    model.addAttribute("books", books);
    return "admin/books/list";
}
```

**Evaluate Expressions (Alt+F8):**
```java
books.size()
books.get(0).getBookId()
books.get(0).getTitle()
model.asMap()
```

### 🛠️ Tool 4: Console Logging

**Thêm log trong Controller:**
```java
@GetMapping("/books")
public String listBooks(Model model) {
    log.debug("=== listBooks START ===");
    
    List<Book> books = bookService.getAllBooks();
    log.debug("Loaded {} books", books.size());
    
    // Log chi tiết
    books.forEach(book -> {
        log.debug("Book: id={}, title={}", 
            book.getBookId(), 
            book.getTitle()
        );
    });
    
    model.addAttribute("books", books);
    
    // Log Model contents
    log.debug("Model attributes: {}", model.asMap().keySet());
    
    log.debug("=== listBooks END ===");
    return "admin/books/list";
}
```

**Console output:**
```
DEBUG: === listBooks START ===
DEBUG: Loaded 10 books
DEBUG: Book: id=book-001, title=Java Programming
DEBUG: Book: id=book-002, title=Spring Boot Guide
DEBUG: Model attributes: [books, totalBooks]
DEBUG: === listBooks END ===
```

### 🛠️ Tool 5: MySQL Workbench

**Kiểm tra data thực tế:**
```sql
-- Xem cấu trúc
DESCRIBE books;

-- Xem data
SELECT book_id, title, price 
FROM books 
LIMIT 5;

-- Check specific record
SELECT * 
FROM books 
WHERE book_id = 'book-001';
```

---

## 5. CHECKLIST NHANH

### ✅ Khi gặp lỗi Thymeleaf

- [ ] **Đọc error message**
  - Expression nào gây lỗi?
  - File template nào?
  - Line number?

- [ ] **Check Entity Class**
  - Field name là gì? (camelCase)
  - Getter method tên gì? (getXxx())
  - Data type là gì?

- [ ] **Check Controller**
  - Model attribute key là gì?
  - Data đã được add vào Model chưa?
  - Data có null không?

- [ ] **So sánh Database → Entity → Template**
  - Database: snake_case (`book_id`)
  - Entity: camelCase (`bookId`)
  - Getter: (`getBookId()`)
  - Template: `${book.bookId}` hoặc `${book.getBookId()}`

- [ ] **Test fix**
  - Refresh browser
  - Check console log
  - Verify data hiển thị đúng

---

## 📊 BẢNG SO SÁNH TỔNG HỢP

### Book Entity - Full Mapping

| Database Column | Entity Field | Data Type | Getter Method | Template Expression |
|----------------|--------------|-----------|---------------|---------------------|
| `book_id` | `bookId` | String | `getBookId()` | `${book.bookId}` |
| `title` | `title` | String | `getTitle()` | `${book.title}` |
| `description` | `description` | String | `getDescription()` | `${book.description}` |
| `price` | `price` | BigDecimal | `getPrice()` | `${#numbers.formatDecimal(book.price, ...)}` |
| `book_category_id` | `bookCategory` | BookCategory | `getBookCategory()` | `${book.bookCategory?.categoryName}` |
| `access_type` | `accessType` | Enum | `getAccessType()` | `${book.accessType}` |
| `view_count` | `viewCount` | Integer | `getViewCount()` | `${book.viewCount}` |
| `average_rating` | `averageRating` | Float | `getAverageRating()` | `${book.averageRating}` |
| `total_reviews` | `totalReviews` | Integer | `getTotalReviews()` | `${book.totalReviews}` |
| `created_at` | `createdAt` | LocalDateTime | `getCreatedAt()` | `${#temporals.format(book.createdAt, 'dd/MM/yyyy')}` |

---

## 💡 TIPS QUAN TRỌNG

### 1. Naming Convention

```
Database:  snake_case     (book_id, access_type)
Java:      camelCase      (bookId, accessType)
Method:    getXxx()       (getBookId(), getAccessType())
Template:  ${object.xxx}  (${book.bookId}, ${book.accessType})
```

### 2. Kiểm tra Data Type

```java
// BigDecimal - Phải format
${#numbers.formatDecimal(book.price, 0, 'COMMA', 0, 'POINT')}

// LocalDateTime - Phải format
${#temporals.format(book.createdAt, 'dd/MM/yyyy HH:mm')}

// Enum - Có thể dùng trực tiếp
${book.accessType}  // Hiển thị: "PURCHASE"

// Relationship - Check null
${book.bookCategory?.categoryName ?: 'N/A'}
```

### 3. Null Safety

```html
<!-- ❌ Unsafe -->
${book.bookCategory.categoryName}

<!-- ✅ Safe - Option 1 -->
${book.bookCategory?.categoryName}

<!-- ✅ Safe - Option 2 -->
${book.bookCategory?.categoryName ?: 'Chưa phân loại'}

<!-- ✅ Safe - Option 3 -->
<span th:if="${book.bookCategory != null}" 
      th:text="${book.bookCategory.categoryName}">Category</span>
```

---

## 📚 TÀI LIỆU THAM KHẢO

- [BOOK_ENTITY_REFERENCE.md](BOOK_ENTITY_REFERENCE.md) - Chi tiết Book entity
- [DEBUG_THYMELEAF_ERRORS.md](DEBUG_THYMELEAF_ERRORS.md) - Các lỗi Thymeleaf
- [DEBUG_COMMON_ERRORS.md](DEBUG_COMMON_ERRORS.md) - Lỗi phổ biến

---

**Cập nhật lần cuối:** 10/12/2025

**Remember:** 
- Database = snake_case
- Entity = camelCase  
- Template = ${entity.field} hoặc ${entity.getField()}
- Luôn check null!

