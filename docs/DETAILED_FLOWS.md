# 🔄 GIẢI THÍCH CHI TIẾT CÁC LUỒNG XỬ LÝ CHÍNH

**Ngày tạo:** 23/11/2025  
**Dự án:** Ebook Store System

---

## 📋 MỤC LỤC

1. [Luồng Authentication & Authorization](#1-luồng-authentication--authorization)
2. [Luồng Book Management (CRUD)](#2-luồng-book-management-crud)
3. [Luồng File Upload](#3-luồng-file-upload)
4. [Luồng Shopping Cart](#4-luồng-shopping-cart)
5. [Luồng Checkout & Order](#5-luồng-checkout--order)
6. [Luồng Payment](#6-luồng-payment)
7. [Luồng Review & Rating](#7-luồng-review--rating)
8. [Luồng Reading & Progress Tracking](#8-luồng-reading--progress-tracking)

---

## 1. LUỒNG AUTHENTICATION & AUTHORIZATION

### 1.1. Luồng Đăng Ký (Register) ✅ HOÀN THÀNH

#### Mô tả
Người dùng tạo tài khoản mới trong hệ thống.

#### Sequence Flow

```
┌──────┐   ┌──────────────┐   ┌─────────────┐   ┌──────────────┐   ┌──────────┐
│ User │───│ AuthController│───│ UserService │───│ UserRepository│───│ Database │
└──────┘   └──────────────┘   └─────────────┘   └──────────────┘   └──────────┘
   │              │                   │                  │                 │
   │ 1. GET /auth/register           │                  │                 │
   │─────────────>│                   │                  │                 │
   │              │                   │                  │                 │
   │<─────────────│ Show register form│                  │                 │
   │              │                   │                  │                 │
   │ 2. Fill form & Submit            │                  │                 │
   │─────────────>│                   │                  │                 │
   │              │                   │                  │                 │
   │              │ 3. registerUser(dto)                 │                 │
   │              │──────────────────>│                  │                 │
   │              │                   │                  │                 │
   │              │                   │ 4. findByUsername()                │
   │              │                   │─────────────────>│                 │
   │              │                   │                  │ SELECT username │
   │              │                   │                  │────────────────>│
   │              │                   │                  │<────────────────│
   │              │                   │<─────────────────│ null (not exist)│
   │              │                   │                  │                 │
   │              │                   │ 5. findByEmail()  │                 │
   │              │                   │─────────────────>│                 │
   │              │                   │                  │ SELECT email    │
   │              │                   │                  │────────────────>│
   │              │                   │                  │<────────────────│
   │              │                   │<─────────────────│ null (not exist)│
   │              │                   │                  │                 │
   │              │                   │ 6. Hash password (BCrypt)           │
   │              │                   │────────┐         │                 │
   │              │                   │<───────┘         │                 │
   │              │                   │                  │                 │
   │              │                   │ 7. Create User entity               │
   │              │                   │────────┐         │                 │
   │              │                   │<───────┘         │                 │
   │              │                   │                  │                 │
   │              │                   │ 8. Set Role = "USER"                │
   │              │                   │────────┐         │                 │
   │              │                   │<───────┘         │                 │
   │              │                   │                  │                 │
   │              │                   │ 9. save(user)     │                 │
   │              │                   │─────────────────>│                 │
   │              │                   │                  │ INSERT INTO users│
   │              │                   │                  │────────────────>│
   │              │                   │                  │<────────────────│
   │              │                   │<─────────────────│ User saved      │
   │              │                   │                  │                 │
   │              │<──────────────────│ User registered  │                 │
   │              │                   │                  │                 │
   │<─────────────│ Redirect to /auth/login?registered=true               │
   │              │                   │                  │                 │
   │ Success message displayed        │                  │                 │
```

#### Chi tiết từng bước

**Bước 1: User truy cập form đăng ký**
- URL: `GET /auth/register`
- Controller method: `AuthController.showRegisterPage()`
- Template: `templates/auth/register.html`
- Model attributes: `registerDto` (empty object)

**Bước 2: User điền thông tin và submit**
- Form fields:
  - `username`: Tên đăng nhập (unique)
  - `email`: Email (unique)
  - `password`: Mật khẩu (min 6 ký tự)
  - `confirmPassword`: Xác nhận mật khẩu
  - `fullName`: Họ tên đầy đủ
  - `phone`: Số điện thoại (optional)
- Method: `POST /auth/register`
- DTO: `RegisterDto` với validation annotations

**Bước 3: Controller gọi Service**
```java
@PostMapping("/register")
public String register(@Valid @ModelAttribute RegisterDto registerDto, 
                      BindingResult result,
                      RedirectAttributes redirectAttributes) {
    // Validation errors?
    if (result.hasErrors()) {
        return "auth/register";
    }
    
    try {
        // Call service to register user
        userService.registerUser(registerDto);
        
        // Success
        redirectAttributes.addFlashAttribute("success", 
            "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/auth/login";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/auth/register";
    }
}
```

**Bước 4-5: Service kiểm tra username và email đã tồn tại**
```java
@Override
public User registerUser(RegisterDto dto) {
    // Check username exists
    if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
        throw new DuplicateException("Username đã tồn tại");
    }
    
    // Check email exists
    if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
        throw new DuplicateException("Email đã được sử dụng");
    }
    
    // Continue...
}
```

**Bước 6: Hash password với BCrypt**
```java
// Encode password
String hashedPassword = passwordEncoder.encode(dto.getPassword());
```

**Bước 7-8: Tạo User entity và gán role**
```java
User user = new User();
user.setUserId(UUID.randomUUID().toString());
user.setUsername(dto.getUsername());
user.setEmail(dto.getEmail());
user.setPassword(hashedPassword); // Hashed password
user.setFullName(dto.getFullName());
user.setPhone(dto.getPhone());
user.setRole("USER"); // Default role
user.setIsActive(true);
```

**Bước 9: Lưu vào database**
```java
return userRepository.save(user);
```

#### Validation Rules

| Field | Validation |
|-------|-----------|
| username | Required, 3-20 chars, unique |
| email | Required, valid email format, unique |
| password | Required, min 6 chars |
| confirmPassword | Must match password |
| fullName | Required, max 100 chars |
| phone | Optional, valid phone format |

#### Error Handling

| Error | Message | Action |
|-------|---------|--------|
| Username exists | "Username đã tồn tại" | Show error, stay on form |
| Email exists | "Email đã được sử dụng" | Show error, stay on form |
| Validation error | Field-specific messages | Show errors on form |
| Server error | "Lỗi hệ thống" | Show error, stay on form |

#### Success Flow
- User được tạo với role = "USER"
- Password được hash bằng BCrypt
- Redirect về trang login với success message
- User có thể đăng nhập ngay

---

### 1.2. Luồng Đăng Nhập (Login) ✅ HOÀN THÀNH

#### Mô tả
Người dùng đăng nhập vào hệ thống với username/password.

#### Sequence Flow

```
┌──────┐   ┌───────���──────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────┐
│ User │───│ Spring Security│───│ UserDetailsService│──│ UserRepository│───│ Database │
└──────┘   └──────────────┘   └──────────────┘   └──────────────┘   └──────────┘
   │              │                   │                   │                  │
   │ 1. GET /auth/login              │                   │                  │
   │─────────────>│                   │                   │                  │
   │              │                   │                   │                  │
   │<─────────────│ Show login form   │                   │                  │
   │              │                   │                   │                  │
   │ 2. Enter credentials & Submit    │                   │                  │
   │─────────────>│                   │                   │                  │
   │              │                   │                   │                  │
   │              │ 3. Authentication attempt              │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 4. loadUserByUsername(username)        │                  │
   │              │──────────────────>│                   │                  │
   │              │                   │                   │                  │
   │              │                   │ 5. findByUsername()│                  │
   │              │                   │──────────────────>│                  │
   │              │                   │                   │ SELECT * FROM users│
   │              │                   │                   │─────────────────>│
   │              │                   │                   │<─────────────────│
   │              │                   │<──────────────────│ User found       │
   │              │                   │                   │                  │
   │              │<──────────────────│ UserDetails       │                  │
   │              │                   │                   │                  │
   │              │ 6. Compare password (BCrypt)           │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 7. Password matches? YES               │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 8. Create Authentication object        │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 9. Store in SecurityContext            │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 10. Create session │                   │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 11. Success Handler│                   │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │              │ 12. Check user role│                   │                  │
   │              │──────────┐        │                   │                  │
   │              │<─────────┘        │                   │                  │
   │              │                   │                   │                  │
   │<─────────────│ Redirect based on role:               │                  │
   │              │   - ADMIN → /admin/dashboard          │                  │
   │              │   - USER → /user/index                 │                  │
```

#### Chi tiết từng bước

**Bước 1: User truy cập login page**
- URL: `GET /auth/login`
- Template: `templates/auth/login.html`
- Form fields: username, password, remember-me

**Bước 2: User submit credentials**
- Method: `POST /auth/login`
- Spring Security tự động intercept request này
- Không cần controller method (Spring Security handle)

**Bước 3-4: Spring Security xử lý authentication**
```java
// Spring Security gọi UserDetailsService
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {
        // Load user from database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> 
                new UsernameNotFoundException("User not found: " + username));
        
        // Check if active
        if (!user.getIsActive()) {
            throw new DisabledException("User is disabled");
        }
        
        // Convert to UserDetails
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword()) // Hashed password
            .authorities("ROLE_" + user.getRole()) // ROLE_ADMIN or ROLE_USER
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(!user.getIsActive())
            .build();
    }
}
```

**Bước 5-6: Load user và verify password**
- UserRepository query database by username
- Spring Security tự động so sánh password:
  - Password trong form (plain text)
  - Password trong DB (BCrypt hashed)
  - Sử dụng `BCryptPasswordEncoder.matches()`

**Bước 7-9: Tạo Authentication object**
```java
// Spring Security tự động tạo
Authentication authentication = new UsernamePasswordAuthenticationToken(
    userDetails,
    password,
    userDetails.getAuthorities()
);

// Store in SecurityContext
SecurityContextHolder.getContext().setAuthentication(authentication);
```

**Bước 10: Tạo session**
- Spring Security tự động tạo HTTP session
- Session timeout: 30 minutes (configurable)
- Session ID được lưu trong cookie

**Bước 11-12: Success handler redirect**
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication) throws IOException {
                
                // Get user authorities
                Collection<? extends GrantedAuthority> authorities = 
                    authentication.getAuthorities();
                
                // Redirect based on role
                String redirectUrl = "/";
                for (GrantedAuthority authority : authorities) {
                    if (authority.getAuthority().equals("ROLE_ADMIN")) {
                        redirectUrl = "/admin/dashboard";
                        break;
                    } else if (authority.getAuthority().equals("ROLE_USER")) {
                        redirectUrl = "/user/index";
                        break;
                    }
                }
                
                response.sendRedirect(redirectUrl);
            }
        };
    }
}
```

#### Security Configuration

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Public URLs
            .requestMatchers("/", "/home", "/auth/**").permitAll()
            .requestMatchers("/Book_Asset/**").permitAll()
            
            // Admin URLs
            .requestMatchers("/admin/**").hasRole("ADMIN")
            
            // User URLs
            .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
            
            // All others need authentication
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/auth/login")
            .loginProcessingUrl("/auth/login") // Form submit URL
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(successHandler())
            .failureUrl("/auth/login?error=true")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/auth/login?logout=true")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        )
        .sessionManagement(session -> session
            .sessionFixation().migrateSession()
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
        )
        .csrf(csrf -> csrf.csrfTokenRepository(
            CookieCsrfTokenRepository.withHttpOnlyFalse()
        ));
    
    return http.build();
}

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10); // Strength 10
}
```

#### Error Handling

| Scenario | URL Redirect | Message |
|----------|-------------|---------|
| Invalid credentials | `/auth/login?error=true` | "Sai tên đăng nhập hoặc mật khẩu" |
| User disabled | `/auth/login?error=true` | "Tài khoản đã bị khóa" |
| Session expired | `/auth/login?expired=true` | "Phiên đăng nhập hết hạn" |
| Successful logout | `/auth/login?logout=true` | "Đăng xuất thành công" |

#### Session Management

- **Session timeout:** 30 minutes
- **Concurrent sessions:** Max 1 session per user
- **Session fixation protection:** Migrate session on login
- **Remember me:** Optional (7 days if checked)

---

### 1.3. Luồng Authorization (Phân Quyền) ✅ HOÀN THÀNH

#### Mô tả
Hệ thống kiểm tra quyền truy cập của user vào các tài nguyên.

#### Sequence Flow

```
┌──────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ User │───│ Spring Security│───│SecurityContext│───│  Controller  │
└──────┘   └──────────────┘   └──────────────┘   └──────────────┘
   │              │                   │                   │
   │ Request URL  │                   │                   │
   │─────────────>│                   │                   │
   │              │                   │                   │
   │              │ 1. Check authentication                │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │              │ NOT authenticated?│                   │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │<─────────────│ Redirect to /auth/login               │
   │              │                   │                   │
   │              │ IS authenticated? │                   │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │              │ 2. Get Authentication from context    │
   │              │──────────────────>│                   │
   │              │<──────────────────│ Authentication    │
   │              │                   │                   │
   │              │ 3. Check URL pattern authorization    │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │              │ /admin/** → Requires ROLE_ADMIN       │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │              │ 4. Check user has required role       │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │              │ Has ROLE_ADMIN? YES                   │
   │              │──────────┐        │                   │
   │              │<─────────┘        │                   │
   │              │                   │                   │
   │              │ 5. Allow access    │                   │
   │              │───────────────────────────────────────>│
   │              │                   │                   │
   │              │                   │ 6. Process request │
   │              │                   │         ┌─────────┤
   │              │                   │         └────────>│
   │              │                   │                   │
   │<──────────────────────────────────────────────────────│ Response
```

#### Authorization Rules

**URL Pattern Matching:**

| URL Pattern | Allowed Roles | Description |
|------------|---------------|-------------|
| `/` | ALL | Home page |
| `/home` | ALL | Home page |
| `/auth/**` | PUBLIC | Login, register pages |
| `/Book_Asset/**` | PUBLIC | Static resources (images, files) |
| `/admin/**` | ROLE_ADMIN | Admin panel |
| `/admin/dashboard` | ROLE_ADMIN | Admin dashboard |
| `/admin/books/**` | ROLE_ADMIN | Book management |
| `/admin/users/**` | ROLE_ADMIN | User management |
| `/user/**` | ROLE_USER, ROLE_ADMIN | User features |
| `/user/index` | ROLE_USER, ROLE_ADMIN | User dashboard |
| `/user/cart/**` | ROLE_USER, ROLE_ADMIN | Shopping cart |
| `/user/orders/**` | ROLE_USER, ROLE_ADMIN | Order management |
| Others | AUTHENTICATED | Require login |

#### Role Hierarchy

```
ROLE_ADMIN
    ├── Can access all /admin/** URLs
    ├── Can access all /user/** URLs
    ├── Full CRUD on all resources
    └── View all statistics

ROLE_USER
    ├── Can access /user/** URLs only
    ├── Cannot access /admin/** URLs
    ├── Can manage own cart, orders
    └── Can view own statistics
```

#### Method-Level Security (Optional)

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/admin/books/delete/{id}")
public ResponseEntity<?> deleteBook(@PathVariable String id) {
    bookService.deleteBook(id);
    return ResponseEntity.ok("Deleted");
}

@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@PostMapping("/user/cart/add")
public String addToCart(@RequestParam String bookId) {
    // Add to cart logic
    return "redirect:/user/cart";
}

@PreAuthorize("@securityService.isOwner(#orderId)")
@GetMapping("/user/orders/{orderId}")
public String viewOrder(@PathVariable String orderId, Model model) {
    // Only allow user to view their own orders
    return "user/order-detail";
}
```

#### Access Denied Handling

**Scenario 1: User not logged in**
```
User requests /admin/dashboard
    ↓
Spring Security checks: Authenticated? NO
    ↓
Redirect to /auth/login
    ↓
After login success → Redirect back to /admin/dashboard
```

**Scenario 2: User logged in but insufficient role**
```
USER requests /admin/dashboard
    ↓
Spring Security checks: Has ROLE_ADMIN? NO
    ↓
Access Denied (403 Forbidden)
    ↓
Show custom error page: "Bạn không có quyền truy cập"
```

**Scenario 3: Session expired**
```
User idle for > 30 minutes
    ↓
Session invalidated automatically
    ↓
User requests any protected URL
    ↓
Redirect to /auth/login?expired=true
```

#### Thymeleaf Security Integration

**Show/hide based on role:**
```html
<!-- Only show to ADMIN -->
<div sec:authorize="hasRole('ADMIN')">
    <a href="/admin/dashboard">Admin Panel</a>
</div>

<!-- Show to USER or ADMIN -->
<div sec:authorize="hasAnyRole('USER', 'ADMIN')">
    <a href="/user/cart">My Cart</a>
</div>

<!-- Show only when authenticated -->
<div sec:authorize="isAuthenticated()">
    <span sec:authentication="name">Username</span>
    <a href="/logout">Logout</a>
</div>

<!-- Show only when NOT authenticated -->
<div sec:authorize="!isAuthenticated()">
    <a href="/auth/login">Login</a>
    <a href="/auth/register">Register</a>
</div>
```

**Get current user info:**
```html
<!-- Get username -->
<span sec:authentication="name"></span>

<!-- Get full user object -->
<span sec:authentication="principal.fullName"></span>

<!-- Check specific authority -->
<button sec:authorize="hasAuthority('ROLE_ADMIN')">
    Delete
</button>
```

---

## 2. LUỒNG BOOK MANAGEMENT (CRUD) ✅ HOÀN THÀNH

### 2.1. Luồng Thêm Sách Mới (Create) ✅

#### Mô tả
Admin thêm sách mới vào hệ thống với thông tin đầy đủ và upload ảnh bìa.

#### Sequence Flow

```
┌───────┐   ┌─────────────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────┐
│ Admin │───│AdminBookController│───│  BookService │───│  Repository│───│ Database │
└───────┘   └─────────────────┘   └──────────────┘   └──────────┘   └──────────┘
   │               │                       │                │               │
   │ 1. GET /admin/books/add               │                │               │
   │──────────────>│                       │                │               │
   │               │                       │                │               │
   │               │ Load data for form    │                │               │
   │               │──────────┐            │                │               │
   │               │<─────────┘            │                │               │
   │               │                       │                │               │
   │<──────────────│ Show form with:       │                │               │
   │               │ - All categories      │                │               │
   │               │ - All authors         │                │               │
   │               │ - Access types enum   │                │               │
   │               │                       │                │               │
   │ 2. Fill form & Upload cover image     │                │               │
   │──────────────>│                       │                │               │
   │               │                       │                │               │
   │               │ POST /admin/books/add │                │               │
   │               │──────────┐            │                │               │
   │               │<─────────┘            │                │               │
   │               │                       │                │               │
   │               │ 3. Validate DTO       │                │               │
   │               │──────────┐            │                │               │
   │               │<─────────┘            │                │               │
   │               │                       │                │               │
   │               │ 4. Upload cover image │                │               │
   │               │──────────────────────>│                │               │
   │               │                       │                │               │
   │               │                       │ Save to disk   │               │
   │               │                       │────────┐       │               │
   │               │                       │<───────┘       │               │
   │               │                       │                │               │
   │               │<──────────────────────│ Return URL     │               │
   │               │ /Book_Asset/image/covers/xxx.jpg       │               │
   │               │                       │                │               │
   │               │ 5. createBook(dto, authorIds)          │               │
   │               │──────────────────────>│                │               │
   │               │                       │                │               │
   │               │                       │ 6. Generate book_id            │
   │               │                       │────────┐       │               │
   │               │                       │<───────┘       │               │
   │               │                       │                │               │
   │               │                       │ 7. Create Book entity          │
   │               │                       │────────┐       │               │
   │               │                       │<───────┘       │               │
   │               │                       │                │               │
   │               │                       │ 8. Link category               │
   │               │                       │────────┐       │               │
   │               │                       │<───────┘       │               │
   │               │                       │                │               │
   │               │                       │ 9. Link authors (Many-to-Many) │
   │               │                       │────────┐       │               │
   │               │                       │<───────┘       │               │
   │               │                       │                │               │
   │               │                       │ 10. save(book) │               │
   │               │                       │───────────────>│               │
   │               │                       │                │ INSERT books  │
   │               │                       │                │──────────────>│
   │               │                       │                │ INSERT book_authors│
   │               │                       │                │──────────────>│
   │               │                       │                │<──────────────│
   │               │                       │<───────────────│ Book saved    │
   │               │                       │                │               │
   │               │<──────────────────────│ Book created   │               │
   │               │                       │                │               │
   │<──────────────│ Redirect /admin/books?success=true     │               │
   │               │                       │                │               │
   │ Success notification displayed        │                │               │
```

#### Chi tiết Implementation

**Step 1: Load Add Form**

```java
@GetMapping("/add")
public String addBookForm(Model model) {
    model.addAttribute("book", new BookDTO());
    model.addAttribute("categories", categoryService.getAllCategories());
    model.addAttribute("authors", authorService.getAllAuthors());
    model.addAttribute("accessTypes", Book.AccessType.values());
    return "admin/books/add";
}
```

**Step 2: Process Form Submission**

```java
@PostMapping("/add")
public String addBook(@Valid @ModelAttribute("book") BookDTO bookDTO,
                     BindingResult result,
                     @RequestParam(required = false) Set<String> authorIds,
                     @RequestParam(required = false) MultipartFile coverImage,
                     RedirectAttributes redirectAttributes) {
    
    // Validation errors?
    if (result.hasErrors()) {
        return "admin/books/add";
    }
    
    try {
        // Step 4: Upload cover image
        if (coverImage != null && !coverImage.isEmpty()) {
            String imageUrl = bookService.uploadCoverImage(coverImage);
            bookDTO.setCoverImageUrl(imageUrl);
        }
        
        // Step 5: Create book
        Book savedBook = bookService.createBook(bookDTO, authorIds);
        
        redirectAttributes.addFlashAttribute("success", 
            "Thêm sách thành công!");
        return "redirect:/admin/books?success=true";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/admin/books/add";
    }
}
```

**Step 4: Upload Cover Image**

```java
@Override
public String uploadCoverImage(MultipartFile file) throws IOException {
    // Validate file
    if (file.isEmpty()) {
        throw new IllegalArgumentException("File không được để trống");
    }
    
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        throw new IllegalArgumentException("File phải là hình ảnh");
    }
    
    // Check file size (max 5MB)
    if (file.getSize() > 5 * 1024 * 1024) {
        throw new IllegalArgumentException("File không được vượt quá 5MB");
    }
    
    // Generate unique filename
    String originalFilename = file.getOriginalFilename();
    String extension = originalFilename.substring(
        originalFilename.lastIndexOf(".")
    );
    String newFilename = "cover_" + System.currentTimeMillis() + extension;
    
    // Save to disk
    Path uploadPath = Paths.get(uploadDir, "book_asset/image/covers/");
    Files.createDirectories(uploadPath);
    
    Path targetPath = uploadPath.resolve(newFilename);
    Files.copy(file.getInputStream(), targetPath, 
        StandardCopyOption.REPLACE_EXISTING);
    
    // Return database URL
    return "/Book_Asset/image/covers/" + newFilename;
}
```

**Step 5-10: Create Book Entity**

```java
@Override
@Transactional
public Book createBook(BookDTO dto, Set<String> authorIds) {
    // Step 6: Generate book_id
    String bookId = "BOOK" + System.currentTimeMillis();
    
    // Step 7: Create Book entity
    Book book = new Book();
    book.setBookId(bookId);
    book.setTitle(dto.getTitle());
    book.setDescription(dto.getDescription());
    book.setPrice(dto.getPrice());
    book.setCoverImageUrl(dto.getCoverImageUrl());
    book.setPublisher(dto.getPublisher());
    book.setPublicationYear(dto.getPublicationYear());
    book.setLanguage(dto.getLanguage());
    book.setPages(dto.getPages());
    book.setIsbn(dto.getIsbn());
    book.setAccessType(dto.getAccessType());
    book.setIsDownloadable(dto.getIsDownloadable());
    
    // Step 8: Link category
    if (dto.getCategoryId() != null) {
        BookCategory category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new NotFoundException("Category not found"));
        book.setCategory(category);
    }
    
    // Step 9: Link authors (Many-to-Many)
    if (authorIds != null && !authorIds.isEmpty()) {
        Set<Author> authors = new HashSet<>();
        for (String authorId : authorIds) {
            Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found: " + authorId));
            authors.add(author);
        }
        book.setAuthors(authors);
    }
    
    // Step 10: Save to database
    Book savedBook = bookRepository.save(book);
    
    return savedBook;
}
```

#### Form HTML (Thymeleaf)

```html
<form th:action="@{/admin/books/add}" 
      th:object="${book}" 
      method="post" 
      enctype="multipart/form-data">
    
    <!-- Title -->
    <div class="form-group">
        <label>Tên sách <span class="text-danger">*</span></label>
        <input type="text" 
               class="form-control" 
               th:field="*{title}" 
               required />
        <span class="text-danger" 
              th:if="${#fields.hasErrors('title')}" 
              th:errors="*{title}"></span>
    </div>
    
    <!-- Category -->
    <div class="form-group">
        <label>Danh mục <span class="text-danger">*</span></label>
        <select class="form-control" th:field="*{categoryId}" required>
            <option value="">-- Chọn danh mục --</option>
            <option th:each="cat : ${categories}" 
                    th:value="${cat.categoryId}" 
                    th:text="${cat.categoryName}"></option>
        </select>
    </div>
    
    <!-- Authors (Multi-select with Select2) -->
    <div class="form-group">
        <label>Tác giả <span class="text-danger">*</span></label>
        <select class="form-control select2" 
                name="authorIds" 
                multiple="multiple" 
                required>
            <option th:each="author : ${authors}" 
                    th:value="${author.authorId}" 
                    th:text="${author.authorName}"></option>
        </select>
    </div>
    
    <!-- Cover Image Upload -->
    <div class="form-group">
        <label>Ảnh bìa</label>
        <input type="file" 
               class="form-control-file" 
               name="coverImage" 
               accept="image/*" />
        <small class="form-text text-muted">
            Chấp nhận JPG, PNG, JPEG, WEBP. Tối đa 5MB.
        </small>
    </div>
    
    <!-- Price -->
    <div class="form-group">
        <label>Giá (VNĐ) <span class="text-danger">*</span></label>
        <input type="number" 
               class="form-control" 
               th:field="*{price}" 
               min="0" 
               step="1000" 
               required />
    </div>
    
    <!-- Access Type -->
    <div class="form-group">
        <label>Loại truy cập <span class="text-danger">*</span></label>
        <select class="form-control" th:field="*{accessType}" required>
            <option th:each="type : ${accessTypes}" 
                    th:value="${type}" 
                    th:text="${type}"></option>
        </select>
    </div>
    
    <!-- Description -->
    <div class="form-group">
        <label>Mô tả</label>
        <textarea class="form-control" 
                  th:field="*{description}" 
                  rows="5"></textarea>
    </div>
    
    <!-- Other fields: publisher, year, language, pages, isbn, isDownloadable -->
    
    <!-- Submit Button -->
    <button type="submit" class="btn btn-primary">
        <i class="fas fa-save"></i> Lưu
    </button>
    <a href="/admin/books" class="btn btn-secondary">
        <i class="fas fa-times"></i> Hủy
    </a>
</form>

<script>
// Initialize Select2 for multi-select
$(document).ready(function() {
    $('.select2').select2({
        placeholder: 'Chọn tác giả',
        allowClear: true
    });
});
</script>
```

#### Validation

**BookDTO Validation:**
```java
@Data
public class BookDTO {
    @NotBlank(message = "Tên sách không được để trống")
    @Size(max = 255, message = "Tên sách không quá 255 ký tự")
    private String title;
    
    @NotNull(message = "Danh mục không được để trống")
    private String categoryId;
    
    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá phải >= 0")
    private BigDecimal price;
    
    @Pattern(regexp = "^(978|979)?\\d{9}(\\d|X)$", 
             message = "ISBN không hợp lệ")
    private String isbn;
    
    @Min(value = 1900, message = "Năm xuất bản không hợp lệ")
    @Max(value = 2100, message = "Năm xuất bản không hợp lệ")
    private Integer publicationYear;
    
    @Min(value = 1, message = "Số trang phải > 0")
    private Integer pages;
    
    // Other fields...
}
```

#### Success/Error Handling

**Success:**
- Book được tạo trong bảng `books`
- Relationships được tạo trong `book_authors`
- Cover image được lưu vào disk
- Redirect về `/admin/books` với success message
- DataTable tự động reload và hiển thị sách mới

**Errors:**
- Validation errors → Stay on form, show field errors
- File upload errors → Show error message
- Duplicate ISBN → Show error "ISBN đã tồn tại"
- Database errors → Show error "Lỗi hệ thống"

---

_(Tiếp tục với các luồng khác...)_

**Tài liệu này được cập nhật:** 23/11/2025  
**Tổng số trang:** Dự kiến 50+ trang khi hoàn thiện tất cả luồng

