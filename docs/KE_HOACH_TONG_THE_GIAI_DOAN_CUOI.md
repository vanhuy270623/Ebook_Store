# KẾ HOẠCH TỔNG THỂ HOÀN THIỆN DỰ ÁN (16/12 - 21/12)

## 1. Mục tiêu
Hoàn thiện toàn bộ hệ thống Ebook Store để sẵn sàng nghiệm thu/triển khai. Kế hoạch bao gồm cả việc lấp các lỗ hổng nghiệp vụ cốt lõi (Download, Device Limit) và các chức năng bổ trợ (Review, Banner).

**Thời gian:** 6 ngày (16/12/2025 - 21/12/2025).

## 2. Trạng thái hiện tại (Status Update)

| Phân hệ | Chức năng | Trạng thái | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Core** | Đăng ký/Đăng nhập | ✅ Hoàn thành | |
| **Core** | Mua hàng/Thanh toán | ✅ Hoàn thành | Đã kiểm tra logic gói đơn hàng. |
| **Core** | Đọc Online (PDF/EPUB) | ✅ Hoàn thành | |
| **Core** | Yêu thích (Favorites) | ✅ Hoàn thành | Đã có API và logic lưu trữ. |
| **Core** | **Tải xuống (Download)** | ⚠️ **Chưa có** | Cần làm gấp (Secure Endpoint). |
| **Core** | **Giới hạn thiết bị** | ⚠️ **Chưa áp dụng** | Backend có service nhưng chưa chặn thực tế. |
| **Aux** | Đánh giá (Review) | ❌ Chưa có | Cần làm cả User & Admin side. |
| **Aux** | Quảng cáo (Banner) | ❌ Chưa có | Cần làm cả User & Admin side. |

## 3. Phân loại ưu tiên (Priorities)

1.  **P1 - Critical (Sống còn):**
    *   **Secure Download:** Bảo vệ file sách, chỉ cho tải khi đúng quyền.
    *   **Device Limit:** Ngăn chặn chia sẻ tài khoản trái phép (Business Rule).
2.  **P2 - High (Trải nghiệm & Marketing):**
    *   **Reviews:** Tăng độ tin cậy cho sách.
    *   **Banners:** Trang trí trang chủ, chạy chiến dịch.
3.  **P3 - Polish (Tinh chỉnh):**
    *   Watermark file (nếu kịp).
    *   UI/UX cleanup.

## 4. Lịch trình chi tiết (Daily Schedule)

### Ngày 1 (16/12): Bảo mật Tải xuống & Giới hạn thiết bị (Backend)
*   **Mục tiêu:** API tải xuống an toàn và cơ chế đếm thiết bị hoạt động.
*   **Công việc:**
    1.  **Download Controller:**
        *   Tạo `GET /api/books/download/{id}`.
        *   Logic: Check Login -> Check Quyền (Mua/VIP) -> Check Device Limit -> Stream File.
        *   *Lưu ý:* Không lộ đường dẫn thực (`F:/datn_uploads/...`).
    2.  **Device Enforcement:**
        *   Tích hợp `UserDeviceService.registerDevice()` vào API Login và API Download.
        *   Chặn nếu `activeDeviceCount > limit`.

### Ngày 2 (17/12): Giao diện Tải xuống & Quản lý thiết bị (Frontend)
*   **Mục tiêu:** Người dùng thao tác được các chức năng backend đã làm hôm qua.
*   **Công việc:**
    1.  **UI Tải xuống:**
        *   Gắn link vào nút "Tải về" trong trang chi tiết sách.
        *   Xử lý lỗi: Hiển thị thông báo nếu hết lượt tải hoặc quá số thiết bị.
    2.  **UI Quản lý thiết bị:**
        *   Tạo trang `user/profile/devices`.
        *   Hiển thị danh sách thiết bị đang đăng nhập.
        *   Nút "Đăng xuất thiết bị này" (Remote Logout) để giải phóng slot.

### Ngày 3 (18/12): Hệ thống Đánh giá (Reviews)
*   **Mục tiêu:** User đánh giá sách, Admin quản lý nội dung.
*   **Công việc:**
    1.  **User Side:**
        *   API `POST /api/reviews`.
        *   UI: Form đánh giá sao + bình luận ở trang chi tiết sách.
        *   Logic: Chỉ hiện form nếu user đã mua hoặc đã đọc.
    2.  **Admin Side:**
        *   Trang `admin/reviews`: List, Approve, Delete.
        *   Logic: Ẩn các review chưa duyệt (hoặc hiện nhưng gắn cờ chờ duyệt).

### Ngày 4 (19/12): Hệ thống Banner & Quảng cáo
*   **Mục tiêu:** Admin thay đổi được banner trang chủ mà không cần sửa code.
*   **Công việc:**
    1.  **Admin Side:**
        *   Trang `admin/banners`: Upload ảnh, đặt Link, chọn Vị trí.
    2.  **User Side:**
        *   Sửa `home.html`: Load banner động từ DB.
        *   Thêm hiệu ứng Slider/Carousel.

### Ngày 5 (20/12): Rà soát & Bảo mật nâng cao (DRM Basic)
*   **Mục tiêu:** Đảm bảo không có lỗ hổng bảo mật ngớ ngẩn.
*   **Công việc:**
    1.  **Chặn truy cập trực tiếp:**
        *   Cấu hình Spring Security chặn `/datn_uploads/**` (nếu đang public).
    2.  **Watermark (Optional):**
        *   Thử nghiệm thư viện PDFBox để thêm dòng text "Licensed to [User]" vào file PDF khi tải.
    3.  **Kiểm tra hồi quy (Regression Test):**
        *   Test lại chức năng Yêu thích, Mua hàng xem có bị ảnh hưởng bởi code mới không.

### Ngày 6 (21/12): Tổng kiểm tra & Đóng gói
*   **Mục tiêu:** Phiên bản Release Candidate.
*   **Công việc:**
    1.  **End-to-End Testing:** Chạy luồng từ Đăng ký -> Mua VIP -> Đọc -> Tải -> Đánh giá -> Quản lý thiết bị.
    2.  **Data Cleanup:** Xóa dữ liệu test rác, reset lại các ID nếu cần.
    3.  **Documentation:** Cập nhật file hướng dẫn cài đặt/deploy.

## 5. Phân công tài nguyên code
*   **Controller cần tạo mới/sửa:**
    *   `DownloadController` (Mới - Quan trọng nhất).
    *   `UserDeviceController` (Mới).
    *   `ReviewController` (User side).
    *   `AdminBannerController` (Hoàn thiện).
*   **View cần tạo mới/sửa:**
    *   `user/profile/devices.html`.
    *   `user/books/view.html` (Thêm phần review & nút download xịn).
    *   `admin/banners/**`.

---
*Kế hoạch này thay thế cho các kế hoạch rời rạc trước đó.*

