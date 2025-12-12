# 🔧 HƯỚNG DẪN DEBUG LỖI "Có lỗi xảy ra khi mở sách"

## 🎯 Các bước kiểm tra

### Bước 1: Test Endpoint (QUAN TRỌNG!)

1. **Chạy server**
2. **Truy cập URL test:**
   ```
   http://localhost:2706/reading/test/{bookId}
   ```
   
   Thay `{bookId}` bằng ID sách bạn muốn test, ví dụ:
   ```
   http://localhost:2706/reading/test/book_01
   ```

3. **Xem kết quả hiển thị:**
   - ✅ Book found → OK
   - ✅ Readable asset found → OK  
   - ✅ File exists on disk → OK
   - ❌ Nếu có dấu X → Đọc tiếp để fix

---

### Bước 2: Kiểm tra Database

```sql
-- Chạy trong MySQL
USE ebook_store;

-- 1. Kiểm tra sách có tồn tại không
SELECT book_id, title, access_type 
FROM books 
WHERE book_id = 'book_01';  -- Thay 'book_01' bằng book ID của bạn

-- 2. Kiểm tra sách có assets không
SELECT 
    ba.book_asset_id,
    ba.file_type,
    ba.file_url,
    ba.file_size
FROM bookassets ba
WHERE ba.book_id = 'book_01';  -- Thay 'book_01' bằng book ID của bạn

-- 3. Nếu không có asset, thêm vào:
INSERT INTO bookassets (
    book_asset_id,
    book_id,
    file_type,
    file_url,
    file_size,
    created_at
) VALUES (
    UUID(),                              -- Auto generate ID
    'book_01',                           -- THAY BẰNG BOOK_ID CỦA BẠN
    'PDF',                               -- Hoặc 'EPUB'
    'kienthuc-hocthuat/sample.pdf',     -- ĐƯỜNG DẪN RELATIVE!
    5242880,                             -- File size in bytes
    NOW()
);
```

---

### Bước 3: Kiểm tra File Trên Disk

**Cấu trúc thư mục phải đúng:**
```
F:/datn_uploads/book_asset/
└── source/                           ← QUAN TRỌNG!
    ├── khoahoc-vientuong/
    ├── kienthuc-hocthuat/
    │   └── sample.pdf                ← File ở đây
    ├── kinhte-quanly/
    ├── tamly-kynangsong/
    └── tieuthuyet-vanhoc/
```

**Kiểm tra:**
1. Mở Windows Explorer
2. Đi đến: `F:\datn_uploads\book_asset\source\`
3. Tìm file theo đường dẫn trong DB

**Ví dụ:**
- DB có: `file_url = 'kienthuc-hocthuat/sample.pdf'`
- File phải ở: `F:\datn_uploads\book_asset\source\kienthuc-hocthuat\sample.pdf`

---

### Bước 4: Kiểm tra Logs

**Mở log file hoặc console trong IntelliJ:**

```
Tìm các dòng:
- "Opening book with ID: xxx"
- "Book found: xxx"
- "Found X assets for book xxx"
- "Readable asset found: PDF - path/to/file"
- ERROR logs (nếu có)
```

**Các lỗi thường gặp:**

#### ❌ "Book not found"
```
✅ Fix: Kiểm tra book_id trong database
SELECT * FROM books WHERE book_id = 'your_book_id';
```

#### ❌ "No readable asset found"
```
✅ Fix: Thêm asset vào database (xem Bước 2)
```

#### ❌ "User not found"
```
✅ Fix: Đăng nhập lại
```

#### ❌ Error khi save reading progress
```
✅ Fix: Kiểm tra bảng reading_progress tồn tại
SHOW TABLES LIKE 'reading_progress';

✅ Nếu chưa có, import SQL:
SOURCE C:/Projects/Ebook_Store/docs/READING_DATABASE_UPDATE.sql;
```

---

### Bước 5: Test Từng Phần

#### Test 1: Kiểm tra Book Service
```java
// Trong một test controller hoặc @PostConstruct
Book book = bookService.getBookById("book_01").orElse(null);
if (book != null) {
    log.info("✅ Book service OK");
} else {
    log.error("❌ Book service failed");
}
```

#### Test 2: Kiểm tra BookAsset Service
```java
List<BookAsset> assets = bookAssetService.getAssetsByBookId("book_01");
log.info("Found {} assets", assets.size());
```

#### Test 3: Kiểm tra File Path
```java
String fileUrl = "kienthuc-hocthuat/sample.pdf";
String fullPath = "F:/datn_uploads/book_asset/source/" + fileUrl;
java.io.File file = new java.io.File(fullPath);
log.info("File exists: {}", file.exists());
```

---

### Bước 6: Common Issues & Solutions

#### 🔥 Issue 1: File path sai

**Triệu chứng:** 404 Not Found khi load PDF/EPUB

**Nguyên nhân:** 
- `file_url` trong DB là đường dẫn ABSOLUTE thay vì RELATIVE
- Ví dụ SAI: `F:/datn_uploads/book_asset/source/file.pdf`
- Ví dụ ĐÚNG: `kienthuc-hocthuat/file.pdf`

**Fix:**
```sql
-- Update sai thành đúng
UPDATE bookassets 
SET file_url = 'kienthuc-hocthuat/sample.pdf'
WHERE book_asset_id = 'asset_xxx';
```

#### 🔥 Issue 2: Bảng reading_progress chưa có

**Triệu chứng:** Error khi tạo reading progress

**Fix:**
```sql
-- Chạy script tạo bảng
SOURCE C:/Projects/Ebook_Store/docs/READING_DATABASE_UPDATE.sql;
```

#### 🔥 Issue 3: Foreign key constraint error

**Triệu chứng:** Cannot add or update child row

**Fix:**
```sql
-- Kiểm tra foreign keys
SELECT * FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'reading_progress';

-- Disable foreign key check tạm thời (NOT RECOMMENDED for production)
SET FOREIGN_KEY_CHECKS = 0;
-- ... insert data ...
SET FOREIGN_KEY_CHECKS = 1;
```

#### 🔥 Issue 4: Lazy loading exception

**Triệu chứng:** LazyInitializationException

**Fix:** Đã xử lý trong code bằng cách bắt exception riêng cho ReadingProgress

---

### Bước 7: Verification Checklist

```
□ Server đang chạy (port 2706)
□ Database ebook_store tồn tại
□ Bảng books có dữ liệu
□ Bảng bookassets có dữ liệu với file_type = 'PDF' hoặc 'EPUB'
□ file_url trong bookassets là RELATIVE path
□ File vật lý tồn tại tại F:/datn_uploads/book_asset/source/{file_url}
□ Bảng reading_progress tồn tại
□ User đã đăng nhập
□ Test endpoint /reading/test/{bookId} hiển thị ✅
```

---

## 🚀 Quick Test Script

```bash
# 1. Test database
mysql -u root -p ebook_store < C:/Projects/Ebook_Store/docs/DEBUG_READING_DATA.sql

# 2. Restart server
cd C:/Projects/Ebook_Store
./mvnw.cmd spring-boot:run

# 3. Test endpoint
# Mở browser:
http://localhost:2706/reading/test/book_01

# 4. Nếu OK, thử đọc sách:
http://localhost:2706/reading/book/book_01
```

---

## 📞 Nếu Vẫn Lỗi

1. **Copy TOÀN BỘ log lỗi** từ console
2. **Chụp màn hình** test endpoint
3. **Export database** (books, bookassets, reading_progress)
4. Gửi cho developer

---

## 💡 Tips

- Luôn check test endpoint TRƯỚC khi đọc sách
- Đảm bảo file_url là RELATIVE path
- File phải tồn tại trên disk
- Database phải có đầy đủ bảng
- Log level = DEBUG để xem chi tiết:
  ```properties
  # application.properties
  logging.level.stu.datn.ebook_store=DEBUG
  ```

---

**Good luck debugging! 🐛→✅**

