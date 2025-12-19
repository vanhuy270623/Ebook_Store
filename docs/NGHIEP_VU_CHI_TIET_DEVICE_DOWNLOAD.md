# MÔ TẢ CHI TIẾT NGHIỆP VỤ: GIỚI HẠN THIẾT BỊ & TẢI XUỐNG

Tài liệu này mô tả chi tiết logic nghiệp vụ (Business Logic) cần thực hiện để đảm bảo tính thực tế và bảo mật cho hệ thống Ebook Store, tập trung vào các câu hỏi về giới hạn thiết bị và tải xuống.

## 1. Nghiệp vụ Giới hạn Thiết bị (Device Limiting)

Trong các hệ thống nội dung số (như Netflix, Spotify, K+), "Giới hạn thiết bị" thường được hiểu theo hai cách:
1.  **Concurrent Streams:** Số người xem/nghe *cùng một lúc*.
2.  **Registered Devices:** Số thiết bị *được phép đăng nhập/tải nội dung*.

**Đối với dự án Ebook Store (Web App), chúng ta chọn mô hình "Registered Devices" (Thiết bị đã đăng ký)** vì dễ kiểm soát hơn với giao thức HTTP stateless.

### 1.1. Logic Đăng ký Thiết bị (Device Registration)
Mỗi khi người dùng đăng nhập thành công trên một trình duyệt/máy mới, hệ thống sẽ coi đó là một "Thiết bị mới".

*   **Định danh thiết bị:** Sử dụng `User-Agent` kết hợp với một `UUID` được sinh ra và lưu vào `LocalStorage` (hoặc Cookie) của trình duyệt client.
    *   Nếu client gửi lên `deviceId` cũ -> Là thiết bị cũ.
    *   Nếu client không gửi `deviceId` -> Là thiết bị mới -> Sinh `deviceId` mới.

### 1.2. Kịch bản: Người dùng gói FREE (Giới hạn 1 thiết bị)
**Tình huống:** User đang đăng nhập trên Laptop (Device A). User cố gắng đăng nhập trên Điện thoại (Device B).

**Quy trình xử lý chuẩn (Recommended Flow):**

1.  **Bước 1: User nhập user/pass trên Điện thoại (Device B).**
2.  **Bước 2: Server kiểm tra thông tin đăng nhập.**
    *   Nếu sai -> Báo lỗi sai mật khẩu.
    *   Nếu đúng -> Sang bước 3.
3.  **Bước 3: Server kiểm tra Device ID.**
    *   Server check xem `deviceId` của Điện thoại này đã có trong bảng `user_devices` của user chưa?
    *   *Kết quả:* Chưa (vì đây là lần đầu dùng điện thoại).
4.  **Bước 4: Server kiểm tra Giới hạn (Quota Check).**
    *   Đếm số thiết bị đang `active` của user: `count = 1` (là cái Laptop).
    *   Lấy giới hạn gói FREE: `limit = 1`.
    *   So sánh: `count (1) >= limit (1)` -> **VƯỢT QUÁ GIỚI HẠN**.
5.  **Bước 5: Xử lý xung đột (Conflict Resolution).**
    *   **Phương án A (Chặn cứng - Dễ làm nhất):**
        *   Trả về lỗi login: *"Bạn đã đạt giới hạn 1 thiết bị. Vui lòng đăng xuất trên thiết bị cũ hoặc vào trang quản lý để xóa thiết bị."*
        *   User không thể vào được.
    *   **Phương án B (Tự động Kick - Giống Spotify):**
        *   Hệ thống tự động `deactivate` thiết bị cũ nhất (Laptop).
        *   Đăng ký thiết bị mới (Điện thoại).
        *   Cho phép đăng nhập.
        *   *Hệ quả:* Khi User quay lại Laptop, session cũ bị hủy, yêu cầu đăng nhập lại -> Lặp lại quy trình.
    *   **Phương án C (Hỏi người dùng - Tốt nhất cho UX):**
        *   Trả về mã lỗi đặc biệt (VD: `DEVICE_LIMIT_REACHED`).
        *   Frontend hiện popup: *"Tài khoản của bạn chỉ dùng được 1 thiết bị. Bạn có muốn đăng xuất thiết bị cũ để vào thiết bị này không?"*
        *   Nếu User chọn "Có" -> Gọi API `ForceLogin` -> Server xóa thiết bị cũ, thêm thiết bị mới.

**👉 Đề xuất cho dự án này:** Chọn **Phương án A (Chặn cứng)** hoặc **Phương án B (Tự động Kick)** để đơn giản hóa code. Nếu làm Phương án A, cần cung cấp giao diện để User tự xóa thiết bị cũ.

---

## 2. Nghiệp vụ Tải xuống (Secure Download)

Khác với việc đọc online (chỉ cần load nội dung ra trình duyệt), tải xuống là trao file vật lý cho người dùng. Rủi ro là người dùng lấy link đó chia sẻ cho cả thế giới.

### 2.1. Quy tắc Quyền hạn (Authorization Rules)
Trước khi cho tải, phải kiểm tra 3 lớp điều kiện:

1.  **Lớp 1: Authentication (Là ai?)**
    *   User phải đang đăng nhập.
2.  **Lớp 2: Ownership (Có quyền không?)**
    *   **Sách Mua lẻ (Retail):** User đã mua đơn hàng thành công (`Order status = COMPLETED`). -> **ĐƯỢC TẢI**.
    *   **Sách Gói VIP (Subscription):**
        *   User đang có gói VIP Active.
        *   Sách nằm trong danh mục VIP.
        *   *Tuy nhiên:* Thông thường nghiệp vụ Ebook **KHÔNG CHO TẢI FILE** với gói VIP (vì user tải xong hủy gói thì sao?). Gói VIP thường chỉ cho **Đọc Offline trên App** (có mã hóa) hoặc **Chỉ đọc Online**.
        *   *Nếu dự án bắt buộc cho tải:* Phải chấp nhận rủi ro user giữ file mãi mãi.
3.  **Lớp 3: Asset Availability (Có file không?)**
    *   Sách có file PDF/EPUB gốc không?
    *   Cờ `isDownloadable` của sách có bật không?

### 2.2. Quy trình Kỹ thuật (Technical Flow)
Tuyệt đối không trả về đường dẫn tĩnh kiểu `http://domain.com/uploads/book.pdf`.

**Luồng đúng:**
1.  User bấm nút "Tải về".
2.  Browser gọi `GET /api/books/download/{bookId}`.
3.  **Controller:**
    *   Lấy User hiện tại.
    *   Kiểm tra quyền (như mục 2.1).
    *   Kiểm tra Device Limit (nếu muốn chặt chẽ: chỉ cho tải trên thiết bị đã đăng ký).
4.  **Service:**
    *   Đọc file từ ổ đĩa (`F:/datn_uploads/...`).
    *   Tạo `InputStreamResource`.
5.  **Response:**
    *   Header `Content-Type: application/pdf`.
    *   Header `Content-Disposition: attachment; filename="HarryPotter_User123.pdf"`.
    *   Body: Stream dữ liệu file.

---

## 3. Các tình huống thực tế khác (Edge Cases)

### 3.1. User mua sách rồi nhưng sau đó bị khóa tài khoản?
*   **Nghiệp vụ:** Không đăng nhập được -> Không tải lại được.
*   **File đã tải:** User vẫn đọc được file cũ đã tải về máy (trừ khi file có DRM check online - quá phức tạp cho đồ án).

### 3.2. User đang tải thì rớt mạng?
*   **Nghiệp vụ:** User bấm tải lại. Server check lại quyền từ đầu.

### 3.3. Gói VIP hết hạn khi đang đọc?
*   **Đọc Online:** Khi chuyển trang hoặc load chương mới, API check quyền sẽ trả về lỗi -> Redirect về trang giới thiệu gói VIP.
*   **Tải xuống:** Nút tải xuống bị mờ đi hoặc bấm vào báo lỗi "Gói VIP của bạn đã hết hạn".

### 3.4. User dùng trình duyệt ẩn danh (Incognito)?
*   **Nghiệp vụ:** Trình duyệt ẩn danh không lưu LocalStorage/Cookie cũ -> Hệ thống nhận diện là **Thiết bị mới**.
*   **Hệ quả:** Nếu gói Free (limit 1), user vừa đăng nhập thường, vừa đăng nhập ẩn danh -> Sẽ bị tính là 2 thiết bị -> Bị chặn. Đây là hành vi đúng.

## 4. Tóm tắt hành động cần làm (Action Items)

1.  **Database:** Đảm bảo bảng `user_devices` có cột `last_active` để biết thiết bị nào cũ nhất.
2.  **API Login:** Thêm logic:
    *   Check `user_devices`.
    *   Nếu full -> Xóa thiết bị cũ nhất (Auto-kick) HOẶC Trả lỗi (Strict).
3.  **API Download:** Viết endpoint stream file, tuyệt đối không return String URL.

