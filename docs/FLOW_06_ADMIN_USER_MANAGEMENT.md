# 👥 FLOW 06: ADMIN USER MANAGEMENT (Quản Lý Người Dùng - Admin)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 6.1: List Users](#flow-61-list-users)
3. [Flow 6.2: View User Details](#flow-62-view-user-details)
4. [Flow 6.3: Create User](#flow-63-create-user)
5. [Flow 6.4: Edit User](#flow-64-edit-user)
6. [Flow 6.5: Soft Delete User](#flow-65-soft-delete-user)
7. [Flow 6.6: Search & Filter Users](#flow-66-search--filter-users)
8. [Role Management](#role-management)
9. [Security & Permissions](#security--permissions)

---

## Tổng Quan

### Admin User Management Overview
```
┌─────────────────┐
│  Admin Login    │
│  /auth/login    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Admin Dashboard │
│  /admin/        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Users List    │
│  /admin/users   │
└────────┬────────┘
         │
         ├──► Create User
         ├──► Edit User
         ├──► View Details
         ├──► Soft Delete
         ├──► Search/Filter
         └──► Assign Roles
```

### Components
- **Controller**: `AdminUserController.java`
- **Service**: `UserService.java`, `RoleService.java`
- **Repository**: `UserRepository.java`, `RoleRepository.java`
- **Entity**: `User.java`, `Role.java`
- **DTO**: `RegisterDto.java`, `UserUpdateDto.java`

### URLs
- `GET /admin/users` - Danh sách users
- `GET /admin/users/create` - Form tạo user
- `POST /admin/users/create` - Xử lý tạo user
- `GET /admin/users/edit/{id}` - Form sửa user
- `POST /admin/users/edit/{id}` - Xử lý sửa user
- `POST /admin/users/delete/{id}` - Soft delete user
- `GET /admin/users/{id}` - Chi tiết user

---

## Flow 6.1: List Users

### Sequence Diagram
```
Admin → Browser → AdminUserController → UserService → UserRepository → Database
  │        │              │                  │              │             │
  │ GET /admin/users                                                      │
  │────────────────────────►│                                             │
  │        │                │ getAllUsers()                               │
  │        │                ├─────────────────►│                          │
  │        │                │                  │ findAll()                │
  │        │                │                  ├─────────────►│           │
  │        │                │                  │              │ SELECT *  │
  │        │                │                  │              │ WHERE     │
  │        │                │                  │              │ deleted_at│
  │        │                │                  │              │ IS NULL   │
  │        │                │                  │              ├──────────►│
  │        │                │                  │              │◄──────────┤
  │        │                │                  │◄─────────────┤           │
  │        │                │◄─────────────────┤                          │
  │        │◄────────────────┤ (return admin/users/list.html)            │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String usersList(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean status,
        Model model) {
    
    List<User> users;
    
    if (keyword != null && !keyword.isEmpty()) {
        users = userService.searchUsers(keyword);
    } else if (role != null) {
        users = userService.getUsersByRole(role);
    } else if (status != null) {
        users = userService.getUsersByStatus(status);
    } else {
        users = userService.getAllUsers();
    }
    
    // Statistics
    long totalUsers = users.size();
    long activeUsers = users.stream().filter(User::isEnabled).count();
    long inactiveUsers = totalUsers - activeUsers;
    long adminCount = users.stream()
        .filter(u -> u.getRoles().stream()
            .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())))
        .count();
    
    model.addAttribute("users", users);
    model.addAttribute("totalUsers", totalUsers);
    model.addAttribute("activeUsers", activeUsers);
    model.addAttribute("inactiveUsers", inactiveUsers);
    model.addAttribute("adminCount", adminCount);
    model.addAttribute("keyword", keyword);
    model.addAttribute("selectedRole", role);
    model.addAttribute("selectedStatus", status);
    
    return "admin/users/list";
}
```

**Service**:
```java
@Override
public List<User> getAllUsers() {
    // Chỉ lấy users chưa bị soft delete
    return userRepository.findByDeletedAtIsNull();
}
```

**SQL Query**:
```sql
SELECT u.*, GROUP_CONCAT(r.role_name) as roles
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.role_id
WHERE u.deleted_at IS NULL
GROUP BY u.user_id
ORDER BY u.created_at DESC;
```

**Response Data**:
```json
{
  "users": [
    {
      "userId": "usr_001",
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "phoneNumber": "0123456789",
      "enabled": true,
      "avatarUrl": "/uploads/avatars/john.jpg",
      "roles": [
        {"roleId": "role_2", "name": "ROLE_USER"}
      ],
      "createdAt": "2025-11-01T10:00:00",
      "deletedAt": null
    }
  ],
  "totalUsers": 150,
  "activeUsers": 145,
  "inactiveUsers": 5,
  "adminCount": 3
}
```

---

## Flow 6.2: View User Details

### Sequence Diagram
```
Admin → Browser → AdminUserController → UserService → Database
  │        │              │                  │             │
  │ GET /admin/users/{id}                                 │
  │────────────────────────►│                             │
  │        │                │ getUserById()               │
  │        │                ├─────────────────►│          │
  │        │                │                  │ SELECT * │
  │        │                │                  │ JOIN...  │
  │        │                │                  ├─────────►│
  │        │                │                  │◄─────────┤
  │        │                │◄─────────────────┤          │
  │        │                │ getUserStatistics()         │
  │        │                ├─────────────────►│          │
  │        │                │◄─────────────────┤          │
  │        │◄────────────────┤ (return detail page)       │
```

**Controller**:
```java
@GetMapping("/{id}")
public String viewUserDetail(@PathVariable String id, Model model) {
    User user = userService.getUserById(id);
    if (user == null || user.getDeletedAt() != null) {
        return "redirect:/admin/users?error=not_found";
    }
    
    // Get user statistics
    Map<String, Object> stats = userService.getUserStatistics(id);
    
    model.addAttribute("user", user);
    model.addAttribute("totalOrders", stats.get("totalOrders"));
    model.addAttribute("totalSpent", stats.get("totalSpent"));
    model.addAttribute("totalBooks", stats.get("totalBooks"));
    model.addAttribute("recentOrders", stats.get("recentOrders"));
    
    return "admin/users/detail";
}
```

---

## Flow 6.3: Create User

### Sequence Diagram
```
Admin → Browser → AdminUserController → UserService → UserRepository → Database
  │        │              │                  │              │             │
  │ GET /admin/users/create                                              │
  │────────────────────────►│                                            │
  │        │◄────────────────┤ (return create form)                      │
  │        │                                                              │
  │ POST /admin/users/create (RegisterDto)                               │
  │────────────────────────►│                                            │
  │        │                │ validateDto()                              │
  │        │                │ generateUserId()                           │
  │        │                │                                             │
  │        │                │ saveUser()                                 │
  │        │                ├─────────────────►│                         │
  │        │                │                  │ existsByUsername()      │
  │        │                │                  ├─────────────►│          │
  │        │                │                  │◄─────────────┤          │
  │        │                │                  │ existsByEmail()         │
  │        │                │                  ├─────────────►│          │
  │        │                │                  │◄─────────────┤          │
  │        │                │                  │                         │
  │        │                │                  │ hashPassword()          │
  │        │                │                  │ save()                  │
  │        │                │                  ├─────────────►│          │
  │        │                │                  │              │ INSERT   │
  │        │                │                  │              ├─────────►│
  │        │                │                  │              │◄─────────┤
  │        │                │                  │◄─────────────┤          │
  │        │                │◄─────────────────┤                         │
  │        │◄────────────────┤ redirect:/admin/users                     │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("user", new RegisterDto());
    model.addAttribute("roles", roleService.getAllRoles());
    model.addAttribute("isEdit", false);
    return "admin/users/form";
}

@PostMapping("/create")
public String createUser(
        @Valid @ModelAttribute RegisterDto registerDto,
        BindingResult bindingResult,
        @RequestParam(required = false) List<String> roleIds,
        RedirectAttributes redirectAttributes,
        Model model) {
    
    // Validation
    if (bindingResult.hasErrors()) {
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("isEdit", false);
        return "admin/users/form";
    }
    
    // Check duplicate username
    if (userService.existsByUsername(registerDto.getUsername())) {
        model.addAttribute("error", "Username đã tồn tại");
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/users/form";
    }
    
    // Check duplicate email
    if (userService.existsByEmail(registerDto.getEmail())) {
        model.addAttribute("error", "Email đã tồn tại");
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/users/form";
    }
    
    try {
        // Create user
        User user = userService.saveUser(registerDto);
        
        // Assign roles
        if (roleIds != null && !roleIds.isEmpty()) {
            userService.assignRoles(user.getUserId(), roleIds);
        }
        
        redirectAttributes.addFlashAttribute("success", "Tạo user thành công!");
        return "redirect:/admin/users";
        
    } catch (Exception e) {
        model.addAttribute("error", "Lỗi tạo user: " + e.getMessage());
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/users/form";
    }
}
```

**Service**:
```java
@Override
@Transactional
public User saveUser(RegisterDto registerDto) {
    // Generate user ID
    String userId = generateNextUserId();
    
    // Hash password
    String hashedPassword = passwordEncoder.encode(registerDto.getPassword());
    
    // Create user
    User user = new User();
    user.setUserId(userId);
    user.setUsername(registerDto.getUsername());
    user.setEmail(registerDto.getEmail());
    user.setPassword(hashedPassword);
    user.setFullName(registerDto.getFullName());
    user.setPhoneNumber(registerDto.getPhoneNumber());
    user.setEnabled(true);
    user.setCreatedAt(LocalDateTime.now());
    
    // Assign default role (USER)
    Role userRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new RuntimeException("Default role not found"));
    user.setRoles(Arrays.asList(userRole));
    
    return userRepository.save(user);
}

private String generateNextUserId() {
    List<User> allUsers = userRepository.findAll();
    int maxNumber = 0;
    
    for (User user : allUsers) {
        String userId = user.getUserId();
        if (userId != null && userId.startsWith("usr_")) {
            try {
                int number = Integer.parseInt(userId.substring(4));
                maxNumber = Math.max(maxNumber, number);
            } catch (NumberFormatException e) {
                // Skip invalid format
            }
        }
    }
    
    return String.format("usr_%03d", maxNumber + 1);
}
```

---

## Flow 6.4: Edit User

### Implementation Details

**Controller**:
```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable String id, Model model) {
    User user = userService.getUserById(id);
    if (user == null || user.getDeletedAt() != null) {
        return "redirect:/admin/users?error=not_found";
    }
    
    // Convert to DTO
    UserUpdateDto dto = new UserUpdateDto();
    dto.setUserId(user.getUserId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setFullName(user.getFullName());
    dto.setPhoneNumber(user.getPhoneNumber());
    dto.setEnabled(user.isEnabled());
    dto.setRoleIds(user.getRoles().stream()
        .map(Role::getRoleId)
        .collect(Collectors.toList()));
    
    model.addAttribute("user", dto);
    model.addAttribute("roles", roleService.getAllRoles());
    model.addAttribute("isEdit", true);
    
    return "admin/users/form";
}

@PostMapping("/edit/{id}")
public String updateUser(
        @PathVariable String id,
        @Valid @ModelAttribute UserUpdateDto userDto,
        BindingResult bindingResult,
        @RequestParam(required = false) List<String> roleIds,
        RedirectAttributes redirectAttributes,
        Model model) {
    
    if (bindingResult.hasErrors()) {
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("isEdit", true);
        return "admin/users/form";
    }
    
    try {
        User user = userService.getUserById(id);
        if (user == null) {
            return "redirect:/admin/users?error=not_found";
        }
        
        // Update fields
        user.setEmail(userDto.getEmail());
        user.setFullName(userDto.getFullName());
        user.setPhoneNumber(userDto.getPhoneNumber());
        user.setEnabled(userDto.getEnabled());
        
        // Update password if provided
        if (userDto.getNewPassword() != null && !userDto.getNewPassword().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(userDto.getNewPassword());
            user.setPassword(hashedPassword);
        }
        
        // Update roles
        if (roleIds != null) {
            List<Role> roles = roleIds.stream()
                .map(roleId -> roleService.getRoleById(roleId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            user.setRoles(roles);
        }
        
        userService.updateUser(user);
        
        redirectAttributes.addFlashAttribute("success", "Cập nhật user thành công!");
        return "redirect:/admin/users";
        
    } catch (Exception e) {
        model.addAttribute("error", "Lỗi cập nhật user: " + e.getMessage());
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/users/form";
    }
}
```

---

## Flow 6.5: Soft Delete User

### Concept
Soft delete không xóa vĩnh viễn user khỏi database, chỉ đánh dấu `deleted_at` field.

### Sequence Diagram
```
Admin → Browser → AdminUserController → UserService → Database
  │        │              │                  │             │
  │ POST /admin/users/delete/{id}                         │
  │────────────────────────►│                             │
  │        │                │ softDeleteUser()            │
  │        │                ├─────────────────►│          │
  │        │                │                  │ UPDATE   │
  │        │                │                  │ SET      │
  │        │                │                  │ deleted_at│
  │        │                │                  │ = NOW()  │
  │        │                │                  ├─────────►│
  │        │                │                  │◄─────────┤
  │        │                │◄─────────────────┤          │
  │        │◄────────────────┤ redirect:/admin/users      │
```

**Implementation**:
```java
@PostMapping("/delete/{id}")
public String softDeleteUser(
        @PathVariable String id,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
    
    try {
        User currentUser = (User) authentication.getPrincipal();
        User userToDelete = userService.getUserById(id);
        
        if (userToDelete == null) {
            redirectAttributes.addFlashAttribute("error", "User không tồn tại");
            return "redirect:/admin/users";
        }
        
        // Prevent self-deletion
        if (userToDelete.getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản đang đăng nhập");
            return "redirect:/admin/users";
        }
        
        // Prevent deleting super admin
        boolean isSuperAdmin = userToDelete.getRoles().stream()
            .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
        if (isSuperAdmin) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa Super Admin");
            return "redirect:/admin/users";
        }
        
        // Soft delete
        userService.softDeleteUser(id);
        
        redirectAttributes.addFlashAttribute("success", "Xóa user thành công!");
        return "redirect:/admin/users";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi xóa user: " + e.getMessage());
        return "redirect:/admin/users";
    }
}
```

**Service**:
```java
@Override
@Transactional
public void softDeleteUser(String userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Set deleted timestamp
    user.setDeletedAt(LocalDateTime.now());
    
    // Disable user
    user.setEnabled(false);
    
    userRepository.save(user);
}
```

**Restore User**:
```java
@PostMapping("/restore/{id}")
public String restoreUser(@PathVariable String id, RedirectAttributes redirectAttributes) {
    try {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getDeletedAt() == null) {
            redirectAttributes.addFlashAttribute("error", "User chưa bị xóa");
            return "redirect:/admin/users";
        }
        
        // Restore
        user.setDeletedAt(null);
        user.setEnabled(true);
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Khôi phục user thành công!");
        return "redirect:/admin/users";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi khôi phục user");
        return "redirect:/admin/users";
    }
}
```

---

## Flow 6.6: Search & Filter Users

### Implementation

**Search by Keyword**:
```java
@Override
public List<User> searchUsers(String keyword) {
    return userRepository.findByUsernameContainingOrEmailContainingOrFullNameContaining(
        keyword, keyword, keyword
    );
}
```

**Filter by Role**:
```java
@Override
public List<User> getUsersByRole(String roleName) {
    return userRepository.findByRoles_Name(roleName);
}
```

**Filter by Status**:
```java
@Override
public List<User> getUsersByStatus(Boolean enabled) {
    return userRepository.findByEnabled(enabled);
}
```

**Combined Filters**:
```java
@Override
public List<User> searchUsersWithFilters(String keyword, String role, Boolean status) {
    if (keyword != null && role != null && status != null) {
        return userRepository.findByKeywordAndRoleAndStatus(keyword, role, status);
    } else if (keyword != null && role != null) {
        return userRepository.findByKeywordAndRole(keyword, role);
    } else if (keyword != null) {
        return searchUsers(keyword);
    } else if (role != null) {
        return getUsersByRole(role);
    } else if (status != null) {
        return getUsersByStatus(status);
    } else {
        return getAllUsers();
    }
}
```

---

## Role Management

### Available Roles
- **ROLE_SUPER_ADMIN**: Full system access, cannot be deleted
- **ROLE_ADMIN**: Admin panel access
- **ROLE_USER**: Regular user access

### Assign Roles
```java
@Override
@Transactional
public void assignRoles(String userId, List<String> roleIds) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    List<Role> roles = roleIds.stream()
        .map(roleId -> roleRepository.findById(roleId)
            .orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    
    user.setRoles(roles);
    userRepository.save(user);
}
```

### Check User Role
```java
public boolean hasRole(User user, String roleName) {
    return user.getRoles().stream()
        .anyMatch(role -> roleName.equals(role.getName()));
}

public boolean isAdmin(User user) {
    return hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_SUPER_ADMIN");
}
```

---

## Security & Permissions

### Access Control
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### Permission Matrix

| Action | SUPER_ADMIN | ADMIN | USER |
|--------|-------------|-------|------|
| View Users List | ✅ | ✅ | ❌ |
| Create User | ✅ | ✅ | ❌ |
| Edit User | ✅ | ✅ (own profile) | ❌ |
| Delete User | ✅ | ✅ (except SUPER_ADMIN) | ❌ |
| Assign ADMIN Role | ✅ | ❌ | ❌ |
| Delete SUPER_ADMIN | ❌ | ❌ | ❌ |

---

## Best Practices

✅ **Always validate permissions**
```java
if (!canDeleteUser(currentUser, userToDelete)) {
    throw new AccessDeniedException();
}
```

✅ **Use soft delete**
```java
user.setDeletedAt(LocalDateTime.now());
```

✅ **Hash passwords**
```java
String hashed = passwordEncoder.encode(password);
```

✅ **Prevent self-deletion**
```java
if (userToDelete.equals(currentUser)) {
    throw new IllegalOperationException();
}
```

✅ **Audit logging**
```java
auditLog.log("User " + admin + " deleted user " + userId);
```

---

**Last Updated:** 06/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

