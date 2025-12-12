# Báo Cáo Tiến Độ - Thanh Toán QR Code Bank Transfer
**Ngày:** 10/12/2025  
**Chức năng:** Thanh toán quét mã QR (Bank Transfer)

---

## 📋 Tổng Quan
Triển khai tính năng thanh toán bằng cách quét mã QR ngân hàng cho cả đơn hàng sách lẻ và gói đăng ký.

---

## ✅ Các Công Việc Đã Hoàn Thành

### 1. Backend - PaymentController

#### 1.1 Endpoint Hiển Thị Trang QR Code
- **Route:** `GET /payment/bank-transfer?orderId={orderId}`
- **Chức năng:** 
  - Lấy thông tin đơn hàng theo orderId
  - Tạo URL QR Code sử dụng VietQR API
  - Truyền dữ liệu đơn hàng và QR code đến view
  - Xử lý cho cả đơn hàng sách lẻ và gói đăng ký
  
- **Thông tin QR Code:**
  - Bank: TP Bank
  - Account Number: 79999276999
  - Account Name: CONG TY EBOOK STORE
  - Format: VietQR API (https://img.vietqr.io)

#### 1.2 Endpoint Xác Nhận Đã Chuyển Khoản
- **Route:** `POST /payment/bank-transfer/confirm`
- **Request Body:** `{"orderId": "order_book_xx"}`
- **Chức năng:**
  - Kiểm tra đơn hàng tồn tại
  - Cập nhật trạng thái đơn hàng thành `WAITING_APPROVAL` (Chờ duyệt)
  - Lưu lại đơn hàng
  - Trả về JSON response với redirectUrl
  
#### 1.3 Endpoint Trang Chờ Duyệt
- **Route:** `GET /payment/bank-transfer/waiting?orderId={orderId}`
- **Chức năng:**
  - Hiển thị trang thông báo đang chờ admin duyệt
  - Truyền thông tin đơn hàng đến view

#### 1.4 API Kiểm Tra Trạng Thái Đơn Hàng (Dự kiến)
- **Route:** `GET /api/order/status?orderId={orderId}` (Đã lên kế hoạch)
- **Mục đích:** Cho phép trang waiting tự động kiểm tra và chuyển hướng khi đơn hàng được duyệt

### 2. Frontend - Templates

#### 2.1 Trang QR Code (`bank-transfer.html`)
- **Vị trí:** `src/main/resources/templates/user/payment/bank-transfer.html`
- **Tính năng:**
  - Hiển thị mã QR Code thanh toán
  - Hiển thị thông tin chuyển khoản (STK, tên TK, ngân hàng, số tiền, nội dung)
  - Hiển thị chi tiết đơn hàng
  - Nút "Tôi đã chuyển khoản"
  - Tích hợp jQuery để gọi API xác nhận

#### 2.2 Trang Chờ Duyệt (`waiting.html`) 
- **Vị trí:** `src/main/resources/templates/user/payment/waiting.html` (Cần tạo)
- **Tính năng dự kiến:**
  - Thông báo đơn hàng đang chờ admin duyệt
  - Tự động kiểm tra trạng thái đơn hàng mỗi 10 giây
  - Tự động chuyển hướng khi đơn hàng được duyệt

### 3. Entity & Enum Updates

#### 3.1 Order.PaymentMethod Enum
Đã có sẵn các giá trị:
- `VNPAY` - Thanh toán VNPay
- `BANK_TRANSFER` - Chuyển khoản ngân hàng
- `COD` - Thanh toán khi nhận hàng (nếu có)

#### 3.2 Order.PaymentStatus Enum
Các trạng thái thanh toán:
- `PENDING` - Chờ thanh toán
- `WAITING_APPROVAL` - Chờ duyệt (sau khi khách xác nhận đã chuyển khoản)
- `PAID` - Đã thanh toán
- `FAILED` - Thất bại
- `CANCELLED` - Đã hủy

---

## 🔄 Quy Trình Thanh Toán Bank Transfer

### Bước 1: Khách hàng đặt hàng
- Tạo đơn hàng với `paymentMethod = BANK_TRANSFER`
- Trạng thái ban đầu: `PENDING`

### Bước 2: Hiển thị QR Code
- Chuyển đến trang `/payment/bank-transfer?orderId=xxx`
- Hiển thị mã QR và thông tin chuyển khoản

### Bước 3: Khách hàng quét mã và chuyển khoản
- Khách hàng sử dụng app ngân hàng quét mã QR
- Thực hiện chuyển khoản

### Bước 4: Khách xác nhận đã chuyển khoản
- Khách bấm nút "Tôi đã chuyển khoản"
- Gọi API `POST /payment/bank-transfer/confirm`
- Cập nhật trạng thái: `WAITING_APPROVAL`

### Bước 5: Chuyển đến trang chờ
- Redirect đến `/payment/bank-transfer/waiting?orderId=xxx`
- Hiển thị thông báo chờ admin duyệt

### Bước 6: Admin duyệt đơn (Backend)
- Admin kiểm tra tiền về tài khoản
- Bấm "Duyệt đơn" trên trang quản trị
- Cập nhật trạng thái: `PAID`

### Bước 7: Hoàn tất
- Trang waiting tự động phát hiện thay đổi
- Chuyển hướng đến trang đơn hàng thành công

---

## 🐛 Các Lỗi Đã Phát Hiện Và Sửa

### Lỗi 1: Template không tìm thấy fragments
**Mô tả:** `Error resolving template [fragments/user_header]`  
**Nguyên nhân:** Sử dụng sai đường dẫn fragment  
**Giải pháp:** Sử dụng `~{user/fragments/header :: header}` thay vì `~{fragments/user_header}`

### Lỗi 2: Property 'price' không tồn tại trong OrderItem
**Mô tả:** `Property or field 'price' cannot be found on object of type 'OrderItem'`  
**Nguyên nhân:** Template sử dụng `item.price` nhưng OrderItem không có field price  
**Giải pháp:** Sử dụng `item.unitPrice` thay vì `item.price`

### Lỗi 3: Data truncated cho column 'payment_status'
**Mô tả:** `Data truncated for column 'payment_status' at row 1`  
**Nguyên nhân:** Giá trị enum `WAITING_APPROVAL` quá dài cho VARCHAR trong database  
**Giải pháp:** Cần kiểm tra và tăng độ dài column payment_status trong DB

### Lỗi 4: QR Code không hiển thị
**Mô tả:** QR Image failed to load  
**Nguyên nhân:** URL QR Code không hợp lệ hoặc có vấn đề với VietQR API  
**Giải pháp:** Kiểm tra URL format và thêm fallback placeholder

### Lỗi 5: Script không tải được
**Mô tả:** `GET http://localhost:2706/user_template/js/script.js 404`  
**Nguyên nhân:** Đường dẫn static resources không đúng  
**Giải pháp:** Xem xét loại bỏ hoặc sử dụng đường dẫn đúng cho static files

### Lỗi 6: API /api/order/status không tồn tại
**Mô tả:** `GET http://localhost:2706/api/order/status?orderId=xxx 404`  
**Nguyên nhân:** Endpoint chưa được tạo  
**Giải pháp:** Cần tạo endpoint trong OrderController

### Lỗi 7: Property 'FREE_ACTIVATION' không tồn tại
**Mô tả:** `Property or field 'FREE_ACTIVATION' cannot be found`  
**Nguyên nhân:** Template orders.html sử dụng enum value không tồn tại  
**Giải pháp:** Xóa hoặc sửa lại điều kiện kiểm tra FREE_ACTIVATION

### Lỗi 8: Chi tiết đơn hàng hiển thị sai
**Mô tả:** Mua 1 sản phẩm nhưng hiển thị 5  
**Nguyên nhân:** Logic lấy orderItems có vấn đề  
**Giải pháp:** Kiểm tra lại cách fetch và hiển thị orderItems

### Lỗi 9: Route /user/order/orders không tồn tại
**Mô tả:** `GET http://localhost:2706/user/order/orders 404`  
**Nguyên nhân:** Có thể thiếu mapping trong OrderController  
**Giải pháp:** Kiểm tra và thêm endpoint nếu cần

### Lỗi 10: Trạng thái hiển thị không đúng
**Mô tả:** Đơn hàng WAITING_APPROVAL hiển thị "Thất bại" thay vì "Chờ duyệt"  
**Giải pháp:** Cần cập nhật logic hiển thị trạng thái trong template orders.html

---

## 📝 Công Việc Còn Lại

### 1. Backend
- [ ] Tạo endpoint `GET /api/order/status` trong OrderController
- [ ] Kiểm tra và sửa lỗi database schema cho payment_status
- [ ] Thêm logic duyệt đơn hàng cho admin (nếu chưa có)
- [ ] Xử lý logic phân biệt rõ ràng PENDING vs WAITING_APPROVAL

### 2. Frontend
- [ ] Tạo template `waiting.html` với auto-refresh
- [ ] Sửa lỗi hiển thị trạng thái trong `orders.html`
- [ ] Sửa lỗi đếm số lượng sản phẩm trong chi tiết đơn hàng
- [ ] Xử lý fallback khi QR code không tải được
- [ ] Kiểm tra và sửa các đường dẫn static resources

### 3. Testing
- [ ] Test full flow từ đặt hàng đến duyệt đơn
- [ ] Test trường hợp QR code không load
- [ ] Test auto-refresh trang waiting
- [ ] Test hiển thị đúng trạng thái đơn hàng

### 4. Database
- [ ] Kiểm tra và tăng độ dài column `payment_status`
- [ ] Verify các enum values trong DB khớp với code

---

## 🎯 Tối Ưu Hóa Trong Tương Lai

1. **Webhook từ ngân hàng:** Tích hợp webhook để tự động xác nhận thanh toán khi có tiền về
2. **OCR hóa đơn:** Tự động đọc và xác minh ảnh hóa đơn chuyển khoản
3. **Notification:** Gửi thông báo email/SMS khi đơn hàng được duyệt
4. **Admin Dashboard:** Trang quản lý danh sách đơn chờ duyệt dễ dàng hơn

---

## 📚 Tài Liệu Tham Khảo

- VietQR API: https://vietqr.io
- QR Code Format: https://img.vietqr.io/image/{BANK}-{ACCOUNT_NUMBER}-{TEMPLATE}.png

---

## 🔗 Related Files

### Backend
- `PaymentController.java` - Controller xử lý thanh toán
- `OrderController.java` - Controller quản lý đơn hàng
- `Order.java` - Entity đơn hàng
- `OrderItem.java` - Entity chi tiết đơn hàng

### Frontend
- `bank-transfer.html` - Trang hiển thị QR code
- `waiting.html` - Trang chờ duyệt (cần tạo)
- `orders.html` - Trang danh sách đơn hàng

---

## ⚠️ Lưu Ý Quan Trọng

1. **Bảo mật:** Không để lộ thông tin tài khoản ngân hàng trong log
2. **Validation:** Luôn validate orderId từ client
3. **Transaction:** Sử dụng @Transactional cho các thao tác cập nhật đơn hàng
4. **Error Handling:** Xử lý đầy đủ các trường hợp lỗi và trả về message rõ ràng

---

**Người thực hiện:** GitHub Copilot  
**Trạng thái:** Đang phát triển (70% hoàn thành)

