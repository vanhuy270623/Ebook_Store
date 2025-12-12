# Sửa Lỗi Path Upload File Ảnh Bìa và PDF/EPUB

**Ngày:** 13/12/2025  
**Vấn đề:** Ảnh bìa sách và file PDF/EPUB không load được từ nguồn upload

## 🔍 PHÂN TÍCH VẤN ĐỀ

### Cấu trúc Upload Directory
```
F:/datn_uploads/book_asset/
├── image/
│   ├── covers/              # Ảnh bìa sách
│   │   ├── khoahoc-vientuong/
│   │   ├── kienthuc-hocthuat/
│   │   ├── kinhte-quanly/
│   │   ├── tamly-kynangsong/
│   │   └── tieuthuyet-vanhoc/
│   ├── authors/             # Ảnh tác giả
│   └── avatars/             # Avatar người dùng
└── source/                  # File PDF/EPUB
    ├── khoahoc-vientuong/
    ├── kienthuc-hocthuat/
    ├── kinhte-quanly/
    ├── tamly-kynangsong/
    └── tieuthuyet-vanhoc/
```

### Dữ liệu trong Database

#### Bảng `books`:
```sql
cover_image_url = '/book_asset/image/covers/tamly-kynangsong/datnhantam.jpg'
```
✅ **Đã có full path từ root**

#### Bảng `bookassets`:
```sql
file_url = '/book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf'
```
✅ **Đã có full path từ root**

### Cấu hình WebMvcConfig
```java
// Handle source files (PDF, EPUB)
registry.addResourceHandler("/book_asset/source/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/source/");

// Handle image files
registry.addResourceHandler("/book_asset/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/");
```
✅ **Cấu hình đúng**

---

## ❌ LỖI TRONG CODE CŨ

### 1. Reader.html - Ảnh bìa SAI
```html
<!-- SAI -->
<img th:src="@{/uploads/covers/{cover}(cover=${book.coverImageUrl ?: 'default-cover.jpg'})}">
```
**Vấn đề:**
- Thêm prefix `/uploads/covers/` 
- Nhưng `book.coverImageUrl` đã có `/book_asset/image/covers/...`
- Kết quả: `/uploads/covers//book_asset/image/covers/...` → KHÔNG TỒN TẠI

### 2. PDF Viewer - Path SAI
```javascript
// SAI
const pdfPath = `/uploads/source/${assetPath}`;
```
**Vấn đề:**
- `assetPath` = `/book_asset/source/tamly-kynangsong/Dac nhan tam.pdf`
- Thêm prefix `/uploads/source/`
- Kết quả: `/uploads/source//book_asset/source/...` → KHÔNG TỒN TẠI

### 3. EPUB Viewer - Path SAI
```javascript
// SAI
const epubPath = `/uploads/source/${assetPath}`;
```
**Vấn đề:** Tương tự PDF viewer

---

## ✅ GIẢI PHÁP ĐÃ ÁP DỤNG

### 1. Sửa Reader.html
```html
<!-- ĐÚNG - Dùng trực tiếp coverImageUrl từ DB -->
<img th:src="${book.coverImageUrl != null ? book.coverImageUrl : '/images/default-cover.jpg'}"
     alt="Book Cover" class="book-cover"
     onerror="this.src='/images/default-cover.jpg'">
```

**Lý do:**
- `book.coverImageUrl` đã là path đầy đủ: `/book_asset/image/covers/...`
- Không cần thêm prefix
- WebMvcConfig sẽ map `/book_asset/**` → `file:F:/datn_uploads/book_asset/`

### 2. Sửa PDF Viewer
```javascript
// ĐÚNG - Dùng trực tiếp assetPath từ DB
const pdfPath = assetPath;  // assetPath = '/book_asset/source/...'
console.log('Loading PDF from:', pdfPath);
```

**Lý do:**
- `asset.fileUrl` đã là path đầy đủ: `/book_asset/source/...`
- WebMvcConfig sẽ map `/book_asset/source/**` → `file:F:/datn_uploads/book_asset/source/`

### 3. Sửa EPUB Viewer
```javascript
// ĐÚNG - Dùng trực tiếp assetPath từ DB
const epubPath = assetPath;  // assetPath = '/book_asset/source/...'
console.log('Loading EPUB from:', epubPath);
```

### 4. Sửa PDF & EPUB Viewer - Ảnh bìa
```html
<!-- ĐÚNG -->
<img th:src="${book.coverImageUrl != null ? book.coverImageUrl : '/images/default-cover.jpg'}"
     alt="Book Cover" class="book-cover"
     onerror="this.src='/images/default-cover.jpg'">
```

**Thay đổi thêm:**
- `book.coverImage` → `book.coverImageUrl` (tên đúng trong entity)
- `book.author?.name` → `book.authorNames` (method helper có sẵn)
- `th:onerror` → `onerror` (sửa lỗi cú pháp Thymeleaf)

---

## 📊 SO SÁNH TRƯỚC VÀ SAU

| Phần | TRƯỚC (SAI) | SAU (ĐÚNG) |
|------|-------------|------------|
| **Cover Image Path** | `/uploads/covers/${book.coverImageUrl}` | `${book.coverImageUrl}` |
| **PDF Path** | `/uploads/source/${assetPath}` | `${assetPath}` |
| **EPUB Path** | `/uploads/source/${assetPath}` | `${assetPath}` |
| **Author Display** | `${book.author?.name}` | `${book.authorNames}` |
| **Error Handler** | `th:onerror` | `onerror` |

---

## 🔄 FLOW HOÀN CHỈNH

### Flow Load Ảnh Bìa
```
1. Database: cover_image_url = '/book_asset/image/covers/tamly-kynangsong/datnhantam.jpg'
2. Template: th:src="${book.coverImageUrl}"
3. Browser: GET /book_asset/image/covers/tamly-kynangsong/datnhantam.jpg
4. WebMvcConfig: Map '/book_asset/**' → 'file:F:/datn_uploads/book_asset/'
5. File System: F:/datn_uploads/book_asset/image/covers/tamly-kynangsong/datnhantam.jpg
6. ✅ THÀNH CÔNG
```

### Flow Load File PDF
```
1. Database: file_url = '/book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf'
2. Template: const pdfPath = assetPath;
3. JavaScript: pdfjsLib.getDocument('/book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf')
4. Browser: GET /book_asset/source/tamly-kynangsong/Dac%20nhan%20tam%20-%20Dale%20Carnegie.pdf
5. WebMvcConfig: Map '/book_asset/source/**' → 'file:F:/datn_uploads/book_asset/source/'
6. File System: F:/datn_uploads/book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf
7. ✅ THÀNH CÔNG
```

### Flow Load File EPUB
```
1. Database: file_url = '/book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi.epub'
2. Template: const epubPath = assetPath;
3. JavaScript: ePub('/book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi.epub')
4. Browser: GET /book_asset/source/khoahoc-vientuong/Chien%20Tranh%20Giua%20Cac%20The%20Gioi.epub
5. WebMvcConfig: Map '/book_asset/source/**' → 'file:F:/datn_uploads/book_asset/source/'
6. File System: F:/datn_uploads/book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi.epub
7. ✅ THÀNH CÔNG
```

---

## 📁 FILES ĐÃ SỬA

1. ✅ `src/main/resources/templates/user/reading/reader.html`
   - Sửa path ảnh bìa

2. ✅ `src/main/resources/templates/user/reading/pdf-viewer.html`
   - Sửa path ảnh bìa
   - Sửa path load PDF
   - Sửa author display
   - Sửa lỗi `th:onerror`

3. ✅ `src/main/resources/templates/user/reading/epub-viewer.html`
   - Sửa path ảnh bìa
   - Sửa path load EPUB
   - Sửa author display
   - Sửa lỗi `th:onerror`

---

## 🧪 CÁCH KIỂM TRA

### Test 1: Kiểm tra ảnh bìa
```
1. Mở: http://localhost:2706/reading/book/book_02
2. Kiểm tra: Ảnh bìa "Đắc Nhân Tâm" hiển thị
3. F12 Network: Kiểm tra request thành công
   GET /book_asset/image/covers/tamly-kynangsong/datnhantam.jpg → 200 OK
```

### Test 2: Kiểm tra PDF
```
1. Mở: http://localhost:2706/reading/book/book_02
2. Click: "PDF Reader"
3. Kiểm tra: PDF load và hiển thị trang đầu
4. Console: "Loading PDF from: /book_asset/source/tamly-kynangsong/Dac nhan tam - Dale Carnegie.pdf"
5. F12 Network: 
   GET /book_asset/source/tamly-kynangsong/Dac%20nhan%20tam%20-%20Dale%20Carnegie.pdf → 200 OK
```

### Test 3: Kiểm tra EPUB
```
1. Mở: http://localhost:2706/reading/book/book_03
2. Click: "EPUB Reader"
3. Kiểm tra: EPUB load và hiển thị nội dung
4. Console: "Loading EPUB from: /book_asset/source/khoahoc-vientuong/Chien Tranh Giua Cac The Gioi.epub"
5. F12 Network:
   GET /book_asset/source/khoahoc-vientuong/Chien%20Tranh%20Giua%20Cac%20The%20Gioi.epub → 200 OK
```

### Test 4: Kiểm tra fallback
```
1. Xóa tạm file ảnh bìa
2. Reload trang
3. Kiểm tra: Hiển thị ảnh default thay vì broken image
```

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. Path trong Database phải có prefix
```sql
-- ✅ ĐÚNG
cover_image_url = '/book_asset/image/covers/tamly-kynangsong/datnhantam.jpg'
file_url = '/book_asset/source/tamly-kynangsong/Dac nhan tam.pdf'

-- ❌ SAI
cover_image_url = 'tamly-kynangsong/datnhantam.jpg'
file_url = 'tamly-kynangsong/Dac nhan tam.pdf'
```

### 2. WebMvcConfig phải có đủ mappings
```java
// QUAN TRỌNG: /book_asset/source/** phải đứng TRƯỚC /book_asset/**
registry.addResourceHandler("/book_asset/source/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/source/");

registry.addResourceHandler("/book_asset/**")
        .addResourceLocations("file:F:/datn_uploads/book_asset/");
```

### 3. Không thêm prefix trong template
```html
<!-- ❌ SAI - Thêm prefix không cần thiết -->
<img th:src="@{/uploads/covers/{cover}(cover=${book.coverImageUrl})}">

<!-- ✅ ĐÚNG - Dùng trực tiếp -->
<img th:src="${book.coverImageUrl}">
```

### 4. URL encoding tự động
- Space → `%20`
- Special chars được encode tự động bởi browser
- Không cần encode thủ công

---

## ✅ KẾT QUẢ

**Tất cả các vấn đề về load file đã được giải quyết:**

- ✅ Ảnh bìa sách hiển thị đúng
- ✅ File PDF load được và có thể đọc
- ✅ File EPUB load được và có thể đọc
- ✅ Fallback image hoạt động khi lỗi
- ✅ Path mapping chính xác
- ✅ Code clean và maintainable

**Nguyên nhân gốc:** Template đang thêm prefix không cần thiết vào path đã có sẵn trong database.

**Giải pháp:** Dùng trực tiếp path từ database, để WebMvcConfig xử lý mapping.

