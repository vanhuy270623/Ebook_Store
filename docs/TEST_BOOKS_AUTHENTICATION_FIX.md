# Hướng dẫn Test Fix Authentication cho /books Routes

## Bước 1: Restart ứng dụng
```bash
# Dừng ứng dụng nếu đang chạy (Ctrl+C)
# Sau đó chạy lại:
mvn spring-boot:run
```

## Bước 2: Test khi chưa đăng nhập

### Test 2.1: Truy cập trực tiếp /books/search
1. Mở trình duyệt ở chế độ Incognito/Private
2. Truy cập: `http://localhost:8080/books/search`
3. **Kỳ vọng**: Redirect về `/auth/login`
4. **Không được**: Hiển thị trang search mà không yêu cầu đăng nhập

### Test 2.2: Truy cập các trang khác trong /books
```
http://localhost:8080/books
http://localhost:8080/books/view/[book-id]
http://localhost:8080/books/category/[category-id]
http://localhost:8080/books/trending
http://localhost:8080/books/newest
http://localhost:8080/books/top-rated
```
**Tất cả đều phải redirect về `/auth/login`**

### Test 2.3: Truy cập /reading và /subscription
```
http://localhost:8080/reading/reader/[book-id]
http://localhost:8080/subscription/plans
http://localhost:8080/subscription/my-subscriptions
```
**Tất cả đều phải redirect về `/auth/login`**

## Bước 3: Test sau khi đăng nhập

### Test 3.1: Đăng nhập
1. Vào `http://localhost:8080/auth/login`
2. Đăng nhập với tài khoản USER:
   - Username: [điền username test]
   - Password: [điền password test]
3. **Kỳ vọng**: Đăng nhập thành công, redirect về dashboard hoặc trang chủ

### Test 3.2: Truy cập /books/search sau khi đăng nhập
1. Truy cập: `http://localhost:8080/books/search`
2. **Kỳ vọng**:
   - ✅ Trang hiển thị bình thường
   - ✅ Navbar hiển thị menu "Người dùng" với dropdown
   - ✅ Có thể thấy giỏ hàng (cart icon)
   - ✅ Session được duy trì

### Test 3.3: Kiểm tra session persistence
1. Ở trang `/books/search`, nhập từ khóa và tìm kiếm
2. Click vào một cuốn sách để xem chi tiết
3. Quay lại trang search
4. **Kỳ vọng**:
   - ✅ Vẫn còn đăng nhập
   - ✅ Không bị redirect về login
   - ✅ Menu "Người dùng" vẫn hiển thị đúng

### Test 3.4: Thêm sách vào giỏ hàng
1. Ở trang search, click "Thêm vào giỏ" cho một cuốn sách
2. **Kỳ vọng**:
   - ✅ Sách được thêm vào giỏ thành công
   - ✅ Có thông báo thành công
   - ✅ Vẫn ở lại trang search (hoặc redirect về đúng như thiết kế)
   - ✅ Session vẫn còn

### Test 3.5: Navigation giữa các trang
Kiểm tra di chuyển giữa các trang:
```
/books/search → /books/view/{id} → /user/dashboard → /books/search
```
**Kỳ vọng**: Session luôn được duy trì, không bị đăng xuất

## Bước 4: Test với role ADMIN

### Test 4.1: Đăng nhập với ADMIN
1. Đăng xuất tài khoản USER
2. Đăng nhập với tài khoản ADMIN
3. Truy cập `/books/search`
4. **Kỳ vọng**: ADMIN cũng có thể truy cập (vì config là `hasAnyRole("USER", "ADMIN")`)

## Bước 5: Test edge cases

### Test 5.1: Session timeout
1. Đăng nhập
2. Truy cập `/books/search`
3. Đợi session timeout (thường 30 phút)
4. Thử click vào một link hoặc submit form
5. **Kỳ vọng**: Redirect về `/auth/login` với message session hết hạn

### Test 5.2: Concurrent sessions
1. Đăng nhập ở tab 1
2. Mở tab 2, cũng đăng nhập
3. Truy cập `/books/search` ở cả 2 tab
4. **Kỳ vọng**: Cả 2 tab đều hoạt động bình thường

### Test 5.3: Direct URL access
1. Copy URL `/books/view/{book-id}` khi đã đăng nhập
2. Đăng xuất
3. Paste URL vào address bar
4. **Kỳ vọng**: Redirect về `/auth/login`
5. Sau khi đăng nhập, **có thể** redirect về URL ban đầu (tùy config)

## Bước 6: Kiểm tra Console Logs

Kiểm tra console logs để đảm bảo không có lỗi:
```
# Không nên thấy các lỗi như:
- Authentication failed
- Access denied
- Session not found
- CSRF token mismatch
```

## Bước 7: Kiểm tra Database

Nếu có session được lưu trong database:
```sql
-- Kiểm tra session của user
SELECT * FROM spring_session WHERE principal_name = '[username]';

-- Kiểm tra session attributes
SELECT * FROM spring_session_attributes;
```

## Checklist tổng quan

- [ ] Không thể truy cập `/books/**` khi chưa đăng nhập
- [ ] Redirect về `/auth/login` khi chưa đăng nhập
- [ ] Sau khi đăng nhập, truy cập được `/books/**` bình thường
- [ ] Session được duy trì khi navigate giữa các trang
- [ ] Navbar hiển thị đúng thông tin user
- [ ] Giỏ hàng hoạt động bình thường
- [ ] Thêm sách vào giỏ không bị mất session
- [ ] ADMIN cũng truy cập được
- [ ] Session timeout xử lý đúng
- [ ] Không có lỗi trong console logs

## Nếu vẫn gặp vấn đề

### Vấn đề 1: Vẫn không redirect về login
**Nguyên nhân có thể**:
- SecurityConfig chưa được load
- Application chưa restart
- Cache browser

**Giải pháp**:
1. Restart application
2. Clear browser cache
3. Kiểm tra SecurityConfig đã compile đúng chưa

### Vấn đề 2: Session bị mất sau khi login
**Nguyên nhân có thể**:
- Session management config sai
- Cookie không được set
- CSRF token issue

**Giải pháp**:
1. Kiểm tra browser cookies
2. Kiểm tra CSRF configuration
3. Kiểm tra session timeout config

### Vấn đề 3: Thêm vào giỏ hàng bị lỗi
**Nguyên nhân có thể**:
- CSRF token không được gửi
- Session không có trong request

**Giải pháp**:
1. Kiểm tra form có `th:name="${_csrf.parameterName}"` và `th:value="${_csrf.token}"`
2. Kiểm tra CartController có xử lý authentication đúng không

## Debug Tips

### Bật debug log cho Security
Thêm vào `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Kiểm tra request headers
Sử dụng Browser DevTools > Network tab để xem:
- Cookies
- Session ID
- CSRF token
- Authentication headers

### Test với curl
```bash
# Test khi chưa login
curl -I http://localhost:8080/books/search

# Test sau khi login (với session cookie)
curl -I -b "JSESSIONID=xxx" http://localhost:8080/books/search
```

