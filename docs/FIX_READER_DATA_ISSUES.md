# Sửa Lỗi Lấy Dữ Liệu Không Đúng Trong Trang Reader

**Ngày:** 13/12/2025  
**Vấn đề:** Trang reader đang không lấy đúng dữ liệu theo entity và SQL

## 🔍 CÁC VẤN ĐỀ ĐÃ PHÁT HIỆN

### 1. **File: `reader.html`**

#### Vấn đề 1.1: Truy cập `progress.book?.pages` (Dòng 110)
- **Lỗi:** Template đang cố truy cập `progress.book.pages` với LAZY fetch
- **Rủi ro:** Có thể gây `LazyInitializationException`
- **Giải pháp:** Sử dụng `book.pages` trực tiếp (đã có `book` trong model)

```html
<!-- SAI -->
<span th:text="${progress.book?.pages ?: 'N/A'}">300</span> trang

<!-- ĐÚNG -->
<span th:if="${book.pages}">
    / <span th:text="${book.pages}">300</span> trang
</span>
```

#### Vấn đề 1.2: Thuộc tính `progress.startDate` không tồn tại (Dòng 117-118)
- **Lỗi:** `startDate` KHÔNG TỒN TẠI trong entity `ReadingProgress`
- **Entity chỉ có:** `createdAt`, `lastReadAt`
- **SQL chỉ có:** `created_at`, `last_read_at`

```html
<!-- SAI -->
<div class="last-read" th:if="${progress.startDate}">
    Bắt đầu đọc từ <span th:text="${#temporals.format(progress.startDate, 'dd/MM/yyyy')}">01/01/2024</span>
</div>

<!-- ĐÚNG -->
<div class="last-read" th:if="${progress.createdAt}">
    Bắt đầu đọc từ <span th:text="${#temporals.format(progress.createdAt, 'dd/MM/yyyy')}">01/01/2024</span>
</div>
```

#### Vấn đề 1.3: Lỗi cú pháp `th:onerror` (Dòng 18)
```html
<!-- SAI -->
th:onerror="this.src='@{/images/default-cover.jpg}'"

<!-- ĐÚNG -->
onerror="this.src='/images/default-cover.jpg'"
```

### 2. **File: `reading-history.html`**

#### Vấn đề 2.1: Thuộc tính `progress.currentPage` không tồn tại (Dòng 135)
- **Lỗi:** `currentPage` KHÔNG TỒN TẠI trong entity `ReadingProgress`
- **Entity chỉ có:** `lastReadLocation` (varchar 500)

```html
<!-- SAI -->
<span th:text="${progress.currentPage != null ? progress.currentPage : 0}">0</span>

<!-- ĐÚNG -->
<span th:text="${progress.lastReadLocation != null ? progress.lastReadLocation : 'Chưa đọc'}">page-1</span>
```

#### Vấn đề 2.2: Thiếu namespace `sec` cho Spring Security
```html
<!-- SAI -->
<html lang="vi" xmlns:th="http://www.thymeleaf.org">

<!-- ĐÚNG -->
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

## ✅ CÁC THAY ĐỔI ĐÃ THỰC HIỆN

### 1. Sửa `reader.html`
- ✅ Thay `progress.book?.pages` → `book.pages`
- ✅ Thay `progress.startDate` → `progress.createdAt`
- ✅ Sửa `th:onerror` → `onerror`
- ✅ Cải thiện cấu trúc HTML cho rõ ràng hơn

### 2. Sửa `reading-history.html`
- ✅ Thay `progress.currentPage` → `progress.lastReadLocation`
- ✅ Thêm namespace `xmlns:sec` cho Spring Security
- ✅ Thay đổi label từ "Trang hiện tại" → "Vị trí đọc"
- ✅ Thay đổi icon từ `fa-file-alt` → `fa-bookmark` (phù hợp hơn)

## 📊 MAPPING DỮ LIỆU ĐÚNG

### Entity `ReadingProgress`
```java
@Entity
@Table(name = "reading_progress")
public class ReadingProgress {
    @Id
    private String progressId;
    
    @ManyToOne(fetch = FetchType.LAZY)  // ⚠️ LAZY - cần FETCH JOIN
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)  // ⚠️ LAZY - cần FETCH JOIN
    private Book book;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private BookAsset bookAsset;
    
    private String lastReadLocation;      // ✅ Có
    private Float progressPercentage;     // ✅ Có
    private Boolean isCompleted;          // ✅ Có
    private Boolean isFavorite;           // ✅ Có
    private AccessType accessType;        // ✅ Có
    private LocalDateTime lastReadAt;     // ✅ Có
    private LocalDateTime createdAt;      // ✅ Có
    
    // ❌ KHÔNG CÓ: currentPage, startDate, lastReadDate
}
```

### SQL Table `reading_progress`
```sql
CREATE TABLE reading_progress (
  progress_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  book_id VARCHAR(50) NOT NULL,
  book_asset_id VARCHAR(50),
  last_read_location VARCHAR(500),      -- ✅ Có
  progress_percentage FLOAT DEFAULT 0,  -- ✅ Có
  is_completed TINYINT(1) DEFAULT 0,   -- ✅ Có
  is_favorite TINYINT(1) DEFAULT 0,    -- ✅ Có
  access_type ENUM(...),               -- ✅ Có
  last_read_at DATETIME,               -- ✅ Có
  created_at DATETIME,                 -- ✅ Có
  
  -- ❌ KHÔNG CÓ: current_page, start_date, last_read_date
);
```

## 🔧 CONTROLLER ĐÃ ĐÚNG

### ReadingController
```java
// ✅ ĐÚNG: Truyền cả book và progress riêng biệt
model.addAttribute("book", book);
model.addAttribute("progress", progress);
```

### UserController - readingHistory()
```java
// ✅ ĐÚNG: Sử dụng FETCH JOIN để tránh LazyInitializationException
List<ReadingProgress> readingProgresses = 
    readingProgressService.getReadingProgressByUserWithBookDetails(currentUser);
```

### Repository
```java
// ✅ ĐÚNG: Query với FETCH JOIN
@Query("SELECT DISTINCT rp FROM ReadingProgress rp " +
       "LEFT JOIN FETCH rp.book b " +
       "LEFT JOIN FETCH b.authors " +
       "LEFT JOIN FETCH b.bookCategory " +
       "WHERE rp.user = :user")
List<ReadingProgress> findByUserWithBookDetails(@Param("user") User user);
```

## 📝 NGUYÊN TẮC LẬP TRÌNH

### ✅ ĐÚNG
1. **Sử dụng dữ liệu trực tiếp từ model** thay vì qua relationship LAZY
2. **Dùng FETCH JOIN** khi cần truy cập relationship trong view
3. **Đặt tên thuộc tính nhất quán** giữa Java (camelCase) và SQL (snake_case)
4. **Kiểm tra null** trước khi truy cập dữ liệu

### ❌ SAI
1. Truy cập relationship LAZY trong template (`progress.book.pages`)
2. Dùng thuộc tính không tồn tại (`currentPage`, `startDate`)
3. Không khai báo namespace cần thiết (`xmlns:sec`)

## 🧪 CÁCH KIỂM TRA

### Test 1: Kiểm tra reader.html
```
URL: /reading/book/{bookId}
```
- ✅ Hiển thị số trang từ `book.pages`
- ✅ Hiển thị ngày bắt đầu từ `progress.createdAt`
- ✅ Hiển thị vị trí đọc từ `progress.lastReadLocation`
- ✅ Không có LazyInitializationException

### Test 2: Kiểm tra reading-history.html
```
URL: /user/reading-history
```
- ✅ Hiển thị vị trí đọc thay vì số trang
- ✅ Hiển thị tên người dùng với `sec:authentication`
- ✅ Không có lỗi namespace

## 📚 TÀI LIỆU THAM KHẢO

- Entity: `src/main/java/stu/datn/ebook_store/entity/ReadingProgress.java`
- SQL: `DB/ebook_store.sql` (dòng 431-465)
- Controller: `src/main/java/stu/datn/ebook_store/controller/user/ReadingController.java`
- Repository: `src/main/java/stu/datn/ebook_store/repository/ReadingProgressRepository.java`

## ⚠️ LƯU Ý QUAN TRỌNG

1. **LAZY Fetch:** Tất cả relationship trong `ReadingProgress` đều là LAZY
   - Phải dùng FETCH JOIN hoặc truyền dữ liệu riêng vào model
   
2. **Đặt tên cột:** 
   - Java: `lastReadLocation`, `createdAt`, `lastReadAt`
   - SQL: `last_read_location`, `created_at`, `last_read_at`
   
3. **Kiểu dữ liệu:**
   - `lastReadLocation`: VARCHAR(500) - có thể chứa "page-50" hoặc EPUB CFI
   - `progressPercentage`: FLOAT - từ 0.0 đến 100.0
   
4. **Template Best Practices:**
   - Luôn check null: `${book.pages}` với `th:if`
   - Dùng Elvis operator: `${progress.progressPercentage ?: 0}`
   - Format datetime: `${#temporals.format(progress.createdAt, 'dd/MM/yyyy')}`

---

**Kết luận:** Tất cả các vấn đề về lấy dữ liệu không đúng đã được sửa. Template giờ đây truy cập đúng các thuộc tính tồn tại trong entity và SQL.

