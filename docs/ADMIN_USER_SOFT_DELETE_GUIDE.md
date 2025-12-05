# Hướng dẫn sử dụng tính năng Soft Delete cho Admin

## Tổng quan
Hệ thống đã được nâng cấp với tính năng **Soft Delete (Xóa mềm)** cho quản lý người dùng. Khi admin xóa một người dùng, dữ liệu sẽ không bị xóa vĩnh viễn khỏi database mà chỉ được đánh dấu là "đã xóa" và có thể khôi phục.

## Các tính năng mới

### 1. Xóa người dùng (Soft Delete)
**Cách thực hiện:**
1. Vào trang **Quản lý người dùng** (`/admin/users`)
2. Tìm người dùng cần xóa
3. Click nút **[🗑️]** (Xóa) ở cột "Thao tác"
4. Xác nhận trong hộp thoại:
   ```
   Bạn có chắc muốn xóa user [username]?
   
   Lưu ý: Đây là xóa mềm, dữ liệu vẫn được lưu trữ 
   và có thể khôi phục sau này.
   ```

**Điều gì xảy ra khi xóa:**
- ✅ Người dùng biến mất khỏi danh sách chính
- ✅ Không thể đăng nhập với tài khoản đã xóa
- ✅ Không thể đăng ký lại với email/username đã xóa
- ✅ Dữ liệu vẫn được lưu trong database
- ✅ Có thể khôi phục bất cứ lúc nào

### 2. Xem người dùng đã xóa
**Chỉ dành cho Root Admin (user_admin_01)**

**Cách thực hiện:**
1. Vào trang **Quản lý người dùng**
2. Tích vào checkbox **"☑️ Hiển thị người dùng đã xóa"**
3. Danh sách sẽ tự động tải lại và hiển thị cả user đã xóa

**Nhận biết user đã xóa:**
- Dòng có màu nền đỏ nhạt
- Badge màu đỏ **"🗑️ Đã xóa"** bên cạnh tên
- Avatar mờ hơn (opacity 50%)
- Cột "Trạng thái xóa" hiển thị thời gian xóa

### 3. Khôi phục người dùng
**Chỉ dành cho Root Admin**

**Cách thực hiện - Từ danh sách:**
1. Bật chế độ "Hiển thị người dùng đã xóa"
2. Tìm người dùng cần khôi phục (dòng màu đỏ)
3. Click nút **[↩️ Khôi phục]** màu xanh lá
4. Xác nhận khôi phục

**Cách thực hiện - Từ trang chi tiết:**
1. Click vào nút **[👁️]** (Xem) của user đã xóa
2. Trong trang chi tiết, bạn sẽ thấy:
   - Thông báo màu đỏ: "Người dùng này đã bị xóa!"
   - Thông tin thời gian xóa
3. Click nút **[↩️ Khôi phục người dùng]** màu xanh lá
4. Xác nhận khôi phục

**Điều gì xảy ra khi khôi phục:**
- ✅ Người dùng xuất hiện lại trong danh sách chính
- ✅ Có thể đăng nhập trở lại
- ✅ Tất cả thông tin được giữ nguyên
- ✅ Trạng thái "is_active" không thay đổi

## Giao diện người dùng

### Danh sách người dùng (List View)
```
┌─────────────────────────────────────────────────────┐
│ Tìm kiếm: [__________] [🔍 Tìm kiếm] [🔄 Làm mới]  │
│ ☑️ Hiển thị người dùng đã xóa  [➕ Thêm mới]       │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ Avatar │ Thông tin │ Email & TT │ Vai trò │ Thao tác │
├──────────────────────────────────────────────────────┤
│ 🔵 │ John Doe      │ active  │ USER │ 👁️ ✏️ 🔒 🗑️   │
│        │ @john        │         │      │              │
├──────────────────────────────────────────────────────┤
│ 🔴 │ Jane Smith    │ locked  │ USER │ 👁️ ✏️ 🔓 🗑️   │
│  (opacity) │ 🗑️ Đã xóa │     │      │              │
│        │ @jane        │         │      │ [↩️ Khôi phục]│
└──────────────────────────────────────────────────────┘
```

### Trang chi tiết (View Page)
```
Khi user CHƯA BỊ XÓA:
┌─────────────────────────┐
│     [Avatar]           │
│   John Doe            │
│   👤 Người dùng       │
├─────────────────────────┤
│ 📧 Email: john@...    │
│ 📅 Ngày tạo: 01/12    │
│ ✅ Đang hoạt động     │
├─────────────────────────┤
│ [✏️ Chỉnh sửa]        │
│ [🔒 Khóa tài khoản]   │
└─────────────────────────┘

Khi user ĐÃ BỊ XÓA:
┌─────────────────────────┐
│ ⚠️ NGƯỜI DÙNG ĐÃ BỊ XÓA │
│ Dữ liệu vẫn được bảo toàn│
├─────────────────────────┤
│     [Avatar - mờ]      │
│   Jane Smith          │
│   👤 Người dùng       │
├─────────────────────────┤
│ 🗑️ TRẠNG THÁI: ĐÃ XÓA │
│ 📅 Xóa lúc: 03/12 10:30│
│ 📧 Email: jane@...    │
├─────────────────────────┤
│ [↩️ Khôi phục người dùng]│
└─────────────────────────┘
```

## Phân quyền

### Root Admin (user_admin_01)
- ✅ Xóa mọi người dùng (trừ chính mình)
- ✅ Xem danh sách user đã xóa
- ✅ Khôi phục user đã xóa
- ✅ Quản lý admin khác

### Admin thường
- ✅ Xóa user thường (không phải admin)
- ❌ Không thể xóa admin khác
- ❌ Không thể xem user đã xóa
- ❌ Không thể khôi phục user

## Lưu ý quan trọng

### ⚠️ Bảo mật
1. **Không thể đăng nhập:** User đã xóa không thể đăng nhập vào hệ thống
2. **Email/Username được bảo lưu:** Không thể đăng ký lại với thông tin đã xóa
3. **Dữ liệu tồn tại:** Foreign keys và quan hệ database vẫn được giữ nguyên

### 💡 Best Practices
1. **Trước khi xóa:**
   - Cân nhắc "Khóa tài khoản" thay vì xóa nếu chỉ muốn tạm thời vô hiệu hóa
   - Kiểm tra lịch sử đơn hàng và hoạt động của user

2. **Khi khôi phục:**
   - Kiểm tra lại thông tin trước khi khôi phục
   - Thông báo cho user nếu cần thiết

3. **Quản lý thường xuyên:**
   - Định kỳ review danh sách user đã xóa
   - Quyết định giữ lại hoặc hard delete nếu cần (cần developer)

## So sánh Soft Delete vs Hard Delete

| Tính năng | Soft Delete (✅ Hiện tại) | Hard Delete (❌ Cũ) |
|-----------|------------------------|-------------------|
| Dữ liệu | Vẫn tồn tại | Mất vĩnh viễn |
| Khôi phục | Có thể | Không thể |
| Foreign Keys | Không ảnh hưởng | Có thể lỗi |
| Audit Trail | Đầy đủ | Mất thông tin |
| Compliance | Đạt chuẩn | Không đạt |

## Kỹ thuật triển khai

### Database
- Thêm cột `deleted_at` (DATETIME, DEFAULT NULL)
- User bình thường: `deleted_at = NULL`
- User đã xóa: `deleted_at = '2025-12-04 10:30:00'`

### Backend
- Tất cả query tự động lọc `WHERE deleted_at IS NULL`
- Soft delete: `UPDATE users SET deleted_at = NOW() WHERE user_id = ?`
- Restore: `UPDATE users SET deleted_at = NULL WHERE user_id = ?`

### Frontend
- Checkbox toggle hiển thị user đã xóa
- Visual indicators (màu đỏ, badge, opacity)
- Nút khôi phục có điều kiện

## Câu hỏi thường gặp (FAQ)

**Q: Có thể xóa vĩnh viễn (hard delete) không?**
A: Hiện tại không có tính năng này trong UI. Cần liên hệ developer để thực hiện hard delete trực tiếp trên database nếu thật sự cần thiết.

**Q: User đã xóa có chiếm chỗ trong hệ thống không?**
A: Có, nhưng không đáng kể. Lợi ích về bảo mật và khả năng khôi phục cao hơn nhiều.

**Q: Làm sao để xóa vĩnh viễn sau một thời gian?**
A: Cần tạo scheduled job tự động hard delete user đã xóa quá X ngày (tính năng tương lai).

**Q: Admin bị xóa nhầm phải làm sao?**
A: Root admin có thể khôi phục ngay lập tức từ danh sách hoặc trang chi tiết.

---

**Tài liệu này được cập nhật:** 04/12/2025
**Phiên bản hệ thống:** v2.0 (Soft Delete Implementation)
