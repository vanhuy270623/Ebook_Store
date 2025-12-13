# Tài Liệu Endpoint /user/library

## Tổng Quan

Endpoint `/user/library` là một trang tổng hợp hiển thị cả **Sách đang đọc** và **Sách đã mua** trong một giao diện thống nhất, giúp người dùng dễ dàng quản lý thư viện cá nhân của mình.

## Thông Tin Kỹ Thuật

### Endpoint
- **URL**: `GET /user/library`
- **Controller**: `UserController.java`
- **Method**: `library()`
- **Template**: `user/library.html`

### Parameters
- `tab` (String, mặc định: "reading"): Tab đang hiển thị
  - `"reading"`: Tab sách đang đọc
  - `"purchased"`: Tab sách đã mua
- `page` (Integer, mặc định: 0): Trang hiện tại (dùng cho phân trang)

### Ví dụ URLs
```
/user/library                          -> Mặc định hiển thị tab "Sách đang đọc"
/user/library?tab=reading              -> Tab "Sách đang đọc"
/user/library?tab=purchased            -> Tab "Sách đã mua"
/user/library?tab=reading&page=1       -> Tab "Sách đang đọc", trang 2
/user/library?tab=purchased&page=2     -> Tab "Sách đã mua", trang 3
```

## Chức Năng

### 1. Tab "Sách Đang Đọc"
- Hiển thị danh sách các sách mà người dùng đang/đã đọc
- Thông tin hiển thị:
  - Ảnh bìa sách
  - Tên sách và tác giả
  - Thể loại sách
  - **Thanh tiến độ đọc** (% hoàn thành)
  - Vị trí đọc cuối cùng (page/location)
  - Thời gian đọc lần cuối
  - Trạng thái: "Đã hoàn thành", "Đang đọc", "Chưa bắt đầu"
- Nút hành động:
  - **"Đọc tiếp"**: Tiếp tục đọc từ vị trí đã lưu
  - **"Bắt đầu đọc"**: Mở sách lần đầu

### 2. Tab "Sách Đã Mua"
- Hiển thị danh sách tất cả sách đã mua thành công
- Lấy từ các đơn hàng có:
  - `PaymentStatus = COMPLETED`
  - `OrderType = BOOK`
- Thông tin hiển thị:
  - Ảnh bìa sách
  - Tên sách và tác giả
  - Thể loại sách
  - Badge "Đã sở hữu"
- Nút hành động:
  - **"Đọc ngay"**: Mở sách để đọc
  - **"Chi tiết"**: Xem thông tin chi tiết sách

### 3. Thống Kê Tổng Quan
Hiển thị ở đầu trang với 2 cards màu gradient:
- **Sách đang đọc**: Số lượng sách có trong reading history
- **Sách đã mua**: Số lượng sách unique đã mua

### 4. Phân Trang
- Mỗi tab có phân trang riêng
- Page size: 12 items/trang
- Có nút Previous/Next và số trang

## Luồng Xử Lý Dữ Liệu

### Backend (UserController.java)

```java
@GetMapping("/library")
public String library(
        @RequestParam(defaultValue = "reading") String tab,
        @RequestParam(defaultValue = "0") int page,
        Authentication authentication,
        Model model)
```

**Bước 1**: Lấy thông tin User hiện tại
```java
User currentUser = getCurrentUser(authentication);
```

**Bước 2**: Lấy danh sách sách đang đọc
```java
List<ReadingProgress> readingProgresses = 
    readingProgressService.getReadingProgressByUserWithBookDetails(currentUser)
        .stream()
        .sorted((a, b) -> b.getLastReadAt().compareTo(a.getLastReadAt()))
        .toList();
```

**Bước 3**: Lấy danh sách sách đã mua
```java
// Lấy tất cả orders đã thanh toán thành công và là loại BOOK
List<Order> completedOrders = orderService.getOrdersByUser(currentUser)
    .stream()
    .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
    .filter(order -> order.getOrderType() == Order.OrderType.BOOK)
    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
    .toList();

// Lấy tất cả sách từ các order items
List<Book> purchasedBooks = completedOrders.stream()
    .flatMap(order -> orderItemService.getOrderItemsByOrderId(order.getOrderId()).stream())
    .map(OrderItem::getBook)
    .distinct()  // Loại bỏ sách trùng lặp
    .toList();
```

**Bước 4**: Phân trang theo tab đang active
```java
int pageSize = 12;
if ("reading".equals(tab)) {
    // Phân trang cho readingProgresses
} else if ("purchased".equals(tab)) {
    // Phân trang cho purchasedBooks
}
```

**Bước 5**: Thêm dữ liệu vào Model
```java
model.addAttribute("totalReadingBooks", readingProgresses.size());
model.addAttribute("totalPurchasedBooks", purchasedBooks.size());
model.addAttribute("activeTab", tab);
model.addAttribute("currentPage", page);
model.addAttribute("totalPages", totalPages);
model.addAttribute("totalBooks", totalBooks);
```

## Giao Diện (Frontend)

### Cấu Trúc HTML

```html
<section class="section py-5">
    <div class="container">
        <!-- 1. Page Header -->
        <h1>Thư viện của tôi</h1>
        
        <!-- 2. Statistics Cards -->
        <div class="stats-card">Sách đang đọc: X</div>
        <div class="stats-card">Sách đã mua: Y</div>
        
        <!-- 3. Tabs Navigation -->
        <ul class="nav nav-tabs">
            <li>Sách đang đọc</li>
            <li>Sách đã mua</li>
        </ul>
        
        <!-- 4. Tab Content -->
        <div class="tab-content">
            <!-- Tab 1: Reading Books -->
            <div class="tab-pane" id="reading">...</div>
            
            <!-- Tab 2: Purchased Books -->
            <div class="tab-pane" id="purchased">...</div>
        </div>
        
        <!-- 5. Pagination -->
        <nav>Phân trang</nav>
    </div>
</section>
```

### Styling Đặc Biệt

**Stats Cards với Gradient:**
```css
.stats-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.stats-card.secondary {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}
```

**Custom Tabs:**
```css
.nav-tabs .nav-link.active {
    border-bottom: 3px solid #0d6efd;
    font-weight: 600;
}
```

## Services Sử Dụng

### 1. ReadingProgressService
- `getReadingProgressByUserWithBookDetails(User user)`: Lấy lịch sử đọc kèm thông tin sách đầy đủ

### 2. OrderService
- `getOrdersByUser(User user)`: Lấy tất cả đơn hàng của user

### 3. OrderItemService
- `getOrderItemsByOrderId(String orderId)`: Lấy các items trong đơn hàng

## Entities Liên Quan

### ReadingProgress
```java
- progressId: String
- user: User
- book: Book
- progressPercentage: Float        // % đã đọc (0-100)
- lastReadLocation: String         // Vị trí đọc (page-1, epubcfi...)
- lastReadAt: LocalDateTime        // Thời gian đọc cuối
- isFavorite: Boolean
- isCompleted: Boolean
```

### Order
```java
- orderId: String
- user: User
- orderType: OrderType             // BOOK, SUBSCRIPTION
- paymentStatus: PaymentStatus     // COMPLETED, PENDING, FAILED
- totalAmount: BigDecimal
- createdAt: LocalDateTime
```

### OrderItem
```java
- orderItemId: String
- order: Order
- book: Book
- priceAtPurchase: BigDecimal
```

## So Sánh với Endpoints Hiện Có

### /user/reading-history
- **Chỉ** hiển thị sách đang đọc
- Không có tab switching
- URL: `/user/reading-history`

### /user/orders
- **Chỉ** hiển thị lịch sử đơn hàng (order list)
- Không hiển thị dạng thư viện sách
- URL: `/user/orders`

### /user/library ⭐ MỚI
- **Kết hợp cả hai** trong một giao diện
- Có tab switching
- Tập trung vào việc quản lý sách (không phải orders)
- URL: `/user/library`

## Lợi Ích

### 1. User Experience
- ✅ Một điểm duy nhất để truy cập toàn bộ thư viện
- ✅ Dễ dàng chuyển đổi giữa "Đang đọc" và "Đã mua"
- ✅ Hiển thị thống kê tổng quan ngay đầu trang
- ✅ Giao diện trực quan với cards và gradient colors

### 2. Technical
- ✅ Tận dụng lại logic từ các endpoints hiện có
- ✅ Không duplicate code
- ✅ Phân trang hiệu quả
- ✅ Query tối ưu với Stream API

### 3. Business
- ✅ Tăng engagement (người dùng quay lại đọc)
- ✅ Dễ phát hiện sách đã mua nhưng chưa đọc
- ✅ Khuyến khích hoàn thành sách

## Ví Dụ Sử Dụng

### Tình huống 1: User muốn đọc tiếp sách
1. Truy cập `/user/library`
2. Mặc định hiển thị tab "Sách đang đọc"
3. Tìm sách đang đọc dở
4. Click "Đọc tiếp" → Chuyển đến `/reading/book/{bookId}`

### Tình huống 2: User muốn xem sách đã mua
1. Truy cập `/user/library?tab=purchased`
2. Hiển thị tất cả sách đã mua
3. Chọn một cuốn sách
4. Click "Đọc ngay" → Mở trình đọc

### Tình huống 3: User có nhiều sách
1. Truy cập `/user/library`
2. Xem thống kê: "15 Sách đang đọc, 50 Sách đã mua"
3. Sử dụng pagination để duyệt qua các trang
4. Chuyển giữa các tabs để tìm sách cần đọc

## Navigation trong Hệ Thống

### Menu Dropdown
```html
<ul class="dropdown-menu">
    <li><a href="/user/dashboard">Dashboard</a></li>
    <li><a href="/user/profile">Hồ sơ</a></li>
    <li><a href="/user/orders">Đơn hàng</a></li>
    <li><a href="/user/library" class="active">Thư viện</a></li>  ← MỚI
    <li><a href="/user/reading-history">Lịch sử đọc</a></li>
    <li><hr></li>
    <li><a href="/auth/logout">Đăng xuất</a></li>
</ul>
```

## Testing

### Test Cases

#### TC01: Hiển thị tab Sách đang đọc
- **URL**: `/user/library` hoặc `/user/library?tab=reading`
- **Expected**: Hiển thị danh sách sách với progress bars
- **Verify**: Stats card "Sách đang đọc" có số đúng

#### TC02: Hiển thị tab Sách đã mua
- **URL**: `/user/library?tab=purchased`
- **Expected**: Hiển thị danh sách sách đã mua (không trùng lặp)
- **Verify**: Stats card "Sách đã mua" có số đúng

#### TC03: Phân trang
- **URL**: `/user/library?tab=reading&page=1`
- **Expected**: Hiển thị trang 2, nút Previous active
- **Verify**: Pagination đúng, không bị lỗi index

#### TC04: Empty state
- **Precondition**: User chưa đọc sách nào
- **URL**: `/user/library`
- **Expected**: Hiển thị message "Chưa có sách đang đọc"

#### TC05: Click "Đọc tiếp"
- **Action**: Click button "Đọc tiếp" trên một sách
- **Expected**: Chuyển đến `/reading/book/{bookId}`

## Troubleshooting

### Lỗi: "Cannot resolve MVC view 'user/library'"
- **Nguyên nhân**: IDE chưa refresh cache
- **Giải pháp**: Build lại project hoặc restart IDE

### Lỗi: Sách trùng lặp trong tab "Sách đã mua"
- **Nguyên nhân**: Không dùng `.distinct()`
- **Giải pháp**: Đã có `.distinct()` trong stream pipeline

### Lỗi: Pagination bị lỗi khi không có data
- **Nguyên nhân**: totalPages = 0
- **Giải pháp**: Đã có check `th:if="${totalPages > 1}"`

## Tích Hợp với Flows Khác

### Flow 03: Thanh toán
- Sau khi thanh toán thành công → Sách xuất hiện trong tab "Sách đã mua"

### Flow 04: Quản lý tài khoản
- Từ Dashboard → Link đến `/user/library`

### Flow 07: Đọc sách
- Từ Library → Click "Đọc tiếp"/"Đọc ngay" → Reading interface
- Reading interface tự động cập nhật progress → Hiển thị trong tab "Sách đang đọc"

## Kết Luận

Endpoint `/user/library` là một tính năng hoàn chỉnh giúp:
- ✅ Tổng hợp 2 loại sách (đang đọc & đã mua) trong 1 trang
- ✅ Cải thiện trải nghiệm người dùng
- ✅ Tận dụng tối đa các service và entity hiện có
- ✅ Không gây conflict với các endpoints cũ

**Trạng thái**: ✅ Hoàn thành 100% - Sẵn sàng sử dụng

---
*Tài liệu này được tạo ngày 13/12/2025*

