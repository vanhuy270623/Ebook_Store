# 🔧 HƯỚNG DẪN SỬA LỖI VÀ TEST CHỨC NĂNG TẢI XUỐNG

## ✅ ĐÃ SỬA

### 1. Lỗi: `Swal is not defined`
**Nguyên nhân:** SweetAlert2 chưa được load vào trang.

**Đã sửa:**
- ✅ Thêm SweetAlert2 CSS vào `head.html`: 
  ```html
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/sweetalert2@11/dist/sweetalert2.min.css">
  ```
- ✅ Thêm SweetAlert2 JS vào `scripts.html`:
  ```html
  <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
  ```
- ✅ Thêm check trong `book-download.js` để tránh lỗi nếu Swal chưa load

### 2. Lỗi: `main.js 500 Error`
**Nguyên nhân:** File `main.js` không tồn tại, tên thật là `custom.js`.

**Đã sửa:**
- ✅ Đổi từ `main.js` → `custom.js` trong `scripts.html`

### 3. Lỗi: Nút tải xuống không hoạt động
**Nguyên nhân:** Database không có order mua sách lẻ (bảng `order_items` trống).

**Cần làm:** Chạy SQL script để tạo test data (xem phần bên dưới).

---

## 🧪 CÁCH TEST

### BƯỚC 1: Restart Server
```bash
# Dừng server Spring Boot (Ctrl+C trong terminal)
# Chạy lại server
```

### BƯỚC 2: Clear Browser Cache
```
1. Mở DevTools (F12)
2. Right-click vào nút Reload
3. Chọn "Empty Cache and Hard Reload"
HOẶC
Ctrl + Shift + Delete → Clear Cache
```

### BƯỚC 3: Insert Test Data vào Database

**Chạy SQL này trong MySQL Workbench hoặc phpMyAdmin:**

```sql
-- User user_normal_01 mua sách book_02 (Đắc Nhân Tâm)
INSERT INTO `orders` (`order_id`, `user_id`, `subscription_id`, `order_type`, `total_amount`, `payment_status`, `payment_method`, `transaction_id`, `start_date`, `end_date`, `created_at`) VALUES
('ORDER_BOOK_001', 'user_normal_01', NULL, 'BOOK', 120000.00, 'COMPLETED', 'VNPAY', 'TXN_001', NULL, NULL, NOW());

INSERT INTO `order_items` (`order_item_id`, `order_id`, `book_id`, `price_at_purchase`) VALUES
('OI_001', 'ORDER_BOOK_001', 'book_02', 120000.00);

-- User user_normal_01 mua sách book_03 (Mắt biếc - BOTH)
INSERT INTO `orders` (`order_id`, `user_id`, `subscription_id`, `order_type`, `total_amount`, `payment_status`, `payment_method`, `transaction_id`, `start_date`, `end_date`, `created_at`) VALUES
('ORDER_BOOK_002', 'user_normal_01', NULL, 'BOOK', 90000.00, 'COMPLETED', 'BANK_TRANSFER', 'TXN_002', NULL, NULL, NOW());

INSERT INTO `order_items` (`order_item_id`, `order_id`, `book_id`, `price_at_purchase`) VALUES
('OI_002', 'ORDER_BOOK_002', 'book_03', 90000.00);

-- User user_normal_01 mua sách book_07 (Dế Mèn - BOTH)
INSERT INTO `orders` (`order_id`, `user_id`, `subscription_id`, `order_type`, `total_amount`, `payment_status`, `payment_method`, `transaction_id`, `start_date`, `end_date`, `created_at`) VALUES
('ORDER_BOOK_003', 'user_normal_01', NULL, 'BOOK', 75000.00, 'PAID', 'VNPAY', 'TXN_003', NULL, NULL, NOW());

INSERT INTO `order_items` (`order_item_id`, `order_id`, `book_id`, `price_at_purchase`) VALUES
('OI_003', 'ORDER_BOOK_003', 'book_07', 75000.00);
```

### BƯỚC 4: Login và Test

1. **Login** với `user_normal_01` / password
2. **Vào trang sách đã mua:**
   - `http://localhost:2706/books/view/book_02` (Đắc Nhân Tâm)
   - `http://localhost:2706/books/view/book_03` (Mắt biếc)
   - `http://localhost:2706/books/view/book_07` (Dế Mèn)
3. **Kiểm tra:**
   - ✅ Có hiện badge "ĐÃ MUA"
   - ✅ Có nút "Tải xuống sách đã mua" màu xanh
   - ✅ Click nút → Hiện loading SweetAlert2
   - ✅ File được tải về (hoặc lỗi 403/404 nếu file không tồn tại)

### BƯỚC 5: Test Các Trường Hợp Khác

#### Test Case 1: FREE Book
- URL: `http://localhost:2706/books/view/book_04` (Conan Tập 1)
- **Kỳ vọng:** Có nút "Tải xuống" (không cần mua)

#### Test Case 2: SUBSCRIPTION Book (có VIP nhưng chưa mua lẻ)
- URL: `http://localhost:2706/books/view/book_05` (Doraemon)
- **Kỳ vọng:** **KHÔNG CÓ** nút tải xuống (chỉ có nút "Đọc sách")

#### Test Case 3: PURCHASE Book chưa mua
- URL: `http://localhost:2706/books/view/book_06` (Tôi thấy hoa vàng)
- **Kỳ vọng:** Không có nút tải, có nút "Thêm vào giỏ hàng"

#### Test Case 4: BOTH Book - có VIP nhưng chưa mua lẻ
- URL: `http://localhost:2706/books/view/book_13` (Nhà giả kim)
- **Kỳ vọng:** 
  - Không có nút tải
  - Có alert: "Mua sách để tải xuống. VIP chỉ đọc online."
  - Có nút "Mua ngay" và "Hoặc dùng VIP"

---

## 🐛 NẾU VẪN LỖI

### Lỗi: "Swal is not defined"
1. Mở DevTools (F12) → Tab Network
2. Refresh trang
3. Kiểm tra có request đến `sweetalert2@11` không
4. Nếu không có → Check lại file `head.html` và `scripts.html`

### Lỗi: "Không có quyền tải xuống"
1. Mở DevTools → Tab Console
2. Gõ: `console.log('Current user:', document.querySelector('[sec:authentication]'))`
3. Kiểm tra user_id có đúng không
4. Vào database check:
   ```sql
   SELECT * FROM order_items oi
   JOIN orders o ON oi.order_id = o.order_id
   WHERE o.user_id = 'user_normal_01';
   ```

### Lỗi: "Không tìm thấy file"
1. Check file tồn tại:
   ```sql
   SELECT * FROM bookassets WHERE book_id = 'book_02';
   ```
2. Check file path:
   - Database: `/book_asset/source/...`
   - Thực tế: `F:/datn_uploads/book_asset/source/...`
3. Check config trong `application.properties`:
   ```properties
   file.upload-dir=F:/datn_uploads
   ```

---

## 📊 KIỂM TRA DATABASE

```sql
-- 1. Check user có order nào không
SELECT o.order_id, o.order_type, o.payment_status, oi.book_id, b.title
FROM orders o
LEFT JOIN order_items oi ON o.order_id = oi.order_id
LEFT JOIN books b ON oi.book_id = b.book_id
WHERE o.user_id = 'user_normal_01';

-- 2. Check sách nào có file
SELECT b.book_id, b.title, b.access_type, b.is_downloadable, ba.file_type, ba.file_url
FROM books b
LEFT JOIN bookassets ba ON b.book_id = ba.book_id
WHERE b.is_downloadable = 1;

-- 3. Check order_items của user
SELECT * FROM order_items oi
JOIN orders o ON oi.order_id = o.order_id
WHERE o.user_id = 'user_normal_01' AND o.order_type = 'BOOK';
```

---

## 📝 CHECKLIST HOÀN THÀNH

- [x] Thêm SweetAlert2 CDN vào head.html
- [x] Thêm SweetAlert2 JS vào scripts.html
- [x] Sửa lỗi main.js → custom.js
- [x] Thêm check Swal trong book-download.js
- [x] Tạo test data SQL script
- [ ] **BẠN CẦN LÀM:** Chạy SQL script insert test data
- [ ] **BẠN CẦN LÀM:** Restart server
- [ ] **BẠN CẦN LÀM:** Clear browser cache
- [ ] **BẠN CẦN LÀM:** Test download với các trường hợp khác nhau

---

## 🎯 KẾT QUẢ KỲ VỌNG

Sau khi làm xong các bước trên:

1. ✅ Không còn lỗi "Swal is not defined"
2. ✅ Không còn lỗi "main.js 500"
3. ✅ Nút tải xuống hiện đúng với sách đã mua
4. ✅ Click nút → Loading animation SweetAlert2
5. ✅ File được tải về hoặc hiện lỗi rõ ràng

**Nếu vẫn lỗi, gửi screenshot console error cho tôi!**

