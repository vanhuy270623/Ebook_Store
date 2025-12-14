# Hướng dẫn Migration User Templates sang Layout System

## ✅ Đã hoàn thành

### Layout Components
- ✅ `user/layout/head.html` - Head fragment với tất cả CSS chung
- ✅ `user/layout/navbar.html` - Navbar fragment  
- ✅ `user/layout/footer.html` - Footer fragment
- ✅ `user/layout/scripts.html` - Scripts fragment với toast notification

### Converted Pages
- ✅ `dashboard.html` - Không có CSS/JS riêng
- ✅ `library.html` - Có CSS riêng: `library.css`
- ✅ `favorites.html` - Không có CSS/JS riêng

## 🔄 Đang tiến hành

### Pages với CSS/JS riêng cần chú ý

#### profile.html
- **CSS riêng:** `profile.css`
- **JS riêng:** Không
- **Template cần:**
```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
    <!-- CSS riêng cho Profile -->
    <link rel="stylesheet" th:href="@{/user_template/css/profile.css}">
</head>
<body>
    <div th:replace="~{user/layout/navbar :: navbar}"></div>
    <div th:if="${success}" th:attr="data-flash-success=${success}" style="display:none;"></div>
    <div th:if="${error}" th:attr="data-flash-error=${error}" style="display:none;"></div>
    
    <!-- NỘI DUNG TRANG -->
    
    <div th:replace="~{user/layout/footer :: footer}"></div>
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
    <script th:src="@{/user_template/js/user-main.js}"></script>
</body>
</html>
```

#### index.html (Homepage)
- **CSS riêng:** Không (đã có trong head chung)
- **JS riêng:** Tiny Slider (đã có trong scripts chung)
- **Đặc biệt:** Có hero section, sliders
- **Template:** Như template cơ bản

## 📋 Cần xử lý

### Main Pages
- [ ] `index.html` - Homepage
- [ ] `profile.html` - Profile page với CSS riêng

### Books Module
- [ ] `books/list.html`
- [ ] `books/detail.html` 
- [ ] `books/search.html` (nếu có)

### Cart Module  
- [ ] `cart/index.html`
- [ ] `cart/checkout.html` (nếu có)

### Order Module
- [ ] `order/list.html`
- [ ] `order/detail.html`
- [ ] `order/history.html` (nếu có)

### Payment Module
- [ ] `payment/checkout.html`
- [ ] `payment/success.html`
- [ ] `payment/cancel.html`

### Reading Module
- [ ] `reading/epub.html` - EPUB reader (có CSS/JS riêng đặc biệt)
- [ ] `reading/pdf.html` - PDF reader (có CSS/JS riêng đặc biệt)

### Subscription Module
- [ ] `subscription/plans.html`
- [ ] `subscription/checkout.html`
- [ ] `subscription/success.html`

## 🎯 Template Migration Pattern

### Pattern 1: Trang đơn giản (không có CSS/JS riêng)

**VÍ DỤ: favorites.html, dashboard.html**

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
</head>
<body>
    <div th:replace="~{user/layout/navbar :: navbar}"></div>
    
    <!-- Flash Messages -->
    <div th:if="${success}" th:attr="data-flash-success=${success}" style="display:none;"></div>
    <div th:if="${error}" th:attr="data-flash-error=${error}" style="display:none;"></div>
    <div th:if="${info}" th:attr="data-flash-info=${info}" style="display:none;"></div>
    
    <!-- PAGE CONTENT HERE -->
    
    <div th:replace="~{user/layout/footer :: footer}"></div>
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
    <script th:src="@{/user_template/js/user-main.js}"></script>
</body>
</html>
```

### Pattern 2: Trang có CSS riêng

**VÍ DỤ: library.html, profile.html**

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
    <!-- CSS riêng cho trang này -->
    <link rel="stylesheet" th:href="@{/user_template/css/CUSTOM.css}">
</head>
<body>
    <div th:replace="~{user/layout/navbar :: navbar}"></div>
    
    <div th:if="${success}" th:attr="data-flash-success=${success}" style="display:none;"></div>
    <div th:if="${error}" th:attr="data-flash-error=${error}" style="display:none;"></div>
    
    <!-- PAGE CONTENT HERE -->
    
    <div th:replace="~{user/layout/footer :: footer}"></div>
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
    <script th:src="@{/user_template/js/user-main.js}"></script>
</body>
</html>
```

### Pattern 3: Trang có CSS và JS riêng

**VÍ DỤ: reading/epub.html, reading/pdf.html**

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
    <!-- CSS riêng -->
    <link rel="stylesheet" th:href="@{/user_template/css/CUSTOM.css}">
</head>
<body>
    <div th:replace="~{user/layout/navbar :: navbar}"></div>
    
    <div th:if="${success}" th:attr="data-flash-success=${success}" style="display:none;"></div>
    <div th:if="${error}" th:attr="data-flash-error=${error}" style="display:none;"></div>
    
    <!-- PAGE CONTENT HERE -->
    
    <div th:replace="~{user/layout/footer :: footer}"></div>
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
    <script th:src="@{/user_template/js/user-main.js}"></script>
    <!-- JS riêng -->
    <script th:src="@{/user_template/js/CUSTOM.js}"></script>
</body>
</html>
```

### Pattern 4: Trang đặc biệt (không có navbar/footer)

**VÍ DỤ: reading/epub.html - fullscreen reader**

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <!-- Chỉ dùng head, không dùng navbar/footer -->
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
    <link rel="stylesheet" th:href="@{/user_template/css/epub-reader.css}">
</head>
<body class="reader-fullscreen">
    <!-- NO NAVBAR - fullscreen reader -->
    
    <!-- READER CONTENT -->
    
    <!-- NO FOOTER - fullscreen reader -->
    
    <!-- Minimal scripts -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script th:src="@{/user_template/js/epub-reader.js}"></script>
</body>
</html>
```

## 🔍 Các bước Migration cho mỗi trang

### Bước 1: Phân tích file
```bash
# Xác định CSS riêng
grep -n "css/" user/FILENAME.html | grep -v "bootstrap\|style\|homepage\|user-custom\|books-shared\|tiny-slider"

# Xác định JS riêng  
grep -n ".js" user/FILENAME.html | grep -v "bootstrap\|jquery\|tiny-slider\|main\|user-main"
```

### Bước 2: Backup
```bash
cp src/main/resources/templates/user/FILENAME.html backup/FILENAME.html.bak
```

### Bước 3: Convert Head
Thay thế từ `<!DOCTYPE html>` đến `</head>` bằng:
```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
    <!-- Thêm CSS riêng ở đây nếu có -->
</head>
```

### Bước 4: Convert Navbar
Xóa toàn bộ `<nav class="navbar...">...</nav>`

Thêm sau `<body>`:
```html
<div th:replace="~{user/layout/navbar :: navbar}"></div>

<!-- Flash Messages -->
<div th:if="${success}" th:attr="data-flash-success=${success}" style="display:none;"></div>
<div th:if="${error}" th:attr="data-flash-error=${error}" style="display:none;"></div>
<div th:if="${info}" th:attr="data-flash-info=${info}" style="display:none;"></div>
```

### Bước 5: Convert Footer & Scripts
Thay thế từ `<!-- Footer -->` đến `</html>` bằng:
```html
    <div th:replace="~{user/layout/footer :: footer}"></div>
    
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
    <script th:src="@{/user_template/js/user-main.js}"></script>
    <!-- Thêm JS riêng ở đây nếu có -->
</body>
</html>
```

### Bước 6: Test
- [ ] Trang hiển thị đúng
- [ ] Navbar hoạt động
- [ ] Footer hiển thị
- [ ] Flash messages hoạt động
- [ ] CSS riêng load đúng
- [ ] JS riêng hoạt động
- [ ] Responsive OK
- [ ] Console không có lỗi

## 💡 Tips & Tricks

### Xử lý inline styles
Nếu trang có inline `<style>` tag, chuyển sang file CSS riêng hoặc giữ nguyên:
```html
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
    <style>
        /* Custom styles for this page only */
        .custom-class { ... }
    </style>
</head>
```

### Xử lý inline scripts
Nếu trang có inline `<script>` tag, giữ nguyên ở cuối body:
```html
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
    <script th:src="@{/user_template/js/user-main.js}"></script>
    <script>
        // Custom inline script
        console.log('Page loaded');
    </script>
</body>
```

### Controller updates
Đảm bảo controller truyền biến `pageTitle` và `currentPage`:
```java
model.addAttribute("pageTitle", "Tên trang");
model.addAttribute("currentPage", "page-id");
```

## 📊 Progress Tracking

| Module | Pages | Converted | Remaining |
|--------|-------|-----------|-----------|
| Main | 4 | 3 | 1 |
| Books | 3 | 0 | 3 |
| Cart | 2 | 0 | 2 |
| Order | 3 | 0 | 3 |
| Payment | 3 | 0 | 3 |
| Reading | 2 | 0 | 2 |
| Subscription | 3 | 0 | 3 |
| **Total** | **20** | **3** | **17** |

## 🚀 Next Steps

1. ✅ Complete main pages (profile, index)
2. Convert books module
3. Convert cart & order modules
4. Convert payment module
5. Convert subscription module
6. Convert reading module (đặc biệt - cần test kỹ)
7. Final testing all pages
8. Update controllers với pageTitle và currentPage
9. Remove old layout code
10. Commit changes

## 📝 Notes

- **KHÔNG** xóa file backup cho đến khi test hoàn toàn
- **LUÔN** test từng trang sau khi convert
- **GHI CHÚ** các CSS/JS riêng của từng trang
- **KIỂM TRA** console browser để phát hiện lỗi JS
- **XEM LẠI** server logs để phát hiện lỗi Thymeleaf

