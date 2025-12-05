# User Activity Timeline - User View Page

## Overview
Phần **Lịch sử hoạt động** trong trang chi tiết người dùng đã được cập nhật để hiển thị đầy đủ các sự kiện quan trọng trong vòng đời của một tài khoản người dùng.

---

## 📊 Timeline Structure

Timeline được sắp xếp theo thứ tự **ngược thời gian** (mới nhất → cũ nhất):

### 1. **Xóa tài khoản** 🔴 (nếu có)
**Điều kiện:** `deletedAt != null`

**Hiển thị:**
- 🗓️ Ngày xóa (label màu đỏ)
- ⏰ Giờ xóa chính xác
- ⚠️ Alert box màu đỏ cảnh báo
- 💡 Thông tin về khả năng khôi phục

**Mục đích:**
- Làm nổi bật trạng thái đã xóa
- Cung cấp thông tin cho admin về thời điểm xóa
- Nhắc nhở về khả năng khôi phục

```html
Icon: fa-trash (màu đỏ)
Header: "Tài khoản đã bị xóa"
Style: Background đỏ, nổi bật
```

---

### 2. **Cập nhật thông tin** 🔵 (nếu có)
**Điều kiện:** 
- `updatedAt != null`
- `updatedAt != createdAt` (đã có sửa đổi)
- `deletedAt == null` (chưa bị xóa)

**Hiển thị:**
- 🗓️ Ngày cập nhật gần nhất
- ⏰ Giờ cập nhật
- 📝 Thông báo về việc cập nhật

**Mục đích:**
- Track thời điểm thay đổi thông tin
- Giúp admin biết tài khoản có hoạt động

```html
Icon: fa-edit (màu xanh dương)
Header: "Cập nhật thông tin"
```

---

### 3. **Đăng nhập lần cuối** 🟢 (nếu có)
**Điều kiện:** `lastLogin != null`

**Hiển thị:**
- 🗓️ Ngày đăng nhập
- ⏰ Giờ đăng nhập chính xác
- 👤 Thông báo đăng nhập

**Mục đích:**
- Hiển thị hoạt động gần đây
- Giúp xác định tài khoản có active không
- Hỗ trợ phát hiện tài khoản không dùng

```html
Icon: fa-sign-in (màu xanh lá)
Header: "Đăng nhập lần cuối"
```

---

### 4. **Trạng thái tài khoản** ⚙️ (luôn có)
**Điều kiện:** Luôn hiển thị

**Hiển thị:**
- ✅ Đang hoạt động (màu xanh)
  - Badge: Đang hoạt động bình thường
  - Badge: Đã/Chưa xác thực email
- 🔒 Đã bị khóa (màu đỏ)
  - Alert box cảnh báo
  - Thông tin về hạn chế truy cập

**Mục đích:**
- Hiển thị rõ trạng thái hiện tại
- Cung cấp thông tin về quyền truy cập
- Visual indicator cho admin

```html
Icon: 
  - fa-check-circle (xanh) nếu active
  - fa-lock (đỏ) nếu bị khóa
Header: "Tài khoản đang hoạt động/đã bị khóa"
```

---

### 5. **Tạo tài khoản** 🆕 (luôn có)
**Điều kiện:** `createdAt != null` (luôn có)

**Hiển thị:**
- 🗓️ Ngày tạo tài khoản
- ⏰ Giờ tạo chính xác
- 👤 Vai trò được gán
- 🆔 User ID
- 🔖 Username

**Mục đích:**
- Điểm bắt đầu của timeline
- Thông tin cơ bản về tài khoản
- Reference point cho các sự kiện khác

```html
Icon: fa-user-plus (màu xanh nhạt)
Header: "Tạo tài khoản"
Details: Role, User ID, Username
```

---

## 🎨 Visual Design

### Color Coding
| Sự kiện | Màu | Icon | Ý nghĩa |
|---------|-----|------|---------|
| Xóa tài khoản | 🔴 Đỏ | fa-trash | Nguy hiểm/Đã xóa |
| Cập nhật | 🔵 Xanh dương | fa-edit | Thông tin/Sửa đổi |
| Đăng nhập | 🟢 Xanh lá | fa-sign-in | Hoạt động/Thành công |
| Active | 🟢 Xanh lá | fa-check-circle | An toàn/Hoạt động |
| Khóa | 🔴 Đỏ | fa-lock | Cảnh báo/Bị khóa |
| Tạo | 🔵 Xanh nhạt | fa-user-plus | Khởi đầu |

### Timeline Layout
```
┌─────────────────────────────────────────┐
│  🔴 04/12/2025  ← Time Label (Đỏ)      │
├─────────────────────────────────────────┤
│ 🗑️ 10:30  Tài khoản đã bị xóa          │
│    ⚠️ Alert: Có thể khôi phục           │
├─────────────────────────────────────────┤
│  🔵 03/12/2025  ← Time Label (Xanh)    │
├─────────────────────────────────────────┤
│ ✏️ 14:30  Cập nhật thông tin            │
│    Thông tin tài khoản đã được cập nhật │
├─────────────────────────────────────────┤
│  🟢 02/12/2025  ← Time Label (Xanh lá)  │
├─────────────────────────────────────────┤
│ 🚪 09:15  Đăng nhập lần cuối            │
│    Người dùng đã đăng nhập vào hệ thống │
├─────────────────────────────────────────┤
│  ⚙️ Trạng thái hiện tại                │
├─────────────────────────────────────────┤
│ ✅ Tài khoản đang hoạt động             │
│    ✓ Hoạt động  ⚠️ Chưa xác thực       │
├─────────────────────────────────────────┤
│  📅 01/12/2025  ← Time Label           │
├─────────────────────────────────────────┤
│ 👤 08:00  Tạo tài khoản                 │
│    Role: USER                           │
│    ID: user_normal_01                   │
│    Username: johndoe                    │
├─────────────────────────────────────────┤
│ 🕐 ← End marker                         │
└─────────────────────────────────────────┘
```

---

## 🔄 Timeline Scenarios

### Scenario 1: User bình thường, đang hoạt động
```
Timeline hiển thị:
1. ⚙️ Trạng thái: Đang hoạt động (xanh)
2. 🚪 Đăng nhập lần cuối: 02/12/2025
3. 👤 Tạo tài khoản: 01/12/2025
```

### Scenario 2: User đã bị xóa
```
Timeline hiển thị:
1. 🗑️ Xóa tài khoản: 04/12/2025 (đỏ, nổi bật)
2. ✏️ Cập nhật: 03/12/2025
3. 🚪 Đăng nhập lần cuối: 02/12/2025
4. 👤 Tạo tài khoản: 01/12/2025
```

### Scenario 3: User bị khóa
```
Timeline hiển thị:
1. ⚙️ Trạng thái: Đã bị khóa (đỏ)
   └─ Alert: "Không thể đăng nhập"
2. 🚪 Đăng nhập lần cuối: 02/12/2025
3. 👤 Tạo tài khoản: 01/12/2025
```

### Scenario 4: User mới tạo, chưa đăng nhập
```
Timeline hiển thị:
1. ⚙️ Trạng thái: Đang hoạt động
   └─ Badge: "Chưa xác thực email"
2. 👤 Tạo tài khoản: 04/12/2025
```

---

## 💻 Technical Implementation

### Data Sources
```java
// User entity fields used:
- createdAt    // Thời điểm tạo
- updatedAt    // Thời điểm cập nhật gần nhất
- lastLogin    // Lần đăng nhập cuối
- deletedAt    // Thời điểm xóa (null = chưa xóa)
- isActive     // Trạng thái khóa/mở
- isVerified   // Trạng thái xác thực email
```

### Conditional Rendering
```html
<!-- Xóa tài khoản -->
<th:block th:if="${user.deletedAt != null}">
  <!-- Show deletion event -->
</th:block>

<!-- Cập nhật (chỉ khi có sửa đổi) -->
<th:block th:if="${updatedAt != createdAt && deletedAt == null}">
  <!-- Show update event -->
</th:block>

<!-- Đăng nhập -->
<th:block th:if="${user.lastLogin != null}">
  <!-- Show last login -->
</th:block>

<!-- Trạng thái (luôn hiển thị) -->
<li>
  <i th:classappend="${user.isActive ? 'bg-green' : 'bg-red'}"></i>
  <!-- Show current status -->
</li>

<!-- Tạo tài khoản (luôn hiển thị) -->
<li>
  <i class="fa fa-user-plus bg-aqua"></i>
  <!-- Show creation event -->
</li>
```

---

## 🎯 User Experience Goals

### For Administrators
1. **Quick Overview:** Hiểu ngay trạng thái tài khoản
2. **Activity Tracking:** Xem hoạt động gần đây
3. **Audit Trail:** Timeline đầy đủ cho việc kiểm tra
4. **Visual Clarity:** Màu sắc và icon rõ ràng

### For Support Staff
1. **Troubleshooting:** Xác định vấn đề nhanh chóng
2. **Account Status:** Biết user có thể đăng nhập không
3. **Recent Changes:** Track thay đổi gần đây
4. **Recovery Info:** Thông tin về khôi phục nếu cần

---

## 📈 Future Enhancements

### Phase 2 (Planned)
- [ ] **Activity Log Integration:** Kết nối với audit log
- [ ] **Admin Actions:** Hiển thị ai thực hiện thay đổi
- [ ] **Detailed Changes:** Chi tiết về thay đổi gì
- [ ] **Order History:** Timeline đơn hàng
- [ ] **Review Activity:** Timeline đánh giá

### Phase 3 (Ideas)
- [ ] **Email Events:** Gửi/nhận email xác thực
- [ ] **Password Changes:** Lịch sử đổi mật khẩu
- [ ] **Login Attempts:** Failed/successful logins
- [ ] **Profile Updates:** Chi tiết các trường thay đổi
- [ ] **Export Timeline:** PDF/CSV export

---

## 🐛 Known Limitations

### Current Limitations
1. **No detailed change tracking:** Không biết thay đổi field nào
2. **No admin attribution:** Không biết admin nào thực hiện
3. **Basic timestamps only:** Chỉ có timestamp, không có detail
4. **No filtering:** Không thể filter theo loại sự kiện

### Workarounds
1. Check application logs for detailed changes
2. Database audit tables for admin actions
3. Manual notes for important changes

---

## 🎓 Admin Training Notes

### What to Look For

**Healthy Account:**
```
✅ Last login: Recent (< 30 days)
✅ Status: Active + Verified
✅ No deletion marker
```

**Inactive Account:**
```
⚠️ Last login: > 90 days ago or never
⚠️ Status: Active but not verified
❓ Consider: Send reminder email
```

**Problematic Account:**
```
🔴 Status: Locked
🔴 Last login: Long ago
🔴 Multiple updates recently
❗ Action: Investigate reason for lock
```

**Deleted Account:**
```
🗑️ Deletion marker present
📅 Check deletion date
💭 Decision: Keep or restore?
```

---

## 📊 Metrics & Analytics

### Suggested Tracking
- Average time between creation and first login
- Percentage of users never logged in
- Average time accounts stay active before deletion
- Lock/unlock frequency
- Update frequency patterns

---

## 📞 Support

**For Questions:**
- Timeline not showing? Check User entity fields
- Missing events? Verify data in database
- Styling issues? Check CSS in view.html

**Resources:**
- User Entity: `entity/User.java`
- View Template: `templates/admin/users/view.html`
- Controller: `controller/admin/AdminUserController.java`

---

**Document Version:** 1.0  
**Last Updated:** December 4, 2025  
**Status:** ✅ Implemented  
**Author:** AI Assistant
