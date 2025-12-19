# 🔐 DEVICE MANAGEMENT - QUICK START

> Hệ thống quản lý thiết bị với giới hạn 3 thiết bị/user, trusted device không thể xóa, tự động khóa tài khoản sau 3 lần vi phạm.

## 🚀 Triển khai nhanh (5 phút)

### 1. Chạy Migration SQL

```bash
mysql -u root -p ebook_store < DB/DEVICE_MANAGEMENT_MIGRATION.sql
```

### 2. Start Application

```bash
mvn spring-boot:run
```

### 3. Test

1. **Login lần đầu:** `http://localhost:8080/auth/login`
   - Username: `vana` / Password: `123`
   - → Device đầu tiên = **Trusted** (không xóa được)

2. **Quản lý thiết bị:** `http://localhost:8080/user/devices`
   - Xem danh sách devices
   - Xóa device thường (không phải trusted)

3. **Test vượt giới hạn:**
   - Login trên 4 trình duyệt khác nhau
   - Lần thứ 4 sẽ bị chặn → Vi phạm +1
   - Lần thứ 7 → Account bị khóa

---

## 📋 Files đã thay đổi/tạo mới

### Database
- ✅ `DB/DEVICE_MANAGEMENT_MIGRATION.sql` **(CHẠY ĐẦU TIÊN)**

### Backend (Java)
- ✅ `entity/User.java` - Thêm device violation fields
- ✅ `entity/UserDevice.java` - Thêm fingerprint, trust score
- ✅ `entity/UserDeviceViolation.java` - **MỚI**
- ✅ `entity/DeviceLoginHistory.java` - **MỚI**
- ✅ `repository/UserDeviceRepository.java` - Thêm query methods
- ✅ `repository/UserDeviceViolationRepository.java` - **MỚI**
- ✅ `repository/DeviceLoginHistoryRepository.java` - **MỚI**
- ✅ `service/UserService.java` - Thêm device management interface
- ✅ `service/impl/UserServiceImpl.java` - **LOGIC CHÍNH Ở ĐÂY**
- ✅ `controller/AuthController.java` - Tích hợp device check
- ✅ `controller/user/UserController.java` - Thêm device endpoints
- ✅ `dto/DeviceInfoDto.java` - **MỚI**
- ✅ `dto/DeviceResponseDto.java` - **MỚI**
- ✅ `util/DeviceFingerprintUtil.java` - **MỚI**

### Frontend
- ✅ `static/shared/device-fingerprint.js` - **MỚI** (300+ lines)
- ✅ `templates/auth/login.html` - Tích hợp fingerprint
- ✅ `templates/user/devices/manage.html` - **MỚI** (giao diện quản lý)

### Docs
- ✅ `docs/DEVICE_MANAGEMENT_IMPLEMENTATION_GUIDE.md` - **Chi tiết đầy đủ**
- ✅ `docs/DEVICE_MANAGEMENT_TECHNICAL_SPEC.md` - Spec kỹ thuật
- ✅ `docs/NGHIEP_VU_CHI_TIET_DEVICE_DOWNLOAD.md` - Đã có sẵn

---

## 🎯 Các API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/auth/login` | Login với device check |
| GET | `/user/devices` | Trang quản lý thiết bị |
| POST | `/user/devices/{id}/remove` | Xóa thiết bị |
| GET | `/user/api/devices` | API lấy devices (JSON) |

---

## 🔑 Logic quan trọng

### authenticateWithDeviceCheck() - UserServiceImpl.java (Line ~273)

```java
// 1. Xác thực user/pass
// 2. Check account locked?
// 3. Lấy device fingerprint
// 4. Check device đã tồn tại?
//    ├─ CÓ → Update & allow login
//    └─ KHÔNG → Check số lượng
//        ├─ < 3 → Đăng ký mới
//        └─ >= 3 → VI PHẠM
//            ├─ violation_count++
//            └─ >= 3 → LOCK ACCOUNT
```

### Device Fingerprint (device-fingerprint.js)

```javascript
// Client-side: Canvas + WebGL + Fonts + ... → SHA256 hash
// Server-side (fallback): IP + User-Agent → SHA256 hash
```

### Trusted Device Rule

```
- Device đầu tiên: is_trusted = true
- Trusted device: KHÔNG THỂ XÓA (button disabled)
- Normal device: Có thể xóa
```

---

## ⚙️ Configuration

### Giới hạn hiện tại (Theo Subscription)

| Gói | Max Devices |
|-----|-------------|
| FREE | 1 thiết bị |
| BASIC | 2 thiết bị |
| PREMIUM/VIP | 3 thiết bị |

```java
// Dynamic theo subscription của user
private int getMaxDevicesForUser(User user) { ... }
private static final int MAX_VIOLATIONS_BEFORE_LOCK = 3;
```

### Có thể thay đổi trong DB:

```sql
-- Xem thiết bị của user
SELECT * FROM user_devices WHERE user_id = 'user_normal_01';

-- Xem vi phạm
SELECT * FROM user_device_violations WHERE user_id = 'user_normal_01';

-- Unlock user (nếu bị khóa)
UPDATE users SET 
  is_active = 1, 
  device_violation_count = 0, 
  account_locked_reason = NULL 
WHERE user_id = 'user_normal_01';

-- Xóa tất cả devices của user (reset)
UPDATE user_devices SET is_active = 0 WHERE user_id = 'user_normal_01';

-- Hoặc dùng Stored Procedure:
CALL sp_unlock_account('user_normal_01', 'admin_01');
```

---

## 🧪 Test Cases

### Test 1: Login bình thường ✅
```
1. Login với vana/123 trên Chrome
2. Check DB: SELECT * FROM user_devices WHERE user_id='user_normal_01'
3. Thấy 1 device với is_trusted=1
4. Login lại trên Chrome → Không tạo device mới
```

### Test 2: Vượt giới hạn ❌
```
1. Login trên Chrome → Device 1
2. Login trên Firefox → Device 2  
3. Login trên Edge → Device 3
4. Login trên Safari → BỊ CHẶN
   → Check: device_violation_count = 1
5. Thử 2 lần nữa → Account locked
```

### Test 3: Xóa device ✅
```
1. Vào /user/devices
2. Thấy 3 devices: 1 trusted + 2 normal
3. Xóa 1 normal device
4. Login trên Safari → OK (còn slot)
```

---

## 🐛 Troubleshooting

### Lỗi: "Account locked"
```sql
-- Unlock ngay:
UPDATE users SET is_active=1, device_violation_count=0 WHERE user_id='...';
```

### Lỗi: "Cannot resolve MVC view 'user/devices/manage'"
```bash
# Check file tồn tại:
ls src/main/resources/templates/user/devices/manage.html
# Nếu không có, tạo lại từ code đã cung cấp
```

### Lỗi: Device bị tạo trùng mỗi lần login
```
# Check JavaScript console:
# - DeviceFingerprint có generate thành công không?
# - Hash có giống nhau mỗi lần không?
# Nếu không stable → Fallback sang server-side fingerprint
```

---

## 📚 Tài liệu chi tiết

📖 **Xem thêm:** [DEVICE_MANAGEMENT_IMPLEMENTATION_GUIDE.md](./DEVICE_MANAGEMENT_IMPLEMENTATION_GUIDE.md)

Tài liệu đầy đủ 900+ lines với:
- Kiến trúc chi tiết
- Database schema
- Security mechanisms
- API reference
- Test scenarios
- Future enhancements

---

## ✅ Checklist hoàn thành

- [x] Database migration
- [x] Backend logic (Service + Controller)
- [x] Entity + Repository
- [x] Device fingerprinting (JavaScript)
- [x] UI quản lý thiết bị
- [x] Login integration
- [x] Violation tracking
- [x] Account locking
- [x] Trusted device protection
- [x] Documentation

**Status:** ✅ Hoàn thành 100%

---

**Happy coding! 🎉**

