# 🚀 QUICK START - CHỨC NĂNG ĐỌC SÁCH

## ⚡ 5 Phút Setup

### Bước 1: Kiểm tra Database (30 giây)

```sql
-- Chạy script này trong MySQL
USE ebook_store;

-- Check tables tồn tại
SHOW TABLES LIKE '%reading%';
SHOW TABLES LIKE '%bookassets%';

-- Nếu chưa có, import từ file
SOURCE C:/Projects/Ebook_Store/docs/READING_DATABASE_UPDATE.sql;
```

### Bước 2: Kiểm tra File Structure (1 phút)

```
F:/datn_uploads/book_asset/
├── source/                  ← QUAN TRỌNG!
│   ├── khoahoc-vientuong/
│   ├── kienthuc-hocthuat/
│   ├── kinhte-quanly/
│   ├── tamly-kynangsong/
│   └── tieuthuyet-vanhoc/
└── image/
    └── covers/
```

**Copy sample files:**
- Copy vài file PDF/EPUB vào thư mục `source/category/`

### Bước 3: Thêm Book Asset vào DB (2 phút)

```sql
-- Sample: Thêm asset cho sách
INSERT INTO bookassets (
    book_asset_id, 
    book_id, 
    file_type, 
    file_url, 
    file_size,
    created_at
) VALUES (
    UUID(),
    'YOUR_BOOK_ID',  -- Lấy từ bảng books
    'PDF',           -- hoặc 'EPUB'
    'kienthuc-hocthuat/Tu Duy Phan Bien - Zoe McKey.pdf',  -- Relative path
    5242880,         -- File size in bytes
    NOW()
);
```

### Bước 4: Khởi động Server (30 giây)

```powershell
cd C:\Projects\Ebook_Store
.\mvnw.cmd spring-boot:run
```

Hoặc chạy từ IntelliJ IDEA: **Run → Run 'EbookStoreApplication'**

### Bước 5: Test! (1 phút)

1. Mở browser: `http://localhost:2706`
2. Đăng nhập vào hệ thống
3. Vào chi tiết một cuốn sách có asset
4. Click **"Đọc sách"**
5. ✅ Xong!

---

## 🎯 Test Checklist Nhanh

```
✅ PDF viewer mở được?
✅ EPUB viewer mở được?
✅ Navigation hoạt động (Previous/Next)?
✅ Zoom hoạt động?
✅ Dark mode hoạt động?
✅ Progress được lưu? (Check DB sau 30s)
```

---

## 🔥 Troubleshooting 1 Phút

### ❌ PDF không load?

**Check Console:**
```
F12 → Console → Tìm error "404 Not Found"
```

**Fix:**
1. Check file tồn tại: `F:/datn_uploads/book_asset/source/{category}/{filename}.pdf`
2. Check `file_url` trong DB: Phải là relative path, VÍ DỤ: `kienthuc-hocthuat/book.pdf`
3. Restart server

### ❌ 404 Not Found?

**Check WebMvcConfig:**
```java
// File: config/WebMvcConfig.java
registry.addResourceHandler("/uploads/source/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/source/");
```

### ❌ Progress không lưu?

**Check Console Network:**
```
F12 → Network → XHR → Tìm POST /reading/api/progress
Status phải là 200 OK
```

**Check Database:**
```sql
SELECT * FROM reading_progress ORDER BY last_read_at DESC LIMIT 5;
```

---

## 📝 Sample Data Script

```sql
-- COPY & PASTE ĐỂ TẠO DATA TEST

-- 1. Tạo sample book (nếu chưa có)
INSERT INTO books (book_id, title, author_id, category_id, access_type, price, created_at)
VALUES ('book_test_001', 'Sách Test PDF', 'author_001', 'cat_001', 'FREE', 0, NOW());

-- 2. Thêm PDF asset
INSERT INTO bookassets (book_asset_id, book_id, file_type, file_url, file_size, created_at)
VALUES (
    'asset_pdf_001',
    'book_test_001',
    'PDF',
    'kienthuc-hocthuat/Tu Duy Phan Bien - Zoe McKey.pdf',
    5242880,
    NOW()
);

-- 3. Verify
SELECT b.title, ba.file_type, ba.file_url 
FROM books b 
JOIN bookassets ba ON b.book_id = ba.book_id 
WHERE b.book_id = 'book_test_001';
```

---

## 🎨 URLs Cần Nhớ

```
Home:           http://localhost:2706
Book Detail:    http://localhost:2706/books/view/{bookId}
Read Book:      http://localhost:2706/reading/book/{bookId}
PDF Direct:     http://localhost:2706/reading/pdf/{bookId}
EPUB Direct:    http://localhost:2706/reading/epub/{bookId}
```

---

## 🔧 Development Tips

### Hot Reload CSS

```
Ctrl + F5 trong browser để reload CSS mới
```

### Debug Mode

```properties
# application.properties
logging.level.stu.datn.ebook_store=DEBUG
```

### Check Logs

```
Mở terminal trong IntelliJ → Xem log real-time
Hoặc check file: logs/spring-boot-logger.log
```

---

## 📚 Files Cần Biết

```
Controller:     src/main/java/.../controller/user/ReadingController.java
PDF Template:   src/main/resources/templates/user/reading/pdf-viewer.html
EPUB Template:  src/main/resources/templates/user/reading/epub-viewer.html
CSS:           src/main/resources/static/user_template/css/reading.css
Config:        src/main/java/.../config/WebMvcConfig.java
```

---

## 🎯 Common Tasks

### Thêm sách mới có file đọc:

1. Upload file PDF/EPUB vào `F:/datn_uploads/book_asset/source/category/`
2. Thêm record vào `bookassets` table
3. Done!

### Update progress tracking:

```sql
-- Xem progress của user
SELECT * FROM reading_progress WHERE user_id = 'user_xxx';

-- Reset progress
UPDATE reading_progress SET progress_percentage = 0, is_completed = FALSE WHERE progress_id = 'xxx';
```

### Customize UI:

1. Sửa `reading.css` cho style
2. Sửa `.html` template cho layout
3. Reload page (Ctrl + F5)

---

## ✅ Success Checklist

```
[✓] Database tables created
[✓] Book assets uploaded
[✓] Sample data inserted
[✓] Server running (port 2706)
[✓] Can open PDF viewer
[✓] Can open EPUB viewer
[✓] Progress auto-saves
[✓] Bookmark works
[✓] Dark mode works
[✓] Responsive on mobile
```

---

## 🎓 Next Steps

1. ✅ **Hoàn thành:** Đọc `READING_FEATURE_COMPLETE.md`
2. 🧪 **Testing:** Đọc `READING_TEST_GUIDE.md`
3. 🏗️ **Architecture:** Đọc `READING_ARCHITECTURE.md`
4. 💡 **Customize:** Modify CSS và templates theo ý muốn

---

## 🆘 Need Help?

**Check logs:**
```powershell
# Windows PowerShell
Get-Content logs\spring-boot-logger.log -Tail 50 -Wait
```

**Common errors:**
- `FileNotFoundException` → Check file path
- `NullPointerException` → Check database data
- `403 Forbidden` → Check authentication
- `404 Not Found` → Check ResourceHandler config

---

**🎉 Chúc mừng! Bạn đã setup xong chức năng đọc sách!**

*Tài liệu này được tạo để setup nhanh trong 5 phút*

