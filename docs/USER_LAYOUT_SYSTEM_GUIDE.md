# Hướng dẫn sử dụng Layout System cho User Templates

## Tổng quan

Hệ thống layout được tổ chức theo cấu trúc fragment của Thymeleaf để tránh lặp code và dễ dàng bảo trì.

## Cấu trúc Layout

```
src/main/resources/templates/user/layout/
├── base.html      - Layout chính (master template)
├── head.html      - Thẻ <head> chứa CSS và meta tags
├── navbar.html    - Navigation bar
├── footer.html    - Footer
└── scripts.html   - JavaScript và scripts
```

## Cách sử dụng Layout

### 1. Sử dụng Base Layout (Khuyến nghị)

Tạo trang mới kế thừa từ `base.html`:

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{user/layout/base}">
<head>
    <title>Tên trang</title>
    <!-- CSS riêng cho trang này -->
    <th:block layout:fragment="extra-head">
        <link rel="stylesheet" th:href="@{/user_template/css/custom-page.css}">
    </th:block>
</head>
<body>
    <!-- Nội dung trang -->
    <main layout:fragment="content">
        <div class="container py-5">
            <h1>Nội dung trang của bạn</h1>
            <!-- Thêm nội dung ở đây -->
        </div>
    </main>

    <!-- JavaScript riêng cho trang này -->
    <th:block layout:fragment="extra-scripts">
        <script th:src="@{/user_template/js/custom-page.js}"></script>
    </th:block>
</body>
</html>
```

### 2. Sử dụng Fragments riêng lẻ (Nếu cần custom nhiều)

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <th:block th:replace="~{user/layout/head :: head}"></th:block>
</head>
<body>
    <!-- Navbar -->
    <th:block th:replace="~{user/layout/navbar :: navbar}"></th:block>
    
    <!-- Flash Messages -->
    <div th:if="${success}" th:attr="data-flash-success=${success}" style="display:none;"></div>
    <div th:if="${error}" th:attr="data-flash-error=${error}" style="display:none;"></div>
    
    <!-- Nội dung trang -->
    <main>
        <!-- Nội dung của bạn -->
    </main>
    
    <!-- Footer -->
    <th:block th:replace="~{user/layout/footer :: footer}"></th:block>
    
    <!-- Scripts -->
    <th:block th:replace="~{user/layout/scripts :: scripts}"></th:block>
</body>
</html>
```

## Các biến quan trọng

### Controller cần truyền các biến sau:

```java
@GetMapping("/your-page")
public String yourPage(Model model) {
    // Bắt buộc
    model.addAttribute("pageTitle", "Tiêu đề trang");
    model.addAttribute("currentPage", "page-name"); // Để highlight menu active
    
    // Tùy chọn
    model.addAttribute("currentUser", getCurrentUser()); // Thông tin user
    model.addAttribute("cartItemCount", getCartItemCount()); // Số lượng giỏ hàng
    
    return "user/your-page";
}
```

### Các giá trị `currentPage` tiêu chuẩn:
- `index` - Trang chủ
- `books` - Danh sách sách
- `categories` - Danh mục
- `subscription` - Gói VIP
- `dashboard` - Dashboard người dùng
- `profile` - Hồ sơ
- `orders` - Đơn hàng
- `library` - Thư viện
- `favorites` - Yêu thích
- `reading-history` - Lịch sử đọc

## Flash Messages

Hệ thống hỗ trợ 3 loại flash message:

```java
// Success
redirectAttributes.addFlashAttribute("success", "Thao tác thành công!");

// Error
redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra!");

// Info
redirectAttributes.addFlashAttribute("info", "Thông tin quan trọng!");
```

## Ví dụ hoàn chỉnh

### Controller:

```java
@Controller
@RequestMapping("/user")
public class UserController {
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        model.addAttribute("cartItemCount", cartService.getCartItemCount());
        
        // Thêm dữ liệu khác
        model.addAttribute("totalBooks", userLibraryService.getTotalBooks());
        model.addAttribute("readingProgress", userLibraryService.getReadingProgress());
        
        return "user/dashboard";
    }
}
```

### Template (dashboard.html):

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{user/layout/base}">
<head>
    <title>Dashboard</title>
    <th:block layout:fragment="extra-head">
        <link rel="stylesheet" th:href="@{/user_template/css/dashboard.css}">
    </th:block>
</head>
<body>
    <main layout:fragment="content">
        <div class="container py-5">
            <h1>Dashboard</h1>
            
            <div class="row">
                <div class="col-md-4">
                    <div class="card">
                        <div class="card-body">
                            <h5>Tổng sách</h5>
                            <p class="h2" th:text="${totalBooks}">0</p>
                        </div>
                    </div>
                </div>
                <!-- Thêm các card khác -->
            </div>
        </div>
    </main>
    
    <th:block layout:fragment="extra-scripts">
        <script th:src="@{/user_template/js/dashboard.js}"></script>
    </th:block>
</body>
</html>
```

## Lợi ích

1. **Không lặp code**: Navbar, footer, head, scripts chỉ viết 1 lần
2. **Dễ bảo trì**: Thay đổi navbar → tất cả trang tự động cập nhật
3. **Nhất quán**: Tất cả trang có cùng cấu trúc và style
4. **Linh hoạt**: Có thể thêm CSS/JS riêng cho từng trang
5. **SEO tốt hơn**: Dễ dàng quản lý meta tags và title

## Migration từ code cũ

Các bước chuyển đổi trang hiện tại:

1. Xóa phần `<head>` → thay bằng layout
2. Xóa navbar → thay bằng fragment
3. Xóa footer → thay bằng fragment
4. Xóa scripts → thay bằng fragment
5. Giữ lại phần content chính
6. Thêm các biến cần thiết vào controller

## Checklist khi tạo trang mới

- [ ] Extends từ `base.html` hoặc include các fragments
- [ ] Đặt `pageTitle` hợp lý
- [ ] Đặt `currentPage` để highlight menu
- [ ] Truyền `currentUser` nếu cần hiển thị thông tin user
- [ ] Truyền `cartItemCount` nếu cần hiển thị giỏ hàng
- [ ] Đặt CSS/JS riêng vào fragment `extra-head` và `extra-scripts`
- [ ] Test flash messages hoạt động đúng

## Troubleshooting

### Lỗi: Layout không load
- Kiểm tra namespace `xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"`
- Kiểm tra dependency `thymeleaf-layout-dialect` trong pom.xml

### Navbar không highlight đúng
- Kiểm tra biến `currentPage` đã được set trong controller
- Kiểm tra giá trị `currentPage` khớp với điều kiện trong navbar

### Flash message không hiển thị
- Kiểm tra biến `success`, `error`, `info` được truyền qua `RedirectAttributes`
- Kiểm tra script trong `scripts.html` đã được load

