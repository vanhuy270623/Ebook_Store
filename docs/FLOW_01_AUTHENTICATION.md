# 🔐 FLOW 01: AUTHENTICATION (Xác Thực Người Dùng)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 1.1: Đăng Ký](#flow-11-đăng-ký)
3. [Flow 1.2: Đăng Nhập với Device Management](#flow-12-đăng-nhập-với-device-management)
4. [Flow 1.3: Đăng Xuất](#flow-13-đăng-xuất)
5. [Device Management & Security](#device-management--security)
6. [Debugging Endpoints](#debugging-endpoints)

---

## Tổng Quan

### Components
- **Controller**: `AuthController.java`
- **Service**: `UserService.java`, `UserServiceImpl.java`, `UserDeviceService.java`
- **Repository**: `UserRepository.java`, `RoleRepository.java`, `UserDeviceRepository.java`
- **Entity**: `User.java`, `Role.java`, `UserDevice.java`
- **Security**: `SecurityConfig.java`
- **Utilities**: `DeviceFingerprintUtil.java`

### URLs
- `GET /auth/register` - Trang đăng ký
- `POST /auth/register` - Xử lý đăng ký
- `GET /auth/login` - Trang đăng nhập
- `POST /auth/login` - Xử lý đăng nhập (với device checking)
- `GET /logout` - Đăng xuất (deprecated)
- `POST /auth/logout` - Đăng xuất (CSRF protected, recommended)

### Key Features
- ✅ Đăng ký tài khoản với validation
- ✅ Đăng nhập với Device Fingerprinting
- ✅ Device Management (giới hạn thiết bị theo subscription)
- ✅ Auto-lock account sau 3 lần vi phạm
- ✅ Soft delete cho users
- ✅ Admin auto-assign VIP subscription
- ✅ Role-based redirection

---

## Flow 1.1: Đăng Ký

### Sequence Diagram
```
User → Browser → AuthController → UserService → UserRepository → Database
  │       │            │               │              │             │
  │  GET /auth/register                                             │
  │───────────────────►│                                            │
  │◄───────────────────┤ (return auth/register.html)               │
  │       │            │                                            │
  │  POST /auth/register (RegisterDto)                             │
  │───────────────────►│                                            │
  │       │            │ validation (BindingResult)                 │
  │       │            │ registerUser()                             │
  │       │            ├──────────────►│                            │
  │       │            │                │ checkUsernameExists()     │
  │       │            │                ├──────────────►│           │
  │       │            │                │ findActiveByUsername()    │
  │       │            │                │                ├──────────►│
  │       │            │                │◄──────────────┤           │
  │       │            │                │ checkEmailExists()        │
  │       │            │                ├──────────────►│           │
  │       │            │                │ findActiveByEmail()       │
  │       │            │                │                ├──────────►│
  │       │            │                │◄──────────────┤           │
  │       │            │                │ generateNextUserId()      │
  │       │            │                │ hashPassword()            │
  │       │            │                │ save()                    │
  │       │            │                ├──────────────►│           │
  │       │            │                │                │ INSERT   │
  │       │            │                │                ├──────────►│
  │       │            │                │◄──────────────┤           │
  │       │            │◄──────────────┤                            │
  │◄───────────────────┤ redirect:/auth/login                       │
```

### Implementation Details

**Endpoint**: `POST /auth/register`

**DTO Class**: `RegisterDto.java`
```java
public class RegisterDto {
    @NotEmpty(message = "Tên người dùng không được để trống")
    @Size(min = 3, max = 50, message = "Tên người dùng phải có từ 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", 
             message = "Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String username;

    @NotEmpty(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotEmpty(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
```

**Request (Form Data)**:
```
username: john_doe
email: john@example.com
password: SecurePass123
```

**Service Logic** (`UserServiceImpl.java`):
```java
@Override
public void registerUser(RegisterDto registerDto) throws Exception {
    // 1. Kiểm tra username đã tồn tại (bao gồm cả user đã bị xóa mềm)
    Optional<User> existingUserByUsername = 
        userRepository.findByUsernameIncludingDeleted(registerDto.getUsername());
    if (existingUserByUsername.isPresent()) {
        User existingUser = existingUserByUsername.get();
        if (existingUser.isDeleted()) {
            throw new Exception("Tên người dùng '" + registerDto.getUsername() +
                "' đã từng được sử dụng bởi tài khoản đã bị xóa. " +
                "Vui lòng chọn tên khác hoặc liên hệ quản trị viên.");
        } else {
            throw new Exception("Tên người dùng '" + registerDto.getUsername() +
                "' đã được sử dụng. Vui lòng chọn tên khác.");
        }
    }
    
    // 2. Kiểm tra email đã tồn tại (bao gồm cả user đã bị xóa mềm)
    Optional<User> existingUserByEmail = 
        userRepository.findByEmailIncludingDeleted(registerDto.getEmail());
    if (existingUserByEmail.isPresent()) {
        User existingUser = existingUserByEmail.get();
        if (existingUser.isDeleted()) {
            throw new Exception("Email '" + registerDto.getEmail() +
                "' đã từng được đăng ký cho tài khoản đã bị xóa. " +
                "Vui lòng sử dụng email khác hoặc liên hệ quản trị viên.");
        } else {
            throw new Exception("Email '" + registerDto.getEmail() +
                "' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
        }
    }
    
    // 3. Tìm role "USER" từ database
    Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
        .orElseThrow(() -> new Exception(
            "Lỗi hệ thống: Không tìm thấy Role 'USER'. " +
            "Vui lòng liên hệ quản trị viên."));
    
    // 4. Tự động sinh User ID theo format "user_normal_XX"
    String newUserId = generateNextUserId(); // VD: user_normal_01, user_normal_02
    
    // 5. Tạo đối tượng User mới
    User user = new User();
    user.setUserId(newUserId);
    user.setUsername(registerDto.getUsername());
    user.setEmail(registerDto.getEmail());
    user.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
    user.setRole(userRole);
    user.setIsActive(true);
    user.setIsVerified(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    
    // 6. Lưu vào database với error handling
    try {
        userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
        // Xử lý lỗi constraint violation từ database
        String errorMessage = e.getMessage();
        if (errorMessage.contains("users.email")) {
            throw new Exception("Email đã được đăng ký. Vui lòng sử dụng email khác.");
        } else if (errorMessage.contains("users.username")) {
            throw new Exception("Tên người dùng đã tồn tại. Vui lòng chọn tên khác.");
        } else {
            throw new Exception("Không thể tạo tài khoản. Vui lòng thử lại.");
        }
    }
}

/**
 * Sinh User ID tự động theo format "user_normal_XX"
 */
private String generateNextUserId() {
    long userCount = userRepository.countActive();
    int nextNumber = (int) userCount + 1;
    return String.format("user_normal_%02d", nextNumber);
}
```

**SQL Queries**:
```sql
-- Check username exists (including soft deleted)
SELECT * FROM users WHERE username = ? AND deleted_at IS NULL

-- Check email exists (including soft deleted)
SELECT * FROM users WHERE email = ? AND deleted_at IS NULL

-- Get USER role
SELECT * FROM roles WHERE role_name = 'USER'

-- Count active users (for ID generation)
SELECT COUNT(*) FROM users WHERE deleted_at IS NULL

-- Insert new user
INSERT INTO users (
    user_id, username, email, password_hash, role_id,
    is_active, is_verified, created_at, updated_at
) VALUES (?, ?, ?, ?, ?, true, false, NOW(), NOW())
```

**Success Response**:
```
Redirect to: /auth/login
Flash Message: "✅ Đăng ký thành công! Vui lòng đăng nhập để tiếp tục."
```

**Error Responses**:
```
Validation Errors:
- "Tên người dùng không được để trống"
- "Tên người dùng phải có từ 3-50 ký tự"
- "Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới"
- "Email không hợp lệ"
- "Mật khẩu phải có ít nhất 6 ký tự"

Business Logic Errors:
- "Tên người dùng '{username}' đã được sử dụng. Vui lòng chọn tên khác."
- "Email '{email}' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."
- "Tên người dùng '{username}' đã từng được sử dụng bởi tài khoản đã bị xóa..."
- "Lỗi hệ thống: Không tìm thấy Role 'USER'..."
```

---

## Flow 1.2: Đăng Nhập với Device Management

### Sequence Diagram
```
User → Browser → AuthController → UserService → DeviceRepo → Database
  │       │            │               │              │          │
  │  GET /auth/login                                             │
  │───────────────────►│                                         │
  │◄───────────────────┤ (return auth/login.html)               │
  │       │            │                                         │
  │  POST /auth/login (username, password, deviceFingerprint)   │
  │───────────────────►│                                         │
  │       │            │ authenticateWithDeviceCheck()           │
  │       │            ├──────────────►│                         │
  │       │            │                │ authenticateUser()     │
  │       │            │                │ check isLocked()       │
  │       │            │                │ findByUsername()       │
  │       │            │                │ verifyPassword()       │
  │       │            │                │                        │
  │       │            │                │ getDeviceFingerprint() │
  │       │            │                │ findActiveDeviceByFingerprint()
  │       │            │                ├──────────────►│        │
  │       │            │                │◄──────────────┤        │
  │       │            │                │                        │
  │       │            │                │ [IF DEVICE EXISTS]     │
  │       │            │                │   updateDeviceOnLogin()│
  │       │            │                │   return SUCCESS       │
  │       │            │                │                        │
  │       │            │                │ [IF NEW DEVICE]        │
  │       │            │                │   getMaxDevicesForUser()│
  │       │            │                │   countActiveDevices() │
  │       │            │                ├──────────────►│        │
  │       │            │                │◄──────────────┤ count  │
  │       │            │                │                        │
  │       │            │                │ [IF count >= max && !ADMIN]
  │       │            │                │   incrementViolation() │
  │       │            │                │   [IF violations >= 3] │
  │       │            │                │     lockAccount()      │
  │       │            │                │   return LIMIT_EXCEEDED│
  │       │            │                │                        │
  │       │            │                │ [IF OK]               │
  │       │            │                │   registerNewDevice()  │
  │       │            │                ├──────────────►│        │
  │       │            │                │                │ INSERT │
  │       │            │                │                ├───────►│
  │       │            │                │◄──────────────┤        │
  │       │            │                │   return SUCCESS       │
  │       │            │◄──────────────┤                         │
  │       │            │                                         │
  │       │            │ createSpringSecurityContext()           │
  │       │            │ setSessionAttributes()                  │
  │       │            │ updateLastLogin()                       │
  │◄───────────────────┤ redirect based on role                 │
```

### Implementation Details

**Endpoint**: `POST /auth/login`

**Request Parameters** (Form Data + Hidden Fields):
```
username: john_doe
password: SecurePass123
deviceFingerprint: a1b2c3d4e5f6... (generated by JavaScript)
deviceName: Chrome on Windows 10 (optional)
deviceType: WEB (optional: WEB, MOBILE, TABLET)
```

**JavaScript - Device Fingerprint Generation** (in login.html):
```javascript
// Tạo device fingerprint từ browser
const fingerprint = btoa(
    navigator.userAgent + 
    screen.width + 'x' + screen.height + 
    navigator.language + 
    new Date().getTimezoneOffset()
);
document.getElementById('deviceFingerprint').value = fingerprint;
```

**Service Logic** (`UserServiceImpl.authenticateWithDeviceCheck()`):
```java
@Override
@Transactional
public Map<String, Object> authenticateWithDeviceCheck(
        String username,
        String password,
        DeviceInfoDto deviceInfo,
        HttpServletRequest request) throws Exception {

    Map<String, Object> result = new HashMap<>();

    // 1. Xác thực username/password
    User user = authenticateUser(username, password);

    // 2. Kiểm tra account đã bị lock chưa
    if (user.isLocked()) {
        result.put("status", "ACCOUNT_LOCKED");
        result.put("reason", user.getAccountLockedReason());
        throw new Exception(user.getAccountLockedReason());
    }

    // 3. Lấy device fingerprint
    String deviceFingerprint = deviceInfo != null && deviceInfo.getDeviceFingerprint() != null
            ? deviceInfo.getDeviceFingerprint()
            : DeviceFingerprintUtil.generateServerSideFingerprint(request);

    String clientIp = DeviceFingerprintUtil.getClientIpAddress(request);

    // 4. Kiểm tra device đã tồn tại chưa
    Optional<UserDevice> existingDevice = deviceRepository
            .findActiveDeviceByFingerprint(user.getUserId(), deviceFingerprint);

    if (existingDevice.isPresent()) {
        // Device đã đăng ký -> Cho phép login
        UserDevice device = existingDevice.get();
        updateDeviceOnLogin(device, clientIp, request);

        result.put("status", "SUCCESS");
        result.put("device", device);
        result.put("user", user);
        return result;
    }

    // 5. Device mới -> Kiểm tra giới hạn (ADMIN BYPASS)
    boolean isAdmin = user.getRole() != null && 
                     user.getRole().getRoleName() == Role.RoleName.ADMIN;

    if (!isAdmin) {
        int maxDevicesAllowed = getMaxDevicesForUser(user);
        int activeDeviceCount = deviceRepository
            .countByUser_UserIdAndIsActiveTrue(user.getUserId());

        if (activeDeviceCount >= maxDevicesAllowed) {
            // VƯỢT QUÁ GIỚI HẠN
            handleDeviceLimitExceeded(user, deviceFingerprint, clientIp, 
                                     request, maxDevicesAllowed);

            result.put("status", "DEVICE_LIMIT_EXCEEDED");
            result.put("activeDeviceCount", activeDeviceCount);
            result.put("maxDevices", maxDevicesAllowed);
            result.put("violationCount", user.getDeviceViolationCount());

            return result;
        }
    }

    // 6. Đăng ký device mới
    UserDevice newDevice = registerNewDevice(user, deviceInfo, 
                                            deviceFingerprint, clientIp, request);

    result.put("status", "SUCCESS");
    result.put("device", newDevice);
    result.put("user", user);
    result.put("isNewDevice", true);

    return result;
}

/**
 * Lấy giới hạn thiết bị theo subscription
 */
private int getMaxDevicesForUser(User user) {
    if (user.getRole() != null && 
        user.getRole().getRoleName() == Role.RoleName.ADMIN) {
        return 999; // ADMIN không giới hạn
    }

    Optional<Subscription> activeSubscription = subscriptionRepository
            .findActiveSubscriptionByUserId(user.getUserId(), LocalDateTime.now());

    if (activeSubscription.isPresent()) {
        Integer maxDevices = activeSubscription.get().getMaxDevices();
        return maxDevices != null ? maxDevices : 1; // DEFAULT_MAX_DEVICES
    }

    return 1; // FREE user: 1 device
}

/**
 * Xử lý khi vượt quá giới hạn thiết bị
 */
private void handleDeviceLimitExceeded(User user, String fingerprint, String ip,
                                      HttpServletRequest request, int maxDevices) 
                                      throws Exception {
    // Tăng violation count
    user.incrementDeviceViolation();

    // KHÓA TÀI KHOẢN nếu vượt quá 3 lần (MAX_VIOLATIONS_BEFORE_LOCK)
    if (user.getDeviceViolationCount() >= 3) {
        user.lockAccount(String.format(
            "Tài khoản đã bị khóa do vượt quá %d lần giới hạn thiết bị. " +
            "Vui lòng liên hệ admin để mở khóa.",
            3
        ));
    }

    userRepository.save(user);

    // Throw exception
    if (user.isLocked()) {
        throw new Exception(user.getAccountLockedReason());
    } else {
        throw new Exception(String.format(
            "Bạn đã đạt giới hạn %d thiết bị theo gói của bạn. " +
            "Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị hoặc nâng cấp gói. " +
            "Vi phạm: %d/3 lần.",
            maxDevices, user.getDeviceViolationCount(), 3
        ));
    }
}
```

**Controller Logic** (`AuthController.processLogin()`):
```java
@PostMapping("/auth/login")
public String processLogin(@ModelAttribute("loginDto") LoginDto loginDto,
                          @RequestParam(required = false) String deviceFingerprint,
                          @RequestParam(required = false) String deviceName,
                          @RequestParam(required = false) String deviceType,
                          BindingResult bindingResult,
                          HttpSession session,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
    
    // Tạo DeviceInfo từ request
    DeviceInfoDto deviceInfo = new DeviceInfoDto();
    deviceInfo.setDeviceFingerprint(deviceFingerprint);
    deviceInfo.setDeviceName(deviceName);
    deviceInfo.setDeviceType(deviceType != null ? deviceType : "WEB");
    deviceInfo.setUserAgent(request.getHeader("User-Agent"));

    // Xác thực với device checking
    Map<String, Object> authResult = userService.authenticateWithDeviceCheck(
        loginDto.getUsername(),
        loginDto.getPassword(),
        deviceInfo,
        request
    );

    String status = (String) authResult.get("status");

    // Xử lý kết quả theo status
    if ("ACCOUNT_LOCKED".equals(status)) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            authResult.get("reason"));
        return "redirect:/auth/login";
    }

    if ("DEVICE_LIMIT_EXCEEDED".equals(status)) {
        int violationCount = (Integer) authResult.get("violationCount");
        int maxDevices = (Integer) authResult.get("maxDevices");

        String errorMsg = String.format(
            "⚠️ Bạn đã đạt giới hạn %d thiết bị. " +
            "Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị. " +
            "Cảnh báo: %d/3 lần vi phạm.",
            maxDevices, violationCount
        );

        redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
        redirectAttributes.addFlashAttribute("showDeviceManagement", true);
        return "redirect:/auth/login";
    }

    // SUCCESS - Tiếp tục login
    User user = (User) authResult.get("user");
    UserDevice device = (UserDevice) authResult.get("device");
    Boolean isNewDevice = (Boolean) authResult.getOrDefault("isNewDevice", false);

    // Tích hợp Spring Security
    String roleName = "ROLE_" + user.getRole().getRoleName().name();

    Authentication authentication = new UsernamePasswordAuthenticationToken(
            user,
            null,
            Collections.singletonList(new SimpleGrantedAuthority(roleName))
    );

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
        securityContext);

    // Lưu session attributes
    session.setAttribute("loggedInUser", user);
    session.setAttribute("userId", user.getUserId());
    session.setAttribute("username", user.getUsername());
    session.setAttribute("role", user.getRole().getRoleName().name());
    session.setAttribute("fullName", user.getFullName());
    session.setAttribute("email", user.getEmail());
    session.setAttribute("currentDeviceId", device.getDeviceId());

    // Cập nhật last_login
    userService.updateLastLogin(user.getUserId());

    // Thông báo nếu là device mới
    if (isNewDevice) {
        redirectAttributes.addFlashAttribute("successMessage",
            "✅ Đăng nhập thành công! Thiết bị mới đã được đăng ký: " + 
            device.getDeviceName());
    }

    // Redirect theo role
    if ("ADMIN".equals(user.getRole().getRoleName().name())) {
        return "redirect:/admin/dashboard";
    } else {
        return "redirect:/user/index";
    }
}
```

**SQL Queries**:
```sql
-- Authenticate user
SELECT * FROM users u 
JOIN roles r ON u.role_id = r.role_id
WHERE u.username = ? AND u.deleted_at IS NULL

-- Check existing device
SELECT * FROM user_devices 
WHERE user_id = ? AND device_fingerprint = ? AND is_active = true

-- Get active subscription
SELECT s.* FROM subscriptions s
JOIN orders o ON s.subscription_id = o.subscription_id
WHERE o.user_id = ? 
  AND o.order_type = 'SUBSCRIPTION'
  AND o.payment_status = 'COMPLETED'
  AND o.start_date <= NOW()
  AND o.end_date >= NOW()
LIMIT 1

-- Count active devices
SELECT COUNT(*) FROM user_devices
WHERE user_id = ? AND is_active = true

-- Insert new device
INSERT INTO user_devices (
    device_id, user_id, device_fingerprint, device_name, device_type,
    ip_address, last_ip, user_agent, browser_name, os_name,
    is_active, is_trusted, login_count, trust_score, last_login
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?, 1, 50, NOW())

-- Update device on login
UPDATE user_devices
SET last_login = NOW(),
    login_count = login_count + 1,
    last_ip = ?,
    trust_score = trust_score + ?
WHERE device_id = ?

-- Update user violation count
UPDATE users
SET device_violation_count = device_violation_count + 1
WHERE user_id = ?

-- Lock account
UPDATE users
SET account_locked_reason = ?,
    locked_at = NOW()
WHERE user_id = ?
```

**Success Responses**:
```
Case 1: Existing Device
- Redirect to: /admin/dashboard (ADMIN) or /user/index (USER)
- Session: loggedInUser, userId, username, role, currentDeviceId

Case 2: New Device Registered
- Redirect to: /admin/dashboard or /user/index
- Flash Message: "✅ Đăng nhập thành công! Thiết bị mới đã được đăng ký: Chrome on Windows 10"
```

**Error Responses**:
```
Case 1: Account Locked
- "Tài khoản đã bị khóa do vượt quá 3 lần giới hạn thiết bị. 
   Vui lòng liên hệ admin để mở khóa."

Case 2: Device Limit Exceeded
- "⚠️ Bạn đã đạt giới hạn 1 thiết bị. 
   Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị. 
   Cảnh báo: 2/3 lần vi phạm."

Case 3: Invalid Credentials
- "Tên đăng nhập hoặc mật khẩu không đúng"

Case 4: Account Inactive
- "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."
```

**Device Limit by Subscription**:
```
FREE (no active subscription): 1 device
BASIC: 2 devices
PREMIUM: 3 devices
VIP: 3 devices
ADMIN: 999 devices (unlimited)
```

---

## Flow 1.3: Đăng Xuất

### Sequence Diagram
```
User → Browser → AuthController → Session Manager → Spring Security
  │       │            │                 │                │
  │  GET /logout (deprecated)            │                │
  │───────────────────►│                                  │
  │       │            │ invalidate()                     │
  │       │            ├────────────────►│                │
  │       │            │ clearContext()                   │
  │       │            ├───────────────────────────────►  │
  │       │            │                                  │
  │◄───────────────────┤ redirect:/auth/login             │
  │       │            │                                  │
  │  POST /auth/logout (CSRF protected, recommended)     │
  │───────────────────►│                                  │
  │       │            │ invalidate()                     │
  │       │            ├────────────────►│                │
  │       │            │ clearContext()                   │
  │       │            ├───────────────────────────────►  │
  │◄───────────────────┤ redirect:/auth/login             │
```

### Implementation Details

**Endpoints**:
- `GET /logout` - Deprecated, maintained for backward compatibility
- `POST /auth/logout` - Recommended (CSRF protected)

**Controller Logic**:
```java
/**
 * GET: Logout (deprecated - use POST /auth/logout)
 */
@GetMapping("/logout")
public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
    // Xóa HttpSession
    session.invalidate();

    // Xóa Spring Security Authentication
    SecurityContextHolder.clearContext();

    redirectAttributes.addFlashAttribute("successMessage", 
        "Đăng xuất thành công!");
    return "redirect:/auth/login";
}

/**
 * POST: Logout (recommended - CSRF protected)
 */
@PostMapping("/auth/logout")
public String logoutPost(HttpSession session, RedirectAttributes redirectAttributes) {
    // Xóa HttpSession
    session.invalidate();

    // Xóa Spring Security Authentication
    SecurityContextHolder.clearContext();

    redirectAttributes.addFlashAttribute("successMessage", 
        "Đăng xuất thành công!");
    return "redirect:/auth/login";
}
```

**Security Configuration** (`SecurityConfig.java`):
```java
http.logout()
    .logoutUrl("/auth/logout")
    .logoutSuccessUrl("/auth/login?logout")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .clearAuthentication(true);
```

**Session Attributes Cleared**:
- `SPRING_SECURITY_CONTEXT`
- `loggedInUser`
- `userId`
- `username`
- `role`
- `fullName`
- `email`
- `currentDeviceId`

**Success Response**:
```
Redirect to: /auth/login
Flash Message: "Đăng xuất thành công!"
Cookies Deleted: JSESSIONID
```

---

## Device Management & Security

### Entity: UserDevice

**Table**: `user_devices`

**Fields**:
```java
@Entity
@Table(name = "user_devices")
public class UserDevice {
    @Id
    private String deviceId; // Format: userid_hash(fingerprint)
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "device_fingerprint", unique = true)
    private String deviceFingerprint; // Browser fingerprint
    
    @Column(name = "device_name")
    private String deviceName; // "Chrome on Windows 10"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType; // WEB, MOBILE, TABLET
    
    @Column(name = "ip_address")
    private String ipAddress; // First login IP
    
    @Column(name = "last_ip")
    private String lastIp; // Last login IP
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "browser_name")
    private String browserName; // Chrome, Firefox, Safari
    
    @Column(name = "os_name")
    private String osName; // Windows 10, MacOS, Linux
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_trusted")
    private Boolean isTrusted = false; // First device = trusted
    
    @Column(name = "login_count")
    private Integer loginCount = 0;
    
    @Column(name = "trust_score")
    private Integer trustScore = 50; // 0-100
    
    @Column(name = "suspicious_login_count")
    private Integer suspiciousLoginCount = 0; // IP changes
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum DeviceType {
        WEB, MOBILE, TABLET
    }
}
```

### Entity: User (Device Management Fields)

**Additional Fields in users table**:
```java
// Device Management & Security
@Column(name = "device_violation_count")
private Integer deviceViolationCount = 0;

@Column(name = "account_locked_reason", length = 500)
private String accountLockedReason;

@Column(name = "locked_at")
private LocalDateTime lockedAt;

@Column(name = "locked_until")
private LocalDateTime lockedUntil;
```

**Helper Methods**:
```java
public void incrementDeviceViolation() {
    this.deviceViolationCount = (this.deviceViolationCount == null ? 0 : 
                                 this.deviceViolationCount) + 1;
}

public void lockAccount(String reason) {
    this.isActive = false;
    this.accountLockedReason = reason;
    this.lockedAt = LocalDateTime.now();
}

public void unlockAccount() {
    this.isActive = true;
    this.accountLockedReason = null;
    this.lockedAt = null;
    this.lockedUntil = null;
    this.deviceViolationCount = 0;
}

public boolean isLocked() {
    return !this.isActive && this.accountLockedReason != null;
}
```

### Device Fingerprinting Utility

**Class**: `DeviceFingerprintUtil.java`

```java
public class DeviceFingerprintUtil {
    
    /**
     * Generate server-side fingerprint from request
     */
    public static String generateServerSideFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        
        String raw = userAgent + "|" + acceptLanguage + "|" + acceptEncoding;
        return DigestUtils.md5DigestAsHex(raw.getBytes());
    }
    
    /**
     * Get client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || 
            "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || 
            "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || 
            "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress != null ? ipAddress : "127.0.0.1";
    }
    
    /**
     * Generate device ID
     */
    public static String generateDeviceId(String userId, String fingerprint) {
        return userId + "_" + 
               DigestUtils.md5DigestAsHex(fingerprint.getBytes()).substring(0, 8);
    }
    
    /**
     * Parse browser name from User-Agent
     */
    public static String parseBrowserName(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) return "IE";
        return "Unknown";
    }
    
    /**
     * Parse OS name from User-Agent
     */
    public static String parseOsName(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Windows NT 10")) return "Windows 10";
        if (userAgent.contains("Windows NT 6.3")) return "Windows 8.1";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac OS X")) return "MacOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iOS")) return "iOS";
        return "Unknown";
    }
    
    /**
     * Check if two IPs are in same subnet (basic check)
     */
    public static boolean isSameSubnet(String ip1, String ip2) {
        if (ip1 == null || ip2 == null) return false;
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");
        if (parts1.length != 4 || parts2.length != 4) return false;
        
        // Check first 3 octets (Class C subnet)
        return parts1[0].equals(parts2[0]) && 
               parts1[1].equals(parts2[1]) && 
               parts1[2].equals(parts2[2]);
    }
}
```

### Security Rules

1. **Device Limit by Subscription**:
   - FREE: 1 device
   - BASIC: 2 devices
   - PREMIUM: 3 devices
   - VIP: 3 devices
   - ADMIN: Unlimited (999)

2. **Violation Tracking**:
   - Each device limit exceeded: +1 violation
   - After 3 violations: Account auto-locked
   - Locked message: "Tài khoản đã bị khóa do vượt quá 3 lần giới hạn thiết bị"

3. **Trust Score System**:
   - New device starts with: 50 points
   - Same IP login: +1 point
   - Same subnet IP: -1 point
   - Different subnet IP: -5 points, +1 suspicious login count
   - First device: Auto-trusted (cannot be deleted)

4. **Admin Unlock**:
   - Admin can unlock via: `/admin/users/{userId}/unlock`
   - Resets: `device_violation_count = 0`, `account_locked_reason = null`

5. **Device Management**:
   - Users can view devices: `/user/devices`
   - Delete devices: Cannot delete trusted device or current device
   - Soft delete: `is_active = false`

---

## Debugging Endpoints

### 1. Test Registration

**cURL Command**:
```bash
curl -X POST http://localhost:8080/auth/register ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "username=testuser&email=test@example.com&password=Test123"
```

**Postman**:
- Method: `POST`
- URL: `http://localhost:8080/auth/register`
- Body (x-www-form-urlencoded):
  - username: `testuser`
  - email: `test@example.com`
  - password: `Test123`

**Expected Response**: 
```
Redirect to: /auth/login
Flash Message: "✅ Đăng ký thành công! Vui lòng đăng nhập để tiếp tục."
```

---

### 2. Test Login (Basic - No Device Fingerprint)

**cURL Command**:
```bash
curl -X POST http://localhost:8080/auth/login ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "username=testuser&password=Test123" ^
  -c cookies.txt -L
```

**Browser Console Test**:
```javascript
// Generate device fingerprint
const fingerprint = btoa(
    navigator.userAgent + 
    screen.width + 'x' + screen.height + 
    navigator.language + 
    new Date().getTimezoneOffset()
);

// Submit login form with fingerprint
const formData = new FormData();
formData.append('username', 'testuser');
formData.append('password', 'Test123');
formData.append('deviceFingerprint', fingerprint);
formData.append('deviceName', 'Chrome on Windows 10');
formData.append('deviceType', 'WEB');

fetch('/auth/login', {
  method: 'POST',
  body: formData
}).then(res => {
  console.log('Login response:', res);
  window.location.reload();
});
```

---

### 3. Check Current User Session

**Controller Endpoint** (`DebugController.java`):
```java
@GetMapping("/debug/current-user")
@ResponseBody
public Map<String, Object> getCurrentUser(Authentication authentication,
                                         HttpSession session) {
    Map<String, Object> result = new HashMap<>();
    
    if (authentication != null && authentication.isAuthenticated()) {
        result.put("authenticated", true);
        result.put("principal", authentication.getPrincipal());
        result.put("authorities", authentication.getAuthorities());
        result.put("userId", session.getAttribute("userId"));
        result.put("username", session.getAttribute("username"));
        result.put("role", session.getAttribute("role"));
        result.put("currentDeviceId", session.getAttribute("currentDeviceId"));
    } else {
        result.put("authenticated", false);
    }
    
    return result;
}
```

**cURL Command**:
```bash
curl http://localhost:8080/debug/current-user -b cookies.txt
```

**Expected Response**:
```json
{
  "authenticated": true,
  "principal": {
    "userId": "user_normal_01",
    "username": "testuser",
    "email": "test@example.com",
    "role": {"roleName": "USER"}
  },
  "authorities": [{"authority": "ROLE_USER"}],
  "userId": "user_normal_01",
  "username": "testuser",
  "role": "USER",
  "currentDeviceId": "user_normal_01_a1b2c3d4"
}
```

---

### 4. View User Devices

**Endpoint**: `GET /user/devices`

**cURL Command**:
```bash
curl http://localhost:8080/user/devices -b cookies.txt
```

**Expected Response** (JSON if REST endpoint):
```json
{
  "devices": [
    {
      "deviceId": "user_normal_01_a1b2c3d4",
      "deviceName": "Chrome on Windows 10",
      "deviceType": "WEB",
      "browserName": "Chrome",
      "osName": "Windows 10",
      "ipAddress": "192.168.1.100",
      "lastIp": "192.168.1.100",
      "isActive": true,
      "isTrusted": true,
      "loginCount": 5,
      "trustScore": 55,
      "lastLogin": "2025-12-30T10:30:00"
    }
  ]
}
```

---

### 5. Test Device Limit (Trigger Violation)

**Steps**:
1. Login from Device 1 (allowed)
2. Login from Device 2 when limit = 1 (violation +1)
3. Login from Device 3 when limit = 1 (violation +2)
4. Login from Device 4 when limit = 1 (violation +3, account locked)

**Simulate Multiple Devices** (change deviceFingerprint):
```bash
# Device 1 (allowed)
curl -X POST http://localhost:8080/auth/login ^
  -d "username=testuser&password=Test123&deviceFingerprint=device1"

# Device 2 (violation +1)
curl -X POST http://localhost:8080/auth/login ^
  -d "username=testuser&password=Test123&deviceFingerprint=device2"
```

**Expected Error (after 3 violations)**:
```
"Tài khoản đã bị khóa do vượt quá 3 lần giới hạn thiết bị. 
Vui lòng liên hệ admin để mở khóa."
```

---

### 6. Admin Unlock Account

**Endpoint**: `POST /admin/users/{userId}/unlock`

**cURL Command**:
```bash
curl -X POST http://localhost:8080/admin/users/user_normal_01/unlock ^
  -H "Cookie: JSESSIONID=..." ^
  -d ""
```

**Expected Response**:
```
Redirect to: /admin/users
Flash Message: "✅ Đã mở khóa tài khoản user_normal_01 thành công"
```

---

### 7. Test Logout

**cURL Command** (POST - recommended):
```bash
curl -X POST http://localhost:8080/auth/logout ^
  -H "Cookie: JSESSIONID=..." ^
  -H "X-CSRF-TOKEN: ..."
```

**Browser Console**:
```javascript
// Using logout form (recommended)
document.querySelector('form[action="/auth/logout"]').submit();

// Or direct GET (deprecated)
window.location.href = '/logout';
```

---

### 8. Database Verification Queries

**Check User Registration**:
```sql
SELECT user_id, username, email, role_id, is_active, is_verified, 
       created_at, deleted_at
FROM users
WHERE username = 'testuser';
```

**Check User Devices**:
```sql
SELECT device_id, device_name, device_type, browser_name, os_name,
       ip_address, last_ip, is_active, is_trusted, login_count, trust_score,
       suspicious_login_count, last_login
FROM user_devices
WHERE user_id = 'user_normal_01'
ORDER BY last_login DESC;
```

**Check Device Violations**:
```sql
SELECT user_id, username, device_violation_count, account_locked_reason,
       locked_at, is_active
FROM users
WHERE device_violation_count > 0;
```

**Check Active Subscriptions**:
```sql
SELECT o.user_id, s.package_name, s.max_devices, o.start_date, o.end_date,
       o.payment_status
FROM orders o
JOIN subscriptions s ON o.subscription_id = s.subscription_id
WHERE o.user_id = 'user_normal_01'
  AND o.order_type = 'SUBSCRIPTION'
  AND o.payment_status = 'COMPLETED'
  AND o.start_date <= NOW()
  AND o.end_date >= NOW();
```

---

## Summary

**FLOW_01: AUTHENTICATION** đã được triển khai đầy đủ với các tính năng:

✅ **Đăng Ký (Registration)**:
- Validation với `@NotEmpty`, `@Email`, `@Pattern`
- Kiểm tra trùng username/email (bao gồm soft-deleted users)
- Auto-generate User ID (`user_normal_XX`)
- Hash password với BCrypt
- Assign default role USER

✅ **Đăng Nhập (Login with Device Management)**:
- Device fingerprinting (client-side + server-side)
- Kiểm tra giới hạn thiết bị theo subscription
- Auto-lock account sau 3 vi phạm
- ADMIN bypass device limit
- Trust score system
- IP tracking và suspicious login detection
- Spring Security integration
- Role-based redirection

✅ **Đăng Xuất (Logout)**:
- Session invalidation
- Security context clearing
- Cookie deletion
- CSRF protection (POST method)

✅ **Device Management**:
- Device registration & tracking
- Trust score calculation
- Violation counting
- Soft delete devices
- Cannot delete trusted device

✅ **Security Features**:
- Account locking mechanism
- Admin unlock function
- Soft delete for users
- VIP subscription for admins

---

**Related Flows**:
- [FLOW_04: User Account Management](FLOW_04_USER_ACCOUNT_MANAGEMENT.md) - Profile editing
- [FLOW_06: Admin User Management](FLOW_06_ADMIN_USER_MANAGEMENT.md) - Admin CRUD users
- [FLOW_10: Subscription Management](FLOW_10_SUBSCRIPTION_MANAGEMENT.md) - Device limits
- [FLOW_19: Device Management](FLOW_19_DEVICE_MANAGEMENT.md) - User device management UI

---

**Last Updated**: December 30, 2025
**Version**: 2.0 (Complete rewrite with actual implementation)
        result.put("username", authentication.getName());
        result.put("authorities", authentication.getAuthorities());
        result.put("principal", authentication.getPrincipal());
    } else {
        result.put("authenticated", false);
    }
    return result;
}
```

**Test URL**: `http://localhost:8080/debug/current-user`

### 4. Database Verification

**Check user created**:
```sql
SELECT u.*, r.role_name
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = 'testuser';
```

**Check password hash**:
```sql
SELECT username, password, enabled
FROM users
WHERE username = 'testuser';
-- Password should be BCrypt hash starting with $2a$ or $2b$
```

### 5. Session Debugging

**Check active sessions**:
```java
@GetMapping("/debug/sessions")
@ResponseBody
public Map<String, Object> getSessions(HttpSession session) {
    Map<String, Object> result = new HashMap<>();
    result.put("sessionId", session.getId());
    result.put("creationTime", new Date(session.getCreationTime()));
    result.put("lastAccessedTime", new Date(session.getLastAccessedTime()));
    result.put("maxInactiveInterval", session.getMaxInactiveInterval());
    
    Enumeration<String> attributeNames = session.getAttributeNames();
    Map<String, Object> attributes = new HashMap<>();
    while (attributeNames.hasMoreElements()) {
        String name = attributeNames.nextElement();
        attributes.put(name, session.getAttribute(name).toString());
    }
    result.put("attributes", attributes);
    
    return result;
}
```

### 6. Common Issues & Solutions

| Issue | Debug Method | Solution |
|-------|-------------|----------|
| User không tạo được | Check logs, SQL query | Verify unique constraints |
| Login failed | Check password hash | Ensure BCrypt encoding |
| Redirect không đúng | Check authorities | Verify role mapping |
| Session expired | Check session timeout | Adjust `server.servlet.session.timeout` |
| CSRF error | Check CSRF token | Add `${_csrf.token}` in form |

### 7. Logging Configuration

**application.properties**:
```properties
# Enable security debug
logging.level.org.springframework.security=DEBUG
logging.level.com.example.ebook_store.controller.AuthController=DEBUG
logging.level.com.example.ebook_store.service.UserService=DEBUG

# SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### 8. Breakpoint Locations

**AuthController**:
- Line: `public String register(@Valid @ModelAttribute("user") RegisterDto registerDto)`
- Line: `userService.saveUser(registerDto)`

**UserService**:
- Line: `userRepository.existsByUsername(registerDto.getUsername())`
- Line: `String hashedPassword = passwordEncoder.encode(registerDto.getPassword())`
- Line: `return userRepository.save(user)`

**SecurityConfig**:
- Line: `.successHandler((request, response, authentication) -> {`

---

## Test Scenarios

### Scenario 1: Đăng ký thành công
1. Truy cập `/auth/register`
2. Điền form đầy đủ
3. Submit form
4. Verify: Redirect to `/auth/login`
5. Verify: User xuất hiện trong database
6. Verify: Password được hash

### Scenario 2: Đăng ký với username trùng
1. Đăng ký với username đã tồn tại
2. Verify: Hiển thị error message
3. Verify: User không được tạo

### Scenario 3: Đăng nhập thành công (User)
1. Login với ROLE_USER
2. Verify: Redirect to `/user/index`
3. Verify: Session được tạo
4. Verify: SecurityContext chứa authentication

### Scenario 4: Đăng nhập thành công (Admin)
1. Login với ROLE_ADMIN
2. Verify: Redirect to `/admin/dashboard`

### Scenario 5: Đăng xuất
1. Click logout
2. Verify: Redirect to `/auth/login?logout`
3. Verify: Session invalidated
4. Verify: Cannot access protected pages

---

**Last Updated**: 30/11/2025
**Version**: 2.0

