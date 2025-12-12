# 🧪 HƯỚNG DẪN TEST CHỨC NĂNG ĐỌC SÁCH

## 📋 Checklist Test

### 1. Chuẩn bị dữ liệu test

#### 1.1. Kiểm tra database
```sql
-- Kiểm tra có sách trong database
SELECT * FROM books LIMIT 5;

-- Kiểm tra có file assets
SELECT * FROM bookassets WHERE file_type IN ('PDF', 'EPUB');

-- Kiểm tra reading progress
SELECT * FROM reading_progress;
```

#### 1.2. Kiểm tra file uploads
```
Truy cập: F:/datn_uploads/book_asset/source/

Cấu trúc:
├── khoahoc-vientuong/
│   ├── *.pdf
│   └── *.epub
├── kienthuc-hocthuat/
├── kinhte-quanly/
├── tamly-kynangsong/
└── tieuthuyet-vanhoc/
```

#### 1.3. Sample BookAsset data
```sql
-- Thêm sample asset nếu chưa có
INSERT INTO bookassets (book_asset_id, book_id, file_type, file_url, file_size, created_at) 
VALUES (
  'asset_test_001', 
  'book_id_here',
  'PDF',
  'kienthuc-hocthuat/Tu Duy Phan Bien - Zoe McKey.pdf',
  1024000,
  NOW()
);
```

---

## 🚀 Test Cases

### Test Case 1: Mở PDF Viewer

**Steps:**
1. Đăng nhập vào hệ thống
2. Truy cập trang chi tiết sách có file PDF
3. Click nút "Đọc sách"
4. Chọn PDF Reader (hoặc tự động mở)

**Expected Result:**
- ✅ PDF viewer hiển thị
- ✅ File PDF load thành công
- ✅ Hiển thị trang đầu tiên
- ✅ Controls hoạt động (Previous/Next)
- ✅ Zoom In/Out hoạt động
- ✅ Progress bar hiển thị

**URL Test:**
```
http://localhost:2706/reading/book/{bookId}
http://localhost:2706/reading/pdf/{bookId}
```

---

### Test Case 2: Mở EPUB Viewer

**Steps:**
1. Đăng nhập vào hệ thống
2. Truy cập trang chi tiết sách có file EPUB
3. Click nút "Đọc sách"
4. Chọn EPUB Reader

**Expected Result:**
- ✅ EPUB viewer hiển thị
- ✅ File EPUB load thành công
- ✅ Table of Contents hiển thị
- ✅ Navigation hoạt động
- ✅ Settings panel hoạt động
- ✅ Font/theme customization hoạt động

**URL Test:**
```
http://localhost:2706/reading/epub/{bookId}
```

---

### Test Case 3: Save Reading Progress

**Steps:**
1. Mở một cuốn sách
2. Đọc đến trang 5 (PDF) hoặc chapter 2 (EPUB)
3. Đợi 30 giây (auto-save) hoặc chuyển trang
4. Thoát ra và mở lại sách

**Expected Result:**
- ✅ Progress được lưu vào database
- ✅ Khi mở lại, sách mở tại vị trí đã lưu
- ✅ Progress percentage chính xác

**Check Database:**
```sql
SELECT * FROM reading_progress 
WHERE user_id = 'your_user_id' 
AND book_id = 'your_book_id';
```

---

### Test Case 4: Bookmark

**Steps:**
1. Mở sách và đọc đến trang bất kỳ
2. Click nút "Bookmark"
3. Nhập ghi chú (optional)
4. Kiểm tra LocalStorage

**Expected Result:**
- ✅ Bookmark được lưu
- ✅ Alert xác nhận "Đã lưu bookmark"
- ✅ Bookmark xuất hiện trong LocalStorage

**Check LocalStorage:**
```javascript
// Mở Console (F12)
localStorage.getItem('bookmarks_' + bookId)
```

---

### Test Case 5: Dark Mode

**Steps:**
1. Mở sách
2. Click nút "Dark Mode"
3. Kiểm tra màu sắc thay đổi

**Expected Result:**
- ✅ Background chuyển sang màu tối
- ✅ Text màu sáng
- ✅ Nút đổi thành "Light Mode"
- ✅ Preference được lưu

---

### Test Case 6: Keyboard Shortcuts

**PDF Viewer:**
- `←`: Previous page ✅
- `→`: Next page ✅
- `+`: Zoom in ✅
- `-`: Zoom out ✅
- `Home`: First page ✅
- `End`: Last page ✅

**EPUB Viewer:**
- `←`: Previous chapter ✅
- `→`: Next chapter ✅

---

### Test Case 7: Responsive Design

**Steps:**
1. Mở sách trên desktop
2. Resize browser window
3. Test trên mobile device (Chrome DevTools)

**Expected Result:**
- ✅ Layout responsive
- ✅ Controls vừa màn hình
- ✅ Touch swipe hoạt động (mobile)
- ✅ Sidebar auto-hide (mobile)

---

### Test Case 8: Complete Book

**Steps:**
1. Mở sách và đọc đến trang cuối
2. Chuyển đến trang cuối (> 99%)
3. Check database

**Expected Result:**
- ✅ `is_completed` = TRUE
- ✅ `progress_percentage` ≈ 100

**Query:**
```sql
SELECT is_completed, progress_percentage 
FROM reading_progress 
WHERE book_id = 'your_book_id';
```

---

## 🔍 Debug Checklist

### Nếu PDF không load:

1. **Check Console Errors:**
   ```
   F12 → Console
   Tìm error: "Failed to load PDF", "404 Not Found", etc.
   ```

2. **Check File Path:**
   ```
   URL: /uploads/source/category/filename.pdf
   Physical: F:/datn_uploads/book_asset/source/category/filename.pdf
   ```

3. **Check ResourceHandler:**
   ```java
   // Trong WebMvcConfig.java
   registry.addResourceHandler("/uploads/source/**")
           .addResourceLocations("file:F:/datn_uploads/book_asset/source/");
   ```

4. **Check BookAsset.fileUrl:**
   ```sql
   SELECT file_url FROM bookassets WHERE book_asset_id = 'xxx';
   -- Should be: 'category/filename.pdf'
   -- NOT full path!
   ```

### Nếu EPUB không load:

1. **Check EPUB file format:**
   - File phải là .epub hợp lệ
   - Không corrupt
   - Đúng EPUB 2.0 hoặc 3.0 standard

2. **Check ePub.js loaded:**
   ```javascript
   // Console
   typeof ePub
   // Should return: "function"
   ```

3. **Check CORS:**
   - EPUB files phải được serve từ cùng domain
   - Hoặc có CORS headers

### Nếu Progress không save:

1. **Check Authentication:**
   ```java
   authentication.getName() // Should return user email
   ```

2. **Check CSRF Token:**
   ```javascript
   // Thymeleaf tự động thêm
   <meta name="_csrf" th:content="${_csrf.token}"/>
   ```

3. **Check API Response:**
   ```javascript
   // Console → Network → XHR
   // Tìm POST /reading/api/progress/{bookId}
   // Check status: 200 OK
   ```

4. **Check Database:**
   ```sql
   SELECT * FROM reading_progress ORDER BY last_read_at DESC LIMIT 5;
   ```

---

## 📊 Performance Test

### 1. Load Time Test

**Mục tiêu:**
- PDF load < 3 seconds (file < 10MB)
- EPUB load < 2 seconds

**Test:**
```javascript
// Console
console.time('Load Time');
// Load book
console.timeEnd('Load Time');
```

### 2. Memory Usage

**Check:**
- Chrome Task Manager (Shift + Esc)
- Memory không vượt quá 500MB

### 3. Render Performance

**Check:**
- Page switch < 500ms
- Smooth scrolling
- No lag khi zoom

---

## 🎯 Acceptance Criteria

**Đạt yêu cầu khi:**

- ✅ Mở được PDF và EPUB
- ✅ Navigation hoạt động mượt mà
- ✅ Progress được lưu tự động
- ✅ Bookmark hoạt động
- ✅ Dark mode hoạt động
- ✅ Responsive trên mobile
- ✅ Không có console errors
- ✅ Load time < 3s
- ✅ UI/UX đẹp và dễ dùng

---

## 📝 Test Report Template

```markdown
## Test Report - Reading Feature

**Tester:** [Tên người test]
**Date:** [Ngày test]
**Environment:** [Localhost / Production]

### Test Results

| Test Case | Status | Notes |
|-----------|--------|-------|
| TC1: PDF Viewer | ✅ PASS | - |
| TC2: EPUB Viewer | ✅ PASS | - |
| TC3: Save Progress | ✅ PASS | - |
| TC4: Bookmark | ✅ PASS | - |
| TC5: Dark Mode | ✅ PASS | - |
| TC6: Keyboard Shortcuts | ✅ PASS | - |
| TC7: Responsive | ✅ PASS | - |
| TC8: Complete Book | ✅ PASS | - |

### Issues Found

1. [Issue description]
   - Severity: [High/Medium/Low]
   - Steps to reproduce: [...]
   - Expected: [...]
   - Actual: [...]

### Recommendations

- [Recommendation 1]
- [Recommendation 2]

### Screenshots

[Attach screenshots if needed]

---
**Overall Status:** ✅ PASS / ❌ FAIL
```

---

## 🔧 Quick Fixes

### Fix 1: PDF không hiển thị

```javascript
// Check PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = 
  'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/build/pdf.worker.min.js';
```

### Fix 2: EPUB bị lỗi format

```javascript
// Try với allowScriptedContent: false
rendition = book.renderTo("epub-viewer", {
    width: "100%",
    height: "100%",
    spread: "none",
    allowScriptedContent: false
});
```

### Fix 3: Progress không sync

```java
// Force flush
@Transactional
public ReadingProgress saveReadingProgress(ReadingProgress progress) {
    ReadingProgress saved = repository.save(progress);
    repository.flush();
    return saved;
}
```

---

## 📞 Support

**Nếu gặp vấn đề:**

1. Check logs: `application.log`
2. Check browser console: F12
3. Check database: phpmyadmin/MySQL Workbench
4. Check file system: Explorer

**Common Errors:**

- **404 Not Found:** Check file path và resource handler
- **403 Forbidden:** Check permissions
- **500 Server Error:** Check logs
- **Cannot read property:** Check null values

---

*Good luck testing! 🚀*

