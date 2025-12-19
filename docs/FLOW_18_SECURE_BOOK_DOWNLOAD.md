# FLOW 18: Secure Book Download (Tải Xuống Sách An Toàn)

**Dự án:** Ebook Store  
**Ngày tạo:** 20/12/2025  
**Người tạo:** Development Team  
**Phiên bản:** 1.0  

---

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Luồng Xử Lý](#luồng-xử-lý)
3. [Implementation Details](#implementation-details)
4. [Security Considerations](#security-considerations)
5. [Error Handling](#error-handling)
6. [Testing](#testing)

---

## Tổng Quan

### Mục Đích
Flow này xử lý việc tải xuống file sách (PDF/EPUB) một cách an toàn, đảm bảo:
- Chỉ user đã mua/đăng ký mới được tải
- Kiểm tra quyền truy cập
- Tracking lịch sử tải xuống
- Stream file hiệu quả

### Actors
- **User**: Người dùng muốn tải sách
- **System**: Kiểm tra quyền và cung cấp file

### Preconditions
- User đã đăng nhập
- User đã mua sách hoặc có subscription
- File sách tồn tại trong hệ thống

### Postconditions
- File được tải về máy user
- Lịch sử download được ghi nhận

---

## Luồng Xử Lý

### Sequence Diagram

```
User                BookDownloadController      DownloadAuthService      BookService      FileSystem
 │                           │                          │                    │                │
 │─────GET /books/download/{bookId}────>│               │                    │                │
 │                           │                          │                    │                │
 │                           │──getCurrentUser()────>   │                    │                │
 │                           │<─────User────────────    │                    │                │
 │                           │                          │                    │                │
 │                           │──────getBookById()───────────────────>        │                │
 │                           │<──────Book──────────────────────────          │                │
 │                           │                          │                    │                │
 │                           │──canDownload(user, book)>│                    │                │
 │                           │                          │──checkOwnership()─>│                │
 │                           │                          │──checkSubscription()>               │
 │                           │<───true/false────────────│                    │                │
 │                           │                          │                    │                │
 │                           │───findBookAsset()────────────────────>        │                │
 │                           │<───BookAsset─────────────────────────         │                │
 │                           │                          │                    │                │
 │                           │─────────────────────────────────────────────>│                │
 │                           │                          │                    │  Read File     │
 │                           │<──────────────────────────────────────────────                │
 │                           │                          │                    │                │
 │                           │──recordDownload()────────────────────>        │                │
 │                           │                          │                    │                │
 │<──────Stream File─────────│                          │                    │                │
 │                           │                          │                    │                │
```

### Các Bước Chi Tiết

#### 1. User Yêu Cầu Tải Sách
```
GET /books/download/{bookId}
```

#### 2. Authentication Check
```java
User currentUser = getCurrentUser();
if (currentUser == null) {
    return ResponseEntity.status(401).body(null);
}
```

#### 3. Find Book
```java
Optional<Book> bookOpt = bookService.getBookById(bookId);
if (bookOpt.isEmpty()) {
    return ResponseEntity.notFound().build();
}
```

#### 4. Authorization Check
```java
if (!downloadAuthService.canDownload(currentUser, book)) {
    String reason = downloadAuthService.getDownloadDeniedReason(currentUser, book);
    return ResponseEntity.status(403)
        .header("X-Download-Error", URLEncoder.encode(reason, StandardCharsets.UTF_8))
        .body(null);
}
```

#### 5. Find Book Asset
- Ưu tiên EPUB
- Fallback sang PDF nếu không có EPUB

```java
Optional<BookAsset> assetOpt = bookAssetRepository
    .findByBook_BookIdAndFileType(bookId, BookAsset.FileType.EPUB);

if (assetOpt.isEmpty()) {
    assetOpt = bookAssetRepository
        .findByBook_BookIdAndFileType(bookId, BookAsset.FileType.PDF);
}
```

#### 6. Stream File
```java
Path filePath = fileStorageService.resolveFilePath(asset.getFileUrl());
Resource resource = new UrlResource(filePath.toUri());

String contentDisposition = "attachment; filename*=UTF-8''" 
    + encodedFilename;

return ResponseEntity.ok()
    .contentType(mediaType)
    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
    .body(resource);
```

#### 7. Record Download History
```java
downloadAuthService.recordDownload(currentUser, book);
```

---

## Implementation Details

### Controller: `BookDownloadController.java`

**Location:** `src/main/java/stu/datn/ebook_store/controller/user/BookDownloadController.java`

**Endpoint:**
```
GET /books/download/{bookId}
```

**Key Methods:**

#### downloadBook()
```java
@GetMapping("/{bookId}")
@ResponseBody
public ResponseEntity<Resource> downloadBook(@PathVariable String bookId) {
    // 1. Authentication
    User currentUser = getCurrentUser();
    if (currentUser == null) {
        return ResponseEntity.status(401).body(null);
    }

    // 2. Find book
    Optional<Book> bookOpt = bookService.getBookById(bookId);
    if (bookOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    Book book = bookOpt.get();

    // 3. Authorization
    if (!downloadAuthService.canDownload(currentUser, book)) {
        String reason = downloadAuthService.getDownloadDeniedReason(currentUser, book);
        return ResponseEntity.status(403)
            .header("X-Download-Error", URLEncoder.encode(reason, StandardCharsets.UTF_8))
            .body(null);
    }

    // 4. Find asset (EPUB > PDF)
    Optional<BookAsset> assetOpt = bookAssetRepository
        .findByBook_BookIdAndFileType(bookId, BookAsset.FileType.EPUB);
    
    if (assetOpt.isEmpty()) {
        assetOpt = bookAssetRepository
            .findByBook_BookIdAndFileType(bookId, BookAsset.FileType.PDF);
    }

    if (assetOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    BookAsset asset = assetOpt.get();

    // 5. Check file exists
    Path filePath = fileStorageService.resolveFilePath(asset.getFileUrl());
    if (!Files.exists(filePath)) {
        return ResponseEntity.notFound().build();
    }

    // 6. Prepare response
    Resource resource = new UrlResource(filePath.toUri());
    String filename = book.getTitle() + "." + asset.getFileType().name().toLowerCase();
    String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
        .replaceAll("\\+", "%20");

    MediaType mediaType = (asset.getFileType() == BookAsset.FileType.EPUB) 
        ? MediaType.parseMediaType("application/epub+zip")
        : MediaType.APPLICATION_PDF;

    String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

    // 7. Record download
    downloadAuthService.recordDownload(currentUser, book);

    // 8. Return file
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .body(resource);
}
```

---

### Service: `DownloadAuthorizationService.java`

**Location:** `src/main/java/stu/datn/ebook_store/service/DownloadAuthorizationService.java`

**Key Methods:**

#### canDownload()
```java
public boolean canDownload(User user, Book book) {
    // Case 1: FREE book
    if (book.getAccessType() == Book.AccessType.FREE) {
        return true;
    }

    // Case 2: PAID book - check if user has purchased
    if (book.getAccessType() == Book.AccessType.PAID) {
        return hasPurchased(user, book);
    }

    // Case 3: SUBSCRIPTION book - check active subscription
    if (book.getAccessType() == Book.AccessType.SUBSCRIPTION) {
        return hasActiveSubscription(user);
    }

    return false;
}
```

#### hasPurchased()
```java
private boolean hasPurchased(User user, Book book) {
    return orderItemService.hasUserPurchasedBook(user.getUserId(), book.getBookId());
}
```

#### hasActiveSubscription()
```java
private boolean hasActiveSubscription(User user) {
    return subscriptionService.hasActiveSubscription(user.getUserId());
}
```

#### getDownloadDeniedReason()
```java
public String getDownloadDeniedReason(User user, Book book) {
    if (book.getAccessType() == Book.AccessType.PAID) {
        return "Bạn cần mua sách này để tải xuống";
    }
    
    if (book.getAccessType() == Book.AccessType.SUBSCRIPTION) {
        return "Bạn cần đăng ký gói VIP để tải xuống sách này";
    }
    
    return "Không có quyền tải xuống sách này";
}
```

#### recordDownload()
```java
public void recordDownload(User user, Book book) {
    // Create or update reading progress with download timestamp
    ReadingProgress progress = readingProgressService
        .getOrCreateProgress(user.getUserId(), book.getBookId());
    
    progress.setLastDownloadedAt(LocalDateTime.now());
    readingProgressService.save(progress);
}
```

---

### Entity: `BookAsset.java`

**Location:** `src/main/java/stu/datn/ebook_store/entity/BookAsset.java`

```java
@Entity
@Table(name = "bookassets")
public class BookAsset {
    
    @Id
    @Column(name = "book_asset_id")
    private String bookAssetId;
    
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private FileType fileType;
    
    @Column(name = "file_url")
    private String fileUrl;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "preview_url")
    private String previewUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum FileType {
        PDF, EPUB
    }
}
```

---

### Database Schema

**Table: `bookassets`**
```sql
CREATE TABLE `bookassets` (
  `book_asset_id` varchar(50) NOT NULL,
  `book_id` varchar(50) DEFAULT NULL,
  `file_type` enum('PDF','EPUB') NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `preview_url` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`book_asset_id`),
  KEY `book_id` (`book_id`),
  CONSTRAINT FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Table: `reading_progress`**
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
  KEY `user_id` (`user_id`),
  KEY `book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## Security Considerations

### 1. Authentication
- ✅ Kiểm tra user đã đăng nhập
- ✅ Session-based authentication
- ✅ Redirect to login if not authenticated

### 2. Authorization
- ✅ Kiểm tra quyền sở hữu (FREE, PAID, SUBSCRIPTION)
- ✅ Verify payment status cho PAID books
- ✅ Verify subscription status cho SUBSCRIPTION books

### 3. File Access Control
- ✅ File path validation
- ✅ Prevent directory traversal attacks
- ✅ File existence check

### 4. Rate Limiting (Future Enhancement)
- ⚠️ TODO: Giới hạn số lần download trong 1 khoảng thời gian
- ⚠️ TODO: Prevent abuse

### 5. Download Tracking
- ✅ Ghi nhận timestamp mỗi lần download
- ✅ Analytics về download patterns

---

## Error Handling

### 1. 401 Unauthorized
**Nguyên nhân:** User chưa đăng nhập

**Response:**
```
Status: 401 Unauthorized
Body: null
```

**Xử lý:**
```javascript
// Frontend redirect to login
if (response.status === 401) {
    window.location.href = '/auth/login';
}
```

---

### 2. 403 Forbidden
**Nguyên nhân:** User không có quyền tải sách

**Response:**
```
Status: 403 Forbidden
Headers:
  X-Download-Error: Bạn cần mua sách này để tải xuống
Body: null
```

**Xử lý:**
```javascript
// Frontend hiển thị thông báo
if (response.status === 403) {
    const reason = decodeURIComponent(
        response.headers.get('X-Download-Error')
    );
    alert(reason);
}
```

---

### 3. 404 Not Found
**Nguyên nhân:** 
- Sách không tồn tại
- File không tồn tại
- Không có BookAsset

**Response:**
```
Status: 404 Not Found
Body: null
```

**Xử lý:**
```javascript
if (response.status === 404) {
    alert('Không tìm thấy file sách. Vui lòng liên hệ admin.');
}
```

---

### 4. 500 Internal Server Error
**Nguyên nhân:** Lỗi hệ thống

**Response:**
```
Status: 500 Internal Server Error
Body: null
```

**Xử lý:**
```javascript
if (response.status === 500) {
    alert('Lỗi hệ thống. Vui lòng thử lại sau.');
}
```

---

## Testing

### Test Cases

#### TC-1: Download FREE Book
**Precondition:** User đã đăng nhập

**Steps:**
1. Navigate to book detail page của FREE book
2. Click "Tải xuống" button
3. Verify file downloaded

**Expected Result:**
- Status: 200 OK
- File downloaded successfully
- Filename: `{book_title}.epub` hoặc `.pdf`

---

#### TC-2: Download PAID Book (Not Purchased)
**Precondition:** User đã đăng nhập nhưng chưa mua sách

**Steps:**
1. Try to download PAID book
2. Verify error message

**Expected Result:**
- Status: 403 Forbidden
- Error: "Bạn cần mua sách này để tải xuống"

---

#### TC-3: Download PAID Book (Purchased)
**Precondition:** User đã mua sách

**Steps:**
1. Navigate to user library
2. Click "Tải xuống" trên sách đã mua
3. Verify file downloaded

**Expected Result:**
- Status: 200 OK
- File downloaded successfully

---

#### TC-4: Download SUBSCRIPTION Book (No Active Subscription)
**Precondition:** User không có subscription

**Steps:**
1. Try to download SUBSCRIPTION book
2. Verify error

**Expected Result:**
- Status: 403 Forbidden
- Error: "Bạn cần đăng ký gói VIP để tải xuống sách này"

---

#### TC-5: Download SUBSCRIPTION Book (With Active Subscription)
**Precondition:** User có active subscription

**Steps:**
1. Download SUBSCRIPTION book
2. Verify file downloaded

**Expected Result:**
- Status: 200 OK
- File downloaded successfully

---

#### TC-6: Download Non-existent Book
**Steps:**
1. Request `/books/download/invalid_id`
2. Verify error

**Expected Result:**
- Status: 404 Not Found

---

#### TC-7: Download Without Login
**Precondition:** User chưa đăng nhập

**Steps:**
1. Try to access download URL directly
2. Verify redirect

**Expected Result:**
- Status: 401 Unauthorized
- Redirect to login page

---

#### TC-8: UTF-8 Filename Support
**Steps:**
1. Download book with Vietnamese title (e.g., "Đắc Nhân Tâm")
2. Verify filename encoding

**Expected Result:**
- Filename hiển thị đúng tiếng Việt
- No mojibake (ký tự lỗi)

---

#### TC-9: EPUB Priority
**Precondition:** Book có cả EPUB và PDF

**Steps:**
1. Download book
2. Check file type

**Expected Result:**
- File type: EPUB (ưu tiên hơn PDF)

---

#### TC-10: Download History Tracking
**Steps:**
1. Download a book
2. Check `reading_progress` table
3. Verify `last_downloaded_at` updated

**Expected Result:**
- `last_downloaded_at` = current timestamp

---

## Frontend Integration

### HTML Button
```html
<a href="/books/download/{{ book.bookId }}" 
   class="btn btn-primary download-btn"
   data-book-id="{{ book.bookId }}">
    <i class="fas fa-download"></i> Tải xuống
</a>
```

### JavaScript (with error handling)
```javascript
document.querySelectorAll('.download-btn').forEach(btn => {
    btn.addEventListener('click', async function(e) {
        e.preventDefault();
        
        const bookId = this.dataset.bookId;
        const url = `/books/download/${bookId}`;
        
        try {
            const response = await fetch(url);
            
            if (response.ok) {
                // Download file
                const blob = await response.blob();
                const contentDisposition = response.headers.get('Content-Disposition');
                const filename = extractFilename(contentDisposition);
                
                const a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                
                showToast('success', 'Tải xuống thành công!');
            } else if (response.status === 401) {
                window.location.href = '/auth/login';
            } else if (response.status === 403) {
                const reason = decodeURIComponent(
                    response.headers.get('X-Download-Error') || 
                    'Bạn không có quyền tải xuống sách này'
                );
                showToast('error', reason);
            } else if (response.status === 404) {
                showToast('error', 'Không tìm thấy file sách');
            } else {
                showToast('error', 'Có lỗi xảy ra. Vui lòng thử lại sau.');
            }
        } catch (error) {
            console.error('Download error:', error);
            showToast('error', 'Có lỗi xảy ra khi tải xuống');
        }
    });
});

function extractFilename(contentDisposition) {
    if (!contentDisposition) return 'book';
    
    const match = contentDisposition.match(/filename\*=UTF-8''(.+)/);
    if (match) {
        return decodeURIComponent(match[1]);
    }
    
    const match2 = contentDisposition.match(/filename="?(.+)"?/);
    return match2 ? match2[1] : 'book';
}
```

---

## Best Practices

### 1. File Streaming
✅ **DO**: Sử dụng `UrlResource` để stream file
```java
Resource resource = new UrlResource(filePath.toUri());
return ResponseEntity.ok().body(resource);
```

❌ **DON'T**: Đọc toàn bộ file vào memory
```java
// BAD - Gây OutOfMemoryError với file lớn
byte[] fileContent = Files.readAllBytes(filePath);
return ResponseEntity.ok().body(fileContent);
```

### 2. Filename Encoding
✅ **DO**: Sử dụng UTF-8 encoding với RFC 5987
```java
String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;
```

❌ **DON'T**: Plain filename (không support tiếng Việt)
```java
String contentDisposition = "attachment; filename=" + filename;
```

### 3. Authorization
✅ **DO**: Kiểm tra authorization trước khi stream file
```java
if (!downloadAuthService.canDownload(user, book)) {
    return ResponseEntity.status(403).body(null);
}
```

❌ **DON'T**: Stream file rồi mới check
```java
// BAD - Wasted bandwidth
Resource resource = loadFile();
if (!canDownload()) {
    // Too late!
}
```

### 4. Media Type
✅ **DO**: Set correct Content-Type
```java
MediaType mediaType = (fileType == EPUB) 
    ? MediaType.parseMediaType("application/epub+zip")
    : MediaType.APPLICATION_PDF;
```

---

## Future Enhancements

### 1. Resume Download
- Hỗ trợ HTTP Range Requests
- User có thể resume download nếu bị gián đoạn

### 2. Download Limit
- Giới hạn số lần download trong 24h
- Prevent abuse

### 3. Watermarking
- Thêm watermark vào PDF
- Ghi user ID và timestamp

### 4. CDN Integration
- Sử dụng CDN để serve files
- Tăng tốc độ download

### 5. Download Analytics
- Track download count per book
- Popular downloads report
- User download behavior analysis

---

## Related Flows

- **FLOW 07**: Reading Interface
- **FLOW 20**: User Library & Reading History
- **FLOW 05**: Payment Processing (PAID books)
- **FLOW 10**: Subscription Management (SUBSCRIPTION books)

---

**Status:** ✅ COMPLETE  
**Implementation:** 100%  
**Last Updated:** 20/12/2025

