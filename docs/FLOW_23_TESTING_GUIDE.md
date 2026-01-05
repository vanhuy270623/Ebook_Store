# Hướng dẫn Test - Chọn định dạng tải xuống

## 🎯 Mục tiêu
Kiểm tra chức năng cho phép người dùng chọn format (PDF/EPUB) khi tải xuống sách.

---

## 📋 Chuẩn bị

### 1. Dữ liệu Test
Cần có sách với các trường hợp sau:

#### Sách A: Chỉ có PDF
- Book ID: `book_001`
- isDownloadable: `true`
- BookAsset: 1 file PDF

#### Sách B: Chỉ có EPUB
- Book ID: `book_002`
- isDownloadable: `true`
- BookAsset: 1 file EPUB

#### Sách C: Có cả PDF và EPUB ⭐
- Book ID: `book_003`
- isDownloadable: `true`
- BookAsset: 2 files (PDF + EPUB)

#### Sách D: Không cho phép tải
- Book ID: `book_004`
- isDownloadable: `false`

### 2. User Accounts

#### User 1: Free User
- Username: `user_free`
- Subscription: None
- Purchased: Sách A (PURCHASE)

#### User 2: VIP User
- Username: `user_vip`
- Subscription: Active
- Purchased: None

#### User 3: Guest
- Not logged in

---

## 🧪 Test Cases

### TC-01: Tải xuống sách có 1 format (PDF)

**Điều kiện:**
- User đã mua Sách A (chỉ có PDF)
- User đã login

**Steps:**
1. Truy cập `/books/view/{bookId}` của Sách A
2. Click nút "Tải xuống sách đã mua"

**Expected:**
- ✅ Không hiện modal chọn format
- ✅ Tải trực tiếp file PDF
- ✅ Toast success: "Tải xuống thành công!"
- ✅ File PDF được download về máy

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-02: Tải xuống sách có 2 format (PDF + EPUB)

**Điều kiện:**
- User đã mua Sách C (có cả PDF và EPUB)
- User đã login

**Steps:**
1. Truy cập `/books/view/{bookId}` của Sách C
2. Click nút "Tải xuống sách đã mua"
3. Quan sát modal hiện ra

**Expected:**
- ✅ Modal "Chọn định dạng tải xuống" hiện ra
- ✅ Có 2 nút: "📄 PDF (x.x MB)" và "📚 EPUB (x.x MB)"
- ✅ Icon và file size hiển thị đúng
- ✅ Modal có thể đóng bằng nút X hoặc click outside

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-03: Chọn download PDF từ modal

**Điều kiện:**
- Tiếp tục từ TC-02
- Modal đang mở

**Steps:**
1. Click nút "📄 PDF"
2. Quan sát

**Expected:**
- ✅ Modal đóng lại ngay lập tức
- ✅ Loading indicator "Đang chuẩn bị tải xuống..." xuất hiện
- ✅ Request gửi đến: `/books/download/{bookId}?fileType=PDF`
- ✅ File PDF được download
- ✅ Toast success: "... (PDF) đã được tải về máy của bạn"

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-04: Chọn download EPUB từ modal

**Điều kiện:**
- Reset TC-02
- Modal đang mở

**Steps:**
1. Click nút "📚 EPUB"
2. Quan sát

**Expected:**
- ✅ Modal đóng lại
- ✅ Loading indicator xuất hiện
- ✅ Request gửi đến: `/books/download/{bookId}?fileType=EPUB`
- ✅ File EPUB được download
- ✅ Toast success: "... (EPUB) đã được tải về máy của bạn"

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-05: Tải sách FREE có nhiều format

**Điều kiện:**
- Sách có accessType = FREE
- Sách có cả PDF và EPUB
- User đã login

**Steps:**
1. Truy cập trang view sách FREE
2. Click nút "Tải xuống"
3. Chọn format trong modal
4. Confirm download

**Expected:**
- ✅ Modal hiện ra với 2 options
- ✅ Có thể chọn và tải xuống thành công
- ✅ Không cần mua sách

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-06: User chưa mua sách cố download

**Điều kiện:**
- Sách có accessType = PURCHASE
- User chưa mua sách này
- User đã login

**Steps:**
1. Truy cập trang view sách chưa mua
2. Quan sát UI

**Expected:**
- ✅ Không có nút "Tải xuống"
- ✅ Chỉ có nút "Thêm vào giỏ hàng"
- ✅ Có nút "Đọc sách" (đọc online)

**Alternative Test:** Nếu force gọi API
```
GET /books/download/{bookId}?fileType=PDF
```

**Expected:**
- ✅ Response: 403 Forbidden
- ✅ Header: `X-Download-Error: Bạn cần mua sách để tải xuống`

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-07: Guest user cố download

**Điều kiện:**
- User chưa login
- Truy cập sách FREE

**Steps:**
1. Logout (nếu đang login)
2. Truy cập sách FREE
3. Click nút "Tải xuống"

**Expected:**
- ✅ Response: 401 Unauthorized
- ✅ Toast error: "Bạn cần đăng nhập để tải xuống sách"
- ✅ Redirect to login page (optional)

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-08: Download file không tồn tại

**Điều kiện:**
- Book có record trong DB
- BookAsset có record fileType = PDF
- Nhưng file vật lý bị xóa khỏi server

**Steps:**
1. Login và mua sách
2. Admin xóa file vật lý (giữ DB record)
3. User click download PDF

**Expected:**
- ✅ Response: 404 Not Found
- ✅ Header: `X-Download-Error: File không tồn tại trên server`
- ✅ Toast error: "File không tồn tại trên server"
- ✅ Không download file rỗng

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-09: Download với fileType không hợp lệ

**Steps:**
1. Manually gọi API:
```
GET /books/download/{bookId}?fileType=INVALID
```

**Expected:**
- ✅ Response: 400 Bad Request
- ✅ Header: `X-Download-Error: Loại file không hợp lệ`
- ✅ Không crash server

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-10: Modal responsive trên mobile

**Điều kiện:**
- Open DevTools
- Toggle device toolbar
- Chọn iPhone 12 Pro

**Steps:**
1. Truy cập trang sách có 2 formats
2. Click nút download
3. Quan sát modal

**Expected:**
- ✅ Modal hiển thị đẹp, không bị overflow
- ✅ Buttons đủ lớn để tap dễ dàng (min 44px height)
- ✅ Text không bị cắt
- ✅ Close button dễ nhấn

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-11: Multiple rapid clicks

**Steps:**
1. Truy cập sách có 2 formats
2. Click nút download
3. Trong modal, rapid click nút PDF nhiều lần (5 lần)

**Expected:**
- ✅ Chỉ download 1 lần
- ✅ Không mở nhiều modal
- ✅ Không duplicate requests
- ✅ Button disable sau click (optional)

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-12: Network error handling

**Steps:**
1. Open DevTools → Network tab
2. Enable "Offline" mode
3. Click download button
4. Try to download

**Expected:**
- ✅ Toast error: "Lỗi khi tải xuống sách. Vui lòng thử lại sau"
- ✅ Không show loading vô hạn
- ✅ Error message rõ ràng

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-13: Filename encoding (Unicode)

**Điều kiện:**
- Sách có tên tiếng Việt có dấu
- VD: "40 Gương Thành Công - Dale Carnegie"

**Steps:**
1. Download sách
2. Kiểm tra filename downloaded

**Expected:**
- ✅ Filename: `40_Guong_Thanh_Cong_-_Dale_Carnegie.pdf`
- ✅ Không có ký tự lỗi: `%20`, `???`, `□□□`
- ✅ Dấu tiếng Việt được giữ hoặc normalize đúng

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-14: Large file download (>50MB)

**Điều kiện:**
- Upload sách có file >50MB

**Steps:**
1. Download large file
2. Monitor progress

**Expected:**
- ✅ Download thành công
- ✅ Không timeout
- ✅ Loading indicator show trong suốt quá trình
- ✅ File integrity (check MD5/SHA)

**Result:** ⬜ PASS / ⬜ FAIL

---

### TC-15: Browser compatibility

**Browsers to test:**
- ✅ Chrome (latest)
- ✅ Firefox (latest)
- ✅ Edge (latest)
- ✅ Safari (if available)

**Expected:**
- ✅ Modal hiển thị đúng trên tất cả browsers
- ✅ Download hoạt động
- ✅ Toast notifications work
- ✅ File save dialog appear

**Result:** ⬜ PASS / ⬜ FAIL

---

## 🔍 Kiểm tra chi tiết

### 1. Inspect Network Request

Khi click download PDF, kiểm tra trong DevTools → Network:

```
Request URL: http://localhost:8080/books/download/book_003?fileType=PDF
Request Method: GET
Status Code: 200 OK

Response Headers:
Content-Type: application/pdf
Content-Disposition: attachment; filename*=UTF-8''Ten_Sach.pdf
Content-Length: 2621440
```

### 2. Inspect Console

Không có error trong console:
- ✅ No JavaScript errors
- ✅ No 404 for assets
- ✅ No CORS errors

### 3. Inspect Modal HTML

Khi modal mở, inspect element:

```html
<div class="modal fade show" style="display: block;">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">...</div>
      <div class="modal-body">
        <div id="downloadOptions">
          <button class="btn btn-outline-primary btn-lg">
            <i class="fas fa-file-pdf text-danger"></i>
            <strong>PDF</strong>
            <span class="text-muted">(2.5 MB)</span>
          </button>
          <button class="btn btn-outline-primary btn-lg">
            <i class="fas fa-book text-success"></i>
            <strong>EPUB</strong>
            <span class="text-muted">(1.8 MB)</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</div>
```

---

## 📊 Test Report Template

```
===========================================
TEST REPORT - Download Format Selection
Date: ___________
Tester: ___________
===========================================

Environment:
- OS: Windows 11 / macOS / Linux
- Browser: Chrome 120.0.6099.109
- Server: localhost:8080
- Database: MySQL 8.0

Test Results:
TC-01: ☑ PASS  ☐ FAIL  Notes: _______
TC-02: ☑ PASS  ☐ FAIL  Notes: _______
TC-03: ☑ PASS  ☐ FAIL  Notes: _______
TC-04: ☑ PASS  ☐ FAIL  Notes: _______
TC-05: ☑ PASS  ☐ FAIL  Notes: _______
TC-06: ☑ PASS  ☐ FAIL  Notes: _______
TC-07: ☑ PASS  ☐ FAIL  Notes: _______
TC-08: ☑ PASS  ☐ FAIL  Notes: _______
TC-09: ☑ PASS  ☐ FAIL  Notes: _______
TC-10: ☑ PASS  ☐ FAIL  Notes: _______
TC-11: ☑ PASS  ☐ FAIL  Notes: _______
TC-12: ☑ PASS  ☐ FAIL  Notes: _______
TC-13: ☑ PASS  ☐ FAIL  Notes: _______
TC-14: ☑ PASS  ☐ FAIL  Notes: _______
TC-15: ☑ PASS  ☐ FAIL  Notes: _______

Total: __/15 passed

Bugs Found:
1. ___________________________
2. ___________________________

===========================================
```

---

## 🎬 Video Test Scenario

Record screen khi test để demo:

1. **Intro** (5s): Show trang sách
2. **Scenario 1** (15s): Click download → Modal hiện → Chọn PDF → Download thành công
3. **Scenario 2** (15s): Click download → Modal hiện → Chọn EPUB → Download thành công
4. **Scenario 3** (10s): Download sách chỉ có 1 format (không có modal)
5. **Error Scenario** (10s): Try download khi chưa login → Error message
6. **Outro** (5s): Close

Total: ~60 seconds

---

## ✅ Acceptance Criteria

Feature được accept khi:

- [ ] 100% test cases PASS
- [ ] Không có critical bugs
- [ ] UI responsive trên mobile
- [ ] Works trên Chrome, Firefox, Edge
- [ ] Performance: Modal load <100ms
- [ ] Download file <5MB trong <10s
- [ ] Code review approved
- [ ] Documentation complete

---

**Happy Testing! 🎉**

