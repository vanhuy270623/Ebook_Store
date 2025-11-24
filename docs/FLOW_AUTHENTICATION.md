# 🔐 LUỒNG XỬ LÝ AUTHENTICATION

## 📋 Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Luồng đăng ký (Register)](#2-luồng-đăng-ký-register)
3. [Luồng đăng nhập (Login)](#3-luồng-đăng-nhập-login)
4. [Luồng đăng xuất (Logout)](#4-luồng-đăng-xuất-logout)
5. [Session Management](#5-session-management)
6. [Security Integration](#6-security-integration)

---

## 1. Tổng quan

### 📌 Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        AUTHENTICATION SYSTEM                     │
└─────────────────────────────────────────────────────────────────┘

┌──────────┐      ┌──────────┐      ┌─────────┐      ┌──────────┐
│ Browser  │ ───> │Controller│ ───> │ Service │ ───> │   DB     │
└──────────┘      └──────────┘      └─────────┘      └──────────┘
     ↑                  │                 │                 │
     │                  ↓                 ↓                 ↓
     │            ┌──────────┐      ┌─────────┐      ┌──────────┐
     └────────────│  Model   │ <──  │Security │ <──  │Repository│
                  └──────────┘      └─────────┘      └──────────┘
```

### 🔑 Components liên quan

| Component | File | Vai trò |
|-----------|------|---------|
| Controller | `AuthController.java` | Xử lý HTTP requests |
| Service | `UserService.java` | Business logic |
| Repository | `UserRepository.java` | Database access |
| Entity | `User.java`, `Role.java` | Data model |
| DTO | `LoginDto.java`, `RegisterDto.java` | Data transfer |
| Config | `SecurityConfig.java` | Security settings |

---

## 2. Luồng đăng ký (Register)

### 📊 Sequence Diagram

```
User        Browser     Controller      Service        Repository      DB
 │            │             │              │                │           │
 │ Fill form │             │              │                │           │
 │───────────>│             │              │                │           │
 │            │             │              │                │           │
 │            │ POST /auth/register        │                │           │
 │            │────────────>│              │                │           │
 │            │             │              │                │           │
 │            │             │ registerUser()│                │           │
 │            │             │─────────────>│                │           │
 │            │             │              │                │           │
 │            │             │              │ findByUsername()│           │
 │            │             │              │───────────────>│           │
 │            │             │              │                │ SELECT    │
 │            │             │              │                │──────────>│
 │            │             │              │                │<──────────│
 │            │             │              │<───────────────│           │
 │            │             │              │                │           │
 │            │             │              │ findByEmail()  │           │
 │            │             │              │───────────────>│           │
 │            │             │              │                │ SELECT    │
 │            │             │              │                │──────────>│
 │            │             │              │                │<──────────│
 │            │             │              │<───────────────│           │
 │            │             │              │                │           │
 │            │             │              │ Hash password  │           │
 │            │             │              │──────┐         │           │
 │            │             │              │<─────┘         │           │
 │            │             │              │                │           │
 │            │             │              │ save()         │           │
 │            │             │              │───────────────>│           │
 │            │             │              │                │ INSERT    │
 │            │             │              │                │──────────>│
 │            │             │              │                │<──────────│
 │            │             │              │<───────────────│           │
 │            │             │<─────────────│                │           │
 │            │             │              │                │           │
 │            │ Redirect /auth/login       │                │           │
 │            │<────────────│              │                │           │
 │<───────────│             │              │                │           │
 │ Success!   │             │              │                │           │
```

### 📝 Chi tiết từng bước

#### **Bước 1: User điền form**

**HTML Form:**
```html
<!-- templates/auth/register.html -->
<form th:action="@{/auth/register}" th:object="${registerDto}" method="post">
    <div>
        <label>Username:</label>
        <input type="text" th:field="*{username}" required />
        <span th:if="${#fields.hasErrors('username')}" th:errors="*{username}"></span>
    </div>
    
    <div>
        <label>Email:</label>
        <input type="email" th:field="*{email}" required />
        <span th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></span>
    </div>
    
    <div>
        <label>Password:</label>
        <input type="password" th:field="*{password}" required />
    </div>
    
    <div>
        <label>Confirm Password:</label>
        <input type="password" th:field="*{confirmPassword}" required />
    </div>
    
    <button type="submit">Đăng ký</button>
</form>
```

**RegisterDto.java:**
```java
@Data
public class RegisterDto {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Password phải ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Confirm password không được để trống")
    private String confirmPassword;

    @AssertTrue(message = "Password không khớp")
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
```

---

#### **Bước 2: Controller nhận request**

```java
@PostMapping("/auth/register")
public String processRegistration(
        @Valid @ModelAttribute("registerDto") RegisterDto registerDto,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {
    
    // 1. Check validation errors
    if (bindingResult.hasErrors()) {
        return "auth/register";  // Return to form with errors
    }

    try {
        // 2. Gọi Service để xử lý
        userService.registerUser(registerDto);
        
        // 3. Success → Redirect với message
        redirectAttributes.addFlashAttribute("successMessage", 
            "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/auth/login";
        
    } catch (Exception e) {
        // 4. Error → Redirect back với error message
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/auth/register";
    }
}
```

**Validation flow:**
```
1. Spring bind form data → RegisterDto
2. @Valid trigger validation annotations
3. Validation errors → BindingResult
4. Check bindingResult.hasErrors()
5. If errors → Return to form with error messages
6. If no errors → Continue processing
```

---

#### **Bước 3: Service xử lý business logic**

```java
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(RegisterDto registerDto) throws Exception {
        
        // 1. Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(registerDto.getUsername()).isPresent()) {
            throw new Exception("Username đã tồn tại");
        }

        // 2. Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(registerDto.getEmail()).isPresent()) {
            throw new Exception("Email đã được sử dụng");
        }

        // 3. Tìm role "USER" từ database
        Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
                .orElseThrow(() -> new Exception("Không tìm thấy Role 'USER'"));

        // 4. Tự động sinh User ID
        String newUserId = generateNextUserId();

        // 5. Tạo User entity
        User user = new User();
        user.setUserId(newUserId);
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        
        // 6. Hash password với BCrypt
        user.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        
        // 7. Set default values
        user.setRole(userRole);
        user.setIsActive(true);
        user.setIsVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 8. Lưu vào database
        userRepository.save(user);
    }

    private String generateNextUserId() {
        long userCount = userRepository.count();
        int nextNumber = (int) userCount + 1;
        return String.format("user_normal_%02d", nextNumber);
    }
}
```

**Key points:**

##### **1. Validation in Service layer**
- Controller: Form validation (format, required, etc.)
- Service: Business validation (uniqueness, consistency, etc.)
- 2 layers of defense

##### **2. Password hashing**
```java
String plainPassword = "mypassword123";
String hashedPassword = passwordEncoder.encode(plainPassword);

// Input:  mypassword123
// Output: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
//         │  │  │                                                      │
//         │  │  └─ Salt (22 chars)                                    └─ Hash (31 chars)
//         │  └─ Cost factor (10 = 2^10 = 1024 rounds)
//         └─ BCrypt version (2a)
```

**Tại sao BCrypt?**
- ✅ One-way hash (không thể reverse)
- ✅ Salt tự động (mỗi password có salt khác nhau)
- ✅ Adaptive (có thể tăng cost factor khi hardware mạnh hơn)
- ✅ Slow by design (chống brute-force)

##### **3. Auto-generate User ID**
```java
user_normal_01
user_normal_02
user_normal_03
...
```

**Alternatives:**
- UUID: `550e8400-e29b-41d4-a716-446655440000`
- Timestamp: `user_1637485940123`
- Sequential: `user_001`, `user_002`

---

#### **Bước 4: Repository lưu vào DB**

```java
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
```

**Generated SQL:**
```sql
-- Check username exists
SELECT * FROM users WHERE username = ?

-- Check email exists
SELECT * FROM users WHERE email = ?

-- Insert new user
INSERT INTO users (
    user_id, username, email, password_hash, 
    role_id, is_active, is_verified, 
    created_at, updated_at
) VALUES (
    'user_normal_03', 'john_doe', 'john@example.com',
    '$2a$10$N9qo8uLOickgx...', 'role_user', 1, 0,
    '2025-11-21 10:30:00', '2025-11-21 10:30:00'
)
```

---

#### **Bước 5: Redirect với flash message**

```java
redirectAttributes.addFlashAttribute("successMessage", 
    "Đăng ký thành công! Vui lòng đăng nhập.");
return "redirect:/auth/login";
```

**Flow:**
```
1. Controller add flash attribute
2. Spring store in session (temporary)
3. Browser redirect to /auth/login (new request)
4. Spring restore flash attribute from session
5. Template access via ${successMessage}
6. After rendering, flash attribute is removed
```

**Login template:**
```html
<!-- Display success message -->
<div th:if="${successMessage}" class="alert alert-success">
    <span th:text="${successMessage}"></span>
</div>
```

---

## 3. Luồng đăng nhập (Login)

### 📊 Sequence Diagram

```
User      Browser    Controller    Service    Repository    DB    SecurityContext
 │          │            │            │            │         │           │
 │ Fill     │            │            │            │         │           │
 │ login    │            │            │            │         │           │
 │─────────>│            │            │            │         │           │
 │          │            │            │            │         │           │
 │          │ POST /auth/login        │            │         │           │
 │          │───────────>│            │            │         │           │
 │          │            │            │            │         │           │
 │          │            │ authenticateUser()      │         │           │
 │          │            │───────────>│            │         │           │
 │          │            │            │            │         │           │
 │          │            │            │ findByUsername()     │           │
 │          │            │            │───────────>│         │           │
 │          │            │            │            │ SELECT  │           │
 │          │            │            │            │────────>│           │
 │          │            │            │            │<────────│           │
 │          │            │            │<───────────│         │           │
 │          │            │            │            │         │           │
 │          │            │            │ Verify password      │           │
 │          │            │            │────────┐   │         │           │
 │          │            │            │<───────┘   │         │           │
 │          │            │            │            │         │           │
 │          │            │<───────────│            │         │           │
 │          │            │ Return User            │         │           │
 │          │            │            │            │         │           │
 │          │            │ Create Authentication  │         │           │
 │          │            │────────────────────────────────────────────>│
 │          │            │            │            │         │      Set  │
 │          │            │            │            │         │      Auth │
 │          │            │<───────────────────────────────────────────┘│
 │          │            │            │            │         │           │
 │          │            │ Save to Session         │         │           │
 │          │            │──────┐     │            │         │           │
 │          │            │<─────┘     │            │         │           │
 │          │            │            │            │         │           │
 │          │            │ updateLastLogin()       │         │           │
 │          │            │───────────>│            │         │           │
 │          │            │            │ save()     │         │           │
 │          │            │            │───────────>│         │           │
 │          │            │            │            │ UPDATE  │           │
 │          │            │            │            │────────>│           │
 │          │            │            │<───────────│         │           │
 │          │            │<───────────│            │         │           │
 │          │            │            │            │         │           │
 │          │ Redirect to dashboard   │            │         │           │
 │          │<───────────│            │            │         │           │
 │<─────────│            │            │            │         │           │
 │ Success! │            │            │            │         │           │
```

### 📝 Chi tiết từng bước

#### **Bước 1: User submit login form**

```html
<form th:action="@{/auth/login}" th:object="${loginDto}" method="post">
    <!-- CSRF token (Thymeleaf tự động thêm) -->
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    <input type="text" th:field="*{username}" placeholder="Username" required />
    <input type="password" th:field="*{password}" placeholder="Password" required />
    
    <button type="submit">Đăng nhập</button>
</form>
```

**HTTP Request:**
```http
POST /auth/login HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID=ABC123

_csrf=xyz789&username=john_doe&password=mypassword123
```

---

#### **Bước 2: Controller xử lý authentication**

```java
@PostMapping("/auth/login")
public String processLogin(
        @ModelAttribute("loginDto") LoginDto loginDto,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

    try {
        // 1. Authenticate user
        User user = userService.authenticateUser(
            loginDto.getUsername(), 
            loginDto.getPassword()
        );

        // 2. Create Spring Security Authentication
        String roleName = "ROLE_" + user.getRole().getRoleName().name();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user,    // Principal
            null,    // Credentials
            Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );

        // 3. Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. Save user info to session
        session.setAttribute("loggedInUser", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole().getRoleName().name());
        session.setAttribute("fullName", user.getFullName());
        
        // 5. Save SecurityContext to session
        session.setAttribute("SPRING_SECURITY_CONTEXT", 
            SecurityContextHolder.getContext());

        // 6. Update last login timestamp
        userService.updateLastLogin(user.getUserId());

        // 7. Redirect based on role
        if ("ADMIN".equals(user.getRole().getRoleName().name())) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/user/index";
        }

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/auth/login";
    }
}
```

---

#### **Bước 3: Service authenticate user**

```java
@Override
public User authenticateUser(String username, String password) throws Exception {
    
    // 1. Tìm user theo username
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("Tên đăng nhập hoặc mật khẩu không đúng"));

    // 2. Kiểm tra tài khoản có bị khóa không
    if (!user.getIsActive()) {
        throw new Exception("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
    }

    // 3. Verify password với BCrypt
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        throw new Exception("Tên đăng nhập hoặc mật khẩu không đúng");
    }

    // 4. Return user nếu tất cả checks pass
    return user;
}
```

**Password verification:**
```java
// Input password
String inputPassword = "mypassword123";

// Hash từ database
String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIj...";

// BCrypt extract salt từ hash và hash input password với cùng salt
boolean matches = passwordEncoder.matches(inputPassword, hashedPassword);

// If matches = true → Password correct
```

**Security considerations:**
1. **Giống nhau message cho username và password sai**
   - Không nói "Username không tồn tại" hay "Password sai"
   - Luôn nói "Tên đăng nhập hoặc mật khẩu không đúng"
   - Tránh attacker enumerate usernames

2. **Check isActive**
   - Admin có thể khóa tài khoản
   - Tài khoản bị khóa không thể login

---

#### **Bước 4: Tạo Spring Security Authentication**

```java
// Role name với prefix
String roleName = "ROLE_" + user.getRole().getRoleName().name();
// VD: "ROLE_ADMIN" hoặc "ROLE_USER"

// Tạo Authentication object
Authentication authentication = new UsernamePasswordAuthenticationToken(
    user,                                                  // Principal
    null,                                                  // Credentials
    Collections.singletonList(new SimpleGrantedAuthority(roleName))  // Authorities
);
```

**Components:**

##### **Principal**
- Đại diện cho user đã authenticated
- Lưu **User object** (không phải String username)
- Access: `authentication.getPrincipal()` → User object

##### **Credentials**
- Password (nhưng set null sau khi authenticated)
- Không cần lưu password sau khi verify

##### **Authorities**
- Danh sách quyền (roles/permissions)
- VD: `[ROLE_ADMIN]`, `[ROLE_USER]`
- Dùng cho authorization checks

**Authority naming:**
```
Database:       ADMIN
Spring:         ROLE_ADMIN
Check:          hasRole("ADMIN")  ← Không cần prefix
Template:       sec:authorize="hasRole('ADMIN')"
```

---

#### **Bước 5: Set Authentication vào SecurityContext**

```java
SecurityContextHolder.getContext().setAuthentication(authentication);
```

**Architecture:**
```
Thread (HTTP Request)
    └── ThreadLocal
            └── SecurityContext
                    └── Authentication
                            ├── Principal (User object)
                            ├── Credentials (null)
                            └── Authorities ([ROLE_ADMIN])
```

**ThreadLocal:**
- Mỗi thread có SecurityContext riêng
- Mỗi HTTP request = 1 thread
- Thread-safe, không conflict giữa các requests

**Access từ anywhere:**
```java
// Get current authentication
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

// Get current user
User user = (User) auth.getPrincipal();

// Check if authenticated
boolean isAuthenticated = auth != null && auth.isAuthenticated();
```

---

#### **Bước 6: Lưu vào HttpSession**

```java
// Save user info
session.setAttribute("loggedInUser", user);
session.setAttribute("userId", user.getUserId());
session.setAttribute("username", user.getUsername());
session.setAttribute("role", user.getRole().getRoleName().name());
session.setAttribute("fullName", user.getFullName());
session.setAttribute("email", user.getEmail());

// Save SecurityContext
session.setAttribute("SPRING_SECURITY_CONTEXT", 
    SecurityContextHolder.getContext());
```

**Tại sao lưu vào session?**
- Request tiếp theo không cần login lại
- Session persists across requests
- Spring Security tự động restore SecurityContext từ session

**Session lifecycle:**
```
1. First request (login) → Create session → JSESSIONID cookie
2. Second request → Browser send JSESSIONID → Restore session
3. SecurityContext restored từ session → User still authenticated
4. Logout → Invalidate session → Create new session
```

**Session storage:**
```
HttpSession {
    "JSESSIONID": "ABC123XYZ",
    "loggedInUser": User object,
    "userId": "user_normal_01",
    "username": "john_doe",
    "role": "USER",
    "fullName": "John Doe",
    "SPRING_SECURITY_CONTEXT": SecurityContext object
}
```

---

#### **Bước 7: Update last login**

```java
userService.updateLastLogin(user.getUserId());
```

```java
@Override
public void updateLastLogin(String userId) {
    userRepository.findById(userId).ifPresent(user -> {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    });
}
```

**Generated SQL:**
```sql
UPDATE users 
SET last_login = '2025-11-21 10:35:42' 
WHERE user_id = 'user_normal_01'
```

---

#### **Bước 8: Role-based redirect**

```java
if ("ADMIN".equals(user.getRole().getRoleName().name())) {
    return "redirect:/admin/dashboard";
} else {
    return "redirect:/user/index";
}
```

**Redirect flow:**
```
1. Controller return "redirect:/admin/dashboard"
2. Spring send HTTP 302 response
   Location: /admin/dashboard
3. Browser receive 302 → Send new request
   GET /admin/dashboard
4. AdminController handle request
5. Check authorization (hasRole("ADMIN"))
6. Return admin/dashboard.html
```

---

## 4. Luồng đăng xuất (Logout)

### 📊 Sequence Diagram

```
User      Browser    Controller    Session    SecurityContext
 │          │            │            │              │
 │ Click    │            │            │              │
 │ logout   │            │            │              │
 │─────────>│            │            │              │
 │          │            │            │              │
 │          │ GET /logout│            │              │
 │          │───────────>│            │              │
 │          │            │            │              │
 │          │            │ invalidate()│              │
 │          │            │───────────>│              │
 │          │            │            │ Clear all    │
 │          │            │            │ attributes   │
 │          │            │<───────────│              │
 │          │            │            │              │
 │          │            │ clearContext()             │
 │          │            │───────────────────────────>│
 │          │            │            │         Clear│
 │          │            │            │    Authentication│
 │          │            │<───────────────────────────│
 │          │            │            │              │
 │          │ Redirect /auth/login    │              │
 │          │<───────────│            │              │
 │<─────────│            │            │              │
```

### 📝 Chi tiết

```java
@GetMapping("/logout")
public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
    
    // 1. Invalidate session (xóa tất cả session data)
    session.invalidate();

    // 2. Clear SecurityContext (xóa Authentication)
    SecurityContextHolder.clearContext();

    // 3. Add success message
    redirectAttributes.addFlashAttribute("successMessage", "Đăng xuất thành công!");
    
    // 4. Redirect to login page
    return "redirect:/auth/login";
}
```

**Session invalidation:**
```java
// Before
HttpSession {
    "JSESSIONID": "ABC123",
    "loggedInUser": User object,
    "SPRING_SECURITY_CONTEXT": SecurityContext
}

session.invalidate();

// After
HttpSession {
    "JSESSIONID": "XYZ789",  ← New session
    // Empty
}
```

**SecurityContext clear:**
```java
// Before
SecurityContext {
    Authentication {
        Principal: User object,
        Authorities: [ROLE_USER]
    }
}

SecurityContextHolder.clearContext();

// After
SecurityContext {
    Authentication: null
}
```

---

## 5. Session Management

### 📊 Session Lifecycle

```
┌─────────────────────────────────────────────────────────┐
│                    SESSION LIFECYCLE                    │
└─────────────────────────────────────────────────────────┘

   ┌──────────┐
   │  Login   │
   └────┬─────┘
        │
        ↓
   ┌────────────────┐
   │ Create Session │ ← JSESSIONID cookie
   └────┬───────────┘
        │
        ↓
   ┌────────────────────┐
   │ Store:             │
   │ - User info        │
   │ - SecurityContext  │
   └────┬───────────────┘
        │
        ↓
   ┌────────────────────┐
   │ Subsequent         │
   │ Requests           │ ← Browser send JSESSIONID
   └────┬───────────────┘
        │
        ↓
   ┌────────────────────┐
   │ Restore            │
   │ SecurityContext    │
   └────┬───────────────┘
        │
        ├──> Logout → Invalidate
        │
        └──> Timeout (30min) → Expire
```

### 🔧 Session Configuration

**application.properties:**
```properties
# Session timeout (default 30 minutes)
server.servlet.session.timeout=30m

# Session tracking mode
server.servlet.session.tracking-modes=cookie

# Cookie settings
server.servlet.session.cookie.name=JSESSIONID
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false  # true trong production với HTTPS
```

### 🍪 Session Cookie

**Cookie structure:**
```
Set-Cookie: JSESSIONID=ABC123XYZ; Path=/; HttpOnly
```

**Attributes:**
- `JSESSIONID`: Session ID
- `Path=/`: Cookie valid cho tất cả paths
- `HttpOnly`: JavaScript không thể access (bảo vệ XSS)
- `Secure`: Chỉ gửi qua HTTPS (production)

---

## 6. Security Integration

### 🔒 Spring Security Filter Chain

```
HTTP Request
    ↓
┌─────────────────────────────────┐
│ SecurityContextPersistenceFilter │ ← Load SecurityContext từ session
└─────────────┬───────────────────┘
              ↓
┌─────────────────────────────────┐
│ CsrfFilter                       │ ← Verify CSRF token
└─────────────┬───────────────────┘
              ↓
┌─────────────────────────────────┐
│ AuthenticationFilter             │ ← (Disabled - dùng custom)
└─────────────┬───────────────────┘
              ↓
┌─────────────────────────────────┐
│ AuthorizationFilter              │ ← Check hasRole, hasAuthority
└─────────────┬───────────────────┘
              ↓
         Controller
```

### 🛡️ Authorization Checks

**SecurityConfig.java:**
```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/", "/auth/**").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
    .anyRequest().authenticated()
)
```

**How it works:**
```
1. Request: GET /admin/dashboard
2. AuthorizationFilter checks SecurityContext
3. Get Authentication → Get Authorities
4. Check if has ROLE_ADMIN
5. If yes → Continue to Controller
6. If no → AccessDeniedException → Redirect to login
```

### 🎯 Accessing Current User

#### **In Controller:**
```java
@GetMapping("/profile")
public String profile(Authentication authentication, Model model) {
    User user = (User) authentication.getPrincipal();
    model.addAttribute("user", user);
    return "user/profile";
}
```

#### **In Service:**
```java
@Service
public class BookService {
    public void purchaseBook(String bookId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        
        // Use user...
    }
}
```

#### **In Thymeleaf:**
```html
<!-- Get username -->
<span sec:authentication="name">Username</span>

<!-- Get user property -->
<span sec:authentication="principal.fullName">Full Name</span>
<span sec:authentication="principal.email">Email</span>

<!-- Check role -->
<div sec:authorize="hasRole('ADMIN')">
    Admin content
</div>

<!-- Session attribute -->
<span th:text="${session.fullName}">Full Name</span>
```

---

## 🎯 Best Practices

### ✅ DO's

1. **Hash passwords với BCrypt**
```java
String hashed = passwordEncoder.encode(plainPassword);
```

2. **Validate ở nhiều layers**
```java
// Controller: Form validation
@Valid @ModelAttribute RegisterDto dto

// Service: Business validation
if (userRepository.findByUsername(username).isPresent()) {
    throw new Exception("Username exists");
}
```

3. **Dùng flash attributes cho messages**
```java
redirectAttributes.addFlashAttribute("successMessage", "Success!");
```

4. **Update last login**
```java
userService.updateLastLogin(userId);
```

5. **Role-based redirect**
```java
if (isAdmin) return "redirect:/admin/dashboard";
else return "redirect:/user/index";
```

---

### ❌ DON'Ts

1. **Không lưu plain password**
```java
// ❌ NEVER DO THIS
user.setPassword(plainPassword);

// ✅ Always hash
user.setPasswordHash(passwordEncoder.encode(plainPassword));
```

2. **Không expose thông tin user trong error messages**
```java
// ❌ Bad: Attacker biết username exists
throw new Exception("Password incorrect");

// ✅ Good: Generic message
throw new Exception("Username or password incorrect");
```

3. **Không dùng GET cho logout**
```java
// ❌ Vulnerable to CSRF
@GetMapping("/logout")

// ✅ Use POST with CSRF token
@PostMapping("/logout")
```

4. **Không quên invalidate session**
```java
// ❌ SecurityContext cleared, nhưng session still active
SecurityContextHolder.clearContext();

// ✅ Clear both
session.invalidate();
SecurityContextHolder.clearContext();
```

---

## 📚 Tài liệu liên quan

- [BACKEND_OVERVIEW.md](BACKEND_OVERVIEW.md) - Tổng quan
- [CONFIG_DETAILED.md](CONFIG_DETAILED.md) - Security configuration
- [CONTROLLERS_DETAILED.md](CONTROLLERS_DETAILED.md) - Controllers
- [SERVICES_DETAILED.md](SERVICES_DETAILED.md) - Services

---

**Cập nhật:** 21/11/2025

