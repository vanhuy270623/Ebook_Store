# 🔄 Quick Guide: Soft Delete Feature

## TL;DR (Too Long, Didn't Read)

**Xóa user = Ẩn user, KHÔNG mất dữ liệu!**

---

## 🎯 3 điều cần biết

### 1️⃣ Khi XÓA user:
- ✅ User biến khỏi danh sách
- ✅ Không thể đăng nhập
- ✅ Dữ liệu VẪN tồn tại
- ✅ Có thể khôi phục

### 2️⃣ Để XEM user đã xóa:
```
☑️ Tick vào: "Hiển thị người dùng đã xóa"
```
*(Chỉ Root Admin)*

### 3️⃣ Để KHÔI PHỤC:
```
Click nút [↩️ Khôi phục] màu xanh lá
```

---

## 🚦 Quick Actions

### Xóa user
1. Tìm user cần xóa
2. Click nút 🗑️ (màu đỏ)
3. Confirm ✓

**Kết quả:** User biến mất (nhưng vẫn trong database)

---

### Xem user đã xóa
1. Vào trang Quản lý người dùng
2. Tick ☑️ "Hiển thị người dùng đã xóa"
3. User đã xóa = dòng màu đỏ nhạt

---

### Khôi phục user
**Cách 1: Từ danh sách**
1. Tick ☑️ hiển thị đã xóa
2. Tìm user (dòng đỏ)
3. Click [↩️ Khôi phục]

**Cách 2: Từ trang chi tiết**
1. Click 👁️ Xem user đã xóa
2. Click [↩️ Khôi phục người dùng]

---

## 🎨 Nhận biết user đã xóa

| Indicator | Ý nghĩa |
|-----------|---------|
| 🔴 Nền đỏ nhạt | User đã bị xóa |
| 🗑️ Badge đỏ | Label "Đã xóa" |
| 😶‍🌫️ Avatar mờ | Opacity 50% |
| 📅 Thời gian | Ngày giờ xóa |

---

## ⚡ Keyboard Shortcuts

*(Chưa có - tính năng tương lai)*

---

## ⚠️ LƯU Ý

### ✅ CÓ THỂ:
- Xóa user bất kỳ (trừ mình)
- Khôi phục bất cứ lúc nào
- Xem lịch sử đã xóa

### ❌ KHÔNG THỂ:
- Đăng ký lại email đã xóa
- Xóa chính mình
- Admin thường xóa admin khác

---

## 🆘 Troubleshooting

### Q: Không thấy checkbox "Hiển thị đã xóa"?
**A:** Bạn không phải Root Admin

### Q: Xóa nhầm user, phải làm sao?
**A:** Bình tĩnh! Khôi phục trong 2 phút:
1. Tick checkbox hiển thị đã xóa
2. Click khôi phục
3. Done! ✓

### Q: User đã xóa có chiếm chỗ không?
**A:** Có, nhưng ít. Lợi ích > Chi phí

### Q: Muốn xóa VĨNH VIỄN?
**A:** Liên hệ developer (cần truy cập database)

---

## 📱 Contact

**Cần hỗ trợ?**
- 📖 Xem: `ADMIN_USER_SOFT_DELETE_GUIDE.md`
- 🔧 Technical: `SOFT_DELETE_IMPLEMENTATION.md`
- 💬 Admin: Liên hệ IT Support

---

## 🎓 Training Video

*(Coming soon)*

---

**Version:** 1.0  
**Date:** 04/12/2025  
**Status:** ✅ Production Ready
