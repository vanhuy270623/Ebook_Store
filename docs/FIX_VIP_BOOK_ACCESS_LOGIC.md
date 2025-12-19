# Sửa Logic Kiểm Tra Quyền Đọc Sách VIP

**Ngày:** 14/12/2024  
**File:** `ReadingController.java`

## Vấn đề

Người dùng không có gói VIP vẫn có thể đọc được sách VIP do logic kiểm tra quyền truy cập bị bỏ qua.

### Nguyên nhân

Trong hàm `canUserAccessBook()`, code đang **luôn return true** ở cuối hàm, bỏ qua việc kiểm tra:
- User đã mua sách chưa (PURCHASE)
- User có subscription active không (SUBSCRIPTION)

```java
// Code cũ - SAI
private boolean canUserAccessBook(User user, Book book) {
    if (user.getRole() != null && user.getRole().getRoleName().name().equals("ADMIN")) {
        return true;
    }
    if (Book.AccessType.FREE.equals(book.getAccessType())) {
        return true;
    }
    // TODO: Kiểm tra user đã mua sách chưa
    // TODO: Kiểm tra subscription active
    
    return true; // ❌ LỖI: Luôn cho phép đọc
}
```

## Giải pháp

### 1. Thêm Dependencies

Thêm `OrderService` và `OrderItemService` vào constructor:

```java
private final OrderService orderService;
private final OrderItemService orderItemService;
```

### 2. Sửa Logic `canUserAccessBook()`

Kiểm tra đúng quyền truy cập dựa trên `AccessType` của sách:

```java
private boolean canUserAccessBook(User user, Book book) {
    // Admin có thể đọc mọi sách
    if (user.getRole() != null && user.getRole().getRoleName().name().equals("ADMIN")) {
        return true;
    }

    // Sách miễn phí thì ai cũng đọc được
    if (Book.AccessType.FREE.equals(book.getAccessType())) {
        return true;
    }

    Book.AccessType accessType = book.getAccessType();
    
    // Kiểm tra sách PURCHASE hoặc BOTH - user đã mua sách chưa
    if (accessType == Book.AccessType.PURCHASE || accessType == Book.AccessType.BOTH) {
        boolean hasPurchased = orderItemService.hasUserPurchasedBook(user.getUserId(), book.getBookId());
        if (hasPurchased) {
            return true;
        }
    }

    // Kiểm tra sách SUBSCRIPTION hoặc BOTH - user có subscription active không
    if (accessType == Book.AccessType.SUBSCRIPTION || accessType == Book.AccessType.BOTH) {
        boolean hasActiveSubscription = hasActiveSubscription(user.getUserId());
        if (hasActiveSubscription) {
            return true;
        }
    }

    // Không có quyền truy cập
    return false;
}
```

### 3. Thêm Helper Method Kiểm Tra Subscription

```java
private boolean hasActiveSubscription(String userId) {
    try {
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);
        
        LocalDateTime now = LocalDateTime.now();
        return subscriptionOrders.stream()
                .anyMatch(order -> 
                    (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED || 
                     order.getPaymentStatus() == Order.PaymentStatus.PAID) &&
                    order.getEndDate() != null &&
                    order.getEndDate().isAfter(now)
                );
    } catch (Exception e) {
        log.error("Error checking subscription status for user {}: {}", userId, e.getMessage());
        return false;
    }
}
```

### 4. Sửa `determineAccessType()`

Thêm tham số `user` và xác định đúng loại truy cập:

```java
private ReadingProgress.AccessType determineAccessType(Book book, User user) {
    if (Book.AccessType.FREE.equals(book.getAccessType())) {
        return ReadingProgress.AccessType.FREE;
    }
    
    // Kiểm tra user đã mua sách chưa
    boolean hasPurchased = orderItemService.hasUserPurchasedBook(user.getUserId(), book.getBookId());
    if (hasPurchased) {
        return ReadingProgress.AccessType.PURCHASED;
    }
    
    // Kiểm tra user có subscription active không
    boolean hasActiveSubscription = hasActiveSubscription(user.getUserId());
    if (hasActiveSubscription) {
        return ReadingProgress.AccessType.SUBSCRIPTION;
    }
    
    return ReadingProgress.AccessType.FREE;
}
```

### 5. Cập nhật các lần gọi `determineAccessType()`

Thay đổi từ:
```java
determineAccessType(book)
```

Thành:
```java
determineAccessType(book, user)
```

Tổng cộng có **4 chỗ** cần cập nhật trong các method:
- `openBook()` - Line ~128
- PDF viewer endpoint - Line ~273  
- `updateProgress()` - Line ~334
- EPUB viewer endpoint - Line ~589

## Logic Flow Mới

### Khi user cố gắng đọc sách:

1. **FREE** → Cho phép đọc ngay
2. **PURCHASE** → Kiểm tra user đã mua chưa
   - ✅ Đã mua → Cho phép đọc
   - ❌ Chưa mua → Chặn
3. **SUBSCRIPTION** → Kiểm tra subscription active
   - ✅ Có VIP còn hạn → Cho phép đọc
   - ❌ Không có VIP hoặc hết hạn → Chặn
4. **BOTH** → Kiểm tra cả 2 điều kiện
   - ✅ Đã mua HOẶC có VIP → Cho phép đọc
   - ❌ Không thỏa mãn → Chặn

### Kiểm tra Subscription Active:

```
User có Subscription Active khi:
- Có Order với OrderType = SUBSCRIPTION
- PaymentStatus = COMPLETED hoặc PAID
- endDate > hiện tại (chưa hết hạn)
```

## Kết quả

✅ User không có VIP không thể đọc sách VIP  
✅ User đã mua sách có thể đọc sách PURCHASE  
✅ User có subscription active có thể đọc sách SUBSCRIPTION  
✅ User đã mua HOẶC có subscription có thể đọc sách BOTH  
✅ Admin vẫn có thể đọc tất cả sách  
✅ Sách FREE ai cũng đọc được  

## Test Cases Cần Kiểm Tra

### 1. Sách VIP (SUBSCRIPTION)
- [ ] User không VIP → Chặn
- [ ] User có VIP còn hạn → Cho phép
- [ ] User có VIP đã hết hạn → Chặn

### 2. Sách Mua (PURCHASE)  
- [ ] User chưa mua → Chặn
- [ ] User đã mua → Cho phép

### 3. Sách BOTH
- [ ] User chưa mua + không VIP → Chặn
- [ ] User đã mua + không VIP → Cho phép
- [ ] User chưa mua + có VIP → Cho phép
- [ ] User đã mua + có VIP → Cho phép

### 4. Sách FREE
- [ ] Bất kỳ user nào → Cho phép

### 5. Admin
- [ ] Admin → Cho phép đọc tất cả

## Build Status

✅ **BUILD SUCCESS** - Project compile thành công không có lỗi

```
[INFO] BUILD SUCCESS
[INFO] Total time:  6.317 s
```

## Files Changed

- `src/main/java/stu/datn/ebook_store/controller/user/ReadingController.java`
  - Added imports: `Order`, `OrderService`, `OrderItemService`
  - Added fields: `orderService`, `orderItemService`
  - Fixed: `canUserAccessBook()` logic
  - Added: `hasActiveSubscription()` helper method
  - Updated: `determineAccessType()` signature and logic
  - Updated: 4 calls to `determineAccessType()`

## Notes

- Subscription được lưu trong bảng `orders` với `order_type = 'SUBSCRIPTION'`
- Purchased books được lưu trong bảng `order_items` của orders đã COMPLETED
- Logic này áp dụng cho cả PDF và EPUB reader

