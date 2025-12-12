# 🚀 QUICK START: Test chức năng đọc sách sau khi fix

## ✅ Checklist trước khi test

- [ ] Server đang chạy: `http://localhost:2706`
- [ ] Database có data (books, bookassets)
- [ ] Folder `F:/datn_uploads/book_asset/source/` có file PDF/EPUB
- [ ] User đã đăng nhập

---

## 🧪 Test ngay lập tức

### Bước 1: Kiểm tra file tồn tại
```
Truy cập: http://localhost:2706/reading/test/{bookId}

Thay {bookId} bằng ID sách thực tế trong database.

Ví dụ: http://localhost:2706/reading/test/BOOK001

Kết quả mong đợi:
✅ Book found: [Tên sách]
✅ Readable asset found: PDF hoặc EPUB
✅ File exists on disk
```

### Bước 2: Test PDF Loading System
```
Truy cập: http://localhost:2706/reading/test-pdf-load

Chạy các test:
1. Test 1: Direct File Access
   - Nhập path: khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf
   - Click "Test Direct Access"
   - Kết quả: Ít nhất 1 path phải OK

2. Test 2: PDF.js Loading
   - Click "Test PDF.js"
   - Kết quả: PDF loaded successfully + hiển thị trang đầu

3. Test 3: Authentication Check
   - Click "Test Auth"
   - Kết quả: Authenticated (nếu đã login)

4. Test 4: All Paths
   - Click "Test All Paths"
   - Kết quả: Hiển thị tất cả paths và status
```

### Bước 3: Test đọc sách thực tế
```
1. Đăng nhập vào hệ thống
2. Vào trang User Dashboard: http://localhost:2706/user/index
3. Chọn 1 cuốn sách bất kỳ
4. Click "Đọc sách"

Kết quả mong đợi:
✅ Không có lỗi "User not found"
✅ Không có lỗi "File not found" (nếu file tồn tại)
✅ PDF/EPUB hiển thị bình thường
✅ Có thể chuyển trang, zoom, bookmark
```

---

## 🔍 Nếu có lỗi

### Lỗi: "User not found"
```
✅ ĐÃ FIX! Nếu vẫn lỗi:

1. Check console log có dòng:
   "User found: {username} ({userId})"
   
2. Nếu không có, check AuthController:
   - Line 66: authentication = new UsernamePasswordAuthenticationToken(user, ...)
   - Đảm bảo principal là User object, không phải String

3. Clear session và login lại
```

### Lỗi: "File sách không tồn tại trên hệ thống"
```
1. Check file thực sự tồn tại:
   F:/datn_uploads/book_asset/source/{category}/{filename}
   
   Ví dụ:
   F:/datn_uploads/book_asset/source/khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf

2. Check database:
   SELECT book_id, file_url FROM bookassets WHERE file_type IN ('PDF', 'EPUB');
   
3. Đảm bảo file_url trong DB khớp với folder structure:
   ✅ ĐÚNG: khoahoc-vientuong/filename.pdf
   ❌ SAI: /khoahoc-vientuong/filename.pdf
   ❌ SAI: filename.pdf (thiếu category folder)
```

### Lỗi: "Không tải được PDF" (trên browser)
```
1. Mở Developer Console (F12)
2. Xem tab Network
3. Tìm request đến /uploads/source/...
4. Check status code:
   - 401: Chưa login → Login lại
   - 404: File không tồn tại → Check đường dẫn
   - 403: Không có quyền → Check SecurityConfig
   - 200: OK → Check PDF.js error trong Console
```

---

## 📊 Expected Console Logs (Success)

Khi mở sách thành công, console sẽ có:

```
INFO  ReadingController : Opening book with ID: BOOK001
DEBUG ReadingController : User found: testuser (USER001)
DEBUG ReadingController : Book found: Các Thế Giới Song Song
DEBUG ReadingController : Found 3 assets for book BOOK001
INFO  ReadingController : Readable asset found: PDF - khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf
INFO  ReadingController : File exists on disk: F:/datn_uploads/book_asset/source/khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf (size: 5242880 bytes)
INFO  ReadingController : Creating new reading progress for user USER001 and book BOOK001
INFO  ReadingController : Reading progress created successfully: PROG001
```

---

## 🎯 Success Criteria

Sau khi test, các điều sau phải thỏa mãn:

- ✅ Không có lỗi "User not found"
- ✅ Không có lỗi "File not found" (nếu file tồn tại)
- ✅ PDF/EPUB load và hiển thị bình thường
- ✅ Có thể chuyển trang (prev/next)
- ✅ Có thể zoom in/out
- ✅ Progress được save (check database: reading_progress table)
- ✅ Bookmark được save

---

## 🛠️ Nếu cần thêm data test

### SQL: Insert test book với asset
```sql
-- 1. Thêm book
INSERT INTO books (book_id, title, description, price, access_type, isbn, publisher_id, category_id, author_id, created_at)
VALUES ('BOOK_TEST_001', 'Test Book - Các Thế Giới Song Song', 'Sách test', 99000, 'FREE', '9999999999', 'PUB001', 'CAT001', 'AUTH001', NOW());

-- 2. Thêm book asset
INSERT INTO bookassets (book_asset_id, book_id, file_type, file_url, file_size, created_at)
VALUES (
    'ASSET_TEST_001', 
    'BOOK_TEST_001', 
    'PDF', 
    'khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf',
    5242880,
    NOW()
);

-- 3. Check
SELECT 
    b.book_id, 
    b.title, 
    ba.file_type, 
    ba.file_url 
FROM books b
JOIN bookassets ba ON b.book_id = ba.book_id
WHERE b.book_id = 'BOOK_TEST_001';
```

---

## 📞 Support

Nếu vẫn có vấn đề, check:
1. `logs/spring.log` - Application logs
2. Browser Console (F12) - Frontend errors
3. Network tab (F12) - Request/Response details
4. Database logs - Query errors

---

**Last updated:** 13/12/2024  
**Status:** ✅ READY TO TEST

