# Luồng Xử Lý Hoàn Chỉnh - Complete Request Flows

**Ngày tạo:** 24/11/2025  
**Mục đích:** Giải thích chi tiết luồng xử lý từ đầu đến cuối cho các chức năng chính

---

## 📋 Mục Lục

1. [User Registration Flow](#1-user-registration-flow)
2. [User Login Flow](#2-user-login-flow)
3. [Admin CRUD User Flow](#3-admin-crud-user-flow)
4. [Book Management Flow](#4-book-management-flow)
5. [File Upload Flow](#5-file-upload-flow)
6. [Security Authorization Flow](#6-security-authorization-flow)

---

## 1. User Registration Flow

### 📝 Tổng Quan

**Mục đích:** User đăng ký tài khoản mới

**Endpoint:** `POST /auth/register`

**Files liên quan:**
- Controller: `AuthController.java`
- Service: `UserServiceImpl.java`
- Repository: `UserRepository.java`, `RoleRepository.java`
- Entity: `User.java`, `Role.java`
- DTO: `RegisterDto.java`
- View: `auth/register.html`

---

### 🔄 Luồng Chi Tiết

#### Step 1: User Fill Form

**View:** `templates/auth/register.html`

```html
<form method="post" th:action="@{/auth/register}" th:object="${registerDto}">
    <!-- CSRF token tự động thêm bởi Thymeleaf -->
    
    <input type="text" th:field="*{username}" required />
    <input type="email" th:field="*{email}" required />
    <input type="password" th:field="*{password}" required />
    <input type="password" th:field="*{confirmPassword}" required />
    
    <button type="submit">Đăng ký</button>
</form>
```

**User actions:**
1. Nhập username: "john_doe"
2. Nhập email: "john@example.com"
3. Nhập password: "mypassword123"
4. Nhập confirm password: "mypassword123"
5. Click "Đăng ký"

---

#### Step 2: Browser Submit Form

**HTTP Request:**
```http
POST /auth/register HTTP/1.1
Host: localhost:2706
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID=xxx; XSRF-TOKEN=csrf-token

username=john_doe&
email=john@example.com&
password=mypassword123&
confirmPassword=mypassword123&
_csrf=csrf-token
```

**Components:**
- Method: POST
- URL: /auth/register
- Headers: Content-Type, Cookie
- Body: Form data (URL-encoded)
- CSRF token: Auto added by Thymeleaf

---

#### Step 3: Spring DispatcherServlet

**Spring internals:**

```
1. DispatcherServlet receives request
   ↓
2. Find HandlerMapping
   - Match URL pattern: /auth/register
   - Match HTTP method: POST
   - Found: AuthController.processRegister()
   ↓
3. Apply Interceptors (if any)
   ↓
4. Invoke Handler (Controller method)
```

---

#### Step 4: AuthController.processRegister()

**File:** `controller/AuthController.java`

```java
@PostMapping("/auth/register")
public String processRegister(@ModelAttribute("registerDto") RegisterDto registerDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
    // 1. Check validation errors
    if (bindingResult.hasErrors()) {
        return "auth/register";
    }

    try {
        // 2. Call service
        userService.registerUser(registerDto);
        
        // 3. Success message
        redirectAttributes.addFlashAttribute("successMessage", 
            "Đăng ký thành công! Vui lòng đăng nhập.");
        
        return "redirect:/auth/login";
        
    } catch (Exception e) {
        // 4. Error handling
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/auth/register";
    }
}
```

**Processing steps:**
1. **@ModelAttribute** - Spring tự động bind form data → RegisterDto
2. **BindingResult** - Check validation errors
3. Call `userService.registerUser()`
4. Handle success/error
5. Return redirect URL

---

#### Step 5: UserServiceImpl.registerUser()

**File:** `service/impl/UserServiceImpl.java`

```java
@Override
@Transactional
public void registerUser(RegisterDto registerDto) throws Exception {
    // 1. Validate username
    if (userRepository.findByUsername(registerDto.getUsername()).isPresent()) {
        throw new Exception("Username đã tồn tại");
    }

    // 2. Validate email
    if (userRepository.findByEmail(registerDto.getEmail()).isPresent()) {
        throw new Exception("Email đã được sử dụng");
    }

    // 3. Find USER role
    Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
            .orElseThrow(() -> new Exception("Không tìm thấy Role 'USER'"));

    // 4. Generate user ID
    String newUserId = generateNextUserId();

    // 5. Create User entity
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

    // 6. Save to database
    userRepository.save(user);
}
```

**Business logic:**
1. **Validation** - Check username và email chưa tồn tại
2. **Find Role** - Lấy Role "USER" từ DB
3. **Generate ID** - Auto-generate user_normal_XX
4. **Hash Password** - BCrypt encryption
5. **Set Defaults** - isActive=true, isVerified=false
6. **Save** - Persist to database

---

#### Step 6: UserRepository.save()

**File:** `repository/UserRepository.java`

```java
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    // save() inherited from JpaRepository
}
```

**JPA/Hibernate processing:**

```
1. EntityManager.persist(user)
   ↓
2. Hibernate generates SQL
   INSERT INTO users (
       user_id, username, email, password_hash, 
       role_id, is_active, is_verified, 
       created_at, updated_at
   ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
   ↓
3. Bind parameters
   - user_id: "user_normal_06"
   - username: "john_doe"
   - email: "john@example.com"
   - password_hash: "$2a$10$xxxxx..." (BCrypt hash)
   - role_id: "role_user_01"
   - is_active: true
   - is_verified: false
   - created_at: 2025-11-24 14:30:00
   - updated_at: 2025-11-24 14:30:00
   ↓
4. Execute query
   ↓
5. Commit transaction (@Transactional)
   ↓
6. Return User entity
```

---

#### Step 7: Success Response

**Controller:**
```java
redirectAttributes.addFlashAttribute("successMessage", 
    "Đăng ký thành công! Vui lòng đăng nhập.");
return "redirect:/auth/login";
```

**Spring processing:**
1. Store flash message in session (temporary)
2. Send redirect response
3. Browser follows redirect
4. Load /auth/login page
5. Display flash message
6. Remove flash message from session

**HTTP Response:**
```http
HTTP/1.1 302 Found
Location: /auth/login
Set-Cookie: JSESSIONID=xxx
```

---

### 📊 Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER                                                         │
│    - Fill registration form                                     │
│    - Click "Đăng ký"                                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. BROWSER                                                      │
│    - POST /auth/register                                        │
│    - Form data + CSRF token                                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. SPRING SECURITY FILTER CHAIN                                 │
│    - CsrfFilter: Validate CSRF token                            │
│    - SessionManagementFilter                                    │
│    - SecurityContextPersistenceFilter                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. DISPATCHER SERVLET                                           │
│    - Find handler mapping                                       │
│    - Route to AuthController.processRegister()                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. AUTH CONTROLLER                                              │
│    - @ModelAttribute bind form → RegisterDto                    │
│    - Validate input                                             │
│    - Call userService.registerUser()                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. USER SERVICE (Business Logic)                                │
│    - Check username exists                                      │
│    - Check email exists                                         │
│    - Find USER role                                             │
│    - Generate user_id                                           │
│    - Hash password (BCrypt)                                     │
│    - Create User entity                                         │
│    - Call userRepository.save()                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. USER REPOSITORY (Data Access)                                │
│    - JPA EntityManager.persist()                                │
│    - Hibernate generates SQL                                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. DATABASE (MySQL)                                             │
│    - Execute INSERT INTO users                                  │
│    - Commit transaction                                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 9. CONTROLLER RESPONSE                                          │
│    - Add flash message: "Đăng ký thành công!"                   │
│    - Redirect: /auth/login                                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 10. BROWSER                                                     │
│     - Follow redirect                                           │
│     - Load login page                                           │
│     - Display success message                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

### 🛡 Error Handling Flow

**Scenario: Username đã tồn tại**

```
1. User submits form với username "john_doe" (đã tồn tại)
   ↓
2. Controller calls userService.registerUser()
   ↓
3. Service checks: userRepository.findByUsername("john_doe")
   ↓ Returns Optional<User> with existing user
   
4. Service throws: new Exception("Username đã tồn tại")
   ↓
5. Controller catches exception
   try-catch block
   ↓
6. Add error flash message
   redirectAttributes.addFlashAttribute("errorMessage", "Username đã tồn tại")
   ↓
7. Redirect back: redirect:/auth/register
   ↓
8. Form shows error message
   User can retry với username khác
```

---

### 🔒 Security Features

1. **CSRF Protection**
   - Token in form (hidden input)
   - CsrfFilter validates token
   - Prevent CSRF attacks

2. **Password Encryption**
   - BCrypt hashing
   - Salt generated automatically
   - One-way encryption

3. **Input Validation**
   - Check username không trùng
   - Check email không trùng
   - Email format validation (client + server)

4. **SQL Injection Prevention**
   - JPA parameterized queries
   - Hibernate automatic escaping

---

## 2. User Login Flow

### 📝 Tổng Quan

**Mục đích:** User đăng nhập vào hệ thống

**Endpoint:** `POST /auth/login`

**Files liên quan:**
- Controller: `AuthController.java`
- Service: `UserServiceImpl.java`
- Repository: `UserRepository.java`
- Config: `SecurityConfig.java`
- Entity: `User.java`, `Role.java`
- DTO: `LoginDto.java`
- View: `auth/login.html`

---

### 🔄 Luồng Chi Tiết

#### Step 1: User Access Login Page

**Request:** `GET /auth/login`

**Controller:**
```java
@GetMapping("/auth/login")
public String showLoginPage(Model model) {
    model.addAttribute("loginDto", new LoginDto());
    return "auth/login";
}
```

**View:** `templates/auth/login.html`
```html
<form method="post" th:action="@{/auth/login}" th:object="${loginDto}">
    <input type="text" th:field="*{username}" placeholder="Username" required />
    <input type="password" th:field="*{password}" placeholder="Password" required />
    <button type="submit">Đăng nhập</button>
</form>
```

---

#### Step 2: User Submit Login Form

**HTTP Request:**
```http
POST /auth/login HTTP/1.1
Host: localhost:2706
Content-Type: application/x-www-form-urlencoded

username=john_doe&password=mypassword123&_csrf=token
```

---

#### Step 3: AuthController.processLogin()

```java
@PostMapping("/auth/login")
public String processLogin(@ModelAttribute("loginDto") LoginDto loginDto,
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
                user,  // Principal = User object
                null,  // Credentials
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );

        // 3. Set to SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. Save to session
        session.setAttribute("SPRING_SECURITY_CONTEXT", 
            SecurityContextHolder.getContext());
        session.setAttribute("loggedInUser", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole().getRoleName().name());

        // 5. Update last login
        userService.updateLastLogin(user.getUserId());

        // 6. Redirect based on role
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

**Key steps:**
1. **Authenticate** - Verify credentials
2. **Create Authentication** - Spring Security object
3. **Set SecurityContext** - Make user authenticated
4. **Store in Session** - Persist across requests
5. **Update last_login** - Track activity
6. **Redirect** - Based on role (ADMIN → /admin/dashboard, USER → /user/index)

---

#### Step 4: UserService.authenticateUser()

```java
@Override
public User authenticateUser(String username, String password) throws Exception {
    // 1. Find user by username
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("Tên đăng nhập hoặc mật khẩu không đúng"));

    // 2. Check account active
    if (!user.getIsActive()) {
        throw new Exception("Tài khoản đã bị khóa");
    }

    // 3. Verify password
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        throw new Exception("Tên đăng nhập hoặc mật khẩu không đúng");
    }

    // 4. Return user
    return user;
}
```

**Security checks:**
1. User exists
2. Account active (not banned)
3. Password correct (BCrypt.matches)

---

#### Step 5: SecurityContext & Session

**Spring Security internals:**

```
1. Create Authentication object
   - Principal: User object
   - Authorities: ["ROLE_ADMIN"] hoặc ["ROLE_USER"]
   ↓
2. Set to SecurityContextHolder
   - Thread-local storage
   - Available for current request
   ↓
3. Save to HttpSession
   - Key: "SPRING_SECURITY_CONTEXT"
   - Persisted across requests
   ↓
4. Filter chain uses SecurityContext
   - Check authenticated: SecurityContextHolder.getContext().getAuthentication()
   - Check roles: authentication.getAuthorities()
```

---

#### Step 6: Subsequent Requests

**Example:** User accesses `/admin/users`

```
1. Browser sends request với JSESSIONID cookie
   GET /admin/users
   Cookie: JSESSIONID=abc123
   ↓
2. SecurityContextPersistenceFilter
   - Load SecurityContext from session
   - Set to SecurityContextHolder
   ↓
3. SecurityConfig authorization rules
   .requestMatchers("/admin/**").hasRole("ADMIN")
   ↓
4. Check Authentication
   - Get from SecurityContextHolder
   - Check authorities contains "ROLE_ADMIN"
   ↓
5. If authorized:
   - Allow access to controller
   
   If not authorized:
   - accessDeniedPage: /auth/login?error=access_denied
```

---

### 📊 Login Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│ USER                                                         │
│  - Enter username & password                                │
│  - Click "Đăng nhập"                                         │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ BROWSER                                                      │
│  - POST /auth/login                                          │
│  - Form data: username, password, _csrf                      │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ SPRING SECURITY FILTER CHAIN                                 │
│  - CsrfFilter: Validate token                                │
│  - SecurityContextPersistenceFilter                          │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ AUTH CONTROLLER                                              │
│  - Bind form data → LoginDto                                 │
│  - Call userService.authenticateUser()                       │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ USER SERVICE                                                 │
│  1. Find user by username                                    │
│  2. Check isActive                                           │
│  3. Verify password (BCrypt)                                 │
│  4. Return User entity                                       │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ AUTH CONTROLLER (continued)                                  │
│  1. Create Authentication object                             │
│  2. Set to SecurityContextHolder                             │
│  3. Save to HttpSession                                      │
│  4. Store user info in session                               │
│  5. Update last_login                                        │
│  6. Redirect based on role                                   │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ REDIRECT RESPONSE                                            │
│  - ADMIN → /admin/dashboard                                  │
│  - USER → /user/index                                        │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ BROWSER                                                      │
│  - Follow redirect                                           │
│  - Load dashboard page                                       │
│  - User now authenticated                                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Admin CRUD User Flow

### 📝 Admin View Users List

**Endpoint:** `GET /admin/users`

**Flow:**
```
1. Admin clicks "Quản lý User" in sidebar
   GET /admin/users
   ↓
2. SecurityConfig checks: hasRole("ADMIN")
   ↓
3. AdminUserController.usersList()
   - Call userService.searchUsers(null)
   - Add users to Model
   - Return "admin/users/list"
   ↓
4. Thymeleaf renders admin/users/list.html
   - Loop through users với th:each
   - Display user info in table
```

---

### 📝 Admin Add New User

**Endpoints:**
- `GET /admin/users/add` - Show form
- `POST /admin/users/save` - Submit form

**Flow:**

```
GET /admin/users/add
  ↓
AdminUserController.showAddForm()
  ↓
Load:
  - roles from roleRepository
  - empty User object
  - canManageAdmin flag
  ↓
Return "admin/users/form"
  ↓
User fills form:
  - fullName
  - email
  - phone
  - role
  - password (optional, default "123456")
  ↓
POST /admin/users/save
  ↓
AdminUserController.saveUser()
  ↓
Check permissions:
  - Only user_admin_01 can create ADMIN users
  ↓
Generate user_id:
  - user_normal_XX
  ↓
Hash password:
  - passwordEncoder.encode()
  ↓
Set defaults:
  - isActive = true
  - isVerified = false
  ↓
userService.saveUser(user)
  ↓
userRepository.save()
  ↓
Redirect to /admin/users với success message
```

---

### 📝 Admin Edit User

**Complex logic với permissions:**

```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
    User adminUser = (User) authentication.getPrincipal();
    User targetUser = userService.getUserById(id).orElse(null);

    if (targetUser == null) {
        redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
        return "redirect:/admin/users";
    }

    // Permission checks
    boolean isTargetUserAdmin = targetUser.getRole().getRoleName() == Role.RoleName.ADMIN;
    boolean canManageAdmin = "user_admin_01".equals(adminUser.getUserId());
    boolean isEditingSelf = adminUser.getUserId().equals(id);

    // Rule: Chỉ user_admin_01 hoặc chính mình mới sửa được admin
    if (isTargetUserAdmin && !canManageAdmin && !isEditingSelf) {
        redirectAttributes.addFlashAttribute("error", 
            "Chỉ admin gốc (user_admin_01) mới có quyền chỉnh sửa admin khác!");
        return "redirect:/admin/users";
    }

    model.addAttribute("user", targetUser);
    model.addAttribute("canManageAdmin", canManageAdmin);
    model.addAttribute("isEditingSelf", isEditingSelf);
    
    return "admin/users/form";
}
```

**Permission matrix:**

| Admin User | Target User | Can Edit? | Notes |
|------------|-------------|-----------|-------|
| user_admin_01 | ANY | ✅ Yes | Root admin |
| user_admin_02 | user_normal_01 | ✅ Yes | Admin edits regular user |
| user_admin_02 | user_admin_01 | ❌ No | Cannot edit root admin |
| user_admin_02 | user_admin_02 | ✅ Yes | Can edit self |
| user_admin_02 | user_admin_03 | ❌ No | Cannot edit other admin |

---

### 📝 Admin Delete User

**Special protections:**

```java
@PostMapping("/delete/{id}")
public String deleteUser(@PathVariable String id,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
    User adminUser = (User) authentication.getPrincipal();
    
    // Protection 1: Cannot delete user_admin_01
    if ("user_admin_01".equals(id)) {
        redirectAttributes.addFlashAttribute("error", 
            "Không thể xóa tài khoản admin gốc!");
        return "redirect:/admin/users";
    }

    // Protection 2: Cannot delete yourself
    if (adminUser.getUserId().equals(id)) {
        redirectAttributes.addFlashAttribute("error", 
            "Bạn không thể xóa tài khoản của chính mình!");
        return "redirect:/admin/users";
    }

    User targetUser = userService.getUserById(id).orElseThrow();

    // Protection 3: Only user_admin_01 can delete other admins
    boolean isTargetAdmin = targetUser.getRole().getRoleName() == Role.RoleName.ADMIN;
    boolean canManageAdmin = "user_admin_01".equals(adminUser.getUserId());
    
    if (isTargetAdmin && !canManageAdmin) {
        redirectAttributes.addFlashAttribute("error", 
            "Chỉ admin gốc mới có quyền xóa admin khác!");
        return "redirect:/admin/users";
    }

    userService.deleteUser(id);
    redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");
    return "redirect:/admin/users";
}
```

---

## 4. Book Management Flow

### 📝 Admin Add Book với Authors

**Complexity:** Many-to-Many relationship

**Flow:**

```
GET /admin/books/add
  ↓
AdminBookController.addBookForm()
  ↓
Load data:
  - categories: categoryService.getAllCategories()
  - authors: authorService.getAllAuthors()
  - accessTypes: Book.AccessType.values()
  ↓
Return "admin/books/add"
  ↓
Admin fills form:
  - title, description, price
  - Select category (dropdown)
  - Select multiple authors (checkboxes)
  - Upload cover image
  - Select accessType
  ↓
POST /admin/books/add
  with MultipartFile coverImage
  with Set<String> authorIds
  ↓
AdminBookController.addBook()
  ↓
1. Upload cover image
   bookService.uploadCoverImage(coverImage)
   ↓ Returns: "/book_asset/image/covers/uuid.jpg"
   
2. Set cover URL to BookDTO
   bookDTO.setCoverImageUrl(imageUrl)
   
3. Create book
   bookService.createBook(bookDTO, authorIds)
   ↓
   
BookServiceImpl.createBook()
  ↓
1. Generate book_id
   
2. Map BookDTO → Book entity
   
3. Set Category (ManyToOne)
   categoryRepository.findById(categoryId)
   book.setBookCategory(category)
   
4. Set Authors (ManyToMany)
   for (authorId : authorIds) {
       Author author = authorRepository.findById(authorId)
       authors.add(author)
   }
   book.setAuthors(authors)
   
5. Save
   bookRepository.save(book)
   ↓
   
Database operations:
  - INSERT INTO books (...)
  - INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)
  - INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)
  ↓
  
Redirect: /admin/books với success message
```

---

## 5. File Upload Flow

### 📝 Upload Cover Image

**Endpoint:** `POST /admin/books/upload-cover`

**Controller:**
```java
@PostMapping("/upload-cover")
@ResponseBody
public ResponseEntity<Map<String, Object>> uploadCoverImage(
        @RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();
    try {
        String imageUrl = bookService.uploadCoverImage(file);
        response.put("success", true);
        response.put("imageUrl", imageUrl);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
```

**Service implementation:**
```java
@Override
public String uploadCoverImage(MultipartFile file) {
    // 1. Validate
    if (file.isEmpty()) {
        throw new RuntimeException("File is empty");
    }
    
    // 2. Check file type
    String contentType = file.getContentType();
    if (!contentType.startsWith("image/")) {
        throw new RuntimeException("Only images allowed");
    }
    
    // 3. Create directory
    Path uploadPath = Paths.get(uploadDir + "/covers");
    if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
    }
    
    // 4. Generate unique filename
    String extension = file.getOriginalFilename()
        .substring(file.getOriginalFilename().lastIndexOf("."));
    String newFilename = UUID.randomUUID().toString() + extension;
    
    // 5. Save file
    Path filePath = uploadPath.resolve(newFilename);
    Files.copy(file.getInputStream(), filePath, 
        StandardCopyOption.REPLACE_EXISTING);
    
    // 6. Return URL
    return "/book_asset/image/covers/" + newFilename;
}
```

**Frontend access:**
```html
<img th:src="@{${book.coverImageUrl}}" alt="Book Cover" />
<!-- Thymeleaf resolves: /book_asset/image/covers/uuid.jpg -->
```

**WebMvcConfig mapping:**
```java
registry.addResourceHandler("/book_asset/**")
    .addResourceLocations("file:F:/datn_uploads/book_asset/");
```

**Result:**
- URL: /book_asset/image/covers/uuid.jpg
- Maps to: F:/datn_uploads/book_asset/image/covers/uuid.jpg

---

## 6. Security Authorization Flow

### 📝 Access Protected Resource

**Scenario:** User tries to access `/admin/users`

**Flow:**

```
1. Browser request
   GET /admin/users
   Cookie: JSESSIONID=abc123
   ↓
   
2. Spring Security Filter Chain starts
   ↓
   
3. SecurityContextPersistenceFilter
   - Load SecurityContext from session (using JSESSIONID)
   - Set to SecurityContextHolder (thread-local)
   ↓
   
4. CsrfFilter
   - Check CSRF token (for POST requests)
   - Skip for GET requests
   ↓
   
5. FilterSecurityInterceptor
   - Check authorization rules from SecurityConfig
   - Rule: .requestMatchers("/admin/**").hasRole("ADMIN")
   ↓
   
6. Get Authentication from SecurityContext
   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
   ↓
   
7. Check authenticated
   if (auth == null || !auth.isAuthenticated()) {
       → authenticationEntryPoint: redirect /auth/login
   }
   ↓
   
8. Check authorities
   Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
   boolean hasAdminRole = authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
   ↓
   
9. If authorized (hasAdminRole == true):
   - Allow request
   - Proceed to AdminUserController.usersList()
   ↓
   
10. If NOT authorized (hasAdminRole == false):
    - accessDeniedPage: /auth/login?error=access_denied
    - HTTP 403 Forbidden
```

---

### 📝 Protected File Access

**Scenario:** User tries to download PDF

**SecurityConfig rule:**
```java
.requestMatchers("/book_asset/source/**").authenticated()
```

**Flow:**

```
1. User clicks "Download PDF"
   GET /book_asset/source/khoahoc-vientuong/CacTheGioiSongSong.pdf
   ↓
   
2. Spring Security Filter Chain
   ↓
   
3. Check rule: .authenticated()
   - Requires authentication (any role)
   - Không check specific role
   ↓
   
4. Get Authentication
   if (auth != null && auth.isAuthenticated()) {
       → Allow access
   } else {
       → Redirect /auth/login
   }
   ↓
   
5. WebMvcConfig Resource Handler
   registry.addResourceHandler("/book_asset/**")
       .addResourceLocations("file:F:/datn_uploads/book_asset/");
   ↓
   
6. Map URL → File
   /book_asset/source/khoahoc-vientuong/CacTheGioiSongSong.pdf
   →
   F:/datn_uploads/book_asset/source/khoahoc-vientuong/CacTheGioiSongSong.pdf
   ↓
   
7. Serve file
   - Read file from disk
   - Set Content-Type: application/pdf
   - Set Content-Disposition: attachment/inline
   - Stream to browser
   ↓
   
8. Browser
   - Download or display PDF
```

---

## 📚 Tổng Kết

### Key Takeaways

1. **Layered Architecture**
   - Controller → Service → Repository → Database
   - Separation of concerns
   - Testability

2. **Spring Security Integration**
   - Filter Chain processes all requests
   - SecurityContext stores authentication
   - Authorization rules in SecurityConfig

3. **Transaction Management**
   - @Transactional ensures ACID
   - Rollback on exceptions
   - Read-only optimization

4. **File Upload & Serving**
   - Upload: MultipartFile → Save to disk
   - Serve: WebMvcConfig resource handlers
   - Security: Protected vs public files

5. **Business Logic in Services**
   - Validation before processing
   - Exception handling
   - Orchestrate multiple repositories

---

**Các tài liệu liên quan:**
- [BACKEND_ARCHITECTURE.md](BACKEND_ARCHITECTURE.md)
- [CONFIG_DOCUMENTATION.md](CONFIG_DOCUMENTATION.md)
- [SERVICE_LAYER_GUIDE.md](SERVICE_LAYER_GUIDE.md)
- [FLOW_AUTHENTICATION.md](FLOW_AUTHENTICATION.md)

---

**Cập nhật:** 24/11/2025

