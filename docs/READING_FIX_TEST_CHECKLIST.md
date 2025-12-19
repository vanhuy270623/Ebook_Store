# 📋 CHECKLIST KIỂM TRA FIX READING AUTHENTICATION

**Ngày:** 14/12/2024  
**Fix:** Reading interface authentication và CSRF token

---

## 🚀 TRƯỚC KHI TEST

### 1. Khởi động lại server
```bash
# Stop server hiện tại (Ctrl+C)
# Sau đó start lại:
cd C:\Projects\Ebook_Store
.\mvnw.cmd spring-boot:run
```

### 2. Clear browser cache
- Mở browser (Chrome/Edge/Firefox)
- Nhấn `Ctrl + Shift + Delete`
- Chọn: Cookies, Cached images and files
- Clear data

### 3. Mở Developer Tools
- Nhấn `F12` hoặc `Ctrl + Shift + I`
- Chọn tab **Console**
- Chọn tab **Network**

---

## ✅ TEST CASES

### Test 1: Đăng nhập và Authentication
- [ ] 1. Truy cập: `http://localhost:2706`
- [ ] 2. Đăng nhập với tài khoản user/admin
- [ ] 3. Kiểm tra session cookie `JSESSIONID` trong DevTools > Application > Cookies
- [ ] 4. Verify: Cookie có giá trị và không expired

**Expected:** Đăng nhập thành công, có session cookie

---

### Test 2: Mở PDF Reader
- [ ] 1. Click vào một cuốn sách PDF bất kỳ
- [ ] 2. Click "Đọc sách"
- [ ] 3. Quan sát Console - không có lỗi
- [ ] 4. Quan sát Network tab - tất cả requests status 200

**Expected:** 
- PDF load thành công
- Console: `✅ PDF.js loaded successfully`
- Network: Tất cả requests 200 OK
- Không có lỗi 401/403

---

### Test 3: CSRF Token trong PDF Reader
- [ ] 1. Mở PDF reader
- [ ] 2. Mở Console, gõ:
```javascript
console.log('CSRF Token:', getCsrfToken());
console.log('CSRF Header:', getCsrfHeader());
```
- [ ] 3. Verify: Cả 2 đều có giá trị (không empty)

**Expected:**
```
CSRF Token: abc123def456...
CSRF Header: X-CSRF-TOKEN
```

---

### Test 4: Save Reading Progress (PDF)
- [ ] 1. Đọc PDF, chuyển sang trang 2, 3, 4...
- [ ] 2. Quan sát Console sau 30 giây
- [ ] 3. Tìm log: `Progress saved successfully - page: X`
- [ ] 4. Quan sát Network > Filter "progress"
- [ ] 5. Click request, xem Headers tab
- [ ] 6. Verify: Có header `X-CSRF-TOKEN: xxx`

**Expected:**
- Console: `Progress saved successfully`
- Network: POST `/reading/api/progress/book_XX` → 200 OK
- Request headers có CSRF token

---

### Test 5: Add Bookmark (PDF)
- [ ] 1. Đang đọc PDF, click "Thêm Bookmark"
- [ ] 2. Nhập note: "Test bookmark 1"
- [ ] 3. Quan sát Network tab
- [ ] 4. Verify: POST request status 200

**Expected:**
- Alert: "✅ Bookmark đã lưu!"
- Network: POST `/reading/api/bookmarks/book_XX` → 200 OK
- Console: `=== SAVING BOOKMARK ===`

---

### Test 6: Load Bookmarks (PDF)
- [ ] 1. Click icon "Bookmarks" trên toolbar
- [ ] 2. Sidebar mở ra bên phải
- [ ] 3. Verify: Danh sách bookmarks hiện ra

**Expected:**
- Sidebar hiển thị bookmarks đã lưu
- Network: GET `/reading/api/bookmarks/book_XX` → 200 OK
- Mỗi bookmark có: số trang, note, timestamp

---

### Test 7: Delete Bookmark (PDF)
- [ ] 1. Mở bookmarks sidebar
- [ ] 2. Click icon trash trên một bookmark
- [ ] 3. Confirm xóa
- [ ] 4. Verify: Bookmark biến mất

**Expected:**
- Bookmark bị xóa khỏi danh sách
- Network: DELETE `/reading/api/bookmarks/book_XX/bookmark_id` → 200 OK

---

### Test 8: Mở EPUB Reader
- [ ] 1. Click vào một cuốn sách EPUB
- [ ] 2. Click "Đọc sách"
- [ ] 3. Quan sát Console
- [ ] 4. Quan sát Network tab

**Expected:**
- EPUB load thành công
- Console: `✅ ePub.js loaded successfully`
- Console: `✅ Spine loaded: X items`
- Network: Tất cả requests 200 OK

---

### Test 9: CSRF Token trong EPUB Reader
- [ ] 1. Mở EPUB reader
- [ ] 2. Mở Console, gõ:
```javascript
console.log('CSRF Token:', getCsrfToken());
console.log('CSRF Header:', getCsrfHeader());
```
- [ ] 3. Verify: Cả 2 đều có giá trị

**Expected:** Tương tự Test 3

---

### Test 10: Save Progress (EPUB)
- [ ] 1. Đọc EPUB, chuyển trang/chapter
- [ ] 2. Đợi 30 giây
- [ ] 3. Quan sát Console
- [ ] 4. Tìm log: `Progress saved: X%`

**Expected:**
- Console: `=== SAVING EPUB PROGRESS ===`
- Console: `Percentage: X% CFI: epubcfi(...)`
- Network: POST `/reading/api/progress/book_XX` → 200 OK

---

### Test 11: Add Bookmark (EPUB)
- [ ] 1. Click "Thêm Bookmark"
- [ ] 2. Nhập note: "Test EPUB bookmark"
- [ ] 3. Verify: Alert thành công

**Expected:**
- Alert: "✅ Bookmark đã lưu!"
- Network: POST `/reading/api/bookmarks/book_XX` → 200 OK
- Console: `=== SAVING EPUB BOOKMARK ===`

---

### Test 12: Jump to Bookmark (EPUB)
- [ ] 1. Mở bookmarks sidebar
- [ ] 2. Click vào một bookmark
- [ ] 3. Verify: EPUB jump đến đúng vị trí

**Expected:**
- EPUB chuyển đến CFI location đã lưu
- Console: `✅ Jumped to bookmark successfully`

---

### Test 13: Toggle Dark Mode
- [ ] 1. Trong reader (PDF hoặc EPUB)
- [ ] 2. Click "Chế độ tối"
- [ ] 3. Quan sát Network tab
- [ ] 4. Refresh page - dark mode có persist?

**Expected:**
- Background chuyển màu tối
- Network: POST `/reading/api/toggle-mode` → 200 OK
- Button text thay đổi: "Chế độ sáng"

---

### Test 14: Server Logs
- [ ] 1. Mở terminal chạy server
- [ ] 2. Tìm logs liên quan đến reading
- [ ] 3. Verify: KHÔNG có lỗi "User not found"

**Expected:**
```
✅ KHÔNG CÒN:
Error loading reader for book book_XX: User not found

✅ CHỈ CÓ:
DEBUG ReadingController - Loading book for user: user@example.com
DEBUG ReadingController - Progress saved for book_XX
```

---

### Test 15: Session Persistence
- [ ] 1. Đang đọc sách
- [ ] 2. Mở tab mới, truy cập `/user/profile`
- [ ] 3. Quay lại tab đọc sách
- [ ] 4. Chuyển trang
- [ ] 5. Verify: Vẫn hoạt động bình thường

**Expected:**
- Session vẫn còn valid
- Không bị logout
- Progress vẫn lưu được

---

### Test 16: Session Expired
- [ ] 1. Đang đọc sách
- [ ] 2. Đợi session timeout (default 30 phút)
- [ ] 3. Hoặc: Xóa cookie JSESSIONID trong DevTools
- [ ] 4. Chuyển trang
- [ ] 5. Quan sát Network

**Expected:**
- API calls trả về 401 Unauthorized
- Browser redirect về `/auth/login`
- Console có message: "Phiên đăng nhập đã hết hạn"

---

### Test 17: Cross-Site Requests (Security)
- [ ] 1. Mở Console
- [ ] 2. Thử gọi API từ external domain:
```javascript
fetch('http://localhost:2706/reading/api/progress/book_1', {
    method: 'POST',
    headers: { 'X-CSRF-TOKEN': 'fake-token' }
})
.then(r => console.log('Status:', r.status))
.catch(e => console.log('Error:', e));
```
- [ ] 3. Verify: Request bị reject

**Expected:**
- CORS error hoặc 403 Forbidden
- CSRF protection hoạt động

---

### Test 18: Multiple Users
- [ ] 1. Đăng nhập user A
- [ ] 2. Đọc sách, lưu bookmark
- [ ] 3. Logout
- [ ] 4. Đăng nhập user B
- [ ] 5. Đọc cùng sách
- [ ] 6. Verify: Không thấy bookmarks của user A

**Expected:**
- Bookmarks riêng biệt cho từng user
- Progress riêng biệt cho từng user

---

## 🐛 COMMON ISSUES

### Issue 1: CSRF Token null
**Triệu chứng:** `getCsrfToken()` trả về empty string

**Fix:**
```
1. View Page Source
2. Tìm: <meta name="_csrf" content=
3. Nếu không có -> Server chưa render
4. Restart server
```

### Issue 2: 403 Forbidden
**Triệu chứng:** API calls trả về 403

**Fix:**
```
1. Kiểm tra CSRF token có được gửi trong headers
2. Kiểm tra token match với server
3. Clear cookies và login lại
```

### Issue 3: User not found vẫn xuất hiện
**Triệu chứng:** Logs vẫn có lỗi "User not found"

**Fix:**
```
1. Verify SecurityConfig đã có /reading/**
2. Restart server (mvnw spring-boot:run)
3. Clear browser cache
4. Login lại
```

---

## 📊 SUMMARY

Tổng số test cases: **18**

**Cần pass:** Tối thiểu 15/18 (83%)

**Critical tests (phải pass):**
- ✅ Test 1: Authentication
- ✅ Test 2: Open PDF Reader
- ✅ Test 3: CSRF Token
- ✅ Test 4: Save Progress
- ✅ Test 8: Open EPUB Reader
- ✅ Test 14: Server Logs (no errors)

---

## ✨ NEXT STEPS

Sau khi tất cả tests pass:

1. **Update documentation**
   - Cập nhật FLOW_07_READING_INTERFACE.md
   - Add screenshots nếu cần

2. **Commit changes**
   ```bash
   git add .
   git commit -m "Fix reading interface authentication and CSRF protection"
   ```

3. **Deploy to production**
   - Test trên staging environment trước
   - Monitor logs sau khi deploy

---

**Người test:** _________________  
**Ngày test:** _________________  
**Kết quả:** _________________

