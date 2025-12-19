# CRITICAL UPDATES FOR EXISTING FLOWS

**Ngày cập nhật:** 20/12/2025  
**Lý do:** Database schema và business logic đã thay đổi  

---

## ⚠️ FLOW 03: Shopping Cart & Checkout - CẦN CẬP NHẬT

### 1. CartItem Structure Changed

**❌ OLD (Không còn đúng):**
```java
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    private Long cartItemId;
    private Integer quantity;      // ❌ FIELD NÀY ĐÃ BỊ XÓA
    private BigDecimal subtotal;   // ❌ FIELD NÀY ĐÃ BỊ XÓA
    // ...
}
```

**✅ NEW (Hiện tại):**
```java
@Entity
@Table(name = "cart_items")
public class CartItem {
    @EmbeddedId
    private CartItemId id;  // Composite key: (cart_id, book_id)
    
    @ManyToOne
    @MapsId("cartId")
    private Cart cart;
    
    @ManyToOne
    @MapsId("bookId")
    private Book book;
    
    @Column(name = "added_at")
    private LocalDateTime addedAt;
    
    // NO quantity field!
    // NO subtotal field!
}

@Embeddable
public class CartItemId implements Serializable {
    private String cartId;
    private String bookId;
}
```

### 2. Database Schema

**OLD:**
```sql
CREATE TABLE cart_items (
  cart_item_id BIGINT PRIMARY KEY,
  user_id BIGINT,
  book_id BIGINT,
  quantity INT,           -- ❌ KHÔNG CÒN
  price DECIMAL(10,2),    -- ❌ KHÔNG CÒN
  subtotal DECIMAL(10,2)  -- ❌ KHÔNG CÒN
);
```

**NEW:**
```sql
CREATE TABLE `cart_items` (
  `cart_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `added_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`,`book_id`)  -- ✅ Composite key
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3. Add to Cart Logic

**❌ OLD:**
- Nếu sách đã có trong giỏ → Tăng quantity
- Cập nhật subtotal = price * quantity

**✅ NEW:**
- Nếu sách đã có trong giỏ → Báo lỗi "Đã có trong giỏ hàng"
- Mỗi sách chỉ thêm được 1 lần
- Không có quantity, không có subtotal

### 4. Remove Coupon

**❌ REMOVED:**
- Flow 3.5: Apply Coupon (toàn bộ section)
- Entity `Coupon` không tồn tại
- Không có mã giảm giá trong dự án

**✅ UPDATE:**
- Xóa tất cả references về Coupon
- Xóa Coupon import statements
- Xóa coupon validation logic
- Xóa coupon UI elements

### 5. Checkout Flow

**Controller:** `OrderController.java`  
**Location:** `/order/checkout`

**NEW Implementation:**
```java
@GetMapping("/checkout")
public String showCheckout(Model model, RedirectAttributes redirectAttributes) {
    User currentUser = getCurrentUser();
    if (currentUser == null) {
        return "redirect:/auth/login";
    }

    Cart cart = cartService.getCartByUser(currentUser).orElse(null);
    if (cart == null) {
        redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
        return "redirect:/cart";
    }

    // Validate cart (no coupon check)
    if (!cartService.isCartValidForCheckout(cart, currentUser)) {
        List<String> errors = cartService.getCartValidationErrors(cart, currentUser);
        redirectAttributes.addFlashAttribute("error", String.join(", ", errors));
        return "redirect:/cart";
    }

    List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
    BigDecimal totalAmount = cartService.calculateCartTotal(cart);  // No discount

    model.addAttribute("cartItems", cartItems);
    model.addAttribute("totalAmount", totalAmount);
    model.addAttribute("itemCount", cartItems.size());

    return "user/order/checkout";
}
```

### 6. Payment Methods

**✅ Supported:**
1. VNPay (FLOW 05)
2. Bank Transfer (FLOW 21)
3. Subscription Payment (FLOW 10)

**❌ NOT supported:**
- COD (Cash on Delivery)
- MoMo
- ZaloPay

---

## ⚠️ FLOW 01: Authentication - CẬN CẬP NHẬT

### Device Management Integration

**NEW:** Authentication bây giờ kết hợp với Device Management

**Controller:** `AuthController.java`

```java
@PostMapping("/auth/login")
public String processLogin(@ModelAttribute("loginDto") LoginDto loginDto,
                          @RequestParam(required = false) String deviceFingerprint,
                          @RequestParam(required = false) String deviceName,
                          @RequestParam(required = false) String deviceType,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
    
    // Tạo DeviceInfo
    DeviceInfoDto deviceInfo = new DeviceInfoDto();
    deviceInfo.setDeviceFingerprint(deviceFingerprint);
    deviceInfo.setDeviceName(deviceName);
    deviceInfo.setDeviceType(deviceType != null ? deviceType : "WEB");
    deviceInfo.setUserAgent(request.getHeader("User-Agent"));

    // XÁC THỰC VỚI DEVICE CHECKING
    Map<String, Object> authResult = userService.authenticateWithDeviceCheck(
        loginDto.getUsername(),
        loginDto.getPassword(),
        deviceInfo,
        request
    );

    String status = (String) authResult.get("status");

    // Handle device limit exceeded
    if ("DEVICE_LIMIT_EXCEEDED".equals(status)) {
        int violationCount = (Integer) authResult.get("violationCount");
        int maxDevices = (Integer) authResult.get("maxDevices");

        String errorMsg = String.format(
            "⚠️ Bạn đã đạt giới hạn %d thiết bị. " +
            "Vui lòng xóa thiết bị cũ. Cảnh báo: %d/3 lần vi phạm.",
            maxDevices, violationCount
        );

        redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
        return "redirect:/auth/login";
    }

    // Handle account locked
    if ("ACCOUNT_LOCKED".equals(status)) {
        redirectAttributes.addFlashAttribute("errorMessage", authResult.get("reason"));
        return "redirect:/auth/login";
    }

    // SUCCESS
    User user = (User) authResult.get("user");
    // ... Continue with normal login flow
}
```

**UserService:** `authenticateWithDeviceCheck()`

```java
@Override
@Transactional
public Map<String, Object> authenticateWithDeviceCheck(
        String username, String password,
        DeviceInfoDto deviceInfo,
        HttpServletRequest request) throws Exception {
    
    Map<String, Object> result = new HashMap<>();

    // 1. Authenticate user
    User user = authenticateUser(username, password);

    // 2. Check device limit
    int maxDevices = getMaxDevicesForUser(user);
    long currentDeviceCount = deviceRepository.countByUser(user);
    
    String deviceFingerprint = deviceInfo.getDeviceFingerprint();
    boolean isKnownDevice = deviceRepository
        .existsByUserAndDeviceFingerprint(user, deviceFingerprint);

    // 3. If new device and limit exceeded
    if (!isKnownDevice && currentDeviceCount >= maxDevices) {
        // Increment violation count
        user.setDeviceViolationCount(user.getDeviceViolationCount() + 1);
        
        // Check if should lock account
        if (user.getDeviceViolationCount() >= MAX_VIOLATIONS_BEFORE_LOCK) {
            user.setIsActive(false);
            user.setAccountLockedReason(
                "Vượt quá 3 lần vi phạm giới hạn thiết bị. " +
                "Vui lòng liên hệ admin."
            );
            userRepository.save(user);
            
            result.put("status", "ACCOUNT_LOCKED");
            result.put("reason", user.getAccountLockedReason());
            return result;
        }
        
        userRepository.save(user);
        
        result.put("status", "DEVICE_LIMIT_EXCEEDED");
        result.put("violationCount", user.getDeviceViolationCount());
        result.put("maxDevices", maxDevices);
        return result;
    }

    // 4. Register or update device
    if (!isKnownDevice) {
        registerNewDevice(user, deviceInfo, request);
    } else {
        updateDeviceLastLogin(user, deviceFingerprint);
    }

    // 5. Update last login
    updateLastLogin(user.getUserId());

    result.put("status", "SUCCESS");
    result.put("user", user);
    return result;
}
```

---

## ⚠️ FLOW 04: User Account Management - CẬP NHẬT

### Simplified Structure

**Controllers được tách:**
1. `UserProfileController.java` - Profile & password
2. `UserOrderController.java` - Order history
3. `UserLibraryController.java` - Library & reading history (NEW - FLOW 20)
4. `UserDeviceController.java` - Device management (NEW - FLOW 19)

**OLD:** Tất cả trong `UserController.java`  
**NEW:** Tách thành nhiều controllers chuyên biệt

---

## ⚠️ FLOW 10: Subscription Management - CẬP NHẬT

### No Coupon Support

**❌ REMOVED:**
- Coupon code input khi đăng ký subscription
- Discount calculation
- Coupon validation

**✅ CURRENT:**
- Chỉ có giá gốc của subscription
- Không có discount
- Direct payment (VNPay hoặc Bank Transfer)

### Cancel Subscription

**NEW Endpoint:**
```java
@PostMapping("/cancel/{subscriptionId}")
public String cancelSubscription(
        @PathVariable String subscriptionId,
        RedirectAttributes redirectAttributes) {
    
    User currentUser = getCurrentUser();
    
    try {
        // Find order by subscription
        Order order = orderService.findByUserAndSubscriptionId(
            currentUser.getUserId(), subscriptionId
        );
        
        // Verify ownership
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("Không có quyền hủy subscription này");
        }
        
        // Update payment status to CANCELLED
        order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderService.save(order);
        
        redirectAttributes.addFlashAttribute("success", 
            "Đã hủy gói đăng ký. Bạn vẫn có thể sử dụng đến hết thời hạn đã trả.");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    
    return "redirect:/subscription/my-subscriptions";
}
```

---

## ✅ Checklist Cập Nhật

### FLOW 03: Shopping Cart & Checkout
- [ ] Remove all Coupon references
- [ ] Update CartItem entity documentation
- [ ] Update database schema section
- [ ] Remove "Update Quantity" flow (keep only "Remove")
- [ ] Update sequence diagrams
- [ ] Update controller code examples
- [ ] Update SQL queries
- [ ] Update Thymeleaf templates

### FLOW 01: Authentication
- [ ] Add Device Management integration
- [ ] Document `authenticateWithDeviceCheck()` method
- [ ] Add device limit error handling
- [ ] Add account locking logic
- [ ] Update sequence diagram

### FLOW 04: User Account Management
- [ ] Document controller split
- [ ] Update to `UserProfileController`
- [ ] Link to FLOW 19 (Devices)
- [ ] Link to FLOW 20 (Library)

### FLOW 10: Subscription Management
- [ ] Remove coupon from subscription purchase
- [ ] Add cancel subscription flow
- [ ] Update payment methods (VNPay + Bank Transfer only)
- [ ] Remove discount calculation

---

## 📝 Các Thay Đổi Khác

### 1. No More Stock Management
- `stock_quantity` field in books table **không còn được sử dụng**
- Ebook không cần quản lý stock
- Luôn available nếu `is_available = true`

### 2. Access Types
```java
public enum AccessType {
    FREE,         // Sách miễn phí
    PAID,         // Mua 1 lần
    SUBSCRIPTION  // Cần subscription VIP
}
```

### 3. Order Types
```java
public enum OrderType {
    BOOK_PURCHASE,  // Mua sách lẻ
    SUBSCRIPTION    // Đăng ký gói VIP
}
```

### 4. Payment Methods
```java
public enum PaymentMethod {
    VNPAY,          // VNPay gateway
    BANK_TRANSFER,  // Chuyển khoản ngân hàng
    // COD - Removed
    // MOMO - Not implemented
}
```

---

**Priority:** HIGH  
**Status:** ⚠️ NEEDS UPDATE  
**Last Updated:** 20/12/2025

