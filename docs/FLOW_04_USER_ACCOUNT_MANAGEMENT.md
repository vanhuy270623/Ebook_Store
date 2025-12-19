# 👤 FLOW 04: USER ACCOUNT MANAGEMENT (Quản Lý Tài Khoản)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 4.1: View Profile](#flow-41-view-profile)
3. [Flow 4.2: Update Profile](#flow-42-update-profile)
4. [Flow 4.3: Change Password](#flow-43-change-password)
5. [Flow 4.4: Update Avatar](#flow-44-update-avatar)
6. [Flow 4.5: Order History](#flow-45-order-history)
7. [Flow 4.6: Reading History](#flow-46-reading-history)
8. [Debugging Endpoints](#debugging-endpoints)

---

## Tổng Quan

### Components

**Controllers** (Đã được tách thành nhiều controllers chuyên biệt):
- `UserProfileController.java` - Profile management, password change
- `UserOrderController.java` - Order history
- `UserLibraryController.java` - Library & reading history (→ See **FLOW 20**)
- `UserDeviceController.java` - Device management (→ See **FLOW 19**)
- `UserSubscriptionController.java` - Subscription management (→ See **FLOW 10**)

**Services**:
- `UserService.java` - User CRUD operations
- `OrderService.java` - Order queries
- `ReadingProgressService.java` - Reading history

**Repositories**:
- `UserRepository.java`
- `OrderRepository.java`
- `ReadingProgressRepository.java`

**Entities**:
- `User.java`
- `Order.java`
- `ReadingProgress.java`

### URLs

**Profile Management:**
- `GET /user/profile` - Xem profile
- `POST /user/profile/update` - Cập nhật profile
- `POST /user/profile/change-password` - Đổi mật khẩu

**Order History:**
- `GET /user/orders` - Lịch sử đơn hàng
- `GET /user/orders/{id}` - Chi tiết đơn hàng

**Library & Reading:** (→ See **FLOW 20**)
- `GET /user/library` - Thư viện sách
- `GET /user/reading-history` - Lịch sử đọc

**Device Management:** (→ See **FLOW 19**)
- `GET /user/devices` - Quản lý thiết bị
- `POST /user/devices/{id}/remove` - Xóa thiết bị

**Subscription:** (→ See **FLOW 10**)
- `GET /subscription/my-subscriptions` - Gói đăng ký của tôi

---

## Flow 4.1: View Profile

### Sequence Diagram
```
User → Browser → UserController → UserService → UserRepository → Database
  │       │           │               │              │              │
  │  GET /user/profile                                              │
  │──────────────────────►│                                         │
  │       │               │ getCurrentUser()                        │
  │       │               ├─────────────►│                          │
  │       │               │               │ findByUsername()        │
  │       │               │               ├────────────►│           │
  │       │               │               │             │ SELECT *  │
  │       │               │               │             ├──────────►│
  │       │               │               │             │◄──────────┤
  │       │               │               │◄────────────┤           │
  │       │               │◄─────────────┤                          │
  │◄──────────────────────┤ (return user/profile.html)             │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/profile")
public String viewProfile(Authentication authentication, Model model) {
    String username = authentication.getName();
    User user = userService.findByUsername(username);
    
    // Get statistics
    int totalOrders = orderService.countByUser(user);
    int totalBooks = orderItemRepository.countDistinctBooksByUser(user);
    BigDecimal totalSpent = orderService.getTotalSpentByUser(user);
    
    model.addAttribute("user", user);
    model.addAttribute("totalOrders", totalOrders);
    model.addAttribute("totalBooks", totalBooks);
    model.addAttribute("totalSpent", totalSpent);
    
    return "user/profile";
}
```

**SQL Query**:
```sql
-- Get user
SELECT u.*, GROUP_CONCAT(r.role_name) as roles
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = ?
GROUP BY u.user_id;

-- Get statistics
SELECT 
    COUNT(DISTINCT o.order_id) as total_orders,
    COUNT(DISTINCT oi.book_id) as total_books,
    SUM(o.total_amount) as total_spent
FROM orders o
LEFT JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.user_id = ?;
```

---

## Flow 4.2: Update Profile

### Sequence Diagram
```
User → Browser → UserController → UserService → UserRepository → Database
  │       │           │               │              │              │
  │  POST /user/profile/update (ProfileDto)                        │
  │──────────────────────►│                                         │
  │       │               │ updateProfile()                         │
  │       │               ├─────────────►│                          │
  │       │               │               │ validateEmail()         │
  │       │               │               │ validatePhone()         │
  │       │               │               │ update()                │
  │       │               │               ├────────────►│           │
  │       │               │               │             │ UPDATE    │
  │       │               │               │             ├──────────►│
  │       │               │               │             │◄──────────┤
  │       │               │               │◄────────────┤           │
  │       │               │◄─────────────┤                          │
  │◄──────────────────────┤ redirect:/user/profile                 │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/profile/update")
public String updateProfile(
    @Valid @ModelAttribute("user") ProfileDto profileDto,
    Authentication authentication,
    RedirectAttributes redirectAttributes) {
    
    try {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        
        // Validate email unique (if changed)
        if (!user.getEmail().equals(profileDto.getEmail())) {
            if (userRepository.existsByEmail(profileDto.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        }
        
        // Update user
        user.setFullName(profileDto.getFullName());
        user.setEmail(profileDto.getEmail());
        user.setPhoneNumber(profileDto.getPhoneNumber());
        user.setAddress(profileDto.getAddress());
        user.setCity(profileDto.getCity());
        user.setDistrict(profileDto.getDistrict());
        user.setWard(profileDto.getWard());
        
        userService.updateUser(user);
        
        redirectAttributes.addFlashAttribute("success", 
            "Cập nhật profile thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    
    return "redirect:/user/profile";
}
```

**SQL Update**:
```sql
UPDATE users SET
    full_name = ?,
    email = ?,
    phone_number = ?,
    address = ?,
    city = ?,
    district = ?,
    ward = ?,
    updated_at = NOW()
WHERE user_id = ?;
```

---

## Flow 4.3: Change Password

### Sequence Diagram
```
User → Browser → UserController → UserService → BCrypt → UserRepository → Database
  │       │           │               │             │         │             │
  │  POST /user/profile/change-password                                    │
  │──────────────────────►│                                                │
  │       │               │ changePassword()                               │
  │       │               ├─────────────►│                                 │
  │       │               │               │ verifyOldPassword()            │
  │       │               │               ├────────────►│                  │
  │       │               │               │◄────────────┤                  │
  │       │               │               │ hashNewPassword()              │
  │       │               │               ├────────────►│                  │
  │       │               │               │◄────────────┤                  │
  │       │               │               │ updatePassword()               │
  │       │               │               ├─────────────────►│             │
  │       │               │               │                  │ UPDATE      │
  │       │               │               │                  ├────────────►│
  │       │               │               │                  │◄────────────┤
  │       │               │               │◄─────────────────┤             │
  │       │               │◄─────────────┤                                 │
  │◄──────────────────────┤ redirect:/user/profile                        │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/profile/change-password")
public String changePassword(
    @RequestParam String currentPassword,
    @RequestParam String newPassword,
    @RequestParam String confirmPassword,
    Authentication authentication,
    RedirectAttributes redirectAttributes) {
    
    try {
        // 1. Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu mới không khớp");
        }
        
        // 2. Validate password strength
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        
        // 3. Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        
        // 4. Update password
        userService.changePassword(user, newPassword);
        
        redirectAttributes.addFlashAttribute("success", 
            "Đổi mật khẩu thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    
    return "redirect:/user/profile";
}
```

**Service**:
```java
@Override
@Transactional
public void changePassword(User user, String newPassword) {
    String hashedPassword = passwordEncoder.encode(newPassword);
    user.setPassword(hashedPassword);
    userRepository.save(user);
}
```

**SQL Update**:
```sql
UPDATE users
SET password = ?,
    updated_at = NOW()
WHERE user_id = ?;
```

---

## Flow 4.4: Update Avatar

### Sequence Diagram
```
User → Browser → UserController → FileStorageService → UserRepository → Database
  │       │           │                    │                 │             │
  │  POST /user/profile/upload-avatar (MultipartFile)                     │
  │──────────────────────►│                                               │
  │       │               │ uploadAvatar()                                │
  │       │               ├───────────────►│                              │
  │       │               │                │ validate()                   │
  │       │               │                │ deleteOld()                  │
  │       │               │                │ saveNew()                    │
  │       │               │◄───────────────┤                              │
  │       │               │ updateUser()                                  │
  │       │               ├──────────────────────────────►│               │
  │       │               │                                │ UPDATE        │
  │       │               │                                ├──────────────►│
  │       │               │                                │◄──────────────┤
  │       │               │◄──────────────────────────────┤               │
  │◄──────────────────────┤ redirect:/user/profile                       │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/profile/upload-avatar")
public String uploadAvatar(
    @RequestParam("avatarFile") MultipartFile avatarFile,
    Authentication authentication,
    RedirectAttributes redirectAttributes) {
    
    try {
        // 1. Validate file
        if (avatarFile.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file");
        }
        
        String contentType = avatarFile.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new RuntimeException("File phải là ảnh");
        }
        
        // 2. Get user
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        
        // 3. Delete old avatar
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteFile(user.getAvatarUrl());
        }
        
        // 4. Save new avatar
        String avatarUrl = fileStorageService.saveAvatar(avatarFile);
        
        // 5. Update user
        user.setAvatarUrl(avatarUrl);
        userService.updateUser(user);
        
        redirectAttributes.addFlashAttribute("success", 
            "Cập nhật avatar thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    
    return "redirect:/user/profile";
}
```

**File Storage Service**:
```java
public String saveAvatar(MultipartFile file) throws IOException {
    // 1. Generate filename
    String extension = getExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID().toString() + extension;
    
    // 2. Create directory
    Path uploadPath = Paths.get(uploadDir, "book_asset", "image", "avatars");
    Files.createDirectories(uploadPath);
    
    // 3. Save file
    Path filePath = uploadPath.resolve(filename);
    Files.copy(file.getInputStream(), filePath, 
        StandardCopyOption.REPLACE_EXISTING);
    
    // 4. Return URL
    return "/uploads/avatars/" + filename;
}
```

---

## Flow 4.5: Order History

### Sequence Diagram
```
User → Browser → UserController → OrderService → OrderRepository → Database
  │       │           │               │               │              │
  │  GET /user/orders                                                │
  │──────────────────────►│                                          │
  │       │               │ getOrdersByUser()                        │
  │       │               ├─────────────►│                           │
  │       │               │               │ findByUser()             │
  │       │               │               ├────────────►│            │
  │       │               │               │             │ SELECT *   │
  │       │               │               │             ├───────────►│
  │       │               │               │             │◄───────────┤
  │       │               │               │◄────────────┤            │
  │       │               │◄─────────────┤                           │
  │◄──────────────────────┤ (return user/orders/list.html)          │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/orders")
public String orderHistory(
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "0") int page,
    Authentication authentication,
    Model model) {
    
    String username = authentication.getName();
    User user = userService.findByUsername(username);
    
    Pageable pageable = PageRequest.of(page, 10, 
        Sort.by("createdAt").descending());
    
    Page<Order> orders;
    if (status != null && !status.isEmpty()) {
        orders = orderService.getOrdersByUserAndStatus(user, status, pageable);
    } else {
        orders = orderService.getOrdersByUser(user, pageable);
    }
    
    model.addAttribute("orders", orders);
    model.addAttribute("currentStatus", status);
    
    return "user/orders/list";
}

@GetMapping("/orders/{orderId}")
public String orderDetail(@PathVariable Long orderId,
                         Authentication authentication,
                         Model model) {
    String username = authentication.getName();
    User user = userService.findByUsername(username);
    
    Order order = orderService.getOrderById(orderId);
    
    // Verify owner
    if (!order.getUser().getUserId().equals(user.getUserId())) {
        throw new AccessDeniedException("Không có quyền truy cập");
    }
    
    model.addAttribute("order", order);
    model.addAttribute("orderItems", order.getOrderItems());
    
    return "user/orders/detail";
}
```

**SQL Query**:
```sql
-- Get orders with pagination
SELECT o.*, 
       COUNT(oi.order_item_id) as item_count
FROM orders o
LEFT JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.user_id = ?
  AND (? IS NULL OR o.order_status = ?)
GROUP BY o.order_id
ORDER BY o.created_at DESC
LIMIT ? OFFSET ?;

-- Get order detail with items
SELECT o.*, 
       oi.order_item_id, oi.book_id, oi.quantity, oi.price, oi.subtotal,
       b.title, b.cover_image_url
FROM orders o
INNER JOIN order_items oi ON o.order_id = oi.order_id
INNER JOIN books b ON oi.book_id = b.book_id
WHERE o.order_id = ?;
```

---

## Flow 4.6: Reading History

### Sequence Diagram
```
User → Browser → UserController → ReadingHistoryService → Repository → Database
  │       │           │                    │                    │          │
  │  GET /user/reading-history                                             │
  │──────────────────────►│                                                │
  │       │               │ getReadingHistory()                            │
  │       │               ├───────────────────►│                           │
  │       │               │                    │ findByUser()              │
  │       │               │                    ├──────────────►│           │
  │       │               │                    │                │ SELECT * │
  │       │               │                    │                ├─────────►│
  │       │               │                    │                │◄─────────┤
  │       │               │                    │◄──────────────┤           │
  │       │               │◄───────────────────┤                           │
  │◄──────────────────────┤ (return user/reading-history.html)            │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/reading-history")
public String readingHistory(
    @RequestParam(defaultValue = "0") int page,
    Authentication authentication,
    Model model) {
    
    String username = authentication.getName();
    User user = userService.findByUsername(username);
    
    Pageable pageable = PageRequest.of(page, 20, 
        Sort.by("lastReadAt").descending());
    
    Page<ReadingHistory> history = readingHistoryService
        .getReadingHistoryByUser(user, pageable);
    
    model.addAttribute("history", history);
    
    return "user/reading-history";
}
```

**Service** (auto-track when user views book):
```java
@Override
@Transactional
public void trackReading(User user, Book book) {
    Optional<ReadingHistory> existing = readingHistoryRepository
        .findByUserAndBook(user, book);
    
    if (existing.isPresent()) {
        // Update last read
        ReadingHistory history = existing.get();
        history.setLastReadAt(LocalDateTime.now());
        history.setReadCount(history.getReadCount() + 1);
        readingHistoryRepository.save(history);
    } else {
        // Create new
        ReadingHistory history = new ReadingHistory();
        history.setUser(user);
        history.setBook(book);
        history.setFirstReadAt(LocalDateTime.now());
        history.setLastReadAt(LocalDateTime.now());
        history.setReadCount(1);
        readingHistoryRepository.save(history);
    }
}
```

**SQL**:
```sql
-- Get reading history
SELECT rh.*, b.title, b.cover_image_url, b.book_id
FROM reading_history rh
INNER JOIN books b ON rh.book_id = b.book_id
WHERE rh.user_id = ?
ORDER BY rh.last_read_at DESC
LIMIT ? OFFSET ?;

-- Track reading
INSERT INTO reading_history (user_id, book_id, first_read_at, last_read_at, read_count)
VALUES (?, ?, NOW(), NOW(), 1)
ON DUPLICATE KEY UPDATE
    last_read_at = NOW(),
    read_count = read_count + 1;
```

---

## Debugging Endpoints

### 1. Check User Profile Data

**Debug Controller**:
```java
@GetMapping("/debug/user-info")
@ResponseBody
public Map<String, Object> debugUserInfo(Authentication authentication) {
    User user = userService.findByUsername(authentication.getName());
    
    Map<String, Object> result = new HashMap<>();
    result.put("userId", user.getUserId());
    result.put("username", user.getUsername());
    result.put("email", user.getEmail());
    result.put("fullName", user.getFullName());
    result.put("phoneNumber", user.getPhoneNumber());
    result.put("avatarUrl", user.getAvatarUrl());
    result.put("enabled", user.isEnabled());
    result.put("roles", user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.toList()));
    result.put("createdAt", user.getCreatedAt());
    
    // Statistics
    result.put("totalOrders", orderService.countByUser(user));
    result.put("totalSpent", orderService.getTotalSpentByUser(user));
    
    return result;
}
```

**Test URL**: `http://localhost:8080/debug/user-info`

### 2. Test Password Change

**cURL**:
```bash
curl -X POST http://localhost:8080/user/profile/change-password \
  -H "Cookie: JSESSIONID=xxx" \
  -d "currentPassword=oldpass&newPassword=newpass&confirmPassword=newpass"
```

### 3. Test Avatar Upload

**Postman**:
- Method: POST
- URL: `http://localhost:8080/user/profile/upload-avatar`
- Body (form-data):
  - avatarFile: [select image file]

### 4. Database Verification

**Check user updated**:
```sql
SELECT user_id, username, email, full_name, phone_number, 
       avatar_url, updated_at
FROM users
WHERE username = ?;
```

**Check password changed**:
```sql
SELECT username, password, updated_at
FROM users
WHERE username = ?;
-- Password should be BCrypt hash
```

**Check orders**:
```sql
SELECT COUNT(*) as total_orders,
       SUM(total_amount) as total_spent
FROM orders
WHERE user_id = ?;
```

**Check reading history**:
```sql
SELECT COUNT(DISTINCT book_id) as books_read,
       SUM(read_count) as total_reads
FROM reading_history
WHERE user_id = ?;
```

### 5. Common Issues & Solutions

| Issue | Debug Method | Solution |
|-------|-------------|----------|
| Email duplicate error | Check database | Validate before update |
| Password not changed | Check BCrypt | Ensure encoder is used |
| Avatar not displayed | Check file path | Verify static resource mapping |
| Old avatar not deleted | Check file service | Add delete logic |
| Access denied to order | Check user_id | Verify order ownership |

### 6. Logging Configuration

```properties
# User profile logging
logging.level.com.example.ebook_store.controller.UserController=DEBUG
logging.level.com.example.ebook_store.service.UserService=DEBUG
logging.level.com.example.ebook_store.service.ReadingHistoryService=DEBUG

# File upload
logging.level.com.example.ebook_store.service.FileStorageService=DEBUG

# Security
logging.level.org.springframework.security=DEBUG
```

### 7. Breakpoint Locations

**UserController**:
- Line: `userService.updateUser(user)`
- Line: `userService.changePassword(user, newPassword)`
- Line: `fileStorageService.saveAvatar(avatarFile)`

**UserService**:
- Line: `userRepository.save(user)`
- Line: `passwordEncoder.encode(newPassword)`

---

## Test Scenarios

### Scenario 1: Update profile
1. Login user
2. Navigate to profile
3. Update full name, email, phone
4. Submit
5. Verify: Data updated in database
6. Verify: Success message shown

### Scenario 2: Change password
1. Enter current password (correct)
2. Enter new password (min 6 chars)
3. Confirm new password
4. Submit
5. Verify: Password hash updated
6. Try login with new password

### Scenario 3: Upload avatar
1. Select image file
2. Upload
3. Verify: Old avatar deleted from disk
4. Verify: New avatar saved
5. Verify: Database updated
6. Verify: Avatar displayed on page

### Scenario 4: View orders
1. Navigate to orders page
2. Verify: All orders displayed
3. Filter by status
4. Click order detail
5. Verify: Items, totals correct

### Scenario 5: Reading history
1. View some books
2. Navigate to reading history
3. Verify: Books appear in history
4. Verify: Last read time correct

---

**Last Updated**: 30/11/2025
**Version**: 2.0

