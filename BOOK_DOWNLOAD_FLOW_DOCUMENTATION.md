# TÀI LIỆU LUỒNG TẢI XUỐNG SÁCH (BOOK DOWNLOAD FLOW)

## 📚 TỔNG QUAN

Hệ thống tải xuống sách được triển khai theo nghiệp vụ:
- **Sách MIỄN PHÍ (FREE)**: Cho phép tải nếu `is_downloadable = true`
- **Sách MUA LẺ (PURCHASE)**: Chỉ cho tải khi đã mua và thanh toán thành công
- **Sách GÓI VIP (SUBSCRIPTION)**: **KHÔNG CHO TẢI**, chỉ đọc online
- **Sách CẢ HAI (BOTH)**: Chỉ cho tải khi đã **MUA LẺ**, gói VIP chỉ đọc online

---

## 🎯 QUY TẮC NGHIỆP VỤ

### 1. Kiểm tra 3 lớp (3-Layer Authorization)

#### Lớp 1: Authentication
```
- User PHẢI đăng nhập
- Nếu chưa đăng nhập → 401 Unauthorized
```

#### Lớp 2: Ownership (Quyền sở hữu)
```sql
-- Kiểm tra user đã MUA LẺ sách chưa
SELECT DISTINCT oi.book_id
FROM order_items oi
JOIN orders o ON oi.order_id = o.order_id
WHERE o.user_id = ?
  AND o.order_type = 'BOOK'           -- CHỈ đơn MUA LẺ
  AND o.payment_status IN ('COMPLETED', 'PAID')
  AND oi.book_id = ?
```

**Logic theo `access_type`:**
- `FREE`: Tự động cho phép (nếu `is_downloadable = true`)
- `PURCHASE`: Phải có trong `order_items` với `order_type = 'BOOK'`
- `SUBSCRIPTION`: **LUÔN TRẢ VỀ FALSE** (không cho tải)
- `BOTH`: Phải MUA LẺ (không tính VIP)

#### Lớp 3: Asset Availability
```
- Sách phải có file trong bảng `bookassets`
- Flag `is_downloadable = true`
- File tồn tại trên filesystem
```

---

## 📁 CẤU TRÚC FILE

### 1. **DownloadAuthorizationService.java**
Interface định nghĩa các phương thức kiểm tra quyền tải xuống.

```java
public interface DownloadAuthorizationService {
    boolean canDownload(User user, Book book);
    void validateDownloadPermission(User user, Book book);
    String getDownloadDeniedReason(User user, Book book);
}
```

### 2. **DownloadAuthorizationServiceImpl.java**
Triển khai logic nghiệp vụ kiểm tra quyền.

**Luồng xử lý:**
```
1. Kiểm tra is_downloadable
2. Kiểm tra access_type (FREE → pass)
3. Gọi OrderItemRepository để kiểm tra đã mua lẻ
4. Return true/false theo logic nghiệp vụ
```

### 3. **BookDownloadController.java**
REST Controller xử lý request tải xuống.

**Endpoint:** `GET /books/download/{bookId}`

**Flow:**
```
1. Verify Authentication
2. Tìm Book
3. Kiểm tra quyền (canDownload)
4. Tìm BookAsset (ưu tiên EPUB → PDF)
5. Load file từ FileStorageService
6. Stream file về client với proper headers
```

**Response Headers:**
```
Content-Type: application/pdf hoặc application/epub+zip
Content-Disposition: attachment; filename*=UTF-8''<tên_sách>.pdf
Content-Length: <kích thước file>
```

**Error Codes:**
- `401`: Chưa đăng nhập
- `403`: Không có quyền tải (kèm lý do trong header `X-Download-Error`)
- `404`: Không tìm thấy sách hoặc file
- `500`: Lỗi server

### 4. **book-download.js**
JavaScript xử lý download với thông báo đẹp (SweetAlert2).

**Features:**
- Loading indicator khi tải
- Xử lý error với message thân thiện
- Tự động decode filename tiếng Việt
- Success notification sau khi tải

---

## 🗄️ DATABASE SCHEMA

### Bảng `books`
```sql
access_type ENUM('FREE','PURCHASE','SUBSCRIPTION','BOTH')
is_downloadable TINYINT(1) DEFAULT 0
```

### Bảng `orders`
```sql
order_type ENUM('BOOK','SUBSCRIPTION')
payment_status ENUM('PENDING','WAITING_APPROVAL','COMPLETED','PAID','FAILED','CANCELLED')
```

### Bảng `order_items`
```sql
order_id VARCHAR(50)  -- Link đến orders
book_id VARCHAR(50)   -- Sách đã mua
```

**Ý nghĩa:**
- `order_items` chỉ tồn tại khi `orders.order_type = 'BOOK'`
- Gói VIP không có `order_items`, chỉ có `orders.subscription_id`

---

## 🔐 MA TRẬN QUYỀN TẢI XUỐNG

| Access Type | Đã mua lẻ | Có VIP | is_downloadable | Kết quả |
|-------------|-----------|--------|-----------------|---------|
| FREE        | -         | -      | true            | ✅ Tải được |
| FREE        | -         | -      | false           | ❌ Không tải |
| PURCHASE    | ✅        | -      | true            | ✅ Tải được |
| PURCHASE    | ❌        | -      | true            | ❌ Chưa mua |
| SUBSCRIPTION| -         | ✅     | true            | ❌ Chỉ đọc online |
| BOTH        | ✅        | -      | true            | ✅ Tải được |
| BOTH        | ❌        | ✅     | true            | ❌ VIP không tải, phải mua lẻ |

---

## 🎨 GIAO DIỆN (view.html)

### Nút tải xuống xuất hiện khi:
1. **FREE**: `is_downloadable = true`
2. **PURCHASE**: `purchasedBookIds.contains(bookId) && is_downloadable`
3. **SUBSCRIPTION**: **KHÔNG BAO GIỜ** hiện nút tải
4. **BOTH**: `purchasedBookIds.contains(bookId) && is_downloadable`

### Thông báo cho user:
- Sách BOTH chưa mua: "Mua sách để tải xuống. VIP chỉ đọc online."
- Sách PURCHASE chưa mua: Hiện nút "Thêm vào giỏ hàng"
- Sách SUBSCRIPTION: Hiện nút "Nâng cấp VIP"

---

## 🧪 TEST CASES

### Test Case 1: User chưa đăng nhập
```
GET /books/download/book_02
Expected: 401 Unauthorized
```

### Test Case 2: Sách FREE, is_downloadable = true
```
Book: book_04 (Conan Tập 1)
User: Bất kỳ (đã login)
Expected: 200 OK + file stream
```

### Test Case 3: Sách PURCHASE đã mua
```
Book: book_02 (Đắc Nhân Tâm)
User: user_normal_01 (đã mua)
Expected: 200 OK + file stream
```

### Test Case 4: Sách PURCHASE chưa mua
```
Book: book_02
User: user_normal_02 (chưa mua)
Expected: 403 Forbidden
Message: "Bạn cần mua sách này để tải xuống..."
```

### Test Case 5: Sách SUBSCRIPTION (có VIP)
```
Book: book_05 (Doraemon Tập 1)
User: user_normal_01 (có VIP active)
Expected: 403 Forbidden
Message: "Sách này thuộc gói VIP và chỉ cho phép đọc online..."
```

### Test Case 6: Sách BOTH - chỉ có VIP
```
Book: book_03 (Mắt biếc)
User: user_normal_01 (VIP active, chưa mua lẻ)
Expected: 403 Forbidden
Message: "Sách này cần mua lẻ để tải xuống. Gói VIP chỉ đọc online..."
```

### Test Case 7: Sách BOTH - đã mua lẻ
```
Book: book_03
User: user_normal_02 (đã mua lẻ book_03)
Expected: 200 OK + file stream
```

### Test Case 8: Sách không có file
```
Book: book_06 (is_downloadable = false)
Expected: 403 Forbidden
Message: "Sách này không hỗ trợ tải xuống. Chỉ đọc online."
```

---

## ⚙️ CẤU HÌNH (application.properties)

```properties
# Base directory cho tất cả file uploads
file.upload-dir=F:/datn_uploads

# Max file size (cho upload admin)
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

**Lưu ý:**
- File trong DB lưu path tương đối: `/book_asset/source/...`
- FileStorageService sẽ ghép: `F:/datn_uploads` + `/book_asset/source/...`
- Kết quả: `F:/datn_uploads/book_asset/source/khoahoc-vientuong/...`

---

## 🚀 DEPLOYMENT CHECKLIST

- [x] Tạo `DownloadAuthorizationService` interface
- [x] Tạo `DownloadAuthorizationServiceImpl` với logic nghiệp vụ
- [x] Tạo `BookDownloadController` với endpoint `/books/download/{bookId}`
- [x] Cập nhật `application.properties` với `file.upload-dir`
- [x] Thêm nút tải xuống vào `view.html` (4 trường hợp: FREE, PURCHASE, BOTH, SUBSCRIPTION)
- [x] Tạo `book-download.js` xử lý download với UX tốt
- [x] Test với các access_type khác nhau
- [ ] Test với file thật (PDF & EPUB)
- [ ] Kiểm tra performance với file lớn
- [ ] Log download history (optional)

---

## 📝 NOTES

### 1. Tại sao SUBSCRIPTION không cho tải?
- Gói VIP là mô hình "streaming" - user trả phí để đọc online
- Nếu cho tải → user có thể hủy VIP nhưng vẫn giữ sách
- Muốn tải → phải MUA LẺ (revenue cao hơn)

### 2. Tại sao BOTH phải mua lẻ mới tải?
- `BOTH` = "Đọc online với VIP HOẶC mua lẻ để sở hữu"
- Tải xuống = quyền sở hữu vĩnh viễn
- Chỉ order_type = 'BOOK' mới tạo quyền sở hữu

### 3. Security
- Không trả về URL tĩnh (tránh share link)
- Kiểm tra quyền mỗi lần download
- Stream file trực tiếp (không lưu temp)

### 4. Performance
- Sử dụng `Resource` với `UrlResource` để stream hiệu quả
- Header `Content-Length` giúp browser hiển thị progress
- File lớn (>100MB) vẫn stream được nhờ Spring's streaming support

---

## 🔗 RELATED FILES

```
src/main/java/stu/datn/ebook_store/
├── controller/user/
│   └── BookDownloadController.java          [NEW]
├── service/
│   ├── DownloadAuthorizationService.java    [NEW]
│   └── impl/
│       └── DownloadAuthorizationServiceImpl.java [NEW]
├── repository/
│   └── OrderItemRepository.java             [EXISTING - uses findPurchasedBookIds]
└── entity/
    ├── Book.java                            [EXISTING]
    ├── Order.java                           [EXISTING]
    └── BookAsset.java                       [EXISTING]

src/main/resources/
├── templates/user/books/
│   └── view.html                            [UPDATED - add download buttons]
├── static/user_template/js/
│   └── book-download.js                     [NEW]
└── application.properties                   [UPDATED - file.upload-dir]
```

---

## 📞 SUPPORT

Nếu có vấn đề:
1. Check log: `spring.jpa.show-sql=true` để xem query
2. Check file path: `file.upload-dir` trong properties
3. Check database: `order_items` có chứa book_id không
4. Check browser console: JavaScript errors
5. Check Network tab: Response headers và status code

---

**Ngày tạo:** 18/12/2025
**Phiên bản:** 1.0
**Trạng thái:** ✅ HOÀN THÀNH

