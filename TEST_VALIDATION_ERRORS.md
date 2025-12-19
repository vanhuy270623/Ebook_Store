# Test Cases - Validation Errors

## Cách Test Thủ Công

### 1. Test Email Không Hợp Lệ
```
Bước 1: Truy cập http://localhost:8080/auth/register
Bước 2: Nhập:
  - Username: testuser
  - Email: invalid-email
  - Password: 123456
Bước 3: Click "Đăng ký"

Kết quả mong đợi:
✅ "Email không hợp lệ. Vui lòng nhập đúng định dạng email (ví dụ: user@example.com)"
```

### 2. Test Username Quá Ngắn
```
Bước 1: Truy cập http://localhost:8080/auth/register
Bước 2: Nhập:
  - Username: ab
  - Email: test@gmail.com
  - Password: 123456
Bước 3: Click "Đăng ký"

Kết quả mong đợi:
✅ "Tên người dùng phải có từ 3-50 ký tự"
```

### 3. Test Username Có Ký Tự Đặc Biệt
```
Bước 1: Truy cập http://localhost:8080/auth/register
Bước 2: Nhập:
  - Username: test@user
  - Email: test@gmail.com
  - Password: 123456
Bước 3: Click "Đăng ký"

Kết quả mong đợi:
✅ "Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới"
```

### 4. Test Password Quá Ngắn
```
Bước 1: Truy cập http://localhost:8080/auth/register
Bước 2: Nhập:
  - Username: testuser
  - Email: test@gmail.com
  - Password: 123
Bước 3: Click "Đăng ký"

Kết quả mong đợi:
✅ "Mật khẩu phải có ít nhất 6 ký tự"
```

### 5. Test Email Đã Được Đăng Ký
```
Bước 1: Đăng ký tài khoản lần 1:
  - Username: user1
  - Email: duplicate@gmail.com
  - Password: 123456

Bước 2: Đăng ký tài khoản lần 2 (email trùng):
  - Username: user2
  - Email: duplicate@gmail.com
  - Password: 123456

Kết quả mong đợi:
✅ "Email 'duplicate@gmail.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."

❌ KHÔNG hiển thị:
"could not execute statement [Duplicate entry 'duplicate@gmail.com' for key 'users.email']..."
```

### 6. Test Username Đã Tồn Tại
```
Bước 1: Đăng ký tài khoản lần 1:
  - Username: testuser
  - Email: test1@gmail.com
  - Password: 123456

Bước 2: Đăng ký tài khoản lần 2 (username trùng):
  - Username: testuser
  - Email: test2@gmail.com
  - Password: 123456

Kết quả mong đợi:
✅ "Tên người dùng 'testuser' đã được sử dụng. Vui lòng chọn tên khác."
```

### 7. Test Nhiều Lỗi Cùng Lúc
```
Bước 1: Truy cập http://localhost:8080/auth/register
Bước 2: Nhập:
  - Username: ab
  - Email: invalid
  - Password: 12
Bước 3: Click "Đăng ký"

Kết quả mong đợi (hiển thị 3 lỗi):
✅ • Tên người dùng phải có từ 3-50 ký tự
✅ • Email không hợp lệ. Vui lòng nhập đúng định dạng email (ví dụ: user@example.com)
✅ • Mật khẩu phải có ít nhất 6 ký tự
```

### 8. Test Đăng Ký Thành Công
```
Bước 1: Truy cập http://localhost:8080/auth/register
Bước 2: Nhập:
  - Username: newuser123
  - Email: newuser@gmail.com
  - Password: password123
Bước 3: Tick checkbox "Tôi đồng ý với Điều khoản"
Bước 4: Click "Đăng ký"

Kết quả mong đợi:
✅ Chuyển đến trang login
✅ Hiển thị: "✅ Đăng ký thành công! Vui lòng đăng nhập để tiếp tục."
```

---

## So Sánh Trước và Sau

### ❌ TRƯỚC (Thông báo kỹ thuật, khó hiểu)
```
could not execute statement [Duplicate entry 'aa@gmail.com' for key 'users.email'] 
[insert into users (account_locked_reason,avatar_url,created_at,deleted_at,
device_violation_count,email,full_name,is_active,is_verified,last_login,
locked_at,locked_until,password_hash,phone,preferred_reading_mode,restored_at,
role_id,updated_at,username,user_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)]; 
SQL [insert into users...]; constraint [users.email]
```

### ✅ SAU (Thông báo thân thiện, dễ hiểu)
```
Email 'aa@gmail.com' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.
```

---

## Checklist Testing

- [ ] Test 1: Email không hợp lệ
- [ ] Test 2: Username quá ngắn
- [ ] Test 3: Username có ký tự đặc biệt
- [ ] Test 4: Password quá ngắn
- [ ] Test 5: Email đã được đăng ký
- [ ] Test 6: Username đã tồn tại
- [ ] Test 7: Nhiều lỗi cùng lúc
- [ ] Test 8: Đăng ký thành công

---

## Lưu Ý Khi Test

1. **Clear cache trình duyệt** trước khi test
2. **Kiểm tra database** để đảm bảo username/email chưa tồn tại
3. **Test trên nhiều trình duyệt**: Chrome, Firefox, Edge
4. **Kiểm tra responsive**: Mobile, Tablet, Desktop
5. **Test performance**: Thời gian phản hồi < 2 giây

---

## Báo Lỗi

Nếu phát hiện lỗi, vui lòng ghi nhận:
- URL trang
- Dữ liệu input
- Thông báo lỗi nhận được
- Thông báo lỗi mong đợi
- Screenshot (nếu có)

