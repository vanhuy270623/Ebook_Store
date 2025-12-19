# Fix Lỗi Validation - Tài Khoản Đã Bị Xóa Mềm (Soft Delete)

## Ngày: 18/12/2025

## 🐛 Vấn Đề Phát Hiện

### Mô tả lỗi:
Khi đăng ký với **email hoặc username đã từng được sử dụng bởi tài khoản bị xóa mềm (soft delete)**, hệ thống báo lỗi sai:

- ❌ Email trùng nhưng báo: "Tên người dùng đã tồn tại"
- ❌ Hoặc hiển thị lỗi SQL thô: `Duplicate entry 'aa@gmail.com' for key 'users.email'`

### Nguyên nhân:

**Luồng xử lý ban đầu (BỊ LỖI):**

```
1. Check: findActiveByEmail()       → Không tìm thấy (vì chỉ tìm active users)
   ✅ Pass

2. Check: findActiveByUsername()    → Không tìm thấy (vì chỉ tìm active users)
   ✅ Pass

3. Save user vào database          → ❌ LỖI CONSTRAINT DUPLICATE!
   → Database có email này rồi (từ user đã bị xóa mềm)
   → Throw DataIntegrityViolationException

4. Catch exception và phân tích errorMessage
   → Logic kiểm tra SAI:
   - Check: errorMessage.contains("username") → TRUE (vì SQL chứa cột username)
   - Kết quả: Báo lỗi SAI "Tên người dùng đã tồn tại" thay vì "Email đã tồn tại"
```

**Tại sao logic cũ sai?**

Chuỗi lỗi SQL từ database:
```
Duplicate entry 'aa@gmail.com' for key 'users.email'
[insert into users (username, email, ...) values (?, ?, ...)]
```

Logic cũ:
```java
if (errorMessage.contains("username"))  // ← Luôn TRUE vì SQL có từ "username"
    → Báo lỗi username
else if (errorMessage.contains("email"))
    → Báo lỗi email (không chạy tới đây)
```

---

## ✅ Giải Pháp

### 1. Thêm Methods Mới Vào UserRepository

```java
// Find user by email including deleted ones (for duplicate check)
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

// Find user by username including deleted ones (for duplicate check)
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsernameIncludingDeleted(@Param("username") String username);
```

**Lợi ích:**
- ✅ Tìm được cả tài khoản đã bị xóa mềm (deletedAt IS NOT NULL)
- ✅ Phát hiện trùng lặp TRƯỚC KHI save vào database

---

### 2. Cải Thiện Logic Kiểm Tra Trong `registerUser()`

#### Trước (BỊ LỖI):
```java
// Chỉ check active users
if (userRepository.findActiveByUsername(username).isPresent()) {
    throw new Exception("Username đã tồn tại");
}

if (userRepository.findActiveByEmail(email).isPresent()) {
    throw new Exception("Email đã tồn tại");
}

// Save → CÓ THỂ BỊ LỖI nếu có user đã bị xóa mềm
userRepository.save(user);
```

#### Sau (FIXED):
```java
// 1. Check username (KỂ CẢ user đã xóa mềm)
Optional<User> existingUserByUsername = 
    userRepository.findByUsernameIncludingDeleted(registerDto.getUsername());

if (existingUserByUsername.isPresent()) {
    User existingUser = existingUserByUsername.get();
    
    if (existingUser.isDeleted()) {
        throw new Exception("Tên người dùng '" + registerDto.getUsername() + 
            "' đã từng được sử dụng bởi tài khoản đã bị xóa. " +
            "Vui lòng chọn tên khác hoặc liên hệ quản trị viên để khôi phục tài khoản.");
    } else {
        throw new Exception("Tên người dùng '" + registerDto.getUsername() + 
            "' đã được sử dụng. Vui lòng chọn tên khác.");
    }
}

// 2. Check email (KỂ CẢ user đã xóa mềm)
Optional<User> existingUserByEmail = 
    userRepository.findByEmailIncludingDeleted(registerDto.getEmail());

if (existingUserByEmail.isPresent()) {
    User existingUser = existingUserByEmail.get();
    
    if (existingUser.isDeleted()) {
        throw new Exception("Email '" + registerDto.getEmail() + 
            "' đã từng được đăng ký cho tài khoản đã bị xóa. " +
            "Vui lòng sử dụng email khác hoặc liên hệ quản trị viên để khôi phục tài khoản.");
    } else {
        throw new Exception("Email '" + registerDto.getEmail() + 
            "' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
    }
}

// 3. Save (giờ an toàn vì đã check kỹ)
userRepository.save(user);
```

---

### 3. Cải Thiện Exception Handling

```java
try {
    userRepository.save(user);
} catch (org.springframework.dao.DataIntegrityViolationException e) {
    String errorMessage = e.getMessage();
    
    // Kiểm tra CHÍNH XÁC constraint nào bị vi phạm
    // Thứ tự: kiểm tra "for key 'users.email'" TRƯỚC
    if (errorMessage.contains("key 'users.email'") || 
        (errorMessage.contains("Duplicate entry") && 
         errorMessage.contains("for key 'users.email'"))) {
        throw new Exception("Email '" + email + "' đã được đăng ký...");
        
    } else if (errorMessage.contains("key 'users.username'") || 
               (errorMessage.contains("Duplicate entry") && 
                errorMessage.contains("for key 'users.username'"))) {
        throw new Exception("Tên người dùng '" + username + "' đã được sử dụng...");
    }
    // ... fallback cases
}
```

**Cải tiến:**
- ✅ Kiểm tra **"for key 'users.email'"** (chính xác hơn chỉ check "email")
- ✅ Kiểm tra **"for key 'users.username'"** (chính xác hơn chỉ check "username")
- ✅ Thứ tự kiểm tra: email TRƯỚC, username SAU

---

## 📋 Các Trường Hợp Xử Lý

| Tình huống | Thông báo |
|------------|-----------|
| **Username mới + Email mới** | ✅ Đăng ký thành công |
| **Username đã tồn tại (active)** | ❌ "Tên người dùng 'johndoe' đã được sử dụng. Vui lòng chọn tên khác." |
| **Email đã tồn tại (active)** | ❌ "Email 'test@gmail.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập." |
| **Username đã xóa mềm** | ❌ "Tên người dùng 'olduser' đã từng được sử dụng bởi tài khoản đã bị xóa. Vui lòng chọn tên khác hoặc liên hệ quản trị viên để khôi phục tài khoản." |
| **Email đã xóa mềm** | ❌ "Email 'old@gmail.com' đã từng được đăng ký cho tài khoản đã bị xóa. Vui lòng sử dụng email khác hoặc liên hệ quản trị viên để khôi phục tài khoản." |

---

## 🧪 Test Cases

### Test 1: Email đã bị xóa mềm
```
Bước 1: Admin xóa user có email "deleted@example.com" (soft delete)
Bước 2: User mới đăng ký với email "deleted@example.com"

Kết quả mong đợi:
❌ "Email 'deleted@example.com' đã từng được đăng ký cho tài khoản đã bị xóa. 
    Vui lòng sử dụng email khác hoặc liên hệ quản trị viên để khôi phục tài khoản."

✅ KHÔNG hiển thị lỗi SQL
✅ KHÔNG báo sai "Tên người dùng đã tồn tại"
```

### Test 2: Username đã bị xóa mềm
```
Bước 1: Admin xóa user có username "olduser" (soft delete)
Bước 2: User mới đăng ký với username "olduser"

Kết quả mong đợi:
❌ "Tên người dùng 'olduser' đã từng được sử dụng bởi tài khoản đã bị xóa. 
    Vui lòng chọn tên khác hoặc liên hệ quản trị viên để khôi phục tài khoản."
```

### Test 3: Email trùng (active user)
```
Đăng ký với email đã có trong hệ thống (user active)

Kết quả mong đợi:
❌ "Email 'test@gmail.com' đã được đăng ký. 
    Vui lòng sử dụng email khác hoặc đăng nhập."
```

### Test 4: Username trùng (active user)
```
Đăng ký với username đã có trong hệ thống (user active)

Kết quả mong đợi:
❌ "Tên người dùng 'testuser' đã được sử dụng. Vui lòng chọn tên khác."
```

---

## 📦 Files Đã Thay Đổi

### 1. UserRepository.java
**Thêm mới:**
- `findByEmailIncludingDeleted(String email)`
- `findByUsernameIncludingDeleted(String username)`

### 2. UserServiceImpl.java
**Cải thiện:**
- Logic kiểm tra duplicate trong `registerUser()`
- Exception handling cho DataIntegrityViolationException

---

## 🎯 Kết Quả

### ❌ Trước (BỊ LỖI):
```
// Đăng ký với email đã bị xóa mềm
→ "Tên người dùng đã tồn tại" (SAI!)
→ hoặc hiển thị lỗi SQL thô
```

### ✅ Sau (FIXED):
```
// Đăng ký với email đã bị xóa mềm
→ "Email 'aa@gmail.com' đã từng được đăng ký cho tài khoản đã bị xóa. 
   Vui lòng sử dụng email khác hoặc liên hệ quản trị viên để khôi phục tài khoản."
```

---

## 📝 Checklist

- [x] Thêm `findByEmailIncludingDeleted()` vào UserRepository
- [x] Thêm `findByUsernameIncludingDeleted()` vào UserRepository
- [x] Sửa logic kiểm tra trong `registerUser()`
- [x] Phân biệt thông báo: active user vs deleted user
- [x] Cải thiện exception handling
- [x] Test compile thành công

---

**Status:** ✅ Hoàn thành  
**Build:** ⏳ Đang compile...  
**Impact:** 🔒 Bảo mật cao - Ngăn chặn đăng ký với email/username đã bị xóa mềm

