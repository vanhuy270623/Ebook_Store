# BÁO CÁO TỔNG HỢP NGHIỆP VỤ DỰ ÁN EBOOK STORE
*Ngày tạo: 16/12/2025*
*Người thực hiện: GitHub Copilot*

## 1. Tổng quan dự án
Dự án xây dựng hệ thống bán và đọc sách điện tử (Ebook) trực tuyến.
- **Công nghệ:** Spring Boot (Backend), Thymeleaf (Frontend), MySQL (Database).
- **Mô hình kinh doanh:** Kết hợp bán lẻ (Retail) và gói thành viên (Subscription).

## 2. Các chức năng đã hoàn thiện (Current State)

Dựa trên mã nguồn và tài liệu trong `docs/`, các module sau đã hoạt động:

### 2.1. Quản lý Người dùng & Xác thực (Authentication)
- Đăng ký, Đăng nhập, Quên mật khẩu.
- Phân quyền: Admin, User.
- Quản lý thông tin cá nhân.

### 2.2. Quản lý Sách & Danh mục (Catalog)
- Hiển thị danh sách sách, lọc theo danh mục, tìm kiếm.
- Chi tiết sách: Thông tin, giá bán, đánh giá.
- Upload và quản lý file sách (PDF, EPUB) ở trang Admin.

### 2.3. Mua hàng & Thanh toán (Commerce)
- Giỏ hàng (Cart): Thêm/xóa sản phẩm.
- Đặt hàng (Order): Tạo đơn hàng mua lẻ hoặc mua gói Subscription.
- Thanh toán: Tích hợp VNPAY và QR Code (mô phỏng).
- Logic phân quyền sách: `FREE`, `PURCHASE`, `SUBSCRIPTION`, `BOTH`.

### 2.4. Đọc sách trực tuyến (Online Reading)
- **PDF Viewer:** Tích hợp PDF.js.
- **EPUB Viewer:** Tích hợp ePub.js.
- **Reading Progress:** Lưu lại trang đang đọc, % hoàn thành.
- **Kiểm tra quyền:** Đã có logic `canUserAccessBook` để chặn người dùng không có quyền truy cập vào Viewer.

### 2.5. Hạ tầng Quản lý Thiết bị (Backend Foundation)
- Đã có Entity `UserDevice` và Service `UserDeviceService`.
- Đã có logic kiểm tra số lượng thiết bị (`hasReachedDeviceLimit`).
- **Tuy nhiên:** Chưa được áp dụng triệt để vào luồng người dùng (xem phần 3).

---

## 3. Các chức năng còn thiếu hoặc chưa hoàn thiện (Gap Analysis)

Đây là các vấn đề trọng tâm cần xử lý để đáp ứng đúng nghiệp vụ "Bán Ebook có giới hạn thiết bị và tải xuống".

### 3.1. Chức năng Tải xuống (Download) - ⚠️ MỨC ĐỘ: NGHIÊM TRỌNG
- **Hiện trạng:**
    - Giao diện có nút "Tải về" (icon download) nhưng chưa có Controller xử lý logic tải file vật lý về máy.
    - `ReadingController` chỉ xử lý việc mở sách xem online (`/reading/book/{id}`), không có endpoint `/reading/download/{id}`.
    - File sách đang được lưu trên server (folder `datn_uploads`), nếu lộ đường dẫn gốc, user có thể tải không cần quyền.
- **Yêu cầu nghiệp vụ thiếu:**
    - Endpoint tải xuống bảo mật (Secure Download Endpoint).
    - Kiểm tra quyền tải xuống (có thể khác quyền đọc online - ví dụ sách Subscription chỉ cho đọc online, sách Mua lẻ mới cho tải).
    - Cơ chế bảo vệ file (Signed URL hoặc Token có thời hạn) để link tải không chia sẻ được.

### 3.2. Giới hạn Thiết bị (Device Limiting) - ⚠️ MỨC ĐỘ: CAO
- **Hiện trạng:**
    - Code Service đã có (`UserDeviceServiceImpl`) nhưng **chưa được gọi** ở các điểm chốt chặn quan trọng.
    - Chưa có cơ chế tự động đăng ký thiết bị khi User đăng nhập hoặc bắt đầu đọc sách.
    - Chưa có giao diện cho User quản lý thiết bị (xem danh sách thiết bị đang đăng nhập, xóa thiết bị cũ).
- **Rủi ro:** User có thể chia sẻ tài khoản cho 100 người khác cùng dùng mà hệ thống không chặn.

### 3.3. Tính toàn vẹn dữ liệu & Logic quyền (Data Integrity)
- **Hiện trạng:**
    - File `CLEANUP_BASED_ON_REAL_DATA.sql` cho thấy có trường hợp User gói FREE vẫn có `ReadingProgress` của sách VIP.
    - Điều này chứng tỏ logic kiểm tra quyền (`canUserAccessBook`) có thể bị bỏ qua hoặc không hoạt động đúng trong một số trường hợp (ví dụ: gọi API trực tiếp).
- **Yêu cầu:** Cần Unit Test chặt chẽ cho service `ReadingProgressService` để đảm bảo không bao giờ tạo record nếu không có quyền.

### 3.4. Bảo vệ bản quyền (DRM / Watermarking) - ⚠️ MỨC ĐỘ: TRUNG BÌNH
- **Hiện trạng:** Chưa có bất kỳ cơ chế nào đóng dấu bản quyền lên file PDF/EPUB khi tải về hoặc xem.
- **Yêu cầu:** Ít nhất nên có Watermark (đóng dấu tên người mua + ID đơn hàng) lên file PDF khi tải về để hạn chế phát tán.

---

## 4. Kế hoạch hành động đề xuất (Action Plan)

Để hoàn thiện dự án, cần thực hiện các bước sau theo thứ tự ưu tiên:

### Giai đoạn 1: Vá lỗ hổng nghiệp vụ (Ngay lập tức)
1.  **Implement Download Controller:**
    - Tạo endpoint `/api/books/download/{bookId}`.
    - Kiểm tra quyền sở hữu (Đã mua hoặc có gói VIP cho phép tải).
    - Trả về `Resource` stream thay vì đường dẫn file tĩnh.
2.  **Kích hoạt Device Limit:**
    - Trong `AuthController` (Login), gọi `UserDeviceService.registerDevice()`.
    - Nếu vượt quá giới hạn -> Chặn đăng nhập hoặc yêu cầu user xóa thiết bị cũ.
    - Thêm trang "Quản lý thiết bị" trong phần User Profile.

### Giai đoạn 2: Củng cố dữ liệu & Bảo mật
1.  **Fix Logic Reading Progress:**
    - Thêm validation trong `ReadingProgressService.save()`: Nếu user không có quyền -> Throw Exception.
    - Chạy script cleanup dữ liệu rác (`CLEANUP_BASED_ON_REAL_DATA.sql`).
2.  **Bảo mật File:**
    - Cấu hình Spring Security chặn truy cập trực tiếp vào thư mục `/datn_uploads/` qua URL tĩnh. Mọi truy cập file phải qua Controller.

### Giai đoạn 3: Nâng cao trải nghiệm
1.  **Watermarking (Optional):** Thêm thư viện đóng dấu PDF (như iText hoặc Apache PDFBox) để ghi tên user vào footer trang sách khi tải về.

## 5. Kết luận
Dự án đã hoàn thành khoảng **70%** khối lượng công việc (Core features đã xong). 30% còn lại nằm ở các tính năng nâng cao về bảo mật, kiểm soát thiết bị và tải xuống - đây là những tính năng "sống còn" của một hệ thống kinh doanh nội dung số thực tế.

