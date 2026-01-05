# Quick Debug Guide - Download Format Selection

## 🔍 Để kiểm tra lỗi "Không tìm thấy file sách"

### 1. Mở Developer Tools (F12)

### 2. Check Console Log

Khi click nút "Tải xuống", bạn sẽ thấy các log sau:

```javascript
Raw bookAssetsData: [{"fileType":"PDF","fileSize":2621440,"fileUrl":"..."},{"fileType":"EPUB","fileSize":1835008,"fileUrl":"..."}]
Parsed bookAssets: (2) [{...}, {...}]
Found 1 download buttons
BookAssets available: 2
Download clicked for bookId: book_001
Available assets: (2) [{...}, {...}]
```

### 3. Kiểm tra từng bước

#### ❌ Nếu thấy: `bookAssetsData element not found`
**Vấn đề:** Script tag không có trong HTML  
**Giải pháp:** Kiểm tra view.html có:
```html
<script id="bookAssetsData" type="application/json" th:utext="${bookAssetsJson}"></script>
```

#### ❌ Nếu thấy: `Raw bookAssetsData: []`
**Vấn đề:** Backend không truyền data hoặc sách không có assets  
**Giải pháp:** 
1. Check server log xem có: `Serialized X book assets for bookId...`
2. Kiểm tra DB: `SELECT * FROM book_asset WHERE book_id = 'xxx'`
3. Đảm bảo sách có file PDF hoặc EPUB

#### ❌ Nếu thấy: `Error parsing book assets`
**Vấn đề:** JSON không hợp lệ  
**Giải pháp:** Check content của script tag trong Elements tab

#### ❌ Nếu thấy: `BookAssets available: 0`
**Vấn đề:** Parse thành công nhưng array rỗng  
**Giải pháp:** Kiểm tra filter trong backend (chỉ lấy PDF/EPUB)

#### ✅ Nếu thấy: `BookAssets available: 2`
**Tốt!** Có 2 formats → Modal sẽ hiện

### 4. Check Network Request

Khi download, check tab Network:

```
Request URL: http://localhost:8080/books/download/book_001?fileType=PDF
Status: 200 OK
```

#### ❌ Nếu 403 Forbidden
→ User chưa có quyền download (chưa mua/subscribe)

#### ❌ Nếu 404 Not Found
→ File không tồn tại trong `F:/datn_uploads/book_asset/source/`

### 5. Check Backend Log

Trong console server, tìm:

```
INFO ... Serialized 2 book assets for bookId book_001: [{"fileType":"PDF",...}]
```

Nếu không thấy → Method viewBook() không được gọi hoặc có exception

### 6. Check Database

```sql
-- Kiểm tra sách có assets không
SELECT ba.*, b.title 
FROM book_asset ba
JOIN book b ON ba.book_id = b.book_id
WHERE b.book_id = 'book_001';

-- Kết quả mong đợi:
-- asset_id | book_id | file_type | file_size | file_url
-- 1        | book_001| PDF       | 2621440   | /book_asset/source/.../file.pdf
-- 2        | book_001| EPUB      | 1835008   | /book_asset/source/.../file.epub
```

### 7. Check Physical Files

```powershell
# Kiểm tra file có tồn tại không
dir "F:\datn_uploads\book_asset\source\" /s | findstr "book"
```

---

## 🛠️ Common Fixes

### Fix 1: Clear Browser Cache
```
Ctrl + Shift + Delete
→ Clear Cached Images and Files
→ Hard Reload: Ctrl + F5
```

### Fix 2: Restart Server
```powershell
# Stop server (Ctrl + C)
# Clean và rebuild
.\mvnw.cmd clean package -DskipTests
# Start lại
.\mvnw.cmd spring-boot:run
```

### Fix 3: Check Lombok
Nếu getters không work:
1. Enable Annotation Processing trong IDE
2. Install Lombok plugin
3. Rebuild project

---

## 📋 Checklist

Trước khi report bug, kiểm tra:

- [ ] Server đang chạy (http://localhost:8080)
- [ ] Login thành công
- [ ] User đã mua sách (hoặc sách là FREE)
- [ ] Sách có `isDownloadable = true`
- [ ] Database có record trong `book_asset`
- [ ] File vật lý tồn tại trong F:/datn_uploads
- [ ] Console không có JavaScript errors
- [ ] Network tab shows request được gửi
- [ ] bookAssetsJson có trong page source

---

## 🎯 Expected Behavior

### Scenario 1: Sách có 2 formats
1. Click "Tải xuống"
2. Modal hiện với 2 options:
   - 📄 PDF (2.5 MB)
   - 📚 EPUB (1.8 MB)
3. Click PDF → Download file PDF
4. Toast: "Tải xuống thành công! ... (PDF) đã được tải về"

### Scenario 2: Sách có 1 format
1. Click "Tải xuống"
2. Không có modal (tải trực tiếp)
3. Loading indicator
4. Download file
5. Toast success

### Scenario 3: Không có assets
1. Click "Tải xuống"
2. Console log: "No assets available, falling back to direct download"
3. Request: `/books/download/book_001` (không có ?fileType)
4. Server auto-select format (EPUB > PDF)

---

## 🚨 If Still Not Working

1. Copy toàn bộ console log
2. Copy server log
3. Copy SQL query result: `SELECT * FROM book_asset WHERE book_id = 'xxx'`
4. Screenshot của Network tab
5. Page source (Ctrl+U) → tìm `bookAssetsData`

Report với đầy đủ thông tin trên để debug nhanh hơn!

