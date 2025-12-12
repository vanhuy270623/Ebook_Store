# ✅ ĐÃ HOÀN THÀNH - Debug "Có lỗi xảy ra khi mở sách"

## 🔧 Các thay đổi đã thực hiện

### 1. Cải thiện Error Logging
✅ Thêm stack trace vào log để dễ debug
✅ Hiển thị message lỗi cụ thể cho user
✅ Thêm log từng bước trong openBook()

### 2. Error Handling cho Reading Progress
✅ Wrap việc tạo ReadingProgress trong try-catch riêng
✅ Nếu lỗi tạo progress, vẫn cho phép đọc sách
✅ Log chi tiết mọi bước tạo progress

### 3. Test Endpoint
✅ Thêm `/reading/test/{bookId}` để kiểm tra nhanh:
   - Book có tồn tại không
   - Assets có không
   - File có trên disk không
   - Đường dẫn đầy đủ

### 4. Documentation
✅ Tạo `DEBUG_READING_ERROR.md` - Hướng dẫn debug chi tiết
✅ Tạo `DEBUG_READING_DATA.sql` - Script kiểm tra database

---

## 🎯 Hướng dẫn sử dụng

### Bước 1: Kiểm tra bằng Test Endpoint

```
1. Chạy server
2. Truy cập: http://localhost:2706/reading/test/book_01
   (Thay book_01 bằng book ID của bạn)
3. Xem kết quả
```

**Kết quả mong đợi:**
```
=== TEST BOOK: book_01 ===

✅ Book found: Tên sách
   Access Type: FREE

📁 Total Assets: 1

   Asset ID: asset_01
   File Type: PDF
   File URL: kienthuc-hocthuat/sample.pdf
   File Size: 5242880 bytes

✅ Readable asset found: PDF
   URL: kienthuc-hocthuat/sample.pdf
   Full Path: F:/datn_uploads/book_asset/source/kienthuc-hocthuat/sample.pdf
   ✅ File exists on disk
   File size: 5242880 bytes
```

### Bước 2: Nếu có lỗi

**Xem log chi tiết:**
- Mở IntelliJ console
- Tìm dòng "Opening book with ID: xxx"
- Đọc các log sau đó để biết lỗi ở đâu

**Đọc hướng dẫn debug:**
- Mở file: `docs/DEBUG_READING_ERROR.md`
- Follow từng bước

### Bước 3: Fix thường gặp

#### ❌ "Book not found"
```sql
-- Kiểm tra book có tồn tại
SELECT * FROM books WHERE book_id = 'your_book_id';
```

#### ❌ "No readable asset found"  
```sql
-- Thêm asset
INSERT INTO bookassets (
    book_asset_id, book_id, file_type, file_url, file_size, created_at
) VALUES (
    UUID(), 'your_book_id', 'PDF', 
    'kienthuc-hocthuat/sample.pdf', 
    5242880, NOW()
);
```

#### ❌ File không tồn tại
```
1. Kiểm tra file_url trong DB là RELATIVE path
   Đúng: 'kienthuc-hocthuat/sample.pdf'
   SAI:  'F:/datn_uploads/...'

2. Copy file PDF/EPUB vào đúng thư mục:
   F:/datn_uploads/book_asset/source/{category}/{filename}
```

#### ❌ Error khi save progress
```sql
-- Kiểm tra bảng tồn tại
SHOW TABLES LIKE 'reading_progress';

-- Nếu chưa có, chạy:
SOURCE C:/Projects/Ebook_Store/docs/READING_DATABASE_UPDATE.sql;
```

---

## 📊 Checklist

```
□ Server chạy thành công
□ Test endpoint hiển thị ✅
□ Database có đầy đủ tables
□ bookassets có data với file_type = PDF/EPUB
□ file_url là relative path (không có F:/)
□ File tồn tại tại F:/datn_uploads/book_asset/source/{file_url}
□ reading_progress table tồn tại
□ User đã đăng nhập
```

---

## 🚀 Test Ngay

```powershell
# 1. Start server
cd C:\Projects\Ebook_Store
.\mvnw.cmd spring-boot:run

# 2. Mở browser test:
# http://localhost:2706/reading/test/book_01

# 3. Nếu OK, thử đọc:
# http://localhost:2706/reading/book/book_01
```

---

## 📝 Log Example

**Log thành công:**
```
INFO  - Opening book with ID: book_01
DEBUG - User email: user@example.com
DEBUG - User found: user_id_01
DEBUG - Book found: Sample Book Title
DEBUG - Found 1 assets for book book_01
INFO  - Readable asset found: PDF - kienthuc-hocthuat/sample.pdf
INFO  - Creating new reading progress for user user_id_01 and book book_01
INFO  - Reading progress created successfully: progress_uuid
```

**Log có lỗi:**
```
INFO  - Opening book with ID: book_01
...
ERROR - Error creating reading progress: constraint violation
WARN  - Continuing without progress tracking
```

Nếu thấy "Continuing without progress tracking" → Vẫn có thể đọc sách nhưng không lưu tiến độ.

---

## 💡 Tips Debug

1. **Luôn check test endpoint TRƯỚC**
2. **Đọc log từ trên xuống** để biết lỗi ở bước nào
3. **file_url phải là RELATIVE path** (không có F:/ hoặc C:/)
4. **File phải TỒN TẠI** trên disk
5. **Restart server** sau mỗi thay đổi code

---

## 📞 Nếu vẫn lỗi

1. Run test endpoint và chụp màn hình
2. Copy TOÀN BỘ log từ console
3. Check database:
   ```sql
   SELECT * FROM books WHERE book_id = 'your_id';
   SELECT * FROM bookassets WHERE book_id = 'your_id';
   ```
4. Kiểm tra file có tồn tại:
   - Mở Explorer → F:\datn_uploads\book_asset\source\
   - Tìm theo đường dẫn trong database

---

## ✅ Summary

**Đã thêm:**
- ✅ Detailed error logging với stack trace
- ✅ Test endpoint `/reading/test/{bookId}`
- ✅ Safe handling cho ReadingProgress errors
- ✅ Debug documentation đầy đủ
- ✅ SQL debug scripts

**Lợi ích:**
- 🔍 Dễ dàng tìm lỗi qua test endpoint
- 📝 Log chi tiết mọi bước
- 🛡️ App không crash khi lỗi progress
- 📚 Tài liệu debug đầy đủ

**Next steps:**
1. Chạy test endpoint
2. Xem kết quả
3. Fix theo hướng dẫn nếu có lỗi
4. Test lại

---

**Good luck! 🎉**

