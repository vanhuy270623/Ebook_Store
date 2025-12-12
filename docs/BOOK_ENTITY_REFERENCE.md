# BOOK ENTITY - FIELD REFERENCE

**Ngày tạo:** 10/12/2025  
**Mục đích:** Tài liệu tham khảo về các fields trong Book entity để tránh lỗi Thymeleaf

---

## 📋 BOOK ENTITY STRUCTURE

### Primary Key
- **`bookId`** (String) - Primary key, UUID format
  - ⚠️ KHÔNG PHẢI `id`!
  - Database column: `book_id`
  - Example: "550e8400-e29b-41d4-a716-446655440000"

### Basic Information
- **`title`** (String) - Tên sách
- **`description`** (String/TEXT) - Mô tả sách
- **`publisher`** (String) - Nhà xuất bản
- **`publicationYear`** (Integer) - Năm xuất bản
- **`language`** (String) - Ngôn ngữ (default: "vi")
- **`pages`** (Integer) - Số trang
- **`isbn`** (String) - Mã ISBN

### Price & Access
- **`price`** (BigDecimal) - Giá sách
  - ⚠️ Sử dụng BigDecimal, KHÔNG PHẢI double!
  - Default: BigDecimal.ZERO
- **`accessType`** (Enum: AccessType)
  - Values: FREE, PURCHASE, SUBSCRIPTION, BOTH
  - Default: PURCHASE

### Media Files
- **`coverImageUrl`** (String) - URL ảnh bìa
- **`isDownloadable`** (Boolean) - Có thể download không
  - Default: false

### Statistics
- **`averageRating`** (Float) - Đánh giá trung bình (0.0 - 5.0)
  - Default: 0.0f
- **`totalReviews`** (Integer) - Tổng số đánh giá
  - Default: 0
- **`viewCount`** (Integer) - Số lượt xem
  - Default: 0

### Timestamps
- **`createdAt`** (LocalDateTime) - Thời gian tạo
- **`updatedAt`** (LocalDateTime) - Thời gian cập nhật

### Relationships

#### Many-to-One
- **`bookCategory`** (BookCategory) - Danh mục sách
  - Lazy loading
  - Access: `book.bookCategory.categoryName`
  - ⚠️ Check null: `book.bookCategory?.categoryName`

#### Many-to-Many
- **`authors`** (Set<Author>) - Danh sách tác giả
  - Join table: book_authors
  - Access: `book.authors`
  - Iterate: `th:each="author : ${book.authors}"`

#### One-to-Many
- **`bookAssets`** (Set<BookAsset>) - Files (source, preview)
- **`reviews`** (Set<Review>) - Đánh giá

---

## ✅ THYMELEAF USAGE EXAMPLES

### ❌ SAI - Các lỗi phổ biến

```html
<!-- ❌ SAI: Không có field "id" -->
<span th:text="${book.id}">ID</span>

<!-- ❌ SAI: Không có field "status" -->
<span th:text="${book.status}">Status</span>

<!-- ❌ SAI: price là BigDecimal, không thể dùng trực tiếp -->
<span th:text="${book.price}">Price</span>

<!-- ❌ SAI: Không check null cho relationship -->
<span th:text="${book.bookCategory.categoryName}">Category</span>
```

### ✅ ĐÚNG - Cách sử dụng đúng

```html
<!-- ✅ ĐÚNG: Sử dụng bookId -->
<span th:text="${book.bookId}">book-id-123</span>

<!-- ✅ ĐÚNG: Format BigDecimal với #numbers -->
<span th:text="${#numbers.formatDecimal(book.price, 0, 'COMMA', 0, 'POINT')} + ' VNĐ'">
    100,000 VNĐ
</span>

<!-- ✅ ĐÚNG: Safe navigation với ?. -->
<span th:text="${book.bookCategory?.categoryName ?: 'Chưa phân loại'}">
    Category
</span>

<!-- ✅ ĐÚNG: Check null trước khi access -->
<div th:if="${book.bookCategory != null}">
    <span th:text="${book.bookCategory.categoryName}">Category</span>
</div>

<!-- ✅ ĐÚNG: Iterate authors -->
<ul>
    <li th:each="author : ${book.authors}" 
        th:text="${author.name}">Author Name</li>
</ul>

<!-- ✅ ĐÚNG: Format LocalDateTime -->
<span th:text="${#temporals.format(book.createdAt, 'dd/MM/yyyy HH:mm')}">
    10/12/2025 14:30
</span>

<!-- ✅ ĐÚNG: Display rating -->
<span th:text="${book.averageRating} + '/5.0'">4.5/5.0</span>

<!-- ✅ ĐÚNG: Check access type -->
<span th:if="${book.accessType == T(stu.datn.ebook_store.entity.Book.AccessType).FREE}">
    🆓 Miễn phí
</span>
```

---

## 🔍 DEBUG CHECKLIST

Khi gặp lỗi Thymeleaf với Book entity:

### 1. Kiểm tra tên field
- [ ] Đúng tên field trong entity? (bookId, NOT id)
- [ ] Field có tồn tại không? (status không tồn tại)
- [ ] Case-sensitive đúng chưa? (bookId, NOT BookId)

### 2. Kiểm tra data type
- [ ] BigDecimal → Dùng #numbers.formatDecimal()
- [ ] LocalDateTime → Dùng #temporals.format()
- [ ] Enum → Dùng T() notation hoặc toString()

### 3. Kiểm tra relationship
- [ ] Lazy loading → Đảm bảo fetch trong Service
- [ ] Null safety → Dùng ?. operator
- [ ] Collection → Check empty trước khi iterate

### 4. Common mistakes
```java
// ❌ book.id → ✅ book.bookId
// ❌ book.status → ✅ (Field không tồn tại)
// ❌ book.category → ✅ book.bookCategory
// ❌ book.price → ✅ #numbers.formatDecimal(book.price, ...)
```

---

## 📊 DATABASE SCHEMA REFERENCE

```sql
CREATE TABLE books (
    book_id VARCHAR(50) PRIMARY KEY,        -- bookId
    book_category_id VARCHAR(50),           -- bookCategory
    title VARCHAR(255) NOT NULL,            -- title
    description TEXT,                       -- description
    price DECIMAL(15,2) NOT NULL,          -- price (BigDecimal)
    cover_image_url VARCHAR(500),          -- coverImageUrl
    publisher VARCHAR(255),                 -- publisher
    publication_year INT,                   -- publicationYear
    language VARCHAR(10) DEFAULT 'vi',      -- language
    pages INT,                              -- pages
    isbn VARCHAR(20),                       -- isbn
    access_type ENUM(...) DEFAULT 'PURCHASE', -- accessType
    is_downloadable BOOLEAN DEFAULT FALSE,  -- isDownloadable
    average_rating FLOAT DEFAULT 0.0,       -- averageRating
    total_reviews INT DEFAULT 0,            -- totalReviews
    view_count INT DEFAULT 0,               -- viewCount
    created_at DATETIME,                    -- createdAt
    updated_at DATETIME,                    -- updatedAt
    FOREIGN KEY (book_category_id) REFERENCES book_categories(category_id)
);
```

---

## 🛠️ QUICK FIX GUIDE

### Lỗi: Cannot invoke "getXxx()" because "book" is null
```html
<!-- Fix: Check null trước -->
<div th:if="${book != null}">
    <span th:text="${book.title}">Title</span>
</div>
```

### Lỗi: Property 'xxx' not found on type 'Book'
```html
<!-- Fix: Kiểm tra tên field đúng -->
<!-- ❌ book.id → ✅ book.bookId -->
<!-- ❌ book.status → Xóa (không tồn tại) -->
```

### Lỗi: Exception evaluating SpringEL expression
```html
<!-- Fix: Check data type -->
<!-- BigDecimal → Dùng #numbers -->
<!-- LocalDateTime → Dùng #temporals -->
<!-- Relationship → Dùng safe navigation ?. -->
```

---

## 📚 RELATED ENTITIES

### BookCategory
- `categoryId` (String) - Primary key
- `categoryName` (String) - Tên danh mục

### Author
- `authorId` (String) - Primary key
- `name` (String) - Tên tác giả
- `bio` (String) - Tiểu sử
- `avatarUrl` (String) - Ảnh đại diện

### BookAsset
- `assetId` (String) - Primary key
- `assetType` (Enum) - SOURCE, PREVIEW, COVER
- `fileUrl` (String) - URL file
- `fileFormat` (String) - PDF, EPUB

---

**Cập nhật lần cuối:** 10/12/2025

**Note:** Luôn tham khảo entity class để biết chính xác tên fields:
`src/main/java/stu/datn/ebook_store/entity/Book.java`

