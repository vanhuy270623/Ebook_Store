# KẾ HOẠCH HOÀN THIỆN CHỨC NĂNG PHỤ TRỢ (16/12 - 21/12)

## 1. Tổng quan
Kế hoạch này tập trung vào việc hoàn thiện các chức năng "ngoài luồng chính" nhưng quan trọng cho trải nghiệm người dùng và vận hành hệ thống, bao gồm:
- Hệ thống Đánh giá & Bình luận (Review & Rating).
- Quản lý Banner & Quảng cáo (Banner Management).
- Quản lý Sách yêu thích (Favorites).
- Giao diện người dùng liên quan.

**Thời gian thực hiện:** 6 ngày (16/12/2025 - 21/12/2025).

## 2. Phân loại mức độ ưu tiên

| Mức độ | Chức năng | Lý do |
| :--- | :--- | :--- |
| **Cao (P1)** | **Đánh giá & Bình luận (User Side)** | Tăng tương tác người dùng, hỗ trợ quyết định mua hàng. |
| **Cao (P1)** | **Quản lý Banner (Admin Side)** | Công cụ marketing quan trọng để đẩy sách mới/khuyến mãi. |
| **Trung bình (P2)** | **Duyệt đánh giá (Admin Side)** | Kiểm soát nội dung spam/xấu (có thể làm sau khi user đã post được). |
| **Trung bình (P2)** | **Sách yêu thích (Favorites)** | Tiện ích cá nhân hóa, giữ chân người dùng. |
| **Thấp (P3)** | **Hiển thị Banner (User Side)** | Trang trí giao diện, làm đẹp trang chủ. |

## 3. Lịch trình chi tiết (Timeline)

### Ngày 1 (16/12): Khởi động & Review System (User Side)
*   **Mục tiêu:** Người dùng có thể viết đánh giá và xem đánh giá trên trang chi tiết sách.
*   **Công việc:**
    1.  **Backend:**
        - Kiểm tra `ReviewService` (đã có interface, cần check implementation).
        - Tạo API/Controller `ReviewController` cho User (POST review, GET reviews).
        - Logic: Chỉ cho phép đánh giá khi đã mua sách (Verified Purchase) hoặc đã đọc (nếu là sách Free/Subscription).
    2.  **Frontend:**
        - Thêm form đánh giá vào trang `user/books/view.html`.
        - Hiển thị danh sách đánh giá dưới thông tin sách.
        - Hiển thị sao (star rating) trung bình.

### Ngày 2 (17/12): Review Management (Admin Side)
*   **Mục tiêu:** Admin có thể duyệt hoặc xóa đánh giá.
*   **Công việc:**
    1.  **Backend:**
        - Hoàn thiện `AdminReviewController` (đã có khung sườn).
        - Implement logic `approveReview`, `rejectReview` trong Service.
    2.  **Frontend:**
        - Hoàn thiện giao diện `admin/reviews/list.html`.
        - Thêm nút Duyệt/Xóa nhanh trên danh sách.
        - Thêm bộ lọc: Chờ duyệt, Đã duyệt, Spam.

### Ngày 3 (18/12): Banner Management (Admin Side)
*   **Mục tiêu:** Admin có thể thêm/sửa/xóa banner quảng cáo.
*   **Công việc:**
    1.  **Backend:**
        - Hoàn thiện `AdminBannerController` (đã có khung sườn).
        - Xử lý upload ảnh banner (dùng `FileStorageService`).
        - Validate kích thước/định dạng ảnh.
    2.  **Frontend:**
        - Tạo trang `admin/banners/list.html`, `create.html`, `edit.html`.
        - Form nhập liệu: Tiêu đề, Ảnh, Link trỏ tới (Target URL), Vị trí (Home/Sidebar).

### Ngày 4 (19/12): Banner Display & Integration (User Side)
*   **Mục tiêu:** Banner hiển thị đẹp mắt trên trang chủ và các trang con.
*   **Công việc:**
    1.  **Frontend:**
        - Sửa `home.html`: Thay thế banner tĩnh bằng dynamic banner từ database.
        - Thêm Carousel (Slider) cho banner chính.
        - Thêm banner phụ ở sidebar hoặc giữa các section sách.
    2.  **Backend:**
        - API lấy danh sách banner active theo vị trí (`getBannersByPosition`).

### Ngày 5 (20/12): Favorites System (Hoàn thiện)
*   **Mục tiêu:** User có thể lưu sách vào danh sách yêu thích và xem lại.
*   **Công việc:**
    1.  **Backend:**
        - Kiểm tra `FavoriteController` (đã có, cần test kỹ).
        - API lấy danh sách yêu thích của user (`/api/favorites/my-list`).
    2.  **Frontend:**
        - Gắn sự kiện click cho nút tim (Heart icon) trên thẻ sách.
        - Tạo trang "Tủ sách yêu thích" (`user/library/favorites`).
        - Hiển thị trạng thái tim (đỏ/trắng) đúng với dữ liệu database.

### Ngày 6 (21/12): Testing & Polish
*   **Mục tiêu:** Đảm bảo mọi thứ hoạt động mượt mà, không lỗi vặt.
*   **Công việc:**
    1.  **Testing:**
        - Test luồng đánh giá: Mua sách -> Đánh giá -> Admin duyệt -> Hiện lên web.
        - Test banner: Thêm banner -> Hiện trang chủ -> Click chuyển trang.
        - Test favorite: Thêm/Xóa -> F5 vẫn giữ trạng thái.
    2.  **UI/UX Polish:**
        - Chỉnh sửa CSS cho đẹp (căn lề, màu sắc, icon).
        - Thêm thông báo (Toast notification) khi thao tác thành công.

## 4. Tài nguyên cần thiết
- **Code:** Các Controller `AdminReviewController`, `AdminBannerController`, `FavoriteController` đã có khung, cần điền logic chi tiết.
- **Database:** Các bảng `reviews`, `banners`, `reading_progress` (chứa flag favorite) đã sẵn sàng.
- **Frontend:** Cần tận dụng lại các fragment `navbar`, `footer` và style có sẵn của template Admin/User.

## 5. Rủi ro & Giải pháp
- **Rủi ro:** Logic "Verified Purchase" phức tạp (check order history).
    - *Giải pháp:* Tạm thời cho phép đánh giá tất cả, nhưng gắn nhãn "Đã mua" nếu tìm thấy order.
- **Rủi ro:** Upload ảnh banner bị lỗi permission/path.
    - *Giải pháp:* Test kỹ `FileStorageService` với đường dẫn tuyệt đối trước.

---
*Kế hoạch này được lập dựa trên cấu trúc dự án hiện tại và các file controller đã tồn tại.*

