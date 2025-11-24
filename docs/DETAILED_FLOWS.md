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

### 2.2. Luồng Sửa Sách (Update) ✅

#### Mô tả
Admin cập nhật thông tin sách đã có trong hệ thống.

#### Sequence Flow
```
┌───────┐   ┌─────────────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────┐
│ Admin │───│AdminBookController│───│  BookService │───│  Repository│───│ Database │
└───────┘   └─────────────────┘   └──────────────┘   └──────────┘   └──────────┘
   │               │                       │                │               │
   │ 1. GET /admin/books/edit/{id}         │                │               │
   │──────────────>│                       │                │               │
   │               │ getBookById(id)       │                │               │
   │               │──────────────────────>│                │               │
   │               │                       │ findById()     │               │
   │               │                       │───────────────>│               │
   │               │                       │                │ SELECT book   │
   │               │                       │                │──────────────>│
   │               │                       │<───────────────│ Book + Authors│
   │               │<──────────────────────│                │               │
   │               │                       │                │               │
   │<──────────────│ Show form with existing data           │               │
   │               │                       │                │               │
   │ 2. Modify & Submit                    │                │               │
   │──────────────>│                       │                │               │
   │               │ updateBook(id, dto)   │                │               │
   │               │──────────────────────>│                │               │
   │               │                       │ Update entity  │               │
   │               │                       │───────────────>│               │
   │               │                       │                │ UPDATE books  │
   │               │                       │                │──────────────>│
   │               │<──────────────────────│                │               │
   │<──────────────│ Redirect with success │                │               │
```

Similar to Create but loads existing data first.

---

### 2.3. Luồng Xóa Sách (Delete) ✅

#### Mô tả
Admin xóa sách khỏi hệ thống (có kiểm tra ràng buộc).

#### Sequence Flow
```
┌───────┐   ┌─────────────────┐   ┌──────────────┐   ┌──────────┐
│ Admin │───│AdminBookController│───│  BookService │───│  Database│
└───────┘   └─────────────────┘   └──────────────┘   └──────────┘
   │               │                       │                │
   │ 1. Click Delete (JavaScript confirm)  │                │
   │──────────────>│                       │                │
   │               │ DELETE /admin/books/delete/{id}        │
   │               │──────────┐            │                │
   │               │<─────────┘            │                │
   │               │                       │                │
   │               │ deleteBook(id)        │                │
   │               │──────────────────────>│                │
   │               │                       │                │
   │               │                       │ Check constraints│
   │               │                       │────────┐       │
   │               │                       │<───────┘       │
   │               │                       │                │
   │               │                       │ Has orders?    │
   │               │                       │───────────────>│
   │               │                       │<───────────────│
   │               │                       │                │
   │               │                       │ If has orders: │
   │               │                       │ - Soft delete  │
   │               │                       │   (set deleted=true)│
   │               │                       │ Else:          │
   │               │                       │ - Hard delete  │
   │               │                       │   (DELETE)     │
   │               │                       │───────────────>│
   │               │<──────────────────────│                │
   │<──────────────│ JSON response         │                │
   │               │ {success: true}       │                │
```

**JavaScript handles UI update:**
```javascript
function deleteBook(bookId) {
    Swal.fire({
        title: 'Xác nhận xóa?',
        text: "Bạn không thể hoàn tác!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            $.ajax({
                url: '/admin/books/delete/' + bookId,
                type: 'DELETE',
                success: function(response) {
                    Swal.fire('Đã xóa!', response.message, 'success');
                    $('#booksTable').DataTable().ajax.reload();
                },
                error: function(xhr) {
                    Swal.fire('Lỗi!', xhr.responseJSON.message, 'error');
                }
            });
        }
    });
}
```

---

## 3. LUỒNG USER MANAGEMENT (CRUD) ✅

### 3.1. Tổng Quan User Management

#### Đặc Điểm
- Admin quản lý tất cả users trong hệ thống
- Có phân quyền đặc biệt:
  - **admin_1** (Admin gốc): Có thể quản lý tất cả, kể cả admin khác
  - **Admin thường**: Chỉ quản lý user thông thường
  - **User**: Không có quyền truy cập admin panel

#### Các Chức Năng
1. ✅ List Users (Danh sách + tìm kiếm)
2. ✅ View User Details (Xem chi tiết)
3. ✅ Create User (Thêm user mới)
4. ✅ Update User (Cập nhật thông tin)
5. ✅ Delete User (Xóa user)
6. ✅ Toggle Status (Kích hoạt/vô hiệu hóa)

---

### 3.2. Luồng Xem Danh Sách Users (List) ✅

#### Mô tả
Admin xem danh sách tất cả users với tính năng tìm kiếm.

#### Sequence Flow

```
┌───────┐   ┌──────────────────┐   ┌─────────────┐   ┌───────────────┐   ┌──────────┐
│ Admin │───│AdminUserController│───│ UserService │───│ UserRepository│───│ Database │
└───────┘   └──────────────────┘   └─────────────┘   └───────────────┘   └──────────┘
   │               │                       │                   │                  │
   │ 1. GET /admin/users                   │                   │                  │
   │──────────────>│                       │                   │                  │
   │               │                       │                   │                  │
   │               │ searchUsers(keyword)  │                   │                  │
   │               │──────────────────────>│                   │                  │
   │               │                       │                   │                  │
   │               │                       │ findAll() or search()                │
   │               │                       │──────────────────>│                  │
   │               │                       │                   │ SELECT users     │
   │               │                       │                   │   JOIN roles     │
   │               │                       │                   │─────────────────>│
   │               │                       │                   │<─────────────────│
   │               │                       │<──────────────────│ List<User>       │
   │               │                       │                   │                  │
   │               │<──────────────────────│ Users with roles  │                  │
   │               │                       │                   │                  │
   │<──────────────│ Render list.html      │                   │                  │
   │               │ - Users data          │                   │                  │
   │               │ - Total count         │                   │                  │
   │               │ - DataTables          │                   │                  │
```

#### Controller Implementation

```java
@GetMapping
public String usersList(@RequestParam(required = false) String search, 
                       Model model) {
    // Search users by keyword
    List<User> users = userService.searchUsers(search);
    
    // Add to model
    model.addAttribute("users", users);
    model.addAttribute("totalUsers", users.size());
    model.addAttribute("search", search);
    
    return "admin/users/list";
}
```

#### Service Implementation

```java
@Override
public List<User> searchUsers(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
        // Return all users if no keyword
        return userRepository.findAll();
    }
    
    // Search by username, email, or full name
    return userRepository.searchByKeyword(keyword.trim());
}
```

#### Repository Query

```java
@Query("SELECT u FROM User u WHERE " +
       "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
List<User> searchByKeyword(@Param("keyword") String keyword);
```

#### Template (Thymeleaf)

```html
<!-- Search Form -->
<form method="get" action="/admin/users">
    <div class="input-group">
        <input type="text" 
               name="search" 
               class="form-control" 
               placeholder="Tìm theo username, email, họ tên..."
               th:value="${search}">
        <button type="submit" class="btn btn-primary">
            <i class="fas fa-search"></i> Tìm
        </button>
    </div>
</form>

<!-- Users Table -->
<table id="usersTable" class="table table-bordered">
    <thead>
        <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Họ Tên</th>
            <th>Email</th>
            <th>Vai Trò</th>
            <th>Trạng Thái</th>
            <th>Hành Động</th>
        </tr>
    </thead>
    <tbody>
        <tr th:each="user : ${users}">
            <td th:text="${user.userId}"></td>
            <td th:text="${user.username}"></td>
            <td th:text="${user.fullName}"></td>
            <td th:text="${user.email}"></td>
            <td>
                <span th:if="${user.role.roleName.name() == 'ADMIN'}" 
                      class="badge badge-danger">
                    Admin
                </span>
                <span th:if="${user.role.roleName.name() == 'USER'}" 
                      class="badge badge-primary">
                    User
                </span>
            </td>
            <td>
                <span th:if="${user.isActive}" 
                      class="badge badge-success">
                    Hoạt động
                </span>
                <span th:unless="${user.isActive}" 
                      class="badge badge-secondary">
                    Vô hiệu
                </span>
            </td>
            <td>
                <a th:href="@{/admin/users/view/{id}(id=${user.userId})}" 
                   class="btn btn-sm btn-info">
                    <i class="fas fa-eye"></i>
                </a>
                <a th:href="@{/admin/users/edit/{id}(id=${user.userId})}" 
                   class="btn btn-sm btn-warning">
                    <i class="fas fa-edit"></i>
                </a>
                <button onclick="deleteUser([[${user.userId}]])" 
                        class="btn btn-sm btn-danger"
                        th:unless="${user.userId == 'admin_1'}">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    </tbody>
</table>

<script>
$(document).ready(function() {
    $('#usersTable').DataTable({
        "pageLength": 10,
        "order": [[0, "asc"]],
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Vietnamese.json"
        }
    });
});
</script>
```

---

### 3.3. Luồng Thêm User Mới (Create) ✅

#### Mô tả
Admin tạo user mới với thông tin đầy đủ và gán role.

#### Sequence Flow

```
┌───────┐   ┌──────────────────┐   ┌─────────────┐   ┌───────────────┐   ┌──────────┐
│ Admin │───│AdminUserController│───│ UserService │───│ UserRepository│───│ Database │
└───────┘   └──────────────────┘   └─────────────┘   └───────────────┘   └──────────┘
   │               │                       │                   │                  │
   │ 1. GET /admin/users/add               │                   │                  │
   │──────────────>│                       │                   │                  │
   │               │                       │                   │                  │
   │               │ Load roles list       │                   │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │               │ Check admin permissions│                  │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │<──────────────│ Show form             │                   │                  │
   │               │ - Empty user object   │                   │                  │
   │               │ - All roles           │                   │                  │
   │               │ - canManageAdmin flag │                   │                  │
   │               │                       │                   │                  │
   │ 2. Fill form & Submit                 │                   │                  │
   │──────────────>│                       │                   │                  │
   │               │                       │                   │                  │
   │               │ POST /admin/users/save│                   │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │               │ Validate permissions  │                   │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │               │ Generate user_id      │                   │                  │
   │               │ (user_normal_XX)      │                   │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │               │ Hash password         │                   │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │               │ Set role              │                   │                  │
   │               │──────────┐            │                   │                  │
   │               │<─────────┘            │                   │                  │
   │               │                       │                   │                  │
   │               │ saveUser(user)        │                   │                  │
   │               │──────────────────────>│                   │                  │
   │               │                       │                   │                  │
   │               │                       │ save(user)        │                  │
   │               │                       │──────────────────>│                  │
   │               │                       │                   │ INSERT INTO users│
   │               │                       │                   │─────────────────>│
   │               │                       │                   │<─────────────────│
   │               │                       │<──────────────────│ User created     │
   │               │                       │                   │                  │
   │               │<──────────────────────│ User saved        │                  │
   │               │                       │                   │                  │
   │<──────────────│ Redirect /admin/users?success=true        │                  │
   │               │ Message: "Thêm người dùng thành công! ID: user_normal_XX"    │
```

#### Controller Implementation

```java
@GetMapping("/add")
public String showAddForm(Authentication authentication, Model model) {
    User adminUser = (User) authentication.getPrincipal();
    List<Role> roles = roleRepository.findAll();
    
    // Chỉ admin_1 có quyền tạo admin
    boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());
    
    model.addAttribute("adminUser", adminUser);
    model.addAttribute("roles", roles);
    model.addAttribute("user", new User());
    model.addAttribute("isEdit", false);
    model.addAttribute("canManageAdmin", canManageAdmin);
    
    return "admin/users/form";
}

@PostMapping("/save")
public String saveUser(@ModelAttribute User user,
                      @RequestParam(required = false) String roleId,
                      @RequestParam(required = false) String password,
                      Authentication authentication,
                      RedirectAttributes redirectAttributes) {
    try {
        User adminUser = (User) authentication.getPrincipal();
        boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());
        boolean isNew = user.getUserId() == null || user.getUserId().isEmpty();
        
        // Kiểm tra quyền tạo admin
        Role targetRole = null;
        if (roleId != null && !roleId.isEmpty()) {
            targetRole = roleRepository.findById(roleId).orElse(null);
            if (targetRole != null && 
                targetRole.getRoleName() == Role.RoleName.ADMIN && 
                !canManageAdmin) {
                redirectAttributes.addFlashAttribute("error", 
                    "Chỉ admin gốc (admin_1) mới có quyền tạo admin khác!");
                return "redirect:/admin/users";
            }
        }
        
        if (isNew) {
            // Generate new user ID
            String newUserId = generateNextUserId();
            user.setUserId(newUserId);
            user.setCreatedAt(LocalDateTime.now());
            
            // Set default password if not provided
            if (password == null || password.isEmpty()) {
                password = "123456"; // Default password
            }
            user.setPasswordHash(passwordEncoder.encode(password));
            
            // Set role
            if (targetRole != null) {
                user.setRole(targetRole);
            } else {
                // Set default USER role
                Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
                    .orElseThrow(() -> new RuntimeException("Default USER role not found"));
                user.setRole(userRole);
            }
            
            // Set default active status
            if (user.getIsActive() == null) {
                user.setIsActive(true);
            }
            
            redirectAttributes.addFlashAttribute("success", 
                "Thêm người dùng thành công! ID: " + newUserId);
        }
        
        userService.saveUser(user);
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
    }
    
    return "redirect:/admin/users";
}

/**
 * Sinh User ID tự động theo format "user_normal_XX"
 */
private String generateNextUserId() {
    long userCount = userService.getTotalUsersCount();
    int nextNumber = (int) userCount + 1;
    return String.format("user_normal_%02d", nextNumber);
}
```

#### Auto-Generate User ID Logic

**Pattern:** `user_normal_01`, `user_normal_02`, `user_normal_03`, ...

**Steps:**
1. Count total users in database
2. Increment by 1
3. Format with 2 digits: `%02d`
4. Result: `user_normal_XX`

**Example:**
- Current users: 15
- Next ID: `user_normal_16`

#### Template Form

```html
<form th:action="@{/admin/users/save}" method="post">
    <!-- Hidden field for edit mode -->
    <input type="hidden" th:field="*{userId}" th:if="${isEdit}" />
    
    <!-- Username (Only for new user) -->
    <div class="form-group" th:unless="${isEdit}">
        <label>Username <span class="text-danger">*</span></label>
        <input type="text" 
               class="form-control" 
               th:field="*{username}" 
               required />
        <small class="form-text text-muted">
            User ID sẽ tự động sinh: user_normal_XX
        </small>
    </div>
    
    <!-- Full Name -->
    <div class="form-group">
        <label>Họ Tên <span class="text-danger">*</span></label>
        <input type="text" 
               class="form-control" 
               th:field="*{fullName}" 
               required />
    </div>
    
    <!-- Email -->
    <div class="form-group">
        <label>Email <span class="text-danger">*</span></label>
        <input type="email" 
               class="form-control" 
               th:field="*{email}" 
               required />
    </div>
    
    <!-- Phone -->
    <div class="form-group">
        <label>Số Điện Thoại</label>
        <input type="tel" 
               class="form-control" 
               th:field="*{phone}" />
    </div>
    
    <!-- Password (Required for new, optional for edit) -->
    <div class="form-group">
        <label>
            Mật Khẩu 
            <span class="text-danger" th:unless="${isEdit}">*</span>
            <small th:if="${isEdit}" class="text-muted">(Để trống nếu không đổi)</small>
        </label>
        <input type="password" 
               class="form-control" 
               name="password"
               th:required="${!isEdit}" />
        <small class="form-text text-muted" th:unless="${isEdit}">
            Mặc định: 123456 nếu để trống
        </small>
    </div>
    
    <!-- Role -->
    <div class="form-group">
        <label>Vai Trò <span class="text-danger">*</span></label>
        <select class="form-control" name="roleId" required>
            <option value="">-- Chọn vai trò --</option>
            <option th:each="role : ${roles}" 
                    th:value="${role.roleId}"
                    th:text="${role.roleName}"
                    th:selected="${user.role != null && user.role.roleId == role.roleId}"
                    th:disabled="${role.roleName.name() == 'ADMIN' && !canManageAdmin}">
            </option>
        </select>
        <small class="form-text text-muted" th:unless="${canManageAdmin}">
            Chỉ admin gốc (admin_1) mới có thể gán quyền Admin
        </small>
    </div>
    
    <!-- Active Status -->
    <div class="form-group">
        <div class="form-check">
            <input type="checkbox" 
                   class="form-check-input" 
                   th:field="*{isActive}"
                   id="isActive" />
            <label class="form-check-label" for="isActive">
                Kích hoạt tài khoản
            </label>
        </div>
    </div>
    
    <!-- Submit Buttons -->
    <button type="submit" class="btn btn-primary">
        <i class="fas fa-save"></i> 
        <span th:text="${isEdit} ? 'Cập Nhật' : 'Thêm Mới'"></span>
    </button>
    <a href="/admin/users" class="btn btn-secondary">
        <i class="fas fa-times"></i> Hủy
    </a>
</form>
```

#### Validation & Security

**Permission Checks:**

| Action | Admin_1 | Admin Thường | User |
|--------|---------|--------------|------|
| Create User | ✅ | ✅ | ❌ |
| Create Admin | ✅ | ❌ | ❌ |
| Edit User | ✅ | ✅ | ❌ |
| Edit Admin | ✅ | ❌ | ❌ |
| Delete User | ✅ | ✅ | ❌ |
| Delete Admin | ✅ | ❌ | ❌ |
| Delete admin_1 | ❌ | ❌ | ❌ |
| Delete Self | ❌ | ❌ | ❌ |

**Business Rules:**
1. **admin_1** không thể bị xóa (super admin)
2. Admin không thể xóa chính mình
3. Admin không thể vô hiệu hóa chính mình
4. Chỉ admin_1 có thể quản lý admin khác
5. Password mặc định là "123456" nếu không nhập

---

### 3.4. Luồng Cập Nhật User (Update) ✅

#### Mô tả
Admin cập nhật thông tin user đã có.

#### Sequence Flow

```
┌───────┐   ┌──────────────────┐   ┌─────────────┐   ┌──────────┐
│ Admin │───│AdminUserController│───│ UserService │───│ Database │
└───────┘   └──────────────────┘   └─────────────┘   └──────────┘
   │               │                       │                  │
   │ 1. GET /admin/users/edit/{id}         │                  │
   │──────────────>│                       │                  │
   │               │ getUserById(id)       │                  │
   │               │──────────────────────>│                  │
   │               │<──────────────────────│ User + Role      │
   │               │                       │                  │
   │               │ Check permissions     │                  │
   │               │──────────┐            │                  │
   │               │<─────────┘            │                  │
   │               │                       │                  │
   │<──────────────│ Show form with data   │                  │
   │               │                       │                  │
   │ 2. Modify & Submit                    │                  │
   │──────────────>│                       │                  │
   │               │ POST /admin/users/save│                  │
   │               │                       │                  │
   │               │ Check permissions     │                  │
   │               │ Update fields         │                  │
   │               │ Hash password (if changed)               │
   │               │ saveUser(user)        │                  │
   │               │──────────────────────>│                  │
   │               │                       │ UPDATE users     │
   │               │                       │─────────────────>│
   │               │<──────────────────────│                  │
   │<──────────────│ Redirect with success │                  │
```

#### Controller Implementation

```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
    User adminUser = (User) authentication.getPrincipal();
    User user = userService.getUserById(id).orElse(null);
    
    if (user == null) {
        redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
        return "redirect:/admin/users";
    }
    
    // Kiểm tra quyền sửa admin
    boolean isTargetUserAdmin = user.getRole() != null && 
                               user.getRole().getRoleName() == Role.RoleName.ADMIN;
    boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());
    
    if (isTargetUserAdmin && !canManageAdmin) {
        redirectAttributes.addFlashAttribute("error", 
            "Chỉ admin gốc mới có quyền chỉnh sửa admin khác!");
        return "redirect:/admin/users";
    }
    
    List<Role> roles = roleRepository.findAll();
    
    model.addAttribute("adminUser", adminUser);
    model.addAttribute("roles", roles);
    model.addAttribute("user", user);
    model.addAttribute("isEdit", true);
    model.addAttribute("canManageAdmin", canManageAdmin);
    
    return "admin/users/form";
}

// Update logic in saveUser() method (same as create)
if (!isNew) {
    // Update existing user
    User existingUser = userService.getUserById(user.getUserId())
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Check permission
    boolean isTargetUserAdmin = existingUser.getRole() != null &&
                               existingUser.getRole().getRoleName() == Role.RoleName.ADMIN;
    if (isTargetUserAdmin && !canManageAdmin) {
        redirectAttributes.addFlashAttribute("error", 
            "Chỉ admin gốc mới có quyền sửa admin khác!");
        return "redirect:/admin/users";
    }
    
    // Update fields
    existingUser.setFullName(user.getFullName());
    existingUser.setEmail(user.getEmail());
    existingUser.setPhone(user.getPhone());
    existingUser.setAvatarUrl(user.getAvatarUrl());
    existingUser.setIsActive(user.getIsActive());
    existingUser.setUpdatedAt(LocalDateTime.now());
    
    // Update password if provided
    if (password != null && !password.isEmpty()) {
        existingUser.setPasswordHash(passwordEncoder.encode(password));
    }
    
    // Update role if provided
    if (targetRole != null) {
        existingUser.setRole(targetRole);
    }
    
    user = existingUser;
    redirectAttributes.addFlashAttribute("success", "Cập nhật người dùng thành công!");
}
```

---

### 3.5. Luồng Xóa User (Delete) ✅

#### Mô tả
Admin xóa user khỏi hệ thống với các ràng buộc bảo mật.

#### Sequence Flow

```
┌───────┐   ┌──────────────────┐   ┌─────────────┐   ┌──────────┐
│ Admin │───│AdminUserController│───│ UserService │───│ Database │
└───────┘   └──────────────────┘   └─────────────┘   └──────────┘
   │               │                       │                  │
   │ 1. Click Delete (SweetAlert confirm)  │                  │
   │──────────────>│                       │                  │
   │               │ POST /admin/users/delete/{id}            │
   │               │                       │                  │
   │               │ Security checks:      │                  │
   │               │ - Not admin_1?        │                  │
   │               │ - Not self?           │                  │
   │               │ - Has permission?     │                  │
   │               │──────────┐            │                  │
   │               │<─────────┘            │                  │
   │               │                       │                  │
   │               │ deleteUser(id)        │                  │
   │               │──────────────────────>│                  │
   │               │                       │                  │
   │               │                       │ Check constraints│
   │               │                       │ - Has orders?    │
   │               │                       │ - Has cart?      │
   │               │                       │──────────┐       │
   │               │                       │<─────────┘       │
   │               │                       │                  │
   │               │                       │ DELETE FROM users│
   │               │                       │─────────────────>│
   │               │                       │<─────────────────│
   │               │<──────────────────────│                  │
   │<──────────────│ Redirect with success │                  │
   │               │ "Xóa người dùng thành công!"             │
```

#### Controller Implementation

```java
@PostMapping("/delete/{id}")
public String deleteUser(@PathVariable String id,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
    try {
        User adminUser = (User) authentication.getPrincipal();
        boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());
        
        // Security Check 1: Prevent deleting admin_1
        if ("admin_1".equals(id)) {
            redirectAttributes.addFlashAttribute("error", 
                "Không thể xóa tài khoản admin gốc (admin_1)!");
            return "redirect:/admin/users";
        }
        
        // Security Check 2: Prevent self-deletion
        if (adminUser.getUserId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", 
                "Bạn không thể xóa tài khoản của chính mình!");
            return "redirect:/admin/users";
        }
        
        User user = userService.getUserById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Security Check 3: Check permission to delete admin
        boolean isTargetUserAdmin = user.getRole() != null &&
                                   user.getRole().getRoleName() == Role.RoleName.ADMIN;
        if (isTargetUserAdmin && !canManageAdmin) {
            redirectAttributes.addFlashAttribute("error", 
                "Chỉ admin gốc mới có quyền xóa admin khác!");
            return "redirect:/admin/users";
        }
        
        // Delete user
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
    }
    
    return "redirect:/admin/users";
}
```

#### JavaScript (SweetAlert2)

```javascript
function deleteUser(userId, username) {
    Swal.fire({
        title: 'Xác nhận xóa?',
        html: `Bạn có chắc muốn xóa user <strong>${username}</strong>?<br>` +
              `<span class="text-danger">Hành động này không thể hoàn tác!</span>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: '<i class="fas fa-trash"></i> Xóa',
        cancelButtonText: '<i class="fas fa-times"></i> Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            // Submit delete form
            $.ajax({
                url: `/admin/users/delete/${userId}`,
                type: 'POST',
                data: {
                    _csrf: $('meta[name="_csrf"]').attr('content')
                },
                success: function(response) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Đã xóa!',
                        text: 'User đã được xóa thành công.',
                        timer: 1500
                    }).then(() => {
                        window.location.reload();
                    });
                },
                error: function(xhr) {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi!',
                        text: xhr.responseJSON?.message || 'Không thể xóa user.'
                    });
                }
            });
        }
    });
}
```

#### Security Rules Summary

```
┌──────────────────────────────────────────────────────────┐
│               DELETE USER - SECURITY MATRIX              │
├──────────────────────────────────────────────────────────┤
│ Target User     │ Admin_1 │ Admin Thường │ Result      │
├─────────────────┼─────────┼──────────────┼─────────────┤
│ admin_1         │    ❌   │      ❌      │ BLOCKED     │
│ Self            │    ❌   │      ❌      │ BLOCKED     │
│ Other Admin     │    ✅   │      ❌      │ CHECK PERM  │
│ Regular User    │    ✅   │      ✅      │ ALLOWED     │
└─────────────────┴─────────┴──────────────┴─────────────┘
```

---

### 3.6. Luồng Toggle Status (Kích hoạt/Vô hiệu hóa) ✅

#### Mô tả
Admin bật/tắt trạng thái active của user.

#### Sequence Flow

```
┌───────┐   ┌──────────────────┐   ┌─────────────┐   ┌──────────┐
│ Admin │───│AdminUserController│───│ UserService │───│ Database │
└───────┘   └──────────────────┘   └─────────────┘   └──────────┘
   │               │                       │                  │
   │ 1. Click Toggle Status Button         │                  │
   │──────────────>│                       │                  │
   │               │ POST /admin/users/toggle-status/{id}     │
   │               │                       │                  │
   │               │ Check: Not self?      │                  │
   │               │──────────┐            │                  │
   │               │<─────────┘            │                  │
   │               │                       │                  │
   │               │ getUserById(id)       │                  │
   │               │──────────────────────>│                  │
   │               │<──────────────────────│ User             │
   │               │                       │                  │
   │               │ Toggle isActive       │                  │
   │               │──────────┐            │                  │
   │               │<─────────┘            │                  │
   │               │                       │                  │
   │               │ saveUser(user)        │                  │
   │               │──────────────────────>│                  │
   │               │                       │ UPDATE is_active │
   │               │                       │─────────────────>│
   │               │<──────────────────────│                  │
   │<──────────────│ Redirect with success │                  │
   │               │ "Đã cập nhật trạng thái!"                │
```

#### Controller Implementation

```java
@PostMapping("/toggle-status/{id}")
public String toggleUserStatus(@PathVariable String id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
    try {
        User adminUser = (User) authentication.getPrincipal();
        
        // Prevent admin from disabling themselves
        if (adminUser.getUserId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", 
                "Bạn không thể thay đổi trạng thái tài khoản của chính mình!");
            return "redirect:/admin/users";
        }
        
        // Get user and toggle status
        User user = userService.getUserById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(!user.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());
        
        userService.saveUser(user);
        
        String status = user.getIsActive() ? "kích hoạt" : "vô hiệu hóa";
        redirectAttributes.addFlashAttribute("success", 
            "Đã " + status + " tài khoản thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
    }
    
    return "redirect:/admin/users";
}
```

#### Template Button

```html
<!-- Toggle Status Button -->
<button onclick="toggleUserStatus('[[${user.userId}]]', [[${user.isActive}]])"
        class="btn btn-sm"
        th:classappend="${user.isActive} ? 'btn-success' : 'btn-secondary'"
        th:unless="${#authentication.principal.userId == user.userId}">
    <i class="fas" 
       th:classappend="${user.isActive} ? 'fa-check-circle' : 'fa-ban'"></i>
    <span th:text="${user.isActive} ? 'Hoạt động' : 'Vô hiệu'"></span>
</button>
```

#### JavaScript Function

```javascript
function toggleUserStatus(userId, currentStatus) {
    let action = currentStatus ? 'vô hiệu hóa' : 'kích hoạt';
    let icon = currentStatus ? 'warning' : 'info';
    
    Swal.fire({
        title: `${action.charAt(0).toUpperCase() + action.slice(1)} tài khoản?`,
        text: `Bạn có chắc muốn ${action} user này?`,
        icon: icon,
        showCancelButton: true,
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            $.ajax({
                url: `/admin/users/toggle-status/${userId}`,
                type: 'POST',
                data: {
                    _csrf: $('meta[name="_csrf"]').attr('content')
                },
                success: function() {
                    Swal.fire({
                        icon: 'success',
                        title: 'Thành công!',
                        text: `Đã ${action} tài khoản.`,
                        timer: 1500
                    }).then(() => {
                        window.location.reload();
                    });
                },
                error: function(xhr) {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi!',
                        text: xhr.responseJSON?.message || 'Có lỗi xảy ra.'
                    });
                }
            });
        }
    });
}
```

---

## 4. HƯỚNG DẪN DEBUG & ENDPOINTS

### 4.1. Cấu Hình Debug Trong IntelliJ IDEA

#### Bước 1: Cấu Hình Run/Debug Configuration

```
1. Click "Add Configuration..." (góc trên phải)
2. Click "+" → "Spring Boot"
3. Cấu hình:
   - Name: Ebook Store Debug
   - Main class: stu.datn.ebook_store.EbookStoreApplication
   - VM options: -Dspring.profiles.active=dev
   - Environment variables (optional):
     * DEBUG=true
     * LOG_LEVEL=DEBUG
4. Click "Apply" → "OK"
```

#### Bước 2: Đặt Breakpoints

**Các vị trí nên đặt breakpoint:**

1. **Controller Layer** (Kiểm tra request):
```java
@PostMapping("/save")
public String saveUser(@ModelAttribute User user,  // ← BREAKPOINT HERE
                      @RequestParam String roleId,
                      RedirectAttributes redirectAttributes) {
    // Debug: Xem dữ liệu từ form
    System.out.println("User data: " + user);  // ← OR HERE
    // ...
}
```

2. **Service Layer** (Kiểm tra business logic):
```java
@Override
public User saveUser(User user) {  // ← BREAKPOINT HERE
    // Debug: Xem user trước khi save
    System.out.println("Saving user: " + user);  // ← OR HERE
    return userRepository.save(user);
}
```

3. **Repository Layer** (Kiểm tra query):
```java
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsername(@Param("username") String username);  // ← BREAKPOINT IN CALLER
```

#### Bước 3: Chạy Debug Mode

```
1. Click icon "Debug" (hình con bọ) hoặc Shift+F9
2. Ứng dụng sẽ start và dừng tại breakpoints
3. Sử dụng các nút:
   - F8: Step Over (bước tiếp theo)
   - F7: Step Into (vào trong method)
   - F9: Resume (chạy tiếp đến breakpoint tiếp theo)
   - Alt+F8: Evaluate Expression (chạy code tạm thời)
```

---

### 4.2. Logging Strategy

#### application.properties

```properties
# Logging Configuration
logging.level.root=INFO
logging.level.stu.datn.ebook_store=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Log file
logging.file.name=logs/ebook-store.log
logging.file.max-size=10MB
logging.file.max-history=30

# Pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

#### Logging trong Code

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);
    
    @PostMapping("/save")
    public String saveUser(@ModelAttribute User user) {
        logger.debug("Received user data: {}", user);
        
        try {
            userService.saveUser(user);
            logger.info("User saved successfully: {}", user.getUserId());
            
        } catch (Exception e) {
            logger.error("Error saving user: {}", e.getMessage(), e);
            throw e;
        }
        
        return "redirect:/admin/users";
    }
}
```

**Log Levels:**
- `TRACE`: Chi tiết nhất (SQL params)
- `DEBUG`: Thông tin debug (flow control)
- `INFO`: Thông tin quan trọng (user actions)
- `WARN`: Cảnh báo (deprecated methods)
- `ERROR`: Lỗi (exceptions)

---

### 4.3. Debug Endpoints Pattern

#### Theo Convention của Dự Án

**Admin Endpoints:**
```
GET  /admin/users              → List users
GET  /admin/users/add          → Show add form
POST /admin/users/save         → Create/Update user
GET  /admin/users/edit/{id}    → Show edit form
POST /admin/users/delete/{id}  → Delete user
GET  /admin/users/view/{id}    → View user details
POST /admin/users/toggle-status/{id} → Toggle active status
```

**Pattern:**
- GET cho hiển thị (list, form, view)
- POST cho hành động (save, delete, toggle)
- RESTful path với resource name số nhiều
- ID trong path parameter: `/{id}`

**Ví dụ Apply cho Books:**
```
GET  /admin/books              → List books
GET  /admin/books/add          → Show add form
POST /admin/books/add          → Create book
GET  /admin/books/edit/{id}    → Show edit form
POST /admin/books/edit/{id}    → Update book
DELETE /admin/books/delete/{id} → Delete book (AJAX)
GET  /admin/books/view/{id}    → View book details
POST /admin/books/upload-cover → Upload cover image
```

---

### 4.4. Testing Endpoints với Postman/cURL

#### Example: Test Create User API

**cURL:**
```bash
curl -X POST http://localhost:8080/admin/users/save \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Cookie: JSESSIONID=your_session_id" \
  -d "username=testuser" \
  -d "fullName=Test User" \
  -d "email=test@example.com" \
  -d "password=123456" \
  -d "roleId=role_user_id" \
  -d "isActive=true"
```

**Postman:**
```
1. Method: POST
2. URL: http://localhost:8080/admin/users/save
3. Headers:
   - Content-Type: application/x-www-form-urlencoded
   - Cookie: JSESSIONID=... (get from browser)
4. Body (x-www-form-urlencoded):
   - username: testuser
   - fullName: Test User
   - email: test@example.com
   - password: 123456
   - roleId: <actual_role_id>
   - isActive: true
5. Send
```

#### Get Session ID from Browser

```javascript
// Run in browser console
document.cookie.split(';').find(c => c.trim().startsWith('JSESSIONID'))
```

---

### 4.5. Common Debug Scenarios

#### Scenario 1: Form Data Not Binding

**Problem:**
```java
@PostMapping("/save")
public String saveUser(@ModelAttribute User user) {
    System.out.println(user); // null or empty fields
}
```

**Debug Steps:**
1. Check form field names match entity fields
2. Add `th:field="*{fieldName}"` in Thymeleaf
3. Check `@ModelAttribute` name matches `th:object="${user}"`
4. Enable debug logging:
```properties
logging.level.org.springframework.web=DEBUG
```

#### Scenario 2: 403 Forbidden (CSRF)

**Problem:**
```
POST request returns 403 Forbidden
```

**Solution:**
```html
<!-- Add in form -->
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />

<!-- Or use th:action (auto adds CSRF) -->
<form th:action="@{/admin/users/save}" method="post">
```

#### Scenario 3: Role Permission Not Working

**Debug:**
```java
@PostMapping("/save")
public String saveUser(Authentication authentication) {
    User adminUser = (User) authentication.getPrincipal();
    System.out.println("Admin ID: " + adminUser.getUserId());  // admin_1?
    System.out.println("Admin Role: " + adminUser.getRole());  // ADMIN?
    
    boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());
    System.out.println("Can manage admin: " + canManageAdmin);  // true/false?
}
```

#### Scenario 4: Password Not Hashing

**Debug:**
```java
String password = "123456";
String hashed = passwordEncoder.encode(password);
System.out.println("Plain: " + password);
System.out.println("Hashed: " + hashed);  // Should start with $2a$
System.out.println("Matches: " + passwordEncoder.matches(password, hashed));  // true?
```

---

### 4.6. Browser Debug Tools

#### Chrome DevTools

**Network Tab:**
```
1. Open DevTools (F12)
2. Go to Network tab
3. Submit form
4. Click on request name
5. Check:
   - Request Headers (Cookie, CSRF token)
   - Form Data (all fields sent?)
   - Response (redirect? error?)
```

**Console Tab:**
```javascript
// Check CSRF token
console.log($('meta[name="_csrf"]').attr('content'));

// Check form data before submit
$('form').on('submit', function(e) {
    console.log($(this).serialize());
});

// Monitor AJAX requests
$(document).ajaxSend(function(event, xhr, settings) {
    console.log('AJAX Request:', settings.url, settings.data);
});
```

---

### 4.7. SQL Query Debugging

#### Show SQL in Console

```properties
# application.properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Output Example:**
```sql
Hibernate: 
    select
        user0_.user_id as user_id1_17_,
        user0_.email as email2_17_,
        user0_.full_name as full_nam3_17_,
        user0_.role_id as role_id8_17_ 
    from
        users user0_ 
    where
        user0_.username=?
TRACE 53668 --- [nio-8080-exec-1] o.h.type.descriptor.sql.BasicBinder: binding parameter [1] as [VARCHAR] - [admin]
```

#### Monitor Queries in Real-time

**MySQL Workbench:**
```sql
-- Show running queries
SHOW FULL PROCESSLIST;

-- Enable query log
SET GLOBAL general_log = 'ON';
SET GLOBAL log_output = 'TABLE';

-- View logs
SELECT * FROM mysql.general_log 
WHERE command_type = 'Query' 
ORDER BY event_time DESC 
LIMIT 100;
```

---

**Tài liệu này được cập nhật:** 23/11/2025  
**Tổng số trang:** 50+ trang với tất cả luồng

