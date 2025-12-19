# HỦY GÓI VIP - DUY TRÌ QUYỀN ĐẾN HẾT THỜI GIAN ĐÃ THANH TOÁN

**Ngày:** 14/12/2024  
**Feature:** Khi user hủy gói VIP, vẫn giữ quyền đọc đến hết thời gian đã trả tiền

---

## 🎯 YÊU CẦU NGHIỆP VỤ

### Kịch Bản:
```
User đăng ký gói VIP 30 ngày:
├── Ngày đăng ký: 01/12/2024
├── End date: 30/12/2024
├── Ngày hủy: 07/12/2024
└── Quyền đọc: 07/12 → 30/12 ✅ Vẫn hoạt động
    Sau 30/12: ❌ Tự động mất quyền, quay về FREE
```

### Lý Do:
- User đã **thanh toán** cho 30 ngày
- Hủy sớm **không đồng nghĩa** với mất quyền ngay lập tức
- Công bằng cho user: Sử dụng hết thời gian đã trả

---

## ✅ GIẢI PHÁP ĐÃ TRIỂN KHAI

### 1. Sửa Logic `ReadingController.hasActiveSubscription()`

**Trước (SAI):**
```java
boolean isValidOrder = (
    order.getPaymentStatus() == COMPLETED ||
    order.getPaymentStatus() == PAID
) && order.getEndDate().isAfter(now);

// ❌ CANCELLED không được chấp nhận
// → User hủy → Mất quyền ngay lập tức
```

**Sau (ĐÚNG):**
```java
boolean isValidPaymentStatus = (
    order.getPaymentStatus() == COMPLETED ||
    order.getPaymentStatus() == PAID ||
    order.getPaymentStatus() == CANCELLED  // ✅ Thêm CANCELLED
);

boolean isNotExpired = order.getEndDate().isAfter(now);

// ✅ CANCELLED vẫn active nếu chưa hết hạn
if (isValidPaymentStatus && isNotExpired) {
    return true;
}
```

---

### 2. Sửa Logic `SubscriptionController.getActiveSubscription()`

**Trước (SAI):**
```java
.filter(order -> {
    if (order.getPaymentStatus() != COMPLETED &&
        order.getPaymentStatus() != PAID) {
        return false;  // ❌ CANCELLED bị reject
    }
    return order.getEndDate().isAfter(now);
})
```

**Sau (ĐÚNG):**
```java
.filter(order -> {
    // Phải đã thanh toán HOẶC đã hủy (CANCELLED vẫn giữ quyền)
    if (order.getPaymentStatus() != COMPLETED &&
        order.getPaymentStatus() != PAID &&
        order.getPaymentStatus() != CANCELLED) {  // ✅ Thêm CANCELLED
        return false;
    }
    return order.getEndDate().isAfter(now);
})
```

---

### 3. Sửa Logic `UserSubscription` DTO

**Trước (SAI):**
```java
if (order.getPaymentStatus() == CANCELLED) {
    this.status = Status.CANCELLED;
    this.isActive = false;  // ❌ SAI: Đánh dấu không active
}
```

**Sau (ĐÚNG):**
```java
if (order.getPaymentStatus() == CANCELLED && isTimeValid) {
    // Đã hủy NHƯNG còn thời gian
    this.status = Status.CANCELLED;
    this.isActive = true;  // ✅ ĐÚNG: Vẫn active!
}
```

---

## 🔄 LUỒNG HOẠT ĐỘNG

### A. User Hủy Gói

```java
// SubscriptionController.cancelSubscription()
order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
orderService.updateOrder(order);

// ✅ Chỉ đổi payment_status
// ❌ KHÔNG đổi end_date
```

### B. Kiểm Tra Quyền Đọc

```java
// ReadingController.hasActiveSubscription()
return subscriptionOrders.stream()
    .anyMatch(order -> {
        boolean isValidPaymentStatus = 
            order.getPaymentStatus() == COMPLETED ||
            order.getPaymentStatus() == PAID ||
            order.getPaymentStatus() == CANCELLED;  // ← Chấp nhận CANCELLED
        
        boolean isNotExpired = 
            order.getEndDate().isAfter(now);  // ← Kiểm tra thời gian
        
        return isValidPaymentStatus && isNotExpired;
    });
```

### C. Hiển Thị Trạng Thái

```java
// UserSubscription.java
if (CANCELLED && endDate > now) {
    status = Status.CANCELLED;
    isActive = true;  // ← Vẫn active!
    
    // UI sẽ hiển thị:
    // "Đã hủy - Có hiệu lực đến 30/12/2024"
}
```

---

## 📊 TRẠNG THÁI SUBSCRIPTION

| Payment Status | End Date | isActive | Can Read VIP | UI Display |
|----------------|----------|----------|--------------|------------|
| COMPLETED | > now | ✅ true | ✅ Yes | "Đang hoạt động" |
| PAID | > now | ✅ true | ✅ Yes | "Đang hoạt động" |
| **CANCELLED** | **> now** | ✅ **true** | ✅ **Yes** | "Đã hủy - Còn hiệu lực đến [date]" |
| CANCELLED | < now | ❌ false | ❌ No | "Đã hết hạn" |
| PENDING | any | ❌ false | ❌ No | "Chờ thanh toán" |
| EXPIRED | < now | ❌ false | ❌ No | "Đã hết hạn" |

---

## 🧪 TEST CASES

### Test 1: User hủy gói VIP còn hạn
```
Given: User có VIP end_date = 30/12/2024
When: User click "Hủy gói" vào 07/12/2024
Then:
  - payment_status → CANCELLED ✅
  - end_date → 30/12/2024 (không đổi) ✅
  - isActive → true ✅
  - Can read VIP books → Yes ✅
  - UI hiển thị: "Đã hủy - Có hiệu lực đến 30/12/2024" ✅
```

### Test 2: Sau khi hết hạn
```
Given: User có VIP CANCELLED, end_date = 30/12/2024
When: Ngày 31/12/2024 (sau end_date)
Then:
  - isActive → false ✅
  - Can read VIP books → No ✅
  - Status → EXPIRED ✅
  - UI hiển thị: "Đã hết hạn" ✅
```

### Test 3: Đọc sách VIP khi đã hủy nhưng còn hạn
```
Given: User CANCELLED, end_date = 30/12/2024, today = 15/12/2024
When: User truy cập /reading/book/{vip_book_id}
Then:
  - hasActiveSubscription() → true ✅
  - canUserAccessBook() → true ✅
  - Cho phép đọc sách ✅
```

### Test 4: Đọc sách VIP khi đã hủy và hết hạn
```
Given: User CANCELLED, end_date = 30/12/2024, today = 31/12/2024
When: User truy cập /reading/book/{vip_book_id}
Then:
  - hasActiveSubscription() → false ✅
  - canUserAccessBook() → false ✅
  - Redirect + Message: "Bạn không có quyền đọc cuốn sách này" ✅
```

---

## 📂 FILES ĐÃ SỬA

### 1. ReadingController.java
```java
// Dòng ~696-745
private boolean hasActiveSubscription(String userId) {
    // ...
    boolean isValidPaymentStatus = (
        order.getPaymentStatus() == COMPLETED ||
        order.getPaymentStatus() == PAID ||
        order.getPaymentStatus() == CANCELLED  // ← Thêm
    );
    // ...
}
```

### 2. SubscriptionController.java
```java
// Dòng ~51-65
private Optional<UserSubscription> getActiveSubscription(String userId) {
    return orders.stream()
        .filter(order -> {
            if (order.getPaymentStatus() != COMPLETED &&
                order.getPaymentStatus() != PAID &&
                order.getPaymentStatus() != CANCELLED) {  // ← Thêm
                return false;
            }
            return order.getEndDate().isAfter(now);
        })
        // ...
}
```

### 3. UserSubscription.java (DTO)
```java
// Dòng ~48-79
public UserSubscription(Order order) {
    // ...
    if (order.getPaymentStatus() == CANCELLED && isTimeValid) {
        this.status = Status.CANCELLED;
        this.isActive = true;  // ← Thay đổi từ false → true
    }
    // ...
}
```

### 4. SQL Scripts
- **`DB/CLEANUP_WITH_CANCELLED_LOGIC.sql`** (Mới) - Bao gồm logic CANCELLED

---

## 🎨 UI CHANGES (Đề xuất)

### Trang My Subscriptions

**Trước:**
```html
<span class="badge badge-danger">Đã hủy</span>
```

**Sau:**
```html
{{#if isActive}}
  <span class="badge badge-warning">
    Đã hủy - Còn hiệu lực đến {{endDate}}
  </span>
  <small class="text-muted">
    Bạn vẫn có thể đọc sách VIP đến hết {{endDate}}
  </small>
{{else}}
  <span class="badge badge-danger">Đã hết hạn</span>
{{/if}}
```

---

## 🔐 BẢO MẬT & BUSINESS LOGIC

### Nguyên Tắc:
1. **User đã trả tiền** → Có quyền sử dụng hết thời gian
2. **Hủy gói** → Không gia hạn tự động, nhưng không cắt quyền ngay
3. **Sau end_date** → Tự động quay về FREE

### Database Consistency:
```sql
-- Order khi hủy:
UPDATE orders 
SET payment_status = 'CANCELLED'
WHERE order_id = 'xxx';

-- ✅ KHÔNG update end_date
-- ✅ KHÔNG xóa reading_progress
-- ✅ Logic kiểm tra quyền dựa vào (payment_status + end_date)
```

---

## 📋 CHECKLIST TRIỂN KHAI

- [x] Sửa `ReadingController.hasActiveSubscription()` ✅
- [x] Sửa `SubscriptionController.getActiveSubscription()` ✅
- [x] Sửa `UserSubscription` DTO ✅
- [x] Tạo SQL script mới (bao gồm CANCELLED) ✅
- [x] Kiểm tra compile - No errors ✅
- [ ] Test case 1: Hủy gói còn hạn
- [ ] Test case 2: Sau khi hết hạn
- [ ] Test case 3: Đọc sách khi CANCELLED
- [ ] Test case 4: Đọc sách khi CANCELLED + hết hạn
- [ ] UI update (optional): Hiển thị message rõ ràng hơn

---

## 💡 LƯU Ý

### 1. Renewal (Gia hạn)
User đã CANCELLED **KHÔNG thể gia hạn tự động**. Muốn tiếp tục, phải:
- Đăng ký gói mới sau khi hết hạn
- Hoặc hủy bỏ trạng thái CANCELLED (admin can reactivate)

### 2. Refund (Hoàn tiền)
Nếu có chính sách hoàn tiền:
```java
// Tính số ngày chưa sử dụng
long daysRemaining = ChronoUnit.DAYS.between(now, endDate);
double refundAmount = (daysRemaining / totalDays) * totalAmount;
```

### 3. Auto-cleanup
Có thể tạo scheduled task để chuyển CANCELLED → EXPIRED sau end_date:
```java
@Scheduled(cron = "0 0 1 * * ?")  // 1AM mỗi ngày
public void cleanupExpiredSubscriptions() {
    // Update CANCELLED orders với end_date < now
}
```

---

## ✅ KẾT QUẢ

**Trước:**
- User hủy → Mất quyền ngay lập tức ❌
- Không công bằng với user đã trả tiền ❌

**Sau:**
- User hủy → Vẫn dùng đến hết thời gian ✅
- Công bằng, hợp lý với nghiệp vụ ✅
- Tự động hết quyền sau end_date ✅

---

**Status:** ✅ ĐÃ HOÀN THÀNH  
**Build:** ✅ SUCCESS (No errors)  
**Next:** Test trên môi trường thực tế

