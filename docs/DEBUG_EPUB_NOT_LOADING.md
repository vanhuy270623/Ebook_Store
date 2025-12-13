# 🔧 DEBUG: KHÔNG ĐỌC ĐƯỢC FILE EPUB

## 🎯 HƯỚNG DẪN DEBUG CHI TIẾT

### Bước 1: Restart Spring Boot
```powershell
# Stop app (Ctrl+C)
cd C:\Projects\Ebook_Store
mvn clean spring-boot:run
```

### Bước 2: Mở Test Page
```
http://localhost:8080/reading/test-epub-load
```

### Bước 3: Test Từng Bước

#### Test 1: Kiểm tra CDN ePub.js
- Trang tự động test khi load
- Xem kết quả ở mục "1. Test CDN ePub.js"
- **Kỳ vọng:** ✅ ePub.js loaded successfully

#### Test 2: Kiểm tra File Paths
- Có 2 file EPUB trong DB:
  1. `/book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi - H. G. Wells.epub`
  2. `/book_asset/source/kinhte-quanly/Sieu Kinh Te Hoc Hai Huoc - STEVEN D. LEVITT.epub`
- Click "Test Path 1" và "Test Path 2"
- **Kỳ vọng:** ✅ File accessible! với Status: 200

#### Test 3: Load EPUB
- Nhấn "Load EPUB"
- Đợi vài giây
- **Kỳ vọng:** ✅ EPUB loaded successfully! và sách hiển thị trong viewer

### Bước 4: Kiểm tra Console Logs
Xem mục "4. Console Logs" để thấy chi tiết từng bước:
```
[timestamp] INFO: Page loaded, testing CDN...
[timestamp] SUCCESS: CDN ePub.js loaded successfully
[timestamp] INFO: Testing file path: /book_asset/source/...
[timestamp] SUCCESS: File accessible: ... (200)
[timestamp] INFO: Starting EPUB load: ...
[timestamp] INFO: Testing file accessibility...
[timestamp] INFO: File test passed (200)
[timestamp] INFO: Creating ePub instance...
[timestamp] INFO: ePub instance created
[timestamp] INFO: Book ready event fired
[timestamp] INFO: Creating rendition...
[timestamp] INFO: Rendition created
[timestamp] INFO: Displaying book...
[timestamp] SUCCESS: Book displayed successfully!
```

---

## ❌ TROUBLESHOOTING

### Lỗi 1: CDN ePub.js không load
```
❌ ePub.js NOT loaded!
```

**Giải pháp:**
```powershell
# Download về local
Invoke-WebRequest -Uri "https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js" `
  -OutFile "C:\Projects\Ebook_Store\src\main\resources\static\libs\epub.min.js"
```

Sau đó sửa `test-epub-load.html`:
```html
<!-- Thay vì: -->
<script src="https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js"></script>

<!-- Dùng: -->
<script src="/libs/epub.min.js"></script>
```

---

### Lỗi 2: File NOT accessible (404)
```
❌ File NOT accessible!
Status: 404 Not Found
```

**Nguyên nhân:** File không tồn tại hoặc path sai

**Kiểm tra:**

#### A. File có tồn tại trên disk không?
```powershell
# Test file 1
Test-Path "F:\datn_uploads\book_asset\source\khoahoc-vientuong\Chien Tranh Giua Cac The Gioi - H. G. Wells.epub"

# Test file 2
Test-Path "F:\datn_uploads\book_asset\source\kinhte-quanly\Sieu Kinh Te Hoc Hai Huoc - STEVEN D. LEVITT.epub"

# Liệt kê tất cả file EPUB
Get-ChildItem -Path "F:\datn_uploads\book_asset\source" -Recurse -Filter "*.epub"
```

**Kỳ vọng:** Phải trả về `True` hoặc list files

#### B. Spring Boot có map đúng resource handler không?
Check file `WebMvcConfig.java`:
```java
registry.addResourceHandler("/book_asset/source/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/source/");
```

#### C. Test trực tiếp trong browser
```
http://localhost:8080/book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi - H. G. Wells.epub
```
**Kỳ vọng:** File download hoặc hiển thị

---

### Lỗi 3: Failed to load EPUB (sau khi file accessible)
```
✅ File accessible
❌ Failed to load EPUB!
Error: ...
```

**Có thể:**
1. File EPUB bị corrupt
2. File không phải EPUB hợp lệ
3. ePub.js không tương thích với file

**Kiểm tra:**

#### Test file bằng tool khác:
- Calibre
- Adobe Digital Editions
- EPUBReader extension

#### Xem error cụ thể trong Console:
```
F12 → Console
Tìm error message màu đỏ
```

---

### Lỗi 4: CORS Error
```
Access to fetch ... from origin ... has been blocked by CORS policy
```

**Giải pháp:** Thêm CORS config trong Spring Boot

Tạo file `CorsConfig.java`:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/book_asset/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "OPTIONS");
    }
}
```

---

## 📊 EXPECTED BEHAVIOR

### ✅ Test thành công:
```
1. Test CDN ePub.js
   ✅ ePub.js loaded successfully!
   Version: 0.3.93

2. Test File Paths
   ✅ File accessible!
   Status: 200
   Content-Type: application/epub+zip
   Size: 2.50 MB

3. Test Load EPUB
   ✅ EPUB loaded successfully!
   Title: Chiến Tranh Giữa Các Thế Giới
   Creator: H. G. Wells
   
   [Viewer hiển thị sách]

4. Console Logs
   [timestamp] SUCCESS: Book displayed successfully!
```

---

## 🔍 KIỂM TRA DATABASE

### Check book assets trong DB:
```sql
-- Xem tất cả EPUB books
SELECT b.book_id, b.title, ba.file_url, ba.file_size
FROM book b
JOIN book_asset ba ON b.book_id = ba.book_id
WHERE ba.file_type = 'EPUB';
```

**Kết quả mong đợi:**
```
book_03 | Chiến Tranh Giữa Các Thế Giới | /book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi - H. G. Wells.epub | 2621440
book_13 | Siêu Kinh Tế Học Hài Hước | /book_asset/source/kinhte-quanly/Sieu Kinh Te Hoc Hai Huoc - STEVEN D. LEVITT.epub | 3789000
```

---

## 🚀 TEST TRONG EPUB VIEWER THẬT

Sau khi test page OK, test trong viewer thật:

### Với book_03:
```
http://localhost:8080/reading/epub/book_03
```

### Với book_13:
```
http://localhost:8080/reading/epub/book_13
```

### Hoặc qua book view:
```
http://localhost:8080/books/view/book_03
→ Click nút "Đọc sách"
```

---

## 📋 CHECKLIST

- [ ] Restart Spring Boot với `mvn clean spring-boot:run`
- [ ] Mở http://localhost:8080/reading/test-epub-load
- [ ] Test 1: CDN ePub.js = ✅
- [ ] Test 2: File Path 1 = ✅
- [ ] Test 2: File Path 2 = ✅
- [ ] Test 3: Load EPUB = ✅ (sách hiển thị)
- [ ] Console logs không có error đỏ
- [ ] Test trong EPUB viewer thật
- [ ] Có thể lật trang, xem TOC

---

## 📞 NẾU VẪN KHÔNG ĐƯỢC

**Gửi cho tôi:**

### 1. Screenshot Test Page:
- Cả 4 sections
- Đặc biệt là Console Logs

### 2. PowerShell output:
```powershell
# Kiểm tra files
Get-ChildItem -Path "F:\datn_uploads\book_asset\source" -Recurse -Filter "*.epub" | Select-Object FullName, Length
```

### 3. Browser Console (F12):
- Screenshot toàn bộ Console
- Screenshot Network tab (filter: epub)

### 4. Error message cụ thể:
- Copy toàn bộ error text

---

## 💡 NOTES

1. **Test page này hoàn toàn độc lập** - không cần authentication
2. **Sử dụng đúng paths từ DB** - đã verify trong `ebook_store.sql`
3. **WebMvcConfig đã map đúng** - `/book_asset/source/**`
4. **Files tồn tại trong** `F:\datn_uploads\book_asset\source\`

Nếu test page hoạt động OK nhưng epub-viewer thật không OK → vấn đề là ở Thymeleaf hoặc Controller logic.

---

**HÃY BẮT ĐẦU TỪ TEST PAGE TRƯỚC!**

Ngày: 13/12/2025

