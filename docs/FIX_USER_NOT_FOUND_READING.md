# 🔧 FIX: Lỗi "User not found" khi đọc sách

## 📋 Tổng quan vấn đề

**Lỗi:** "Có lỗi xảy ra khi mở sách - User not found" dù đã đăng nhập

**Nguyên nhân chính:**
1. ❌ Sử dụng `authentication.getName()` để lấy email/username
2. ❌ Database query dùng `findByEmail()` nhưng authentication.getName() không trả về email
3. ❌ SecurityConfig có thứ tự requestMatchers không đúng

---

## ✅ Các thay đổi đã thực hiện

### 1. ReadingController.java - Fix Authentication

**Thêm helper method:**
```java
private User getCurrentUser(Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
        return null;
    }
    return (User) authentication.getPrincipal();
}
```

**Sửa tất cả các method:**
- ✅ `openBook()` - Dùng `getCurrentUser()` thay vì `authentication.getName()` + `findByEmail()`
- ✅ `reader()` - Dùng `getCurrentUser()`
- ✅ `saveProgress()` - Dùng `getCurrentUser()`
- ✅ `getProgress()` - Dùng `getCurrentUser()`
- ✅ `prepareReaderView()` - Dùng `getCurrentUser()`

**Xóa dependency không dùng:**
- ❌ Removed: `UserRepository` (không còn cần thiết)

### 2. SecurityConfig.java - Fix Request Matcher Order

**Vấn đề:**
```java
// ❌ SAI: /book_asset/** được permitAll() TRƯỚC
.requestMatchers("/book_asset/**").permitAll()
.requestMatchers("/book_asset/source/**").authenticated()
```

**Giải pháp:**
```java
// ✅ ĐÚNG: /book_asset/source/** được check authenticated() TRƯỚC
.requestMatchers("/book_asset/source/**", "/uploads/source/**").authenticated()
.requestMatchers("/book_asset/**", "/uploads/**").permitAll()
```

**Lý do:** Spring Security áp dụng rule **ĐẦU TIÊN** match được. Nếu `/book_asset/**` ở trước, nó sẽ match luôn và cho phép truy cập không cần login.

### 3. WebMvcConfig.java - Fix Resource Handler Order

**Thay đổi:**
```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // ✅ Handle source files FIRST (PDF, EPUB)
    registry.addResourceHandler("/book_asset/source/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/source/");
    
    registry.addResourceHandler("/uploads/source/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/source/");

    // Then handle general paths
    registry.addResourceHandler("/book_asset/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/");

    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/");

    registry.addResourceHandler("/books/**")
            .addResourceLocations("file:F:/datn_uploads/book_asset/image/covers/");
}
```

### 4. Thêm Logging & File Validation

**Trong openBook():**
```java
// Kiểm tra file tồn tại trên disk
String fullPath = "F:/datn_uploads/book_asset/source/" + readableAsset.getFileUrl();
java.io.File file = new java.io.File(fullPath);
if (!file.exists()) {
    log.error("File not found on disk: {}", fullPath);
    redirectAttributes.addFlashAttribute("error", "File sách không tồn tại trên hệ thống");
    return "redirect:/books/view/" + bookId;
}
log.info("File exists on disk: {} (size: {} bytes)", fullPath, file.length());
```

### 5. Thêm Test Tools

**Tạo file test:** `test/test-pdf-load.html`
- Test direct file access
- Test PDF.js loading
- Test authentication
- Test all possible paths

**Endpoint:** `GET /reading/test-pdf-load`

---

## 🧪 Cách test

### Test 1: Kiểm tra Authentication
```
1. Đăng nhập vào hệ thống
2. Truy cập: http://localhost:2706/user/index
3. Kiểm tra session có user_id không
```

### Test 2: Test Book Assets
```
1. Truy cập: http://localhost:2706/reading/test/{bookId}
2. Xem có file PDF/EPUB không
3. Kiểm tra file có tồn tại trên disk không
```

### Test 3: Test PDF Loading
```
1. Truy cập: http://localhost:2706/reading/test-pdf-load
2. Chạy các test:
   - Test Direct Access
   - Test PDF.js Loading
   - Test Authentication
   - Test All Paths
```

### Test 4: Test Reading
```
1. Đăng nhập
2. Vào trang Book Detail
3. Click "Đọc sách"
4. Kiểm tra:
   - ✅ Không có lỗi "User not found"
   - ✅ PDF/EPUB load được
   - ✅ Progress được save
```

---

## 📝 Các URL paths được hỗ trợ

### Cho ảnh cover, preview:
- ✅ `/book_asset/image/covers/{filename}` - Public access
- ✅ `/uploads/covers/{filename}` - Public access
- ✅ `/books/{filename}` - Public access (backward compatibility)

### Cho file PDF/EPUB (cần authentication):
- 🔐 `/book_asset/source/{category}/{filename}` - Authenticated
- 🔐 `/uploads/source/{category}/{filename}` - Authenticated

**Ví dụ:**
```
/uploads/source/khoahoc-vientuong/Cac The Gioi Song Song - Michio Kaku.pdf
/book_asset/source/kienthuc-hocthuat/Bi Quyet Thuyet Trinh Cua Steve - Carmine Gallo.pdf
```

---

## 🔍 Debugging

### Nếu vẫn lỗi "User not found":
```
1. Check console log: "User found: {username} ({userId})"
2. Check AuthController có set User object vào principal không
3. Check session có attribute "loggedInUser" không
```

### Nếu lỗi "File not found":
```
1. Check file tồn tại: F:/datn_uploads/book_asset/source/{path}
2. Check database: SELECT file_url FROM bookassets WHERE book_id = ?
3. Check log: "File exists on disk: ... (size: ... bytes)"
```

### Nếu lỗi "Access Denied":
```
1. Check SecurityConfig: /book_asset/source/** authenticated()
2. Check user đã login chưa
3. Check session có SecurityContext không
```

---

## 📚 File liên quan

- ✅ `ReadingController.java` - Fixed authentication logic
- ✅ `SecurityConfig.java` - Fixed request matcher order
- ✅ `WebMvcConfig.java` - Fixed resource handler order
- ✅ `test/test-pdf-load.html` - New test page
- ✅ `AuthController.java` - Already correct (sets User object as principal)

---

## 🎯 Kết luận

**Root cause:** 
- Code cũ dùng `authentication.getName()` mà nghĩ nó trả về email, nhưng thực tế nó trả về `user.toString()` vì principal là User object, không phải String.

**Solution:**
- Dùng `(User) authentication.getPrincipal()` để lấy User object trực tiếp
- Sửa thứ tự request matchers trong SecurityConfig
- Thêm validation để check file tồn tại

**Result:**
- ✅ Không còn lỗi "User not found"
- ✅ File PDF/EPUB load được (nếu file tồn tại)
- ✅ Reading progress được save đúng
- ✅ Authentication được check đúng cho source files

---

**Ngày fix:** 13/12/2024  
**Files changed:** 3 files  
**New files:** 1 test file

