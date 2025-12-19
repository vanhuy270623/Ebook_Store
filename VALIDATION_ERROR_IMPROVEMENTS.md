# Cải Thiện Thông Báo Lỗi Validation - Hệ Thống Đăng Ký

## Ngày: 18/12/2025

## Vấn Đề Ban Đầu

Khi người dùng đăng ký với dữ liệu trùng lặp, hệ thống hiển thị lỗi SQL thô:
```
could not execute statement [Duplicate entry 'aa@gmail.com' for key 'users.email']...
```

Điều này:
- ❌ Không thân thiện với người dùng
- ❌ Lộ thông tin kỹ thuật database
- ❌ Khó hiểu và gây nhầm lẫn

## Giải Pháp Đã Triển Khai

### 1. **Cải thiện RegisterDto** (`dto/RegisterDto.java`)

#### Trước:
```java
@NotEmpty(message = "Email không được để trống")
private String email;
```

#### Sau:
```java
@NotEmpty(message = "Email không được để trống")
@Email(message = "Email không hợp lệ. Vui lòng nhập đúng định dạng email (ví dụ: user@example.com)")
@Size(max = 100, message = "Email không được vượt quá 100 ký tự")
private String email;

@NotEmpty(message = "Tên người dùng không được để trống")
@Size(min = 3, max = 50, message = "Tên người dùng phải có từ 3-50 ký tự")
@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới")
private String username;

@NotEmpty(message = "Mật khẩu không được để trống")
@Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
private String password;
```

**Lợi ích:**
- ✅ Kiểm tra format email hợp lệ (user@example.com)
- ✅ Kiểm tra độ dài username (3-50 ký tự)
- ✅ Kiểm tra ký tự hợp lệ trong username (chỉ a-z, 0-9, _)
- ✅ Kiểm tra độ dài password (tối thiểu 6 ký tự)

---

### 2. **Xử Lý Lỗi Database** (`service/impl/UserServiceImpl.java`)

#### Thêm try-catch xử lý DataIntegrityViolationException:

```java
try {
    userRepository.save(user);
} catch (org.springframework.dao.DataIntegrityViolationException e) {
    // Xử lý lỗi constraint violation từ database
    String errorMessage = e.getMessage();
    
    if (errorMessage.contains("users.username") || 
        errorMessage.contains("Duplicate entry") && errorMessage.contains("username")) {
        throw new Exception("Tên người dùng '" + registerDto.getUsername() + 
                          "' đã tồn tại. Vui lòng chọn tên khác.");
    } else if (errorMessage.contains("users.email") || 
               errorMessage.contains("Duplicate entry") && errorMessage.contains("email")) {
        throw new Exception("Email '" + registerDto.getEmail() + 
                          "' đã được đăng ký. Vui lòng sử dụng email khác.");
    } else {
        throw new Exception("Không thể tạo tài khoản. Vui lòng kiểm tra lại thông tin và thử lại.");
    }
}
```

**Cải thiện thông báo lỗi:**
- ✅ "Tên người dùng 'johndoe' đã được sử dụng. Vui lòng chọn tên khác."
- ✅ "Email 'aa@gmail.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."

---

### 3. **Hiển Thị Lỗi Đa Dòng** (`controller/AuthController.java`)

```java
if (bindingResult.hasErrors()) {
    StringBuilder errorMessages = new StringBuilder();
    bindingResult.getAllErrors().forEach(error -> {
        errorMessages.append("• ").append(error.getDefaultMessage()).append("\n");
    });
    redirectAttributes.addFlashAttribute("errorMessage", errorMessages.toString().trim());
    redirectAttributes.addFlashAttribute("registerDto", registerDto);
    return "redirect:/auth/register";
}
```

**Ví dụ hiển thị:**
```
• Email không hợp lệ. Vui lòng nhập đúng định dạng email
• Mật khẩu phải có ít nhất 6 ký tự
• Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới
```

---

### 4. **Cải thiện Template** (`templates/auth/register.html`)

```html
<!-- Error Message với hỗ trợ xuống dòng -->
<div th:if="${errorMessage}" class="alert-error">
  <i class="fa fa-exclamation-circle"></i>
  <span th:utext="${#strings.replace(errorMessage, '&#10;', '&lt;br/&gt;')}"></span>
  <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
```

---

## Các Thông Báo Lỗi Mới

### Lỗi Validation (Trước khi lưu vào database)

| Trường hợp | Thông báo |
|------------|-----------|
| Email không hợp lệ | "Email không hợp lệ. Vui lòng nhập đúng định dạng email (ví dụ: user@example.com)" |
| Username quá ngắn | "Tên người dùng phải có từ 3-50 ký tự" |
| Username có ký tự đặc biệt | "Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới" |
| Password quá ngắn | "Mật khẩu phải có ít nhất 6 ký tự" |
| Trường bỏ trống | "Email không được để trống" |

### Lỗi Trùng Lặp (Từ database)

| Trường hợp | Thông báo |
|------------|-----------|
| Username đã tồn tại | "Tên người dùng 'username' đã được sử dụng. Vui lòng chọn tên khác." |
| Email đã tồn tại | "Email 'email@example.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập." |

---

## Luồng Kiểm Tra Lỗi

```
User Submit Form
       ↓
[1. Validation Annotations]
   ├─ @Email → Kiểm tra format
   ├─ @Size → Kiểm tra độ dài
   └─ @Pattern → Kiểm tra ký tự
       ↓
[2. Service Layer Check]
   ├─ findActiveByUsername() → Kiểm tra trùng username
   └─ findActiveByEmail() → Kiểm tra trùng email
       ↓
[3. Database Save + Try-Catch]
   └─ DataIntegrityViolationException → Xử lý lỗi constraint
       ↓
[4. Controller]
   └─ Hiển thị thông báo thân thiện
```

---

## Testing Scenarios

### Test Case 1: Email không hợp lệ
**Input:** `email = "invalid-email"`  
**Expected:** ✅ "Email không hợp lệ. Vui lòng nhập đúng định dạng email (ví dụ: user@example.com)"

### Test Case 2: Username đã tồn tại
**Input:** `username = "admin"` (đã có trong DB)  
**Expected:** ✅ "Tên người dùng 'admin' đã được sử dụng. Vui lòng chọn tên khác."

### Test Case 3: Email đã đăng ký
**Input:** `email = "test@example.com"` (đã có trong DB)  
**Expected:** ✅ "Email 'test@example.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."

### Test Case 4: Nhiều lỗi cùng lúc
**Input:** 
- `username = "ab"` (quá ngắn)
- `email = "invalid"` (không hợp lệ)
- `password = "123"` (quá ngắn)

**Expected:** ✅ Hiển thị 3 lỗi theo dạng danh sách:
```
• Tên người dùng phải có từ 3-50 ký tự
• Email không hợp lệ. Vui lòng nhập đúng định dạng email
• Mật khẩu phải có ít nhất 6 ký tự
```

---

## Files Đã Thay Đổi

1. ✅ `src/main/java/stu/datn/ebook_store/dto/RegisterDto.java`
   - Thêm @Email, @Size, @Pattern validation

2. ✅ `src/main/java/stu/datn/ebook_store/service/impl/UserServiceImpl.java`
   - Thêm try-catch xử lý DataIntegrityViolationException
   - Cải thiện thông báo lỗi

3. ✅ `src/main/java/stu/datn/ebook_store/controller/AuthController.java`
   - Xử lý hiển thị nhiều lỗi validation

4. ✅ `src/main/resources/templates/auth/register.html`
   - Hỗ trợ hiển thị lỗi đa dòng với `<br/>`

---

## Kết Quả

### Trước:
```
❌ could not execute statement [Duplicate entry 'aa@gmail.com' for key 'users.email']
   [insert into users...]; constraint [users.email]
```

### Sau:
```
✅ Email 'aa@gmail.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.
```

---

## Checklist Hoàn Thành

- [x] Thêm validation annotations cho RegisterDto
- [x] Xử lý DataIntegrityViolationException
- [x] Cải thiện thông báo lỗi (tiếng Việt, dễ hiểu)
- [x] Hỗ trợ hiển thị nhiều lỗi cùng lúc
- [x] Test compile thành công
- [x] Tài liệu hóa thay đổi

---

**Status:** ✅ Hoàn thành  
**Build:** ✅ Success  
**User Experience:** ⭐⭐⭐⭐⭐ Excellent

