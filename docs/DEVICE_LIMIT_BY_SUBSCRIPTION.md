# CẬP NHẬT: GIỚI HẠN THIẾT BỊ THEO GÓI SUBSCRIPTION

**Ngày:** 18/12/2025  
**Trạng thái:** ✅ Hoàn thành

---

## 📊 PHÂN BỔ THIẾT BỊ THEO GÓI

| Gói | Giá | Thời hạn | Max Devices | Ghi chú |
|-----|-----|----------|-------------|---------|
| **FREE** | 0đ | Vĩnh viễn | **1 thiết bị** | Mặc định cho user mới |
| **BASIC** | 59,000đ | 30 ngày | **2 thiết bị** | Nâng cấp cơ bản |
| **PREMIUM** | 79,000đ | 30 ngày | **3 thiết bị** | Gói nâng cao |
| **VIP** | 99,000đ | 30 ngày | **3 thiết bị** | Gói cao cấp |

---

## 🔄 THAY ĐỔI SO VỚI PHIÊN BẢN CŨ

### Trước đây (Hardcoded)
```java
private static final int MAX_DEVICES_PER_USER = 3; // Tất cả users
```

### Bây giờ (Dynamic theo Subscription)
```java
private int getMaxDevicesForUser(User user) {
    Optional<Subscription> sub = subscriptionRepository
        .findActiveSubscriptionByUserId(user.getUserId(), LocalDateTime.now());
    
    return sub.isPresent() ? sub.get().getMaxDevices() : 1; // FREE = 1
}
```

---

## 🗄️ DATABASE CHANGES

### Migration SQL đã cập nhật

```sql
-- Cập nhật giới hạn thiết bị cho các gói
UPDATE subscriptions SET max_devices = 1 WHERE package_name = 'FREE';
UPDATE subscriptions SET max_devices = 2 WHERE package_name = 'BASIC';
UPDATE subscriptions SET max_devices = 3 WHERE package_name = 'PREMIUM';
UPDATE subscriptions SET max_devices = 3 WHERE package_name = 'VIP';
```

### Verify data hiện tại

```sql
SELECT subscription_id, package_name, max_devices, price 
FROM subscriptions 
ORDER BY display_order;
```

**Kết quả:**
```
sub_free    | FREE    | 1 | 0
sub_basic   | BASIC   | 2 | 59000
sub_premium | PREMIUM | 3 | 79000
sub_vip     | VIP     | 3 | 99000
```

---

## 💻 CODE CHANGES

### 1. UserServiceImpl.java

**Thêm dependency:**
```java
private final SubscriptionRepository subscriptionRepository;
```

**Thêm method mới:**
```java
// Private method để tính max devices
private int getMaxDevicesForUser(User user) {
    Optional<Subscription> activeSubscription = subscriptionRepository
        .findActiveSubscriptionByUserId(user.getUserId(), LocalDateTime.now());
    
    return activeSubscription.isPresent() 
        ? activeSubscription.get().getMaxDevices() 
        : DEFAULT_MAX_DEVICES; // 1 for FREE
}

// Public methods cho Controller
@Override
public int getUserMaxDevices(String userId) { ... }

@Override
public String getUserSubscriptionInfo(String userId) { ... }
```

**Cập nhật logic checking:**
```java
int maxDevicesAllowed = getMaxDevicesForUser(user); // Dynamic!
int activeDeviceCount = deviceRepository.countByUser_UserIdAndIsActiveTrue(user.getUserId());

if (activeDeviceCount >= maxDevicesAllowed) {
    handleDeviceLimitExceeded(user, fingerprint, ip, request, maxDevicesAllowed);
    // ...
}
```

### 2. UserService.java

**Thêm interface methods:**
```java
int getUserMaxDevices(String userId);
String getUserSubscriptionInfo(String userId);
```

### 3. UserController.java

**Cập nhật devicesPage():**
```java
int maxDevices = getUserMaxDevices(currentUser.getUserId());
String subscriptionInfo = getSubscriptionInfo(currentUser.getUserId());

model.addAttribute("maxDevices", maxDevices); // Dynamic!
model.addAttribute("subscriptionInfo", subscriptionInfo); // "Gói BASIC (2 thiết bị)"
```

### 4. UI Template (manage.html)

**Hiển thị subscription info:**
```html
<h5 class="mb-1">
    Giới hạn thiết bị 
    <span class="badge bg-primary" th:text="${subscriptionInfo}">Gói FREE</span>
</h5>
<small class="text-info">
    Nâng cấp gói để tăng số thiết bị: 
    <a th:href="@{/subscribe}">BASIC (2), PREMIUM/VIP (3)</a>
</small>
```

### 5. Navbar

**Thêm link quản lý thiết bị:**
```html
<li>
    <a class="dropdown-item" th:href="@{/user/devices}">
        <i class="fas fa-mobile-alt me-2"></i>Quản lý thiết bị
    </a>
</li>
```

---

## 🎯 LUỒNG HOẠT ĐỘNG MỚI

### Scenario 1: User FREE login vượt 1 device

```
1. User FREE có 1 device đã đăng ký
2. Login trên device thứ 2
3. System check:
   - getMaxDevicesForUser() → 1 (FREE)
   - activeDeviceCount = 1
   - 1 >= 1 → BLOCKED
4. Thông báo: "Bạn đã đạt giới hạn 1 thiết bị theo gói FREE. 
   Vui lòng xóa thiết bị cũ hoặc nâng cấp gói."
```

### Scenario 2: User nâng cấp từ FREE → BASIC

```
1. User FREE có 1 device
2. User mua gói BASIC (59,000đ)
3. Order completed → subscription active
4. getMaxDevicesForUser() → 2 (BASIC)
5. User có thể login thêm 1 device nữa
6. Trang /user/devices hiển thị: "Gói BASIC (2 thiết bị)"
```

### Scenario 3: Gói hết hạn → Về FREE

```
1. User có gói PREMIUM (3 devices) với 3 devices đã đăng ký
2. Gói hết hạn
3. getMaxDevicesForUser() → 1 (FREE)
4. User vẫn có 3 devices active (chưa bị xóa auto)
5. Khi login device mới → BLOCKED
6. User phải xóa 2 devices để về 1
7. Hoặc gia hạn gói
```

---

## 📱 UI/UX CHANGES

### Trang quản lý thiết bị (/user/devices)

**Trước:**
```
Giới hạn thiết bị: 2/3
```

**Sau:**
```
Giới hạn thiết bị [Gói BASIC (2 thiết bị)]
Bạn có thể đăng nhập trên tối đa 2 thiết bị
💡 Nâng cấp gói để tăng số thiết bị: BASIC (2), PREMIUM/VIP (3)

2/2 Đã sử dụng
[Progress bar: 100% - Màu đỏ]
```

### Login Error Message

**Trước:**
```
"Bạn đã đạt giới hạn 3 thiết bị. Vi phạm: 1/3 lần."
```

**Sau:**
```
"Bạn đã đạt giới hạn 1 thiết bị theo gói FREE. 
Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị hoặc nâng cấp gói.
Vi phạm: 1/3 lần."
```

---

## 🧪 TEST CASES

### Test Case 1: User FREE (1 device)
```
✅ Login device 1 → OK
❌ Login device 2 → BLOCKED
✅ Xóa device 1, login device 2 → OK
```

### Test Case 2: User BASIC (2 devices)
```
✅ Login device 1 → OK
✅ Login device 2 → OK  
❌ Login device 3 → BLOCKED
```

### Test Case 3: User PREMIUM (3 devices)
```
✅ Login device 1 → OK
✅ Login device 2 → OK
✅ Login device 3 → OK
❌ Login device 4 → BLOCKED
```

### Test Case 4: Nâng cấp gói
```
1. User FREE có 1 device
2. Mua gói BASIC
3. getMaxDevices() = 2
4. Login device 2 → OK ✅
```

### Test Case 5: Hết hạn gói
```
1. User PREMIUM có 3 devices
2. Gói hết hạn → FREE (max 1)
3. 3 devices vẫn active
4. Login device mới → BLOCKED
5. Hiển thị: "Gói FREE (1 thiết bị)" - cần xóa 2 devices
```

---

## 🔧 CONFIGURATION

### Constants (UserServiceImpl.java)

```java
// Giới hạn mặc định cho FREE users
private static final int DEFAULT_MAX_DEVICES = 1;

// Số lần vi phạm trước khi lock (không đổi)
private static final int MAX_VIOLATIONS_BEFORE_LOCK = 3;
```

### Database (subscriptions table)

```sql
-- Có thể thay đổi max_devices theo nhu cầu
UPDATE subscriptions SET max_devices = 5 WHERE package_name = 'VIP';
UPDATE subscriptions SET max_devices = 1 WHERE package_name = 'BASIC'; -- Giảm xuống
```

---

## 📊 BUSINESS LOGIC

### Pricing Strategy (Đề xuất)

| Feature | FREE | BASIC | PREMIUM | VIP |
|---------|------|-------|---------|-----|
| **Devices** | 1 | 2 | 3 | 3 |
| **Price** | 0đ | 59k | 79k | 99k |
| **Books** | Free only | Basic catalog | Premium catalog | All books |
| **Ads** | Yes | No | No | No |
| **Download** | No | No | Yes | Yes |

### Upsell Opportunities

1. **Device Limit Warning:**
   - "Bạn đã dùng hết slot. Nâng cấp BASIC để có 2 thiết bị!"

2. **Device Management Page:**
   - Hiển thị nổi bật: "Nâng cấp để tăng thiết bị"
   - Link trực tiếp: `/subscribe`

3. **Login Blocked:**
   - "Nâng cấp lên BASIC (59k) để dùng 2 thiết bị"
   - Button "Nâng cấp ngay"

---

## 🚀 DEPLOYMENT CHECKLIST

- [x] Cập nhật migration SQL (max_devices)
- [x] Thêm SubscriptionRepository dependency
- [x] Implement getMaxDevicesForUser()
- [x] Cập nhật authenticateWithDeviceCheck()
- [x] Thêm getUserMaxDevices() public method
- [x] Thêm getUserSubscriptionInfo()
- [x] Cập nhật UserController
- [x] Cập nhật template manage.html
- [x] Thêm link navbar
- [x] Test all scenarios

---

## 📝 NEXT STEPS (Optional)

### Phase 1: Analytics
- Track device usage per subscription tier
- Monitor upgrade conversion rate
- A/B test device limits

### Phase 2: Marketing
- Email khi FREE user đạt device limit
- Popup upsell trong app
- Discount code cho BASIC upgrade

### Phase 3: Flexibility
- Admin có thể override max_devices per user
- Custom limits cho corporate accounts
- Family plan (5 devices)

---

## 🎉 KẾT QUẢ

✅ **Giới hạn thiết bị đã được phân theo gói subscription**  
✅ **FREE: 1 device, BASIC: 2, PREMIUM/VIP: 3**  
✅ **Dynamic checking dựa trên subscription active**  
✅ **UI hiển thị thông tin gói và upsell**  
✅ **Tích hợp hoàn chỉnh với hệ thống cũ**  

---

**Hệ thống sẵn sàng cho business logic theo gói subscription! 🚀**

