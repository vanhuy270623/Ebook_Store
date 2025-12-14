# Migration Script - Convert User Templates to Layout System

## Danh sách các trang cần chuyển đổi

### Đã chuyển đổi
- [ ] dashboard.html → dashboard-new.html (mẫu)

### Cần chuyển đổi
- [ ] index.html
- [ ] library.html
- [ ] favorites.html
- [ ] profile.html
- [ ] books/list.html
- [ ] books/detail.html
- [ ] cart/index.html
- [ ] order/list.html
- [ ] order/detail.html
- [ ] payment/*
- [ ] reading/*
- [ ] subscription/*

## Các bước chuyển đổi cho mỗi trang

### 1. Backup file gốc
```bash
# Tạo backup folder nếu chưa có
mkdir -p backup/templates/user

# Copy file gốc vào backup
cp src/main/resources/templates/user/[filename].html backup/templates/user/[filename].html.bak
```

### 2. Xác định phần cần giữ lại
Trong mỗi trang, xác định:
- ✅ Giữ: Nội dung chính (content) - phần unique của trang
- ❌ Xóa: `<head>` section - thay bằng layout
- ❌ Xóa: Navbar - thay bằng fragment
- ❌ Xóa: Footer - thay bằng fragment  
- ❌ Xóa: Common scripts - thay bằng fragment
- ✅ Giữ: Page-specific CSS/JS - đưa vào extra-head/extra-scripts

### 3. Template conversion

**Cấu trúc cũ:**
```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <!-- All CSS and meta tags -->
</head>
<body>
    <!-- Navbar (repeated in every file) -->
    <nav>...</nav>
    
    <!-- Page Content -->
    <main>
        <!-- Unique content here -->
    </main>
    
    <!-- Footer (repeated in every file) -->
    <footer>...</footer>
    
    <!-- Scripts (repeated in every file) -->
    <script>...</script>
</body>
</html>
```

**Cấu trúc mới:**
```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{user/layout/base}">
<head>
    <title>Page Title</title>
    <!-- Page-specific CSS -->
    <th:block layout:fragment="extra-head">
        <link rel="stylesheet" th:href="@{/user_template/css/page-specific.css}">
    </th:block>
</head>
<body>
    <!-- Only page content -->
    <main layout:fragment="content">
        <!-- Unique content here -->
    </main>
    
    <!-- Page-specific JS -->
    <th:block layout:fragment="extra-scripts">
        <script th:src="@{/user_template/js/page-specific.js}"></script>
    </th:block>
</body>
</html>
```

### 4. Cập nhật Controller

Thêm các biến bắt buộc:
```java
model.addAttribute("pageTitle", "Tiêu đề trang");
model.addAttribute("currentPage", "page-identifier");
```

### 5. Test

- [ ] Kiểm tra trang hiển thị đúng
- [ ] Kiểm tra navbar highlight đúng menu
- [ ] Kiểm tra flash messages hoạt động
- [ ] Kiểm tra responsive
- [ ] Kiểm tra tất cả links và forms

## Chi tiết chuyển đổi từng trang

### index.html (Homepage)
**Đặc biệt:**
- Có hero section
- Có book sliders (cần Tiny Slider)
- Có nhiều sections

**Variables cần:**
```java
model.addAttribute("pageTitle", "Trang chủ");
model.addAttribute("currentPage", "index");
```

### library.html
**Đặc biệt:**
- Có filter/search
- Có library.css riêng
- Grid layout cho books

**Variables cần:**
```java
model.addAttribute("pageTitle", "Thư viện của tôi");
model.addAttribute("currentPage", "library");
```

### favorites.html
**Đặc biệt:**
- Grid layout tương tự library
- Like/Unlike actions

**Variables cần:**
```java
model.addAttribute("pageTitle", "Sách yêu thích");
model.addAttribute("currentPage", "favorites");
```

### profile.html
**Đặc biệt:**
- Form upload avatar
- Validation
- Tab navigation

**Variables cần:**
```java
model.addAttribute("pageTitle", "Hồ sơ");
model.addAttribute("currentPage", "profile");
```

## Script tự động (PowerShell)

```powershell
# migrate-templates.ps1

$templates = @(
    "index",
    "library", 
    "favorites",
    "profile",
    "dashboard"
)

foreach ($template in $templates) {
    $source = "src/main/resources/templates/user/$template.html"
    $backup = "backup/templates/user/$template.html.bak"
    
    # Create backup
    Copy-Item $source $backup -Force
    Write-Host "✓ Backed up $template.html" -ForegroundColor Green
}
```

## Rollback procedure

Nếu có vấn đề, khôi phục từ backup:
```powershell
Copy-Item "backup/templates/user/[filename].html.bak" "src/main/resources/templates/user/[filename].html" -Force
```

## Notes

- **QUAN TRỌNG:** Backup tất cả files trước khi chuyển đổi
- Test từng trang sau khi chuyển đổi
- Cập nhật documentation nếu có thay đổi
- Commit sau mỗi trang thành công để dễ rollback
- Kiểm tra browser console cho JS errors
- Kiểm tra server logs cho Thymeleaf errors

## Timeline

- Phase 1: Core pages (index, dashboard, library, profile) - 1-2 hours
- Phase 2: Feature pages (books, cart, orders) - 2-3 hours  
- Phase 3: Complex pages (reading, payment, subscription) - 3-4 hours
- Phase 4: Testing & fixes - 1-2 hours

**Total estimate:** 1 working day

## Benefits After Migration

1. **Code reduction:** ~60% less HTML code
2. **Maintenance:** Update navbar once → all pages updated
3. **Consistency:** Same structure across all pages
4. **Performance:** Browser can cache layout components
5. **Development speed:** New pages take 50% less time

