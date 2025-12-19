# HỆ THỐNG QUẢN LÝ THIẾT BỊ & BẢO MẬT - IMPLEMENTATION GUIDE

**Ngày:** 18/12/2025  
**Phiên bản:** 1.0  
**Trạng thái:** ✅ Hoàn thành triển khai

---

## 📋 TỔNG QUAN

Hệ thống quản lý thiết bị nâng cao đã được tích hợp vào dự án Ebook Store với các tính năng:

### ✨ Tính năng chính

1. **Device Fingerprinting** - Nhận diện thiết bị duy nhất
2. **Trusted Device** - Thiết bị đầu tiên không thể xóa  
3. **Device Limit** - Giới hạn 3 thiết bị/user
4. **Account Locking** - Khóa tự động sau 3 lần vi phạm
5. **Security Tracking** - Theo dõi login history và trust score

---

## 🗂️ CẤU TRÚC DỰ ÁN

### 1. Database (DB/)

```
DB/
├── ebook_store.sql                      # Schema chính
├── DEVICE_MANAGEMENT_MIGRATION.sql      # Migration script mới
```

**Bảng mới:**
- `user_devices` (đã cập nhật): Thêm fingerprint, IP, trust score
- `user_device_violations`: Log vi phạm giới hạn
- `device_login_history`: Lịch sử đăng nhập theo device

**Cột mới trong `users`:**
- `device_violation_count`: Đếm số lần vi phạm
- `account_locked_reason`: Lý do khóa
- `locked_at`, `locked_until`: Thời gian khóa

### 2. Backend (src/main/java/)

#### Entity Layer
```
entity/
├── User.java                     ✅ Đã cập nhật (thêm device management fields)
├── UserDevice.java               ✅ Đã cập nhật (thêm fingerprint, tracking)
├── UserDeviceViolation.java      ✅ Mới tạo
└── DeviceLoginHistory.java       ✅ Mới tạo
```

#### Repository Layer
```
repository/
├── UserDeviceRepository.java              ✅ Đã cập nhật (thêm query methods)
├── UserDeviceViolationRepository.java     ✅ Mới tạo
└── DeviceLoginHistoryRepository.java      ✅ Mới tạo
```

#### Service Layer
```
service/
├── UserService.java              ✅ Đã cập nhật (thêm device methods)
└── impl/
    └── UserServiceImpl.java      ✅ Đã cập nhật (logic chính tại đây)
```

**Logic chính trong UserServiceImpl:**
- `authenticateWithDeviceCheck()` - Xác thực + kiểm tra device
- `handleDeviceLimitExceeded()` - Xử lý vượt giới hạn
- `registerNewDevice()` - Đăng ký device mới
- `removeDevice()` - Xóa device (không cho xóa trusted)
- `unlockUserAccount()` - Unlock tài khoản (admin)

#### Controller Layer
```
controller/
├── AuthController.java                ✅ Đã cập nhật (tích hợp device check)
└── user/
    └── UserController.java            ✅ Đã cập nhật (thêm device endpoints)
```

**Endpoints mới:**
- `GET /user/devices` - Trang quản lý thiết bị
- `POST /user/devices/{deviceId}/remove` - Xóa thiết bị
- `GET /user/api/devices` - API lấy danh sách devices (JSON)

#### DTO Layer
```
dto/
├── DeviceInfoDto.java            ✅ Mới tạo (device info từ client)
└── DeviceResponseDto.java        ✅ Mới tạo (device info cho view)
```

#### Utility Layer
```
util/
└── DeviceFingerprintUtil.java    ✅ Mới tạo
```

**Chức năng:**
- `getClientIpAddress()` - Lấy IP thực (xử lý proxy)
- `generateServerSideFingerprint()` - Tạo fingerprint từ server
- `parseBrowserName()` - Parse browser từ User-Agent
- `parseOsName()` - Parse OS từ User-Agent
- `sha256Hash()` - Hash SHA256

### 3. Frontend (src/main/resources/)

#### JavaScript
```
static/shared/
└── device-fingerprint.js         ✅ Mới tạo (300+ lines)
```

**Chức năng:**
- Canvas fingerprinting
- WebGL fingerprinting
- Font detection
- Touch support detection
- Browser/OS detection
- SHA256 hashing (Crypto API)

#### Templates
```
templates/
├── auth/
│   └── login.html                ✅ Đã cập nhật (thêm fingerprint collection)
└── user/
    └── devices/
        └── manage.html           ✅ Mới tạo (giao diện quản lý)
```

---

## 🔄 LUỒNG HOẠT ĐỘNG

### 1. Login Flow với Device Checking

```
1. User nhập username/password trên login.html
2. JavaScript thu thập device fingerprint
3. Form submit với: username, password, deviceFingerprint, deviceName, deviceType
4. AuthController.processLogin() nhận request
5. Gọi UserService.authenticateWithDeviceCheck()
   
   ├─ 5.1. Xác thực username/password
   ├─ 5.2. Kiểm tra account có bị lock không
   ├─ 5.3. Kiểm tra device fingerprint đã tồn tại chưa
   │   ├─ Nếu TỒN TẠI → Update device info → Allow login → SUCCESS
   │   └─ Nếu CHƯA TỒN TẠI → Kiểm tra số lượng device
   │       ├─ Nếu < 3 → Đăng ký device mới → Allow login → SUCCESS
   │       └─ Nếu >= 3 → VƯỢT QUÁ GIỚI HẠN
   │           ├─ Tăng violation_count
   │           ├─ Log violation vào database
   │           ├─ Nếu violation_count >= 3 → LOCK ACCOUNT
   │           └─ Return DEVICE_LIMIT_EXCEEDED
   
6. AuthController xử lý kết quả
   ├─ SUCCESS → Tạo session → Redirect theo role
   ├─ DEVICE_LIMIT_EXCEEDED → Hiển thị lỗi với số lần vi phạm
   └─ ACCOUNT_LOCKED → Hiển thị lỗi "Liên hệ admin"
```

### 2. Device Management Flow

```
User vào /user/devices
├─ Hiển thị danh sách devices
│   ├─ Device hiện tại (badge xanh)
│   ├─ Trusted device (badge xanh dương, nút xóa disabled)
│   └─ Normal device (có thể xóa)
│
├─ User bấm "Xóa" trên device bình thường
│   ├─ Show confirmation modal
│   ├─ User confirm
│   ├─ AJAX POST /user/devices/{id}/remove
│   ├─ Backend check: device.isTrusted?
│   │   ├─ Nếu TRUE → Return error
│   │   └─ Nếu FALSE → Set isActive = false
│   └─ Reload page
│
└─ User bấm "Xóa" trên trusted device
    └─ Không cho phép (button disabled)
```

### 3. Admin Unlock Account Flow

```
Admin vào danh sách users → Thấy user bị lock
├─ Admin bấm "Unlock"
├─ Gọi UserService.unlockUserAccount(userId, adminId)
│   ├─ Set user.isActive = true
│   ├─ Clear account_locked_reason
│   ├─ Reset device_violation_count = 0
│   └─ Đánh dấu tất cả violations là resolved
└─ User có thể login lại bình thường
```

---

## 🔐 CƠ CHẾ BẢO MẬT

### 1. Device Fingerprinting

**Client-side (JavaScript):**
```javascript
{
  userAgent: "...",
  language: "vi-VN",
  screenResolution: "1920x1080",
  timezone: "Asia/Ho_Chi_Minh",
  canvas: "hash_of_canvas_drawing",
  webgl: { vendor: "...", renderer: "..." },
  fonts: "Arial,Verdana,...",
  touchSupport: { maxTouchPoints: 0 }
}
→ SHA256 hash → "a1b2c3d4e5f6..."
```

**Server-side (Fallback):**
```java
IP + User-Agent + Accept-Language + Accept-Encoding
→ SHA256 hash
```

### 2. Trust Score System

**Điểm khởi đầu:** 100

**Tăng điểm:**
- Login thành công từ cùng IP: +1 (max 100)

**Giảm điểm:**
- Đổi IP cùng subnet: -1
- Đổi IP khác subnet: -5 (đánh dấu suspicious)
- Fingerprint mismatch: -10

**Hành động:**
- Trust score < 50: Cảnh báo (có thể require 2FA trong tương lai)
- Trust score < 20: Tự động lock account

### 3. Violation Tracking

**Loại vi phạm:**
- `DEVICE_LIMIT_EXCEEDED` - Vượt quá 3 thiết bị
- `SUSPICIOUS_IP` - IP đột ngột đổi quốc gia
- `FINGERPRINT_MISMATCH` - Cùng IP nhưng fingerprint khác
- `RAPID_DEVICE_CHANGE` - Thêm >2 device trong 5 phút
- `UNTRUSTED_DEVICE_DELETE_ATTEMPT` - Cố xóa trusted device

**Severity:**
- LOW, MEDIUM, HIGH, CRITICAL

---

## 📊 DATABASE SCHEMA CHI TIẾT

### user_devices (Đã cập nhật)

```sql
device_id              VARCHAR(50) PK
user_id                VARCHAR(50) FK → users
device_name            VARCHAR(255)
device_type            ENUM('WEB','MOBILE','TABLET','DESKTOP')
device_token           VARCHAR(500) UNIQUE

-- Device Fingerprinting
device_fingerprint     VARCHAR(64) UNIQUE        -- SHA256 hash
ip_address             VARCHAR(45)               -- IPv4/IPv6
user_agent             VARCHAR(1000)
browser_name           VARCHAR(100)
os_name                VARCHAR(100)

-- Security
is_trusted             TINYINT(1) DEFAULT 0      -- Thiết bị đầu tiên
is_active              TINYINT(1) DEFAULT 1
trust_score            INT DEFAULT 100           -- 0-100

-- Tracking
login_count            INT DEFAULT 0
suspicious_login_count INT DEFAULT 0
last_ip                VARCHAR(45)
last_login             DATETIME
created_at             DATETIME
updated_at             DATETIME
```

### user_device_violations (Mới)

```sql
violation_id       BIGINT PK AUTO_INCREMENT
user_id            VARCHAR(50) FK → users
violation_type     ENUM(...)
device_id          VARCHAR(50)
device_fingerprint VARCHAR(64)
ip_address         VARCHAR(45)
description        TEXT
severity           ENUM('LOW','MEDIUM','HIGH','CRITICAL')
is_resolved        TINYINT(1) DEFAULT 0
resolved_by        VARCHAR(50)
resolved_at        DATETIME
created_at         DATETIME
```

### device_login_history (Mới)

```sql
history_id         BIGINT PK AUTO_INCREMENT
user_id            VARCHAR(50) FK → users
device_id          VARCHAR(50) FK → user_devices
device_fingerprint VARCHAR(64)
ip_address         VARCHAR(45)
user_agent         VARCHAR(1000)
login_status       ENUM('SUCCESS','FAILED','BLOCKED')
failure_reason     VARCHAR(500)
login_at           DATETIME
```

---

## 🚀 HƯỚNG DẪN TRIỂN KHAI

### Bước 1: Chạy Migration

```sql
-- Backup database trước khi chạy
mysqldump -u root -p ebook_store > backup_before_device_migration.sql

-- Chạy migration script
mysql -u root -p ebook_store < DB/DEVICE_MANAGEMENT_MIGRATION.sql

-- Kiểm tra kết quả
SELECT COUNT(*) FROM user_devices WHERE is_trusted = 1;  -- Phải có ít nhất 1
SELECT COUNT(*) FROM user_device_violations;
```

### Bước 2: Build & Deploy Backend

```bash
cd C:/Projects/Ebook_Store
mvn clean install -DskipTests
mvn spring-boot:run
```

### Bước 3: Test chức năng

**Test Case 1: Login bình thường**
- Login với user `vana` trên Chrome
- Kiểm tra device đã được đăng ký (is_trusted = 1)
- Đăng nhập lại → Không tạo device mới

**Test Case 2: Vượt giới hạn**
- Login trên Chrome → Device 1
- Login trên Firefox → Device 2
- Login trên Edge → Device 3
- Login trên Safari → BỊ CHẶN (violation_count = 1)
- Thử 2 lần nữa → ACCOUNT LOCKED

**Test Case 3: Xóa device**
- Vào `/user/devices`
- Thấy 3 devices, 1 trusted (không xóa được)
- Xóa device thường → OK
- Login trên device mới → OK (còn slot)

**Test Case 4: Admin unlock**
- User bị lock
- Admin vào admin panel
- Unlock user
- User login lại được

---

## 📱 GIAO DIỆN NGƯỜI DÙNG

### 1. Login Page (`/auth/login`)

**Cập nhật:**
- ✅ Tự động thu thập device fingerprint khi load page
- ✅ Hidden fields: deviceFingerprint, deviceName, deviceType
- ✅ Loading state khi submit
- ✅ Hiển thị lỗi chi tiết khi vượt giới hạn

**Thông báo lỗi mới:**
```
⚠️ Bạn đã đạt giới hạn 3 thiết bị. 
Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị. 
Cảnh báo: 1/3 lần vi phạm.
```

### 2. Device Management Page (`/user/devices`)

**Sections:**

1. **Device Limit Info Card**
   - Hiển thị: 2/3 devices đã sử dụng
   - Progress bar màu (xanh → vàng → đỏ)

2. **Violation Warning** (nếu có)
   - Cảnh báo đỏ khi vi phạm > 0
   - Countdown: "Sau 3 lần tài khoản sẽ bị khóa"

3. **Device List**
   - Card cho mỗi device:
     ```
     [Icon] Chrome on Windows
            Thiết bị hiện tại  Tin cậy
     
     IP: 192.168.1.1     Login: 15 lần
     3 ngày trước        Tin cậy: 95/100
     
     [Progress bar trust score]
     
     Đăng ký: 01/12/2025    [Xóa] / [Không thể xóa]
     ```

4. **Info Cards**
   - Lưu ý quan trọng
   - Hướng dẫn bảo mật

**Tính năng:**
- ✅ Real-time display current device
- ✅ Badge "Thiết bị tin cậy" (xanh dương)
- ✅ Badge "Thiết bị hiện tại" (xanh lá)
- ✅ Nút "Xóa" disabled cho trusted device
- ✅ Modal xác nhận trước khi xóa
- ✅ AJAX xóa không reload page

---

## 🧪 TEST SCENARIOS

### Scenario 1: Normal User Journey
```
1. User mới đăng ký → vana@gmail.com
2. Login lần đầu trên Chrome Windows → Device 1 (trusted)
3. Logout
4. Login lại trên Chrome Windows → Không tạo device mới (match fingerprint)
5. Login trên Firefox Windows → Device 2
6. Login trên Safari Mac → Device 3
7. Vào /user/devices → Thấy 3 devices
8. Xóa Device 2 (Firefox)
9. Login trên Edge → OK (vì còn 2/3 devices)
```

### Scenario 2: Violation Journey
```
1. User có 3 devices đầy
2. Cố login trên device thứ 4 → Violation #1
   → Thông báo: "Cảnh báo: 1/3 lần vi phạm"
3. Cố lại → Violation #2
   → Thông báo: "Cảnh báo: 2/3 lần vi phạm"
4. Cố lại → Violation #3
   → ACCOUNT LOCKED
   → "Tài khoản đã bị khóa. Liên hệ admin."
5. User không thể login được nữa
6. Admin unlock
7. User login OK
```

### Scenario 3: Security Detection
```
1. User login từ Vietnam IP → 103.x.x.x
2. Sau 10 phút login từ US IP → 45.x.x.x
3. System log: SUSPICIOUS_IP violation
4. Trust score giảm -5
5. Nếu tiếp tục: -5, -5, ... → < 20 → Auto lock
```

---

## 🔧 CẤU HÌNH

### application.properties

```properties
# Device Management Configuration
device.max-per-user=3
device.violation-threshold=3
device.trust-score-threshold=50
device.ip-change-window-minutes=60
device.suspicious-activity-check=true

# Optional: Geolocation API (để phát hiện IP jumping)
# geolocation.api.key=YOUR_API_KEY
# geolocation.api.url=https://ipapi.co/{ip}/json/
```

### Constants trong UserServiceImpl.java

```java
private static final int MAX_DEVICES_PER_USER = 3;
private static final int MAX_VIOLATIONS_BEFORE_LOCK = 3;
```

**Có thể thay đổi:**
- Tăng MAX_DEVICES lên 5 cho user VIP
- Giảm MAX_VIOLATIONS xuống 2 để chặt chẽ hơn

---

## 📈 FUTURE ENHANCEMENTS

### Phase 2 (Nếu cần mở rộng)

1. **2FA Integration**
   - Require OTP khi trust score < 50
   - SMS/Email verification cho device mới

2. **Geolocation Blocking**
   - Tích hợp IP Geolocation API
   - Block login từ quốc gia lạ
   - Whitelist countries

3. **Device Auto-cleanup**
   - Xóa auto devices không dùng > 90 ngày
   - Email thông báo trước khi xóa

4. **Advanced Analytics**
   - Dashboard admin: Top suspicious users
   - Chart: Login patterns, IP maps
   - Real-time alerts

5. **User Notifications**
   - Email khi có device mới đăng ký
   - Email cảnh báo khi vi phạm
   - Push notification (nếu có mobile app)

---

## 🐛 TROUBLESHOOTING

### Vấn đề 1: Device bị tạo trùng

**Triệu chứng:** Mỗi lần login tạo device mới

**Nguyên nhân:** Fingerprint không stable (khác nhau mỗi lần)

**Giải pháp:**
- Check JavaScript console xem fingerprint có giống nhau không
- Fallback sang server-side fingerprint (IP + User-Agent)
- Thêm tolerance cho small changes

### Vấn đề 2: Không xóa được trusted device

**Triệu chứng:** Nút "Xóa" bị disabled

**Nguyên nhân:** Đúng behavior, đây là thiết bị tin cậy

**Giải pháp:**
- User cần liên hệ admin để force delete
- Hoặc admin vào database: `UPDATE user_devices SET is_trusted=0 WHERE ...`

### Vấn đề 3: Account bị lock nhầm

**Triệu chứng:** User bình thường bị lock

**Nguyên nhân:** IP động (3G/4G), fingerprint thay đổi

**Giải pháp:**
- Admin unlock ngay: `CALL sp_unlock_account('user_id', 'admin_id')`
- Tăng MAX_VIOLATIONS lên 5
- Implement IP subnet matching (đã có trong code)

---

## 📚 API REFERENCE

### Public Endpoints

```
POST /auth/login
  Body: username, password, deviceFingerprint, deviceName, deviceType
  Return: Redirect hoặc Error
```

### User Endpoints (Authenticated)

```
GET /user/devices
  Return: HTML page with device list

POST /user/devices/{deviceId}/remove
  Return: { success: true/false, message: "..." }

GET /user/api/devices
  Return: { success: true, devices: [...], currentCount: 2, maxDevices: 3 }
```

### Admin Endpoints (Future)

```
GET /admin/violations
  Return: List of all violations

POST /admin/users/{userId}/unlock
  Return: { success: true }

DELETE /admin/devices/{deviceId}
  Return: { success: true } (Force delete any device)
```

---

## ✅ CHECKLIST TRIỂN KHAI

- [x] Database migration script
- [x] Entity classes (User, UserDevice, Violation, History)
- [x] Repository interfaces
- [x] Service logic (authenticateWithDeviceCheck, removeDevice, unlock)
- [x] Controller endpoints (AuthController, UserController)
- [x] DTO classes
- [x] Utility classes (DeviceFingerprintUtil)
- [x] Frontend JavaScript (device-fingerprint.js)
- [x] Login page integration
- [x] Device management page
- [x] Documentation

---

## 📞 SUPPORT

**Nếu gặp vấn đề:**
1. Check console logs (backend & browser)
2. Check database: `SELECT * FROM user_devices WHERE user_id='...'`
3. Check violations: `SELECT * FROM user_device_violations WHERE user_id='...'`
4. Review this documentation

**Liên hệ:**
- Developer: [Your Name]
- Email: [Your Email]

---

**Chúc triển khai thành công! 🎉**

