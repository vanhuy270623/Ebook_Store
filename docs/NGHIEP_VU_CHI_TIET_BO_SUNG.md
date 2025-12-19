# MÔ TẢ CHI TIẾT NGHIỆP VỤ BỔ SUNG (SUPPLEMENTARY BUSINESS LOGIC)

Tài liệu này mô tả chi tiết các luồng nghiệp vụ quan trọng khác ngoài "Tải xuống" và "Thiết bị", nhằm đảm bảo hệ thống vận hành chặt chẽ, tránh lỗ hổng logic.

## 1. Nghiệp vụ Gói Hội Viên (Subscription Lifecycle)

Quản lý vòng đời của một gói đăng ký (VIP) là phần phức tạp nhất trong logic kinh doanh.

### 1.1. Đăng ký & Kích hoạt
*   **Thời điểm kích hoạt:** Ngay khi thanh toán thành công (`PaymentStatus = COMPLETED`).
*   **Tính toán ngày hết hạn:** `EndDate = StartDate + Duration` (Ví dụ: 30 ngày hoặc 1 năm).
*   **Xử lý khi User đang có gói:**
    *   *Trường hợp 1 (Gia hạn - Extend):* Nếu User mua cùng một gói (VD: đang dùng gói Tháng, mua thêm gói Tháng) -> `NewEndDate = OldEndDate + 30 days`.
    *   *Trường hợp 2 (Nâng cấp/Đổi gói - Switch):* Nếu User đang dùng gói Tháng, muốn chuyển sang gói Năm -> Thường sẽ kết thúc gói cũ ngay lập tức và bắt đầu gói mới, HOẶC cộng dồn thời gian (phức tạp).
    *   **👉 Đề xuất:** Chỉ cho phép mua gói mới khi gói cũ đã hết hạn hoặc sắp hết hạn (còn dưới 3 ngày).

### 1.2. Hủy gói (Cancellation)
*   **Hành vi:** User bấm "Hủy gói".
*   **Logic đúng:** Không hoàn tiền. User **VẪN GIỮ QUYỀN TRUY CẬP** cho đến ngày hết hạn (`end_date`).
*   **Trạng thái:** Chuyển `is_auto_renew` = `false` (nếu có tính năng tự gia hạn). Hệ thống không xóa Subscription ngay, chỉ đánh dấu.

### 1.3. Hết hạn (Expiration)
*   **Cơ chế:** Job chạy ngầm (Scheduled Task) mỗi đêm hoặc kiểm tra Real-time mỗi khi User truy cập sách.
*   **Hành động:**
    *   Nếu `CurrentDate > EndDate`: Thu hồi quyền truy cập sách VIP.
    *   Sách đang đọc dở: Vẫn giữ `ReadingProgress` nhưng khi bấm vào đọc sẽ báo "Vui lòng gia hạn để đọc tiếp".

---

## 2. Nghiệp vụ Đánh giá & Bình luận (Review Integrity)

Để tránh spam và đánh giá ảo, cần áp dụng logic "Verified Purchase" (Đã mua hàng).

### 2.1. Điều kiện được phép đánh giá
User chỉ được hiện nút "Viết đánh giá" hoặc Submit form thành công nếu thỏa mãn **MỘT TRONG CÁC** điều kiện sau:
1.  **Sách Mua lẻ:** Có đơn hàng chứa sách này với trạng thái `COMPLETED`.
2.  **Sách VIP/Free:** Có `ReadingProgress` với `progressPercentage > 10%` (Chứng tỏ đã đọc thực sự).

### 2.2. Quy trình Kiểm duyệt (Moderation)
*   **Phương án A (Hậu kiểm - Post-moderation):** Review hiện ngay lập tức. Admin có công cụ quét từ khóa tục tĩu (Bad words filter) và nút "Ẩn/Xóa" sau. -> **Tốt cho trải nghiệm User.**
*   **Phương án B (Tiền kiểm - Pre-moderation):** Review ở trạng thái `PENDING`. Admin duyệt mới hiện. -> **An toàn nhưng chậm.**
*   **👉 Đề xuất:** Phương án A + Bộ lọc từ khóa cơ bản.

---

## 3. Nghiệp vụ Đồng bộ Tiến độ đọc (Reading Synchronization)

Khi User đọc trên nhiều thiết bị (Sáng đọc trên PC, Tối đọc trên Mobile), tiến độ phải khớp nhau.

### 3.1. Cơ chế "Last Write Wins" (Ghi đè mới nhất)
*   Mỗi khi User lật trang hoặc đóng sách, Client gửi API `UPDATE_PROGRESS` kèm `timestamp`.
*   Server so sánh `timestamp` gửi lên với `last_read_at` trong DB.
    *   Nếu `new_timestamp > old_timestamp`: Cập nhật trang mới.
    *   Nếu `new_timestamp < old_timestamp`: Bỏ qua (Do mạng lag hoặc thiết bị cũ gửi chậm).

### 3.2. Xử lý xung đột khi mở sách
*   Khi User mở sách (`GET /reading/book/{id}`):
    *   Server trả về `lastPage` mới nhất.
    *   Client **BẮT BUỘC** phải nhảy tới trang đó, không được dùng cache cũ của trình duyệt.

---

## 4. Nghiệp vụ Giỏ hàng & Thanh toán (Cart & Checkout)

### 4.1. Logic thêm vào giỏ (Add to Cart)
*   **Sách đã mua:** Không cho thêm vào giỏ. Nút "Thêm vào giỏ" chuyển thành "Đọc ngay".
*   **Sách đã có trong giỏ:** Không thêm trùng, chỉ thông báo "Sách đã có trong giỏ".
*   **Sách VIP:** Nếu User đang có gói VIP active, hiển thị thông báo "Bạn đang có gói VIP, có thể đọc sách này miễn phí" (nhưng vẫn cho mua nếu User muốn sở hữu vĩnh viễn).

### 4.2. Mã giảm giá (Coupons)
*   **Quy tắc áp dụng:**
    *   Chỉ 1 mã/đơn hàng.
    *   Kiểm tra `min_order_value` (Giá trị đơn tối thiểu).
    *   Kiểm tra `usage_limit` (Số lượt dùng tối đa của mã).
    *   Kiểm tra `user_usage_limit` (Mỗi người chỉ dùng được 1 lần).
*   **Hoàn lại mã:** Nếu đơn hàng bị Hủy (Cancelled) hoặc Lỗi thanh toán -> Mã giảm giá phải được hoàn lại lượt dùng (Revert usage).

---

## 5. Nghiệp vụ Bảo mật File (File Security - Bổ sung)

Ngoài việc check quyền tải xuống, cần bảo vệ chính các file tĩnh (nếu có).

### 5.1. Chặn truy cập trực tiếp (Direct Access Prevention)
*   Thư mục `F:/datn_uploads/` **KHÔNG ĐƯỢC** map trực tiếp ra URL tĩnh (như `/uploads/...`).
*   Tất cả file sách (PDF/EPUB) phải nằm ngoài thư mục `src/main/resources/static` hoặc `webapp`.
*   Việc phục vụ file (Serving files) phải đi qua Controller (`StreamingResponseBody`).

### 5.2. Hotlink Protection (Chống trộm link)
*   Kiểm tra Header `Referer` trong request tải xuống.
*   Nếu request không đến từ domain của Web App (ví dụ: user copy link paste vào tab mới hoặc tool download) -> Có thể chặn hoặc yêu cầu xác thực lại (Login redirect).

---

## 6. Tóm tắt các điểm còn thiếu trong Code hiện tại

Dựa trên phân tích code, các điểm sau cần bổ sung logic:

1.  **Review:** Chưa có logic check "Verified Purchase" (đã mua/đã đọc).
2.  **Subscription:** Chưa xử lý logic "Gia hạn" (cộng dồn ngày) - hiện tại có thể đang tạo mới đè lên cũ.
3.  **Cart:** Cần check kỹ việc User mua lại sách đã sở hữu (Duplicate Purchase Prevention).
4.  **Sync:** API đọc sách cần trả về đúng vị trí mới nhất, Frontend cần xử lý việc jump to page khi load.

