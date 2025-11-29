# Security Configuration - Ebook Store

## Tổng Quan Bảo Mật

Hệ thống Ebook Store sử dụng **Spring Security 6** để xử lý authentication và authorization. Authentication được thực hiện qua **Session-based login** với **BCrypt password hashing**.

---

## Authentication Flow

### 1. Login Process

```
┌──────────┐
│  User    │
└────┬─────┘
     │ 1. GET /auth/login
     ▼
┌──────────────────┐
│ AuthController   │
└────┬─────────────┘
     │ 2. Show login form
     ▼
┌──────────────────┐
│  User enters     │
│  credentials     │
└────┬─────────────┘
     │ 3. POST /auth/login
     ▼
┌──────────────────┐
│ AuthController   │
│  - Validate      │
│  - Load user     │
│  - Check pwd     │
└────┬─────────────┘
     │ 4a. Success
     ▼
┌──────────────────┐
│ Create Session   │
│ Set Principal    │
└────┬─────────────┘
     │ 5. Redirect
     ▼
┌──────────────────┐
│ /home or /admin  │
└──────────────────┘
```

### 2. Registration Process

```
┌──────────┐
│  User    │
└────┬─────┘
     │ 1. GET /auth/register
     ▼
┌──────────────────┐
│  Register Form   │
└────┬─────────────┘
     │ 2. Fill form
     │ 3. POST /auth/register
     ▼
┌──────────────────┐
│ AuthController   │
│  - Validate      │
│  - Check unique  │
│  - Hash password │
│  - Save user     │
└────┬─────────────┘
     │ 4. Success
     ▼
┌──────────────────┐
│ Redirect to      │
│ /auth/login      │
└──────────────────┘
```

---

## Security Configuration Details

### File: `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Configuration...
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Authorization Rules

### 1. Public Access (No Authentication Required)

```java
.requestMatchers(
    "/", "/home", "/login", "/register",  // Main pages
    "/auth/**",                            // Auth endpoints
    "/css/**", "/js/**", "/images/**",    // Static resources
    "/user_template/**",                  // User template
    "/admin_template/**",                 // Admin template
    "/shared/**",                         // Shared resources
    "/static/**",                         // Static path
    "/book_asset/**",                     // Book assets (images)
    "/uploads/**"                         // Upload directory
).permitAll()
```

**Accessible to**: Everyone (no login required)

**Use cases**:
- Homepage
- Login/Register pages
- Static resources (CSS, JS, images)
- Book cover images

---

### 2. Protected Book Source Files (Authenticated Users)

```java
.requestMatchers("/book_asset/source/**")
    .authenticated()
```

**Accessible to**: Logged-in users only (USER or ADMIN)

**Use cases**:
- Download PDF/EPUB files
- Read ebooks online

**Files protected**:
- `/book_asset/source/khoahoc-vientuong/*.pdf`
- `/book_asset/source/tamly-kynangsong/*.epub`
- etc.

---

### 3. Admin-Only Access

```java
.requestMatchers("/admin/**")
    .hasRole("ADMIN")
```

**Accessible to**: ADMIN role only

**Use cases**:
- Admin dashboard
- Manage books, users, orders
- System configuration
- Statistics and reports

---

### 4. User and Admin Access

```java
.requestMatchers(
    "/user/**",              // User routes
    "/subscription/manage"   // Subscription management
).hasAnyRole("USER", "ADMIN")
```

**Accessible to**: USER or ADMIN roles

**Use cases**:
- User dashboard
- Profile management
- Shopping cart
- Order history
- Reading progress

---

### 5. Default Rule

```java
.anyRequest().authenticated()
```

**All other routes**: Require authentication

---

## CSRF Protection

### Configuration

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .ignoringRequestMatchers("/admin/books/delete/**")
)
```

### CSRF Token Details

**Type**: Cookie-based CSRF tokens

**Cookie Name**: `XSRF-TOKEN`

**Header Name**: `X-CSRF-TOKEN`

**HttpOnly**: `false` (để JavaScript có thể đọc)

### Protected Requests

- All POST requests
- All PUT requests
- All DELETE requests

**Except**:
- `/admin/books/delete/**` (ignored for convenience)

### How to Use CSRF Token

**In HTML Forms**:
```html
<form method="post" th:action="@{/user/profile}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <!-- form fields -->
</form>
```

**In AJAX (jQuery)**:
```javascript
$.ajax({
    url: '/api/endpoint',
    method: 'POST',
    headers: {
        'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
    },
    data: { /* data */ }
});
```

**In Thymeleaf meta tag**:
```html
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

---

## Session Management

### Configuration

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)
```

### Session Details

**Creation Policy**: `IF_REQUIRED` (tạo session khi cần)

**Session Cookie**: `JSESSIONID`

**Session Timeout**: Default 30 minutes (configurable in `application.properties`)

**Concurrent Sessions**: Unlimited (có thể giới hạn)

### Session Storage

**In-memory**: Default (HttpSession)

**Future**: Redis session store for scalability

---

## Exception Handling

### 1. Access Denied Handler (403 Forbidden)

**Scenario**: User đã login nhưng không có quyền truy cập

```java
.accessDeniedHandler((request, response, accessDeniedException) -> {
    String ajaxHeader = request.getHeader("X-Requested-With");
    boolean isAjax = "XMLHttpRequest".equals(ajaxHeader);
    
    if (isAjax) {
        // AJAX request
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"message\":\"Không có quyền truy cập!\"}"
        );
    } else {
        // Browser request
        response.sendRedirect("/auth/login?error=access_denied");
    }
})
```

**Response**:
- **AJAX**: JSON error message
- **Browser**: Redirect to login with error

---

### 2. Authentication Entry Point (401 Unauthorized)

**Scenario**: User chưa login hoặc session expired

```java
.authenticationEntryPoint((request, response, authException) -> {
    String ajaxHeader = request.getHeader("X-Requested-With");
    String acceptHeader = request.getHeader("Accept");
    
    boolean isAjax = "XMLHttpRequest".equals(ajaxHeader) ||
                   (acceptHeader != null && acceptHeader.contains("application/json"));
    
    if (isAjax) {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"message\":\"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!\"}"
        );
    } else {
        response.sendRedirect("/auth/login");
    }
})
```

**Response**:
- **AJAX**: 401 with JSON error
- **Browser**: Redirect to login page

---

## Password Security

### Password Encoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Algorithm**: BCrypt with salt

**Strength**: Default (10 rounds)

**Format**: `$2a$10$...` (60 characters)

### Password Hashing Process

```
Plain Password
    ↓
BCryptPasswordEncoder.encode()
    ↓
$2a$10$N9qo8uLOickgx2ZMRZoMye... (60 chars)
    ↓
Store in database
```

### Password Verification

```
Input Password
    ↓
BCryptPasswordEncoder.matches(input, storedHash)
    ↓
Boolean (true/false)
```

---

## User Authentication Process

### Custom Authentication (No default Spring form)

**Form Login**: DISABLED  
**HTTP Basic**: DISABLED  
**Custom Controller**: `AuthController.java`

### Login Method

```java
@PostMapping("/login")
public String login(
    @RequestParam String username,
    @RequestParam String password,
    HttpSession session,
    HttpServletRequest request
) {
    // 1. Load user from database
    User user = userService.findByUsername(username);
    
    // 2. Verify password
    if (passwordEncoder.matches(password, user.getPasswordHash())) {
        // 3. Create authentication
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities()
        );
        
        // 4. Set security context
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // 5. Store in session
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );
        
        // 6. Redirect based on role
        if (user.hasRole("ADMIN")) {
            return "redirect:/admin";
        } else {
            return "redirect:/home";
        }
    }
    
    // Failed login
    return "redirect:/auth/login?error=true";
}
```

---

## Role-Based Access Control (RBAC)

### Roles

| Role ID | Role Name | Description |
|---------|-----------|-------------|
| role_user | USER | Người dùng thông thường |
| role_admin | ADMIN | Quản trị viên |

### Role Hierarchy

```
ADMIN
  ↓ (includes all USER permissions)
USER
```

**Note**: Hiện tại không có role hierarchy config, nhưng ADMIN có thể access tất cả USER endpoints.

### Checking Permissions

**In Controller**:
```java
@PreAuthorize("hasRole('ADMIN')")
public String adminOnly() {
    // Admin only
}

@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public String userAndAdmin() {
    // User or Admin
}
```

**In Service**:
```java
@Secured("ROLE_ADMIN")
public void deleteUser(String userId) {
    // Admin only
}
```

**In Thymeleaf**:
```html
<div sec:authorize="hasRole('ADMIN')">
    Admin content
</div>

<div sec:authorize="isAuthenticated()">
    Logged in user content
</div>
```

---

## Security Headers

### Default Spring Security Headers

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, must-revalidate
Pragma: no-cache
Expires: 0
```

### HTTPS (Production)

**Current**: HTTP only (development)

**Production**: Should enable HTTPS

```java
.requiresChannel(channel -> channel
    .anyRequest().requiresSecure()
)
```

---

## Logout Process

### Custom Logout

**Endpoint**: `/auth/logout` (POST)

**Handler**: `AuthController.logout()`

```java
@PostMapping("/logout")
public String logout(HttpSession session) {
    // Clear security context
    SecurityContextHolder.clearContext();
    
    // Invalidate session
    session.invalidate();
    
    // Redirect to login
    return "redirect:/auth/login?logout=true";
}
```

---

## Security Best Practices Implemented

✅ **Password Hashing**: BCrypt with salt  
✅ **CSRF Protection**: Cookie-based tokens  
✅ **Session Management**: Secure session handling  
✅ **Role-Based Access**: Granular permissions  
✅ **XSS Prevention**: Thymeleaf auto-escaping  
✅ **SQL Injection Prevention**: JPA parameterized queries  
✅ **File Upload Validation**: Type and size checks  
✅ **Error Handling**: No sensitive info in errors  

---

## Security Recommendations

### For Production

1. **Enable HTTPS**: Use SSL/TLS certificates
2. **Secure Cookies**: Set `secure=true`, `httpOnly=true`
3. **Session Timeout**: Configure appropriate timeout
4. **Rate Limiting**: Prevent brute force attacks
5. **CORS Configuration**: Restrict allowed origins
6. **Content Security Policy**: Add CSP headers
7. **Regular Updates**: Keep dependencies updated
8. **Security Audit**: Regular security testing
9. **Logging**: Log security events (login attempts, etc.)
10. **Backup**: Regular database backups

### Current Vulnerabilities to Address

⚠️ **No rate limiting**: Can be brute-forced  
⚠️ **Session in memory**: Not scalable  
⚠️ **HTTP only**: Should use HTTPS  
⚠️ **No 2FA**: Single factor authentication  
⚠️ **No password policy**: Weak passwords allowed  

---

## Testing Security

### Manual Testing

1. **Access Control**:
   - Try accessing `/admin` without login → Should redirect
   - Try accessing `/admin` as USER → Should deny
   - Try accessing `/user` without login → Should redirect

2. **CSRF Protection**:
   - POST without CSRF token → Should fail
   - POST with valid token → Should succeed

3. **Session Management**:
   - Login → Check session created
   - Logout → Check session destroyed
   - Session timeout → Should redirect to login

### Automated Testing (Future)

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {
    
    @Test
    void testAdminAccessWithoutAuth() {
        mockMvc.perform(get("/admin"))
               .andExpect(status().is3xxRedirection());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void testAdminAccessAsUser() {
        mockMvc.perform(get("/admin"))
               .andExpect(status().isForbidden());
    }
}
```

---

**Tài liệu liên quan**:
- [Cấu trúc dự án](PROJECT_STRUCTURE.md)
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Database Schema](DATABASE_SCHEMA.md)

