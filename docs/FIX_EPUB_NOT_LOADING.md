# 🔧 FIX: EPUB KHÔNG LOAD - "Đang tải EPUB..."

## 🎯 VẤN ĐỀ
- CDN hoạt động tốt ✅
- Nhưng file EPUB không load được ❌
- Màn hình chỉ hiện "Đang tải EPUB..." mãi

## ✅ ĐÃ SỬA
Thêm mechanism giống PDF viewer:
1. ✅ Chờ ePub.js library load xong trước khi init
2. ✅ Kiểm tra file path trước khi load
3. ✅ Error handling và debug logs chi tiết
4. ✅ Alert rõ ràng nếu có lỗi

---

## 🧪 HƯỚNG DẪN DEBUG

### Bước 1: Mở Console (F12)
```
F12 → Console Tab
```

### Bước 2: Reload trang và xem logs
Tìm các logs này:

#### ✅ GOOD - Nếu thấy:
```javascript
DOM loaded, checking ePub.js...
✅ ePub.js loaded successfully after 100ms
ePub object: {VERSION: "0.3.93", ...}
=== EPUB LOADING DEBUG ===
Asset Path from Thymeleaf: /book_asset/source/...
Testing file accessibility...
Test HEAD request status: 200
✅ Book is ready
✅ Book displayed successfully
=== EPUB LOADED SUCCESSFULLY ===
```

#### ❌ BAD - Nếu thấy:
```javascript
❌ ePub.js failed to load after 5000ms
// → CDN bị block, xem giải pháp bên dưới
```

```javascript
❌ Test HEAD request status: 404
// → File không tồn tại, kiểm tra path
```

```javascript
❌ Error loading EPUB: ...
// → Xem message cụ thể
```

---

## 🔍 COMMON ERRORS

### Lỗi 1: "ePub.js failed to load"
**Nguyên nhân:** CDN bị chặn

**Giải pháp:**
```powershell
# Download ePub.js về local
Invoke-WebRequest -Uri "https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js" `
  -OutFile "C:\Projects\Ebook_Store\src\main\resources\static\libs\epub.min.js"
```

Trong `epub-viewer.html`:
```html
<!-- Thay vì: -->
<script src="https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js"></script>

<!-- Dùng: -->
<script th:src="@{/libs/epub.min.js}"></script>
```

---

### Lỗi 2: "HTTP 404 - File không tồn tại"
**Nguyên nhân:** File EPUB không tồn tại hoặc path sai

**Kiểm tra:**

#### 1. File có tồn tại không?
```powershell
# Kiểm tra file trong thư mục uploads
Test-Path "F:\datn_uploads\book_asset\source\kienthuc-hocthuat\*.epub"
```

#### 2. Path trong DB có đúng không?
```sql
-- Kiểm tra asset path
SELECT ba.asset_id, ba.asset_type, ba.file_url, b.title
FROM book_asset ba
JOIN book b ON ba.book_id = b.book_id
WHERE ba.asset_type = 'SOURCE' AND ba.file_url LIKE '%.epub';
```

**Kết quả mong đợi:**
```
file_url: /book_asset/source/kienthuc-hocthuat/Phi Ly Tri - Dan Ariely.epub
```

#### 3. Spring Boot có map đúng path không?
Kiểm tra file: `application.properties` hoặc `WebConfig.java`
```properties
# Phải có config này:
file.upload-dir=F:/datn_uploads
```

```java
// Phải có config này trong WebConfig:
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/book_asset/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/");
}
```

---

### Lỗi 3: "Cannot read property 'renderTo' of undefined"
**Nguyên nhân:** Book instance không được tạo đúng

**Debug:**
1. Check Console log: `Book instance created: ...`
2. Nếu `undefined` → File EPUB bị corrupt hoặc không đọc được

**Giải pháp:**
- Thử mở file EPUB bằng app khác (Calibre, Adobe Digital Editions)
- Nếu file lỗi → Upload file mới

---

### Lỗi 4: "Element #epub-viewer không tồn tại"
**Nguyên nhân:** DOM element không tìm thấy

**Kiểm tra:**
```javascript
// Trong Console:
document.getElementById('epub-viewer')
// Phải trả về: <div id="epub-viewer"></div>
// Không phải: null
```

**Giải pháp:**
- Hard refresh: Ctrl + Shift + R
- Clear cache và reload

---

## 📊 CHECKLIST DEBUG

### [ ] 1. Kiểm tra Console Logs
```
F12 → Console
Reload trang
Tìm "=== EPUB LOADING DEBUG ==="
```

### [ ] 2. Kiểm tra Network Tab
```
F12 → Network
Filter: "epub"
Reload trang
```

**Nên thấy:**
- ✅ `epubjs@0.3.93/dist/epub.min.js` - Status: 200
- ✅ `Phi Ly Tri - Dan Ariely.epub` - Status: 200

**Nếu thấy:**
- ❌ Status: 404 → File không tồn tại
- ❌ Status: 403 → Không có quyền truy cập
- ❌ (failed) → Network error hoặc CORS

### [ ] 3. Kiểm tra File Path
```javascript
// Trong Console, chạy:
console.log(assetPath);
// Kết quả phải là: /book_asset/source/.../file.epub
```

### [ ] 4. Test File Trực Tiếp
Mở trình duyệt, truy cập:
```
http://localhost:8080/book_asset/source/kienthuc-hocthuat/Phi Ly Tri - Dan Ariely.epub
```

**Nếu:**
- ✅ Download file → Path đúng, Spring config OK
- ❌ 404 → Path sai hoặc Spring config thiếu
- ❌ 403 → Permission issue

### [ ] 5. Kiểm tra Browser Compatibility
Test trên các browser:
- ✅ Chrome (recommended)
- ✅ Firefox
- ✅ Edge
- ⚠️ Safari (có thể có vấn đề với EPUB.js)

---

## 🚀 QUICK FIX COMMANDS

### Fix 1: Download Library Local
```powershell
# Tạo thư mục
New-Item -ItemType Directory -Force -Path "C:\Projects\Ebook_Store\src\main\resources\static\libs"

# Download ePub.js
Invoke-WebRequest -Uri "https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js" `
  -OutFile "C:\Projects\Ebook_Store\src\main\resources\static\libs\epub.min.js"

# Verify
Test-Path "C:\Projects\Ebook_Store\src\main\resources\static\libs\epub.min.js"
```

### Fix 2: Restart Spring Boot
```powershell
# Stop nếu đang chạy (Ctrl+C)
# Sau đó start lại:
cd C:\Projects\Ebook_Store
mvn clean spring-boot:run
```

### Fix 3: Clear Browser Cache
```
Chrome: Ctrl + Shift + Delete → Clear cache
Firefox: Ctrl + Shift + Delete → Clear cache
Edge: Ctrl + Shift + Delete → Clear cache
```

---

## 📝 NHỮNG GÌ ĐÃ THÊM VÀO CODE

### 1. Wait for Library Load
```javascript
function initEPUBViewer() {
    if (typeof ePub === 'undefined') {
        // Retry every 100ms, max 5 seconds
        setTimeout(initEPUBViewer, 100);
        return;
    }
    loadEPUB();
}
```

### 2. File Path Validation
```javascript
// Kiểm tra path không trống
if (!epubPath || epubPath.trim() === '') {
    throw new Error('Đường dẫn file EPUB không hợp lệ');
}

// Test URL accessibility
const testResponse = await fetch(epubPath, { method: 'HEAD' });
if (!testResponse.ok) {
    throw new Error(`File không tồn tại. HTTP ${testResponse.status}`);
}
```

### 3. Detailed Debug Logs
```javascript
console.log('=== EPUB LOADING DEBUG ===');
console.log('Asset Path:', epubPath);
console.log('Full URL:', window.location.origin + epubPath);
console.log('Test HEAD status:', testResponse.status);
console.log('✅ Book displayed successfully');
```

### 4. Better Error Messages
```javascript
if (error.message.includes('HTTP 404')) {
    errorMessage += '❌ File không tồn tại (404)\n';
    errorMessage += 'Kiểm tra:\n';
    errorMessage += '1. File EPUB có trong uploads?\n';
    errorMessage += '2. Path trong DB đúng?\n';
}
```

---

## 🎯 NEXT STEPS

1. **Reload trang EPUB viewer**
2. **Mở Console (F12)**
3. **Xem logs và tìm lỗi cụ thể**
4. **Follow hướng dẫn debug ở trên**
5. **Report lại kết quả:**
   - Screenshot Console logs
   - Screenshot Network tab
   - Error message (nếu có)

---

## 📞 EXPECTED BEHAVIOR

### ✅ Khi thành công:
1. Loading overlay hiện "Đang tải EPUB..."
2. Console logs:
   ```
   ✅ ePub.js loaded successfully
   File URL test passed
   ✅ Book is ready
   ✅ Book displayed successfully
   ```
3. Loading biến mất
4. Sách hiển thị trong viewer
5. Có thể lật trang, xem TOC

### ❌ Khi lỗi:
1. Loading overlay hiện mãi
2. Console có error logs đỏ
3. Alert popup với thông báo lỗi rõ ràng

---

**File đã sửa:**
- ✅ `src/main/resources/templates/user/reading/epub-viewer.html`

**Thay đổi chính:**
1. Thêm `initEPUBViewer()` để chờ library load
2. Thêm validation và test file path
3. Thêm debug logs chi tiết
4. Cải thiện error messages

**Ngày:** 13/12/2025

