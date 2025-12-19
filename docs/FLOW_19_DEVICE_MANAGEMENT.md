# FLOW 19: Device Management (Quản Lý Thiết Bị)

**Dự án:** Ebook Store  
**Ngày tạo:** 20/12/2025  
**Người tạo:** Development Team  
**Phiên bản:** 1.0  

---

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Luồng Xử Lý](#luồng-xử-lý)
3. [Implementation Details](#implementation-details)
4. [Security Considerations](#security-considerations)
5. [Database Schema](#database-schema)
6. [Testing](#testing)

---

## Tổng Quan

### Mục Đích
Quản lý giới hạn số thiết bị đăng nhập đồng thời cho user, ngăn chặn chia sẻ tài khoản không kiểm soát.

### Business Rules
- **FREE users:** Tối đa 1 thiết bị
- **BASIC subscription:** Tối đa 2 thiết bị
- **PREMIUM subscription:** Tối đa 3 thiết bị
- **VIP subscription:** Tối đa 5 thiết bị

### Actors
- **User**: Quản lý các thiết bị của mình
- **System**: Tracking và enforce device limits

### Key Features
- ✅ Hiển thị danh sách thiết bị đang đăng nhập
- ✅ Xóa thiết bị cũ để đăng nhập thiết bị mới
- ✅ Hiển thị thiết bị hiện tại
- ✅ Hiển thị thông tin subscription và giới hạn
- ✅ Warning khi vượt quá giới hạn
- ✅ Auto-lock account sau 3 lần vi phạm

---

## Luồng Xử Lý

### Sequence Diagram: View Devices

```
User            UserDeviceController      UserService         UserDeviceRepo      Database
 │                       │                      │                   │                │
 │──GET /user/devices──>│                      │                   │                │
 │                       │                      │                   │                │
 │                       │──getCurrentUser()──>│                   │                │
 │                       │<─────User───────────│                   │                │
 │                       │                      │                   │                │
 │                       │──getUserDevices()──>│                   │                │
 │                       │                      │──findByUserId()─>│                │
 │                       │                      │                   │──SELECT──────>│
 │                       │                      │                   │<─devices─────│
 │                       │                      │<─List<Device>────│                │
 │                       │<─────devices─────────│                   │                │
 │                       │                      │                   │                │
 │                       │──getUserMaxDevices()>│                   │                │
 │                       │<─────maxDevices─────│                   │                │
 │                       │                      │                   │                │
 │<──devices page────────│                      │                   │                │
 │                       │                      │                   │                │
```

### Sequence Diagram: Remove Device

```
User            UserDeviceController      UserService         UserDeviceRepo      Session
 │                       │                      │                   │                │
 │─POST /devices/{id}/remove>│                  │                   │                │
 │                       │                      │                   │                │
 │                       │──getCurrentUser()──>│                   │                │
 │                       │<─────User───────────│                   │                │
 │                       │                      │                   │                │
 │                       │──removeDevice()────>│                   │                │
 │                       │                      │──findById()─────>│                │
 │                       │                      │<───device────────│                │
 │                       │                      │                   │                │
 │                       │                      │──Verify Ownership│                │
 │                       │                      │                   │                │
 │                       │                      │──deleteById()───>│                │
 │                       │                      │                   │──DELETE──────>DB
 │                       │                      │                   │<─OK──────────│
 │                       │                      │<───success───────│                │
 │                       │<─────success─────────│                   │                │
 │                       │                      │                   │                │
 │                       │───invalidateSession(deviceId)──────────>│                │
 │                       │                      │                   │                │
 │<──{success:true}──────│                      │                   │                │
 │                       │                      │                   │                │
```

### Sequence Diagram: Device Limit Enforcement (Login Flow)

```
User            AuthController          DeviceTrackingService    UserService         Database
 │                  │                            │                     │                │
 │──POST /login───>│                            │                     │                │
 │                  │──validate credentials────>│                     │                │
 │                  │<───User valid─────────────│                     │                │
 │                  │                            │                     │                │
 │                  │──trackDevice(user, req)──>│                     │                │
 │                  │                            │──getDeviceInfo()──│                 │
 │                  │                            │──getUserMaxDevices()────────>       │
 │                  │                            │<─────maxDevices──────────────       │
 │                  │                            │                     │                │
 │                  │                            │──countDevices()────────────────────>│
 │                  │                            │<─────currentCount──────────────────│
 │                  │                            │                     │                │
 │                  │                            │──IF currentCount >= maxDevices──    │
 │                  │                            │    THEN throw DeviceLimitException  │
 │                  │                            │                     │                │
 │                  │                            │──ELSE registerDevice()─────────────>│
 │                  │                            │<─────success───────────────────────│
 │                  │<───device registered──────│                     │                │
 │                  │                            │                     │                │
 │<──Login Success──│                            │                     │                │
 │  (with deviceId) │                            │                     │                │
```

---

## Implementation Details

### Controller: `UserDeviceController.java`

**Location:** `src/main/java/stu/datn/ebook_store/controller/user/UserDeviceController.java`

**Endpoints:**
```
GET  /user/devices                    : Trang quản lý thiết bị
POST /user/devices/{deviceId}/remove  : Xóa thiết bị
GET  /user/api/devices                : API lấy danh sách thiết bị (JSON)
```

#### 1. View Devices Page
```java
@GetMapping("/devices")
public String devicesPage(Authentication authentication,
                         HttpSession session,
                         Model model) {
    User currentUser = getCurrentUser(authentication);
    String currentDeviceId = (String) session.getAttribute("currentDeviceId");

    // Lấy danh sách devices
    List<UserDevice> devices = userService.getUserDevices(currentUser.getUserId());

    // Lấy subscription hiện tại để biết max_devices
    int maxDevices = getUserMaxDevices(currentUser.getUserId());
    String subscriptionInfo = getSubscriptionInfo(currentUser.getUserId());

    // Tạo DTO cho view
    List<DeviceResponseDto> deviceDtos = devices.stream()
        .map(d -> DeviceResponseDto.fromEntity(
            d, d.getDeviceId().equals(currentDeviceId)))
        .collect(Collectors.toList());

    model.addAttribute("devices", deviceDtos);
    model.addAttribute("currentDeviceId", currentDeviceId);
    model.addAttribute("maxDevices", maxDevices);
    model.addAttribute("currentCount", devices.size());
    model.addAttribute("violationCount", currentUser.getDeviceViolationCount());
    model.addAttribute("subscriptionInfo", subscriptionInfo);

    return "user/devices/manage";
}
```

#### 2. Remove Device
```java
@PostMapping("/devices/{deviceId}/remove")
@ResponseBody
public Map<String, Object> removeDevice(
        @PathVariable String deviceId,
        Authentication authentication,
        HttpSession session) {

    Map<String, Object> response = new HashMap<>();

    try {
        User currentUser = getCurrentUser(authentication);
        String currentDeviceId = (String) session.getAttribute("currentDeviceId");

        // Không cho phép xóa thiết bị đang dùng
        if (deviceId.equals(currentDeviceId)) {
            response.put("success", false);
            response.put("message", "Không thể xóa thiết bị đang sử dụng");
            return response;
        }

        // Xóa thiết bị
        boolean removed = userService.removeDevice(
            currentUser.getUserId(), 
            deviceId
        );

        if (removed) {
            response.put("success", true);
            response.put("message", "Đã xóa thiết bị thành công");
        } else {
            response.put("success", false);
            response.put("message", "Không tìm thấy thiết bị hoặc không có quyền xóa");
        }

        return response;

    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        return response;
    }
}
```

---

### Service: `UserService.java`

**Location:** `src/main/java/stu/datn/ebook_store/service/UserService.java`

#### getUserDevices()
```java
public List<UserDevice> getUserDevices(String userId) {
    return userDeviceRepository.findByUser_UserId(userId);
}
```

#### removeDevice()
```java
public boolean removeDevice(String userId, String deviceId) {
    Optional<UserDevice> deviceOpt = userDeviceRepository.findById(deviceId);
    
    if (deviceOpt.isEmpty()) {
        return false;
    }
    
    UserDevice device = deviceOpt.get();
    
    // Verify ownership
    if (!device.getUser().getUserId().equals(userId)) {
        throw new UnauthorizedException("Không có quyền xóa thiết bị này");
    }
    
    // Delete device
    userDeviceRepository.deleteById(deviceId);
    
    // Invalidate sessions for this device (if implemented)
    sessionRegistry.invalidateDeviceSessions(deviceId);
    
    return true;
}
```

#### getUserMaxDevices()
```java
public int getUserMaxDevices(String userId) {
    // Tìm subscription active nhất của user
    Optional<Order> activeSubOrder = orderRepository
        .findActiveSubscriptionOrder(userId);
    
    if (activeSubOrder.isEmpty()) {
        return 1; // FREE user: 1 device
    }
    
    Order order = activeSubOrder.get();
    Subscription subscription = order.getSubscription();
    
    if (subscription == null) {
        return 1;
    }
    
    return subscription.getMaxDevices();
}
```

---

### Service: `UserDeviceService.java`

**Location:** `src/main/java/stu/datn/ebook_store/service/UserDeviceService.java`

#### trackDevice() - Called during login
```java
@Transactional
public UserDevice trackDevice(User user, HttpServletRequest request) {
    String deviceInfo = extractDeviceInfo(request);
    String ipAddress = extractIpAddress(request);
    
    // Check if device already exists
    Optional<UserDevice> existingDevice = userDeviceRepository
        .findByUser_UserIdAndDeviceInfo(user.getUserId(), deviceInfo);
    
    if (existingDevice.isPresent()) {
        // Update last login
        UserDevice device = existingDevice.get();
        device.setLastLoginAt(LocalDateTime.now());
        device.setIpAddress(ipAddress);
        return userDeviceRepository.save(device);
    }
    
    // Check device limit
    int maxDevices = userService.getUserMaxDevices(user.getUserId());
    long currentCount = userDeviceRepository.countByUser_UserId(user.getUserId());
    
    if (currentCount >= maxDevices) {
        // Increment violation count
        user.setDeviceViolationCount(user.getDeviceViolationCount() + 1);
        userRepository.save(user);
        
        // Check if should lock account
        if (user.getDeviceViolationCount() >= 3) {
            lockAccount(user);
        }
        
        throw new DeviceLimitExceededException(
            "Đã vượt quá số thiết bị cho phép (" + maxDevices + "). " +
            "Vui lòng xóa thiết bị cũ tại trang Quản lý thiết bị."
        );
    }
    
    // Register new device
    UserDevice newDevice = new UserDevice();
    newDevice.setDeviceId(generateDeviceId());
    newDevice.setUser(user);
    newDevice.setDeviceInfo(deviceInfo);
    newDevice.setIpAddress(ipAddress);
    newDevice.setFirstLoginAt(LocalDateTime.now());
    newDevice.setLastLoginAt(LocalDateTime.now());
    
    return userDeviceRepository.save(newDevice);
}
```

#### extractDeviceInfo()
```java
private String extractDeviceInfo(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    
    // Parse user agent to extract:
    // - Browser (Chrome, Firefox, Safari, Edge)
    // - OS (Windows, Mac, Linux, Android, iOS)
    // - Device type (Desktop, Mobile, Tablet)
    
    return parseUserAgent(userAgent);
}
```

#### generateDeviceId()
```java
private String generateDeviceId() {
    return "device_" + UUID.randomUUID().toString();
}
```

#### lockAccount()
```java
private void lockAccount(User user) {
    user.setIsActive(false);
    user.setAccountLockedReason(
        "Vượt quá 3 lần vi phạm giới hạn thiết bị. " +
        "Vui lòng liên hệ admin để mở khóa."
    );
    user.setLockedAt(LocalDateTime.now());
    userRepository.save(user);
    
    // Send notification email
    emailService.sendAccountLockedEmail(user);
}
```

---

### Entity: `UserDevice.java`

**Location:** `src/main/java/stu/datn/ebook_store/entity/UserDevice.java`

```java
@Entity
@Table(name = "user_devices")
public class UserDevice {
    
    @Id
    @Column(name = "device_id")
    private String deviceId;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "device_info", length = 500)
    private String deviceInfo; // User-Agent parsed
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "first_login_at")
    private LocalDateTime firstLoginAt;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and Setters
}
```

### DTO: `DeviceResponseDto.java`

```java
public class DeviceResponseDto {
    private String deviceId;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime firstLoginAt;
    private LocalDateTime lastLoginAt;
    private boolean isCurrentDevice;
    
    public static DeviceResponseDto fromEntity(UserDevice device, boolean isCurrent) {
        DeviceResponseDto dto = new DeviceResponseDto();
        dto.setDeviceId(device.getDeviceId());
        dto.setDeviceInfo(device.getDeviceInfo());
        dto.setIpAddress(device.getIpAddress());
        dto.setFirstLoginAt(device.getFirstLoginAt());
        dto.setLastLoginAt(device.getLastLoginAt());
        dto.setIsCurrentDevice(isCurrent);
        return dto;
    }
}
```

---

## Database Schema

### Table: `user_devices`

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
  KEY `user_id` (`user_id`),
  CONSTRAINT FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) 
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Table: `users` (updated fields)

```sql
ALTER TABLE `users` 
ADD COLUMN `device_violation_count` int DEFAULT 0,
ADD COLUMN `account_locked_reason` text,
ADD COLUMN `locked_at` datetime,
ADD COLUMN `locked_until` datetime;
```

### Stored Procedure: Check and Lock Account

```sql
DELIMITER $$
CREATE PROCEDURE sp_check_and_lock_account(IN p_user_id VARCHAR(50))
BEGIN
  DECLARE v_violation_count INT;

  SELECT device_violation_count INTO v_violation_count
  FROM users
  WHERE user_id = p_user_id;

  IF v_violation_count >= 3 THEN
    UPDATE users
    SET is_active = 0,
        account_locked_reason = 'Vượt quá 3 lần vi phạm giới hạn thiết bị.',
        locked_at = NOW()
    WHERE user_id = p_user_id;
  END IF;
END$$
DELIMITER ;
```

---

## Security Considerations

### 1. Device Fingerprinting
- ✅ User-Agent parsing
- ✅ IP address tracking
- ⚠️ TODO: Advanced fingerprinting (Canvas, WebGL, fonts)

### 2. Session Management
- ✅ Store deviceId in session
- ✅ Invalidate sessions when device removed
- ✅ Prevent session hijacking

### 3. Account Locking
- ✅ Auto-lock sau 3 lần vi phạm
- ✅ Email notification
- ✅ Admin unlock capability

### 4. Privacy
- ⚠️ GDPR compliance: User có quyền xóa device history
- ⚠️ Data retention policy

---

## Frontend Implementation

### Devices Management Page

**Template:** `user/devices/manage.html`

```html
<div class="container">
    <h2>Quản Lý Thiết Bị</h2>
    
    <div class="alert alert-info">
        <strong>Gói hiện tại:</strong> [[${subscriptionInfo}]]<br>
        <strong>Giới hạn:</strong> [[${currentCount}]] / [[${maxDevices}]] thiết bị
    </div>
    
    <div th:if="${violationCount > 0}" class="alert alert-warning">
        <i class="fas fa-exclamation-triangle"></i>
        Bạn đã vi phạm giới hạn thiết bị <strong>[[${violationCount}]]</strong> lần.
        Sau 3 lần vi phạm, tài khoản sẽ bị khóa.
    </div>
    
    <table class="table">
        <thead>
            <tr>
                <th>Thiết Bị</th>
                <th>IP Address</th>
                <th>Đăng Nhập Lần Đầu</th>
                <th>Đăng Nhập Gần Nhất</th>
                <th>Hành Động</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="device : ${devices}">
                <td>
                    [[${device.deviceInfo}]]
                    <span th:if="${device.isCurrentDevice}" 
                          class="badge badge-success">Thiết bị này</span>
                </td>
                <td>[[${device.ipAddress}]]</td>
                <td>[[${#temporals.format(device.firstLoginAt, 'dd/MM/yyyy HH:mm')}]]</td>
                <td>[[${#temporals.format(device.lastLoginAt, 'dd/MM/yyyy HH:mm')}]]</td>
                <td>
                    <button th:if="${!device.isCurrentDevice}"
                            class="btn btn-danger btn-sm remove-device-btn"
                            th:data-device-id="${device.deviceId}"
                            th:data-device-info="${device.deviceInfo}">
                        <i class="fas fa-trash"></i> Xóa
                    </button>
                    <span th:if="${device.isCurrentDevice}" class="text-muted">
                        Đang sử dụng
                    </span>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

### JavaScript: Remove Device

```javascript
document.querySelectorAll('.remove-device-btn').forEach(btn => {
    btn.addEventListener('click', async function() {
        const deviceId = this.dataset.deviceId;
        const deviceInfo = this.dataset.deviceInfo;
        
        if (!confirm(`Bạn có chắc muốn xóa thiết bị "${deviceInfo}"?`)) {
            return;
        }
        
        try {
            const response = await fetch(`/user/devices/${deviceId}/remove`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                showToast('success', data.message);
                // Reload page
                setTimeout(() => location.reload(), 1000);
            } else {
                showToast('error', data.message);
            }
        } catch (error) {
            console.error('Error removing device:', error);
            showToast('error', 'Có lỗi xảy ra khi xóa thiết bị');
        }
    });
});
```

---

## Testing

### Test Cases

#### TC-1: View Devices
**Precondition:** User đã đăng nhập từ 2 thiết bị

**Steps:**
1. Login from Device A
2. Login from Device B
3. Navigate to `/user/devices`
4. Verify both devices shown

**Expected Result:**
- 2 devices listed
- Current device marked with badge
- Max devices shown correctly

---

#### TC-2: Remove Device
**Precondition:** User có 2 devices

**Steps:**
1. On Device A, go to `/user/devices`
2. Click "Xóa" on Device B
3. Confirm deletion

**Expected Result:**
- Device B removed from list
- Success message shown
- Device B session invalidated

---

#### TC-3: Cannot Remove Current Device
**Steps:**
1. Go to `/user/devices`
2. Try to remove current device

**Expected Result:**
- Remove button disabled or hidden
- Message: "Đang sử dụng"

---

#### TC-4: Device Limit - FREE User
**Precondition:** FREE user (no subscription)

**Steps:**
1. Login from Device A (success)
2. Try to login from Device B

**Expected Result:**
- Login fails with error
- Error: "Đã vượt quá số thiết bị cho phép (1)"
- Violation count = 1

---

#### TC-5: Device Limit - After Upgrade
**Precondition:** User nâng cấp từ FREE → PREMIUM

**Steps:**
1. User has 1 device (FREE limit)
2. Upgrade to PREMIUM (max 3 devices)
3. Login from 2 more devices

**Expected Result:**
- All 3 logins successful
- No violations

---

#### TC-6: Account Lock After 3 Violations
**Steps:**
1. FREE user tries to login from Device 2 (violation 1)
2. Tries to login from Device 3 (violation 2)
3. Tries to login from Device 4 (violation 3)

**Expected Result:**
- After 3rd violation: Account locked
- is_active = 0
- account_locked_reason set
- Cannot login anymore

---

#### TC-7: Admin Unlock Account
**Precondition:** User account locked

**Steps:**
1. Admin calls `sp_unlock_account(user_id, admin_id)`
2. User tries to login

**Expected Result:**
- Account unlocked
- device_violation_count = 0
- User can login normally

---

## Best Practices

### 1. Device Identification
✅ **DO**: Combine multiple factors
```java
String fingerprint = userAgent + "|" + ipAddress + "|" + acceptLanguage;
```

❌ **DON'T**: Rely only on IP (can change)
```java
String fingerprint = ipAddress; // BAD
```

### 2. User Experience
✅ **DO**: Allow user to remove old devices
```java
// User-friendly device management UI
```

❌ **DON'T**: Hard-block without option to remove
```java
throw new Exception("Too many devices"); // BAD - No way to recover
```

### 3. Security vs UX Balance
✅ **DO**: Warn before locking
```
"Bạn đã vi phạm 2/3 lần. Cẩn thận!"
```

❌ **DON'T**: Lock immediately
```java
if (violations > 0) lock(); // Too harsh
```

---

## Future Enhancements

### 1. Device Nicknames
- User có thể đặt tên cho thiết bị
- "iPhone của tôi", "Laptop công ty"

### 2. Trusted Devices
- Mark device as "trusted"
- No 2FA required for trusted devices

### 3. Device Notifications
- Email khi có thiết bị mới đăng nhập
- Push notification

### 4. Advanced Fingerprinting
- Canvas fingerprinting
- WebGL fingerprinting
- Font detection

### 5. Geolocation Tracking
- Show device location on map
- Alert if login from unusual location

---

## Related Flows

- **FLOW 01**: Authentication
- **FLOW 10**: Subscription Management (affects max devices)
- **FLOW 04**: User Account Management

---

**Status:** ✅ COMPLETE  
**Implementation:** 100%  
**Last Updated:** 20/12/2025

