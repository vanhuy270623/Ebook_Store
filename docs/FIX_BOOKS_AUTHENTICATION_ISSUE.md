# Fix: Vấn đề không lưu thông tin đăng nhập khi truy cập /books

## Ngày: 13/12/2024

## Vấn đề
Khi truy cập bất kỳ trang template nào từ đường dẫn `user/books` (ví dụ: `/books/search`, `/books/view/{id}`, v.v.), hệ thống không yêu cầu đăng nhập và không lưu thông tin authentication của người dùng đã đăng nhập.

## Nguyên nhân
Trong file `SecurityConfig.java`, các route sau không được cấu hình yêu cầu authentication:
- `/books/**` - Controller `UserBookController` sử dụng mapping này
- `/reading/**` - Controller `ReadingController` sử dụng mapping này  
- `/subscription/**` - Controller `SubscriptionController` sử dụng mapping này (chỉ có `/subscription/manage`)

Mặc dù các controller này nằm trong package `controller.user` và yêu cầu authentication, nhưng SecurityConfig chỉ bảo vệ route `/user/**` mà không bảo vệ các route trên.

## Giải pháp
Cập nhật `SecurityConfig.java` để thêm các route sau vào danh sách yêu cầu authentication với role USER hoặc ADMIN:

```java
.requestMatchers(
    "/user/**",        // All user routes
    "/books/**",       // Book browsing, search, view (requires login) ✅ MỚI THÊM
    "/reading/**",     // Reading interface (requires login) ✅ MỚI THÊM
    "/subscription/**", // Subscription management ✅ MỚI THÊM
    "/payment/**",     // Payment routes
    "/order/**",       // Order routes
    "/cart/**"         // Cart routes
).hasAnyRole("USER", "ADMIN")
```

## File thay đổi
- `src/main/java/stu/datn/ebook_store/config/SecurityConfig.java`

## Các route được bảo vệ
Sau khi sửa, các route sau sẽ yêu cầu đăng nhập:

### /books/** (UserBookController)
- `GET /books` - Danh sách sách
- `GET /books/view/{id}` - Chi tiết sách
- `GET /books/search` - Tìm kiếm sách
- `GET /books/category/{categoryId}` - Sách theo danh mục
- `GET /books/by-access-type` - Sách theo loại truy cập
- `GET /books/trending` - Sách thịnh hành
- `GET /books/newest` - Sách mới nhất
- `GET /books/top-rated` - Sách đánh giá cao

### /reading/** (ReadingController)
- `GET /reading/reader/{bookId}` - Đọc sách
- `GET /reading/pdf-viewer/{bookId}` - Xem PDF
- `GET /reading/epub-viewer/{bookId}` - Xem EPUB
- `POST /reading/progress` - Lưu tiến độ đọc
- Các endpoint khác liên quan đến đọc sách

### /subscription/** (SubscriptionController)
- `GET /subscription/plans` - Xem các gói subscription
- `GET /subscription/my-subscriptions` - Quản lý subscription của tôi
- `POST /subscription/subscribe` - Đăng ký subscription
- Các endpoint khác liên quan đến subscription

## Testing
1. Restart ứng dụng
2. Đăng xuất (nếu đang đăng nhập)
3. Thử truy cập `/books/search` - Sẽ redirect về `/auth/login`
4. Đăng nhập
5. Truy cập lại `/books/search` - Sẽ hiển thị trang với thông tin user đã đăng nhập
6. Kiểm tra navbar hiển thị đúng tên user và avatar
7. Kiểm tra các chức năng khác (thêm vào giỏ hàng, xem sách đã mua, v.v.)

## Lưu ý
- Các route này giờ sẽ yêu cầu cả USER và ADMIN role
- Nếu chưa đăng nhập, user sẽ được redirect về `/auth/login`
- Session sẽ được lưu và duy trì sau khi đăng nhập
- Các link trong template không cần thay đổi vì vẫn giữ nguyên đường dẫn `/books/**`

## Các controller khác đã được bảo vệ
Các controller sau đã được cấu hình đúng từ đầu:
- ✅ `/user/**` - UserController
- ✅ `/cart/**` - CartController  
- ✅ `/order/**` - OrderController
- ✅ `/payment/**` - PaymentController

## Kiểm tra thêm
Nên kiểm tra xem có controller nào khác trong package `controller.user` mà chưa được bảo vệ không:
```bash
grep -r "@RequestMapping" src/main/java/stu/datn/ebook_store/controller/user/
```

