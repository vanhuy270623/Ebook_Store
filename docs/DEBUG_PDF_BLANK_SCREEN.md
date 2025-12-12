# Debug Lỗi PDF Màn Hình Trắng (Blank Screen)

**Ngày:** 13/12/2025  
**Vấn đề:** File PDF 901KB không load được, màn hình trắng

---

## 🔍 CÁC NGUYÊN NHÂN CÓ THỂ

### 1. **URL Path Không Đúng**
```
File trong DB: /book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf
Browser request: /book_asset/source/tamly-kynangsong/Dac%20nhan%20tam%20-%20Dale%20Carnegie.pdf
```
**Vấn đề:** Space trong tên file → URL encoding → Server không map đúng

### 2. **CORS / Content-Type Issues**
```
Response Headers cần có:
- Content-Type: application/pdf
- Access-Control-Allow-Origin: * (nếu cần)
```

### 3. **File Permissions**
```
File phải có readable permissions:
F:\datn_uploads\book_asset\source\tamly-kynangsong\Dac nhan tam - Dale Carnegie.pdf
```

### 4. **WebMvcConfig Mapping Sai**
```java
registry.addResourceHandler("/book_asset/source/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/source/");
```

### 5. **PDF.js Worker Lỗi**
```javascript
pdfjsLib.GlobalWorkerOptions.workerSrc = 
    'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/build/pdf.worker.min.js';
```

---

## 🛠️ CÁCH DEBUG

### Bước 1: Test File Access Trực Tiếp

**Mở URL này trong browser:**
```
http://localhost:2706/test-pdf-access.html
```

Trang này sẽ test:
- ✅ File có accessible không?
- ✅ Content-Type đúng chưa?
- ✅ PDF.js có load được không?
- ✅ URL encoding có issue không?

### Bước 2: Kiểm Tra Browser Console

**F12 → Console → Xem log:**
```javascript
=== PDF LOADING DEBUG ===
Asset Path from Thymeleaf: /book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf
Full URL will be: http://localhost:2706/book_asset/source/tamly-kynangsong/Dac%20nhan%20tam%20-%20Dale%20Carnegie.pdf
Test HEAD request status: 200 ✅ hoặc 404 ❌
```

### Bước 3: Kiểm Tra Network Tab

**F12 → Network → XHR/Fetch:**
```
Request URL: /book_asset/source/tamly-kynangsong/Dac%20nhan%20tam%20-%20Dale%20Carnegie.pdf
Status: 200 ✅ hoặc 404 ❌
Response Headers:
  Content-Type: application/pdf ✅ hoặc text/html ❌
  Content-Length: 782081 (size đúng)
```

### Bước 4: Test Direct Link

**Copy paste vào browser:**
```
http://localhost:2706/book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf
```

**Kết quả mong đợi:**
- ✅ Browser mở PDF viewer nội bộ
- ❌ 404 Not Found → Path sai
- ❌ Download file HTML → Mapping sai

---

## ✅ GIẢI PHÁP ĐÃ ÁP DỤNG

### 1. **Thêm Debug Logging Chi Tiết**

```javascript
async function loadPDF() {
    console.log('=== PDF LOADING DEBUG ===');
    console.log('Asset Path:', assetPath);
    console.log('Full URL:', window.location.origin + assetPath);
    
    // Test URL trước
    const testResponse = await fetch(pdfPath, { method: 'HEAD' });
    console.log('Test HEAD status:', testResponse.status);
    console.log('Content-Type:', testResponse.headers.get('Content-Type'));
    
    if (!testResponse.ok) {
        throw new Error(`HTTP ${testResponse.status}`);
    }
}
```

**Output sẽ cho biết:**
- Path có đúng không?
- HTTP status code là gì?
- Content-Type có phải application/pdf?

### 2. **Better Error Messages**

```javascript
catch (error) {
    console.error('=== PDF LOADING ERROR ===');
    console.error('Error name:', error.name);
    console.error('Error message:', error.message);
    console.error('Asset path:', assetPath);
    
    let errorMessage = 'Lỗi: ';
    if (error.name === 'MissingPDFException') {
        errorMessage += 'File không tồn tại\nPath: ' + assetPath;
    } else if (error.name === 'UnexpectedResponseException') {
        errorMessage += 'Server error\nKiểm tra:\n1. File tồn tại?\n2. WebMvcConfig đúng?\n3. Permissions OK?';
    }
    
    alert(errorMessage);
}
```

### 3. **Test Page Để Debug**

File: `static/test-pdf-access.html`

**Chức năng:**
- Test các URL khác nhau
- Test với/không có URL encoding
- Show chi tiết response headers
- Test PDF.js loading

---

## 🔧 FIX THƯỜNG GẶP

### Fix 1: URL Encoding Issue

**Nếu file có space, cần encode:**
```javascript
// Trong DB lưu:
/book_asset/source/tamly-kynangsong/Dac nhan tam.pdf

// Browser sẽ tự encode thành:
/book_asset/source/tamly-kynangsong/Dac%20nhan%20tam.pdf

// Server phải decode đúng
```

**Fix WebMvcConfig nếu cần:**
```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/book_asset/source/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/source/")
            .setCachePeriod(3600)
            .resourceChain(true);
}
```

### Fix 2: Content-Type Sai

**Nếu server trả về text/html thay vì application/pdf:**

Thêm vào `application.properties`:
```properties
spring.mvc.contentnegotiation.favor-path-extension=false
spring.mvc.pathmatch.use-suffix-pattern=false
```

### Fix 3: CORS Issue

**Nếu có CORS error trong console:**

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/book_asset/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD");
    }
}
```

### Fix 4: File Path Sai

**Kiểm tra file có tồn tại:**
```powershell
Test-Path "F:\datn_uploads\book_asset\source\tamly-kynangsong\Dac nhan tam - Dale Carnegie.pdf"
```

**Output: True** ✅ → File OK  
**Output: False** ❌ → File không tồn tại

---

## 📋 CHECKLIST DEBUG

### [ ] 1. Kiểm tra file tồn tại
```powershell
Get-ChildItem "F:\datn_uploads\book_asset\source" -Recurse -Filter "*.pdf"
```

### [ ] 2. Kiểm tra path trong database
```sql
SELECT book_id, file_type, file_url 
FROM bookassets 
WHERE file_type = 'PDF';
```

### [ ] 3. Test URL trực tiếp trong browser
```
http://localhost:2706/book_asset/source/tamly-kynangsong/Dac%20nhan%20tam%20-%20Dale%20Carnegie.pdf
```

### [ ] 4. Kiểm tra Browser Console
```
F12 → Console → Tìm "PDF LOADING DEBUG"
```

### [ ] 5. Kiểm tra Network Tab
```
F12 → Network → Filter: PDF
Request URL: ...
Status: 200 hoặc 404?
Response Headers: Content-Type?
```

### [ ] 6. Test với file khác (không có space)
```
/book_asset/source/kienthuc-hocthuat/BiQuyetTruyenTrinh.pdf
```

### [ ] 7. Kiểm tra WebMvcConfig
```java
/book_asset/source/** → file:F:/datn_uploads/book_asset/source/
```

### [ ] 8. Test page
```
http://localhost:2706/test-pdf-access.html
```

---

## 🎯 HÀNH ĐỘNG NGAY

### 1. Mở PDF viewer bất kỳ
```
URL: http://localhost:2706/reading/pdf/book_02
```

### 2. Mở Console (F12)
```
Xem log "=== PDF LOADING DEBUG ==="
```

### 3. Nếu thấy 404:
- Kiểm tra path trong console
- Test direct URL trong browser
- Kiểm tra WebMvcConfig

### 4. Nếu thấy 200 nhưng không render:
- Kiểm tra Content-Type
- Xem PDF.js error
- Test với file khác

### 5. Chụp màn hình Console và gửi log
```
Console output:
=== PDF LOADING DEBUG ===
Asset Path: ...
Test HEAD status: ...
Error: ...
```

---

## 📞 SUPPORT

**Nếu vẫn lỗi, cần cung cấp:**

1. **Console log đầy đủ:**
   ```
   Copy toàn bộ output trong Console
   ```

2. **Network tab screenshot:**
   ```
   Show request URL, status, headers
   ```

3. **Test page results:**
   ```
   http://localhost:2706/test-pdf-access.html
   Copy output
   ```

4. **Database info:**
   ```sql
   SELECT * FROM bookassets WHERE book_id = 'book_02';
   ```

5. **File system check:**
   ```powershell
   Get-ChildItem "F:\datn_uploads\book_asset\source" -Recurse | 
   Where-Object Name -like "*Dac*"
   ```

---

**Tạo:** 13/12/2025  
**Trạng thái:** Chờ user test và cung cấp console log

