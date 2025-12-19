# TÀI LIỆU KỸ THUẬT: HỆ THỐNG QUẢN LÝ THIẾT BỊ NÂNG CAO

## 1. Tổng quan

Hệ thống quản lý thiết bị nâng cao với các tính năng:
- **Device Fingerprinting**: Kết hợp IP Address + User-Agent + Canvas/WebGL fingerprint
- **Trusted Device**: Thiết bị đăng ký lần đầu không thể xóa
- **Account Locking**: Khóa tài khoản sau 3 lần vượt quá giới hạn thiết bị
- **Security Tracking**: Theo dõi các hành vi đáng ngờ

## 2. Database Schema

### 2.1. Bảng `user_devices` (Cập nhật)
```sql
CREATE TABLE `user_devices` (
  `device_id` varchar(50) NOT NULL PRIMARY KEY,
  `user_id` varchar(50) NOT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `device_type` enum('WEB','MOBILE','TABLET','DESKTOP') DEFAULT 'WEB',
  
  -- Device Fingerprinting
  `device_fingerprint` varchar(64) NOT NULL COMMENT 'SHA256 hash của fingerprint',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IPv4 hoặc IPv6',
  `user_agent` varchar(1000) DEFAULT NULL,
  
  -- Security & Trust
  `is_trusted` tinyint(1) DEFAULT 0 COMMENT 'Thiết bị đầu tiên = trusted',
  `is_active` tinyint(1) DEFAULT 1,
  `trust_score` int DEFAULT 100 COMMENT 'Điểm tin cậy (0-100)',
  
  -- Tracking
  `last_login` datetime DEFAULT NULL,
  `login_count` int DEFAULT 0,
  `suspicious_login_count` int DEFAULT 0 COMMENT 'Số lần đăng nhập đáng ngờ',
  
  -- Timestamps
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  -- Indexes
  UNIQUE KEY `device_fingerprint_unique` (`device_fingerprint`),
  KEY `user_id` (`user_id`),
  KEY `ip_address` (`ip_address`),
  
  CONSTRAINT `devices_user_fk` FOREIGN KEY (`user_id`) 
    REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.2. Bảng `user_device_violations` (Mới)
```sql
CREATE TABLE `user_device_violations` (
  `violation_id` bigint AUTO_INCREMENT PRIMARY KEY,
  `user_id` varchar(50) NOT NULL,
  `violation_type` enum('DEVICE_LIMIT_EXCEEDED','SUSPICIOUS_IP','FINGERPRINT_MISMATCH','RAPID_DEVICE_CHANGE') NOT NULL,
  `device_fingerprint` varchar(64),
  `ip_address` varchar(45),
  `description` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  
  KEY `user_id` (`user_id`),
  KEY `created_at` (`created_at`),
  
  CONSTRAINT `violations_user_fk` FOREIGN KEY (`user_id`) 
    REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.3. Bảng `users` (Thêm cột)
```sql
ALTER TABLE `users` 
ADD COLUMN `device_violation_count` int DEFAULT 0 COMMENT 'Số lần vi phạm giới hạn thiết bị',
ADD COLUMN `account_locked_reason` varchar(500) DEFAULT NULL,
ADD COLUMN `locked_at` datetime DEFAULT NULL,
ADD COLUMN `locked_until` datetime DEFAULT NULL;
```

## 3. Device Fingerprinting Strategy

### 3.1. Client-Side Collection (JavaScript)
```javascript
// Tạo fingerprint từ:
const fingerprint = {
  userAgent: navigator.userAgent,
  language: navigator.language,
  platform: navigator.platform,
  screenResolution: `${screen.width}x${screen.height}`,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  canvasFingerprint: generateCanvasFingerprint(),
  webglFingerprint: generateWebGLFingerprint()
};

// Hash tất cả thành SHA256
const deviceFingerprint = sha256(JSON.stringify(fingerprint));
```

### 3.2. Server-Side Validation
```java
public String calculateDeviceFingerprint(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    
    // IP Address
    String ipAddress = getClientIP(request);
    sb.append(ipAddress).append("|");
    
    // User-Agent
    String userAgent = request.getHeader("User-Agent");
    sb.append(userAgent).append("|");
    
    // Accept-Language
    String language = request.getHeader("Accept-Language");
    sb.append(language);
    
    // SHA256 hash
    return DigestUtils.sha256Hex(sb.toString());
}
```

## 4. Business Logic Flow

### 4.1. Login Flow với Device Check
```
1. User nhập username/password
2. Validate credentials
3. Extract device fingerprint từ request
4. Check existing device:
   - Nếu fingerprint match + IP gần đúng → Allow (update last_login)
   - Nếu fingerprint mới:
     a. Count active devices của user
     b. Nếu < limit (3 devices):
        - Register new device
        - Nếu là device đầu tiên → Set is_trusted = true
        - Allow login
     c. Nếu >= limit:
        - Increment violation_count
        - Log violation
        - Nếu violation_count >= 3:
          * Lock account
          * Send notification to admin
          * Return error "Account locked, contact admin"
        - Else:
          * Return error "Device limit reached"
```

### 4.2. Trusted Device Rules
```
- Device đầu tiên đăng ký: is_trusted = true, KHÔNG THỂ XÓA
- Mọi device khác: is_trusted = false, có thể xóa
- User có thể xem danh sách devices, nhưng nút "Xóa" bị disable cho trusted device
```

### 4.3. Account Locking Logic
```java
if (user.getDeviceViolationCount() >= 3) {
    user.setIsActive(false);
    user.setAccountLockedReason("Vượt quá 3 lần giới hạn thiết bị. Vui lòng liên hệ admin.");
    user.setLockedAt(LocalDateTime.now());
    // Admin phải manually unlock
}
```

## 5. Security Features

### 5.1. Suspicious Activity Detection
- **IP Jump**: Đăng nhập từ 2 quốc gia khác nhau trong < 1 giờ
- **Rapid Device Change**: Thêm > 2 devices trong 5 phút
- **Fingerprint Mismatch**: Cùng IP nhưng fingerprint đổi hoàn toàn

### 5.2. Trust Score System
```
Initial score: 100
- Mỗi lần login thành công từ cùng IP: +1 (max 100)
- Mỗi lần đổi IP đột ngột: -5
- Mỗi lần fingerprint mismatch: -10
- Score < 50: Require email verification
- Score < 20: Auto lock account
```

## 6. API Endpoints

### 6.1. Device Management APIs
```
POST   /api/devices/register        - Đăng ký thiết bị mới
GET    /api/devices/list            - Danh sách thiết bị của user
DELETE /api/devices/{deviceId}      - Xóa thiết bị (không cho phép xóa trusted)
GET    /api/devices/current         - Thông tin thiết bị hiện tại
POST   /api/devices/verify          - Verify thiết bị qua email/SMS
```

### 6.2. Admin APIs
```
GET    /api/admin/violations        - Danh sách vi phạm
POST   /api/admin/users/{userId}/unlock  - Mở khóa tài khoản
GET    /api/admin/users/{userId}/devices - Xem tất cả devices của user
DELETE /api/admin/devices/{deviceId}     - Force delete device (admin only)
```

## 7. Frontend Integration

### 7.1. Login Page Enhancement
```html
<!-- Thêm vào form login -->
<input type="hidden" id="deviceFingerprint" name="deviceFingerprint">
<input type="hidden" id="deviceInfo" name="deviceInfo">

<script src="/js/device-fingerprint.js"></script>
<script>
  document.querySelector('form').addEventListener('submit', async (e) => {
    const fp = await generateFingerprint();
    document.getElementById('deviceFingerprint').value = fp.hash;
    document.getElementById('deviceInfo').value = JSON.stringify(fp.details);
  });
</script>
```

### 7.2. Device Management Page
```
/user/devices
- Hiển thị danh sách devices
- Hiển thị badge "Thiết bị tin cậy" cho trusted device
- Nút "Xóa" disabled cho trusted device
- Hiển thị last login, IP, location
```

## 8. Configuration

### 8.1. application.properties
```properties
# Device Management
device.max-per-user=3
device.violation-threshold=3
device.trust-score-threshold=50
device.ip-change-window-minutes=60
device.suspicious-activity-check=true

# Geolocation API (optional)
geolocation.api.key=YOUR_API_KEY
geolocation.api.url=https://ipapi.co/{ip}/json/
```

## 9. Testing Scenarios

### 9.1. Normal User Flow
1. User đăng nhập lần đầu trên Chrome → Device A (trusted)
2. User đăng nhập trên Firefox → Device B (normal)
3. User đăng nhập trên Safari → Device C (normal)
4. User cố đăng nhập trên Edge → Bị từ chối (vượt quá 3)

### 9.2. Violation Scenario
1. User cố đăng nhập device thứ 4 → Violation #1
2. User cố lại → Violation #2
3. User cố lại → Violation #3 → Account LOCKED

### 9.3. Trusted Device Protection
1. User vào /user/devices
2. Thấy Device A có badge "Thiết bị tin cậy"
3. Bấm "Xóa" → Không được (button disabled)

## 10. Migration Steps

1. **Backup database**
2. **Run migration SQL** (thêm cột, tạo bảng mới)
3. **Deploy new code**
4. **Set existing first device as trusted** (data migration)
5. **Test login flow**
6. **Monitor violations table**

---

## APPENDIX A: Sample Data Migration Script
```sql
-- Đánh dấu device cũ nhất của mỗi user là trusted
UPDATE user_devices ud1
SET is_trusted = 1
WHERE device_id IN (
  SELECT device_id FROM (
    SELECT device_id, 
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at ASC) as rn
    FROM user_devices
  ) sub
  WHERE rn = 1
);
```

## APPENDIX B: Device Fingerprint JavaScript Library
Location: `/src/main/resources/static/js/device-fingerprint.js`
(See implementation in code)

