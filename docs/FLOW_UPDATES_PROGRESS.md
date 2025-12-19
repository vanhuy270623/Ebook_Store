# FLOW UPDATES SUMMARY - 20/12/2025

## ✅ Flows Đã Hoàn Chỉnh (Không Cần Update)

### FLOW 02: Admin Book Management
- ✅ Chính xác với code hiện tại
- ✅ Không có coupon dependency
- ✅ File upload paths đúng

### FLOW 05: Payment VNPay  
- ✅ Implementation đầy đủ
- ✅ Callback handling chính xác
- ✅ IPN webhook documented

### FLOW 06: Admin User Management
- ✅ CRUD operations đầy đủ
- ✅ Soft delete implemented
- ✅ Role management correct

### FLOW 07: Reading Interface
- ✅ PDF.js và ePub.js integration
- ✅ Progress tracking correct
- ✅ Bookmark system

### FLOW 08: Admin Order Management
- ✅ Order statuses correct
- ✅ Filter và search
- ✅ Export functionality

### FLOW 09: Admin Dashboard
- ✅ Statistics calculation
- ✅ Charts và graphs
- ✅ Real-time data

### FLOW 12: Admin Category Management
- ✅ CRUD operations
- ✅ Icon upload
- ✅ Validation

### FLOW 13: Admin Author Management
- ✅ CRUD operations
- ✅ Avatar upload
- ✅ Book count

### FLOW 14: Admin Banner Management
- ✅ CRUD operations
- ✅ Position management
- ✅ Active/inactive toggle

### FLOW 15: Admin Post Management
- ✅ CRUD operations
- ✅ Slug generation
- ✅ Published status

### FLOW 17: Home Page & Browse
- ✅ Book listing
- ✅ Search và filter
- ✅ Pagination

---

## ⚠️ Flows Cần Cập Nhật

### FLOW 01: Authentication
**Cần thêm:** Device Management Integration

**Thay đổi chính:**
1. Login flow bây giờ check device limit
2. Track device fingerprint
3. Auto-lock account sau 3 violations
4. Session lưu currentDeviceId

**Updated Controller:** `AuthController.java`
```java
@PostMapping("/auth/login")
public String processLogin(
    @ModelAttribute("loginDto") LoginDto loginDto,
    @RequestParam(required = false) String deviceFingerprint,
    @RequestParam(required = false) String deviceName,
    @RequestParam(required = false) String deviceType,
    HttpServletRequest request) {
    
    // Device info
    DeviceInfoDto deviceInfo = new DeviceInfoDto();
    deviceInfo.setDeviceFingerprint(deviceFingerprint);
    deviceInfo.setDeviceName(deviceName);
    deviceInfo.setDeviceType(deviceType != null ? deviceType : "WEB");
    
    // Authenticate với device check
    Map<String, Object> authResult = userService.authenticateWithDeviceCheck(
        loginDto.getUsername(),
        loginDto.getPassword(),
        deviceInfo,
        request
    );
    
    String status = (String) authResult.get("status");
    
    if ("DEVICE_LIMIT_EXCEEDED".equals(status)) {
        // Handle device limit error
    }
    
    if ("ACCOUNT_LOCKED".equals(status)) {
        // Handle locked account
    }
    
    // SUCCESS - continue normal login
}
```

---

### FLOW 03: Shopping Cart & Checkout
**Cần update:** CartItem structure và remove Coupon

**Thay đổi chính:**
1. ❌ CartItem KHÔNG CÒN quantity field
2. ❌ CartItem KHÔNG CÒN subtotal field
3. ✅ CartItem dùng composite key (cart_id, book_id)
4. ❌ XÓA toàn bộ Coupon logic

**Database Schema:**
```sql
CREATE TABLE `cart_items` (
  `cart_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `added_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`,`book_id`)  -- Composite key
) ENGINE=InnoDB;
```

**Entity:**
```java
@Entity
@Table(name = "cart_items")
public class CartItem {
    @EmbeddedId
    private CartItemId id;  // Composite key
    
    @ManyToOne
    @MapsId("cartId")
    private Cart cart;
    
    @ManyToOne
    @MapsId("bookId")
    private Book book;
    
    @Column(name = "added_at")
    private LocalDateTime addedAt;
    
    // NO quantity!
    // NO subtotal!
}
```

**Add to Cart Logic:**
```java
// OLD: If exists, increase quantity
// NEW: If exists, return error "Already in cart"

if (cartItemService.exists(cartId, bookId)) {
    throw new Exception("Sách này đã có trong giỏ hàng");
}

// Create new (no quantity)
CartItem item = new CartItem();
item.setCart(cart);
item.setBook(book);
// No setQuantity()!
```

**Calculate Total:**
```java
// OLD: sum(price * quantity)
// NEW: sum(price) only

BigDecimal total = cartItems.stream()
    .map(item -> item.getBook().getPrice())
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

**Sections to Remove:**
- Flow 3.5: Apply Coupon (entire section)
- All coupon references in checkout
- Coupon validation logic
- Discount calculation

---

### FLOW 04: User Account Management
**Cần update:** Controller split

**Thay đổi chính:**
Controllers đã được tách:
1. `UserProfileController.java` - Profile và change password
2. `UserOrderController.java` - Order history
3. `UserLibraryController.java` - Library (→ FLOW 20)
4. `UserDeviceController.java` - Devices (→ FLOW 19)

**Endpoints:**
```
# Profile
GET  /user/profile
POST /user/profile/update
POST /user/profile/change-password

# Orders  
GET  /user/orders
GET  /user/orders/{id}

# Library (NEW - see FLOW 20)
GET  /user/library
GET  /user/reading-history

# Devices (NEW - see FLOW 19)
GET  /user/devices
POST /user/devices/{id}/remove
```

---

### FLOW 10: Subscription Management
**Cần update:** Remove coupon và add cancel

**Thay đổi chính:**
1. ❌ XÓA coupon code input
2. ❌ XÓA discount calculation
3. ✅ THÊM cancel subscription endpoint

**Cancel Subscription:**
```java
@PostMapping("/cancel/{subscriptionId}")
public String cancelSubscription(
        @PathVariable String subscriptionId,
        RedirectAttributes redirectAttributes) {
    
    User currentUser = getCurrentUser();
    
    // Find order
    Order order = orderService.findByUserAndSubscriptionId(
        currentUser.getUserId(), subscriptionId
    );
    
    // Update to CANCELLED
    order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
    order.setCancelledAt(LocalDateTime.now());
    orderService.save(order);
    
    // User vẫn dùng được đến hết thời hạn đã trả
    
    redirectAttributes.addFlashAttribute("success", 
        "Đã hủy gói đăng ký. Bạn vẫn có thể sử dụng đến hết thời hạn.");
    
    return "redirect:/subscription/my-subscriptions";
}
```

**Payment Flow:**
- Không còn apply coupon step
- Chỉ có giá gốc
- Direct to payment (VNPay hoặc Bank Transfer)

---

### FLOW 11: Review & Rating System
**Cần update:** Implementation status

**Thay đổi chính:**
- ⚠️ Backend: 90% complete
- ⚠️ Frontend: 40% complete (thiếu review form UI)
- Admin moderation có nhưng chưa hoàn chỉnh

**Thiếu:**
1. User review submission form
2. Review display in book detail page
3. Admin moderation UI improvements

---

## 📊 Database Schema Updates

### users table
```sql
ALTER TABLE `users` 
ADD COLUMN `device_violation_count` int DEFAULT 0,
ADD COLUMN `account_locked_reason` text,
ADD COLUMN `locked_at` datetime,
ADD COLUMN `locked_until` datetime;
```

### user_devices table (NEW)
```sql
CREATE TABLE `user_devices` (
  `device_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `device_info` varchar(500) DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `first_login_at` datetime DEFAULT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`device_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB;
```

### cart_items table (CHANGED)
```sql
-- OLD: cart_item_id as PK, had quantity and subtotal
-- NEW: composite key, no quantity
CREATE TABLE `cart_items` (
  `cart_id` varchar(50) NOT NULL,
  `book_id` varchar(50) NOT NULL,
  `added_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`,`book_id`)
) ENGINE=InnoDB;
```

### coupons table (REMOVED)
```sql
-- Table này KHÔNG TỒN TẠI trong database hiện tại
-- ❌ DROP TABLE coupons;
-- ❌ DROP TABLE coupon_usage;
```

---

## 🔧 Code Updates Needed

### 1. Remove All Coupon References
**Files to update:**
- `FLOW_03_SHOPPING_CART_CHECKOUT.md` - Remove Flow 3.5
- Any checkout templates referencing coupon
- OrderController - remove coupon parameter

### 2. Update CartItem Documentation
**Files:**
- `FLOW_03_SHOPPING_CART_CHECKOUT.md` - Update entity docs
- Any service documentation referencing quantity

### 3. Add Device Management
**Files:**
- `FLOW_01_AUTHENTICATION.md` - Add device check flow
- `FLOW_19_DEVICE_MANAGEMENT.md` - Already created ✅

### 4. Add Library Features
**Files:**
- `FLOW_04_USER_ACCOUNT_MANAGEMENT.md` - Link to FLOW 20
- `FLOW_20_USER_LIBRARY_READING_HISTORY.md` - Already created ✅

### 5. Update Subscription
**Files:**
- `FLOW_10_SUBSCRIPTION_MANAGEMENT.md` - Add cancel, remove coupon

---

## 🎯 Priority Updates

### HIGH Priority (Core Business Logic)
1. ✅ **FLOW 03** - CartItem và Coupon (CRITICAL)
2. ⚠️ **FLOW 01** - Device Management integration
3. ⚠️ **FLOW 10** - Cancel subscription

### MEDIUM Priority (Documentation Accuracy)
4. ⚠️ **FLOW 04** - Controller split references
5. ⚠️ **FLOW 11** - Implementation status

### LOW Priority (Minor Updates)
6. ✅ All other flows - Already accurate

---

## ✅ Action Items

- [x] Create FLOW 18: Secure Download
- [x] Create FLOW 19: Device Management
- [x] Create FLOW 20: User Library
- [x] Create FLOW 21: Bank Transfer
- [x] Create FLOW 22: Favorites
- [x] Remove FLOW 16: Coupon Management
- [x] Update FLOW_INDEX.md
- [ ] **Update FLOW 01: Add device management**
- [ ] **Update FLOW 03: Remove coupon, fix CartItem**
- [ ] **Update FLOW 04: Controller split**
- [ ] **Update FLOW 10: Cancel subscription**
- [ ] **Update FLOW 11: Implementation status**

---

**Status:** 60% Complete (12/20 flows updated)  
**Remaining:** 5 flows need updates  
**Priority:** HIGH - Business logic critical  
**Last Updated:** 20/12/2025

