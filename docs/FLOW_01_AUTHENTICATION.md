# 🔐 FLOW 01: AUTHENTICATION (Xác Thực Người Dùng)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 1.1: Đăng Ký](#flow-11-đăng-ký)
3. [Flow 1.2: Đăng Nhập](#flow-12-đăng-nhập)
4. [Flow 1.3: Đăng Xuất](#flow-13-đăng-xuất)
5. [Debugging Endpoints](#debugging-endpoints)

---

## Tổng Quan

### Components
- **Controller**: `AuthController.java`
- **Service**: `UserService.java`
- **Repository**: `UserRepository.java`, `RoleRepository.java`
- **Entity**: `User.java`, `Role.java`
- **Security**: `SecurityConfig.java`

### URLs
- `/auth/register` - Trang đăng ký
- `/auth/login` - Trang đăng nhập
- `/auth/logout` - Đăng xuất

---

## Flow 1.1: Đăng Ký

### Sequence Diagram
```
User → Browser → AuthController → UserService → UserRepository → Database
  │       │            │               │              │             │
  │  GET /auth/register                                             │
  │───────────────────►│                                            │
  │◄───────────────────┤ (return register.html)                    │
  │       │            │                                            │
  │  POST /auth/register (RegisterDto)                             │
  │───────────────────►│                                            │
  │       │            │ saveUser()                                 │
  │       │            ├──────────────►│                            │
  │       │            │                │ existsByUsername()        │
  │       │            │                ├──────────────►│           │
  │       │            │                │◄──────────────┤           │
  │       │            │                │ existsByEmail()           │
  │       │            │                ├──────────────►│           │
  │       │            │                │◄──────────────┤           │
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

**Request Body**:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "confirmPassword": "SecurePass123",
  "fullName": "John Doe",
  "phoneNumber": "0123456789"
}
```

**Service Logic**:
```java
public User saveUser(RegisterDto registerDto) {
    // 1. Validate username không tồn tại
    if (userRepository.existsByUsername(registerDto.getUsername())) {
        throw new RuntimeException("Username đã tồn tại");
    }
    
    // 2. Validate email không tồn tại
    if (userRepository.existsByEmail(registerDto.getEmail())) {
        throw new RuntimeException("Email đã tồn tại");
    }
    
    // 3. Hash password
    String hashedPassword = passwordEncoder.encode(registerDto.getPassword());
    
    // 4. Tạo user mới
    User user = new User();
    user.setUsername(registerDto.getUsername());
    user.setEmail(registerDto.getEmail());
    user.setPassword(hashedPassword);
    user.setFullName(registerDto.getFullName());
    user.setPhoneNumber(registerDto.getPhoneNumber());
    user.setEnabled(true);
    
    // 5. Gán role USER mặc định
    Role userRole = roleRepository.findByName("ROLE_USER");
    user.setRoles(Arrays.asList(userRole));
    
    // 6. Lưu vào database
    return userRepository.save(user);
}
```

**SQL Queries**:
```sql
-- Check username exists
SELECT COUNT(*) FROM users WHERE username = ?

-- Check email exists
SELECT COUNT(*) FROM users WHERE email = ?

-- Insert new user
INSERT INTO users (username, email, password, full_name, phone_number, enabled, created_at)
VALUES (?, ?, ?, ?, ?, true, NOW())

-- Insert user_roles
INSERT INTO user_roles (user_id, role_id)
VALUES (?, (SELECT role_id FROM roles WHERE role_name = 'ROLE_USER'))
```

---

## Flow 1.2: Đăng Nhập

### Sequence Diagram
```
User → Browser → AuthController → Spring Security → UserService → Database
  │       │            │                 │              │            │
  │  GET /auth/login                                                 │
  │───────────────────►│                                             │
  │◄───────────────────┤ (return login.html)                        │
  │       │            │                                             │
  │  POST /auth/login (username, password)                          │
  │───────────────────►│                                             │
  │       │            │ authenticate()                              │
  │       │            ├───────────────►│                            │
  │       │            │                │ loadUserByUsername()       │
  │       │            │                ├──────────────►│            │
  │       │            │                │                │ findByUsername
  │       │            │                │                ├───────────►│
  │       │            │                │                │◄───────────┤
  │       │            │                │◄──────────────┤            │
  │       │            │                │ verifyPassword()           │
  │       │            │                │ createAuthToken()          │
  │       │            │                │ setInContext()             │
  │       │            │◄───────────────┤                            │
  │◄───────────────────┤ redirect based on role                     │
```

### Implementation Details

**Endpoint**: `POST /auth/login`

**Request Parameters**:
- `username`: string
- `password`: string
- `remember-me`: boolean (optional)

**Service Logic**:
```java
public UserDetails loadUserByUsername(String username) {
    // 1. Tìm user theo username
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User không tồn tại"));
    
    // 2. Check enabled
    if (!user.isEnabled()) {
        throw new DisabledException("Account đã bị vô hiệu hóa");
    }
    
    // 3. Convert roles
    Set<GrantedAuthority> authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .collect(Collectors.toSet());
    
    // 4. Return UserDetails
    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        authorities
    );
}
```

**Security Configuration**:
```java
// Redirect sau khi login thành công
.successHandler((request, response, authentication) -> {
    if (authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
        response.sendRedirect("/admin/dashboard");
    } else {
        response.sendRedirect("/user/index");
    }
})
```

**SQL Query**:
```sql
SELECT u.*, r.role_id, r.role_name
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = ?
```

---

## Flow 1.3: Đăng Xuất

### Sequence Diagram
```
User → Browser → Spring Security → Session Manager
  │       │            │                 │
  │  GET /auth/logout                    │
  │───────────────────►│                 │
  │       │            │ invalidate()    │
  │       │            ├────────────────►│
  │       │            │ clearContext()  │
  │       │            │ deleteCookies() │
  │       │            │◄────────────────┤
  │◄───────────────────┤ redirect:/auth/login?logout
```

### Implementation Details

**Endpoint**: `GET /auth/logout`

**Security Configuration**:
```java
http.logout()
    .logoutUrl("/auth/logout")
    .logoutSuccessUrl("/auth/login?logout")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .clearAuthentication(true);
```

---

## Debugging Endpoints

### 1. Test Registration

**cURL Command**:
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser&email=test@example.com&password=Test123&confirmPassword=Test123&fullName=Test User&phoneNumber=0123456789"
```

**Postman**:
- Method: `POST`
- URL: `http://localhost:8080/auth/register`
- Body (form-data):
  - username: testuser
  - email: test@example.com
  - password: Test123
  - confirmPassword: Test123
  - fullName: Test User
  - phoneNumber: 0123456789

**Expected Response**: Redirect to `/auth/login`

### 2. Test Login

**cURL Command**:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser&password=Test123" \
  -c cookies.txt -L
```

**Browser Console Test**:
```javascript
// In browser console on login page
const form = document.querySelector('form');
const formData = new FormData(form);
formData.append('username', 'testuser');
formData.append('password', 'Test123');
fetch('/auth/login', {
  method: 'POST',
  body: formData
}).then(res => console.log(res));
```

### 3. Check Current User Session

**Controller Endpoint** (Tạo endpoint debug):
```java
@GetMapping("/debug/current-user")
@ResponseBody
public Map<String, Object> getCurrentUser(Authentication authentication) {
    Map<String, Object> result = new HashMap<>();
    if (authentication != null) {
        result.put("authenticated", true);
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

