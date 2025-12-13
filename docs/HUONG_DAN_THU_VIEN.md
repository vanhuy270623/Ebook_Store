# Hướng Dẫn Sử Dụng Thư Viện Cá Nhân (/user/library)

## 📚 Tổng Quan

Trang **Thư viện của tôi** (`/user/library`) là nơi tập trung quản lý tất cả sách của bạn, bao gồm:
- ✅ **Sách đang đọc**: Các cuốn sách bạn đang đọc dở, kèm tiến độ
- ✅ **Sách đã mua**: Tất cả sách bạn đã mua và sở hữu vĩnh viễn

## 🎯 Cách Sử Dụng

### Truy Cập Thư Viện
1. Đăng nhập vào tài khoản
2. Click vào menu người dùng (góc phải trên)
3. Chọn **"Thư viện"**
4. Hoặc truy cập trực tiếp: `https://your-domain.com/user/library`

### Xem Sách Đang Đọc
- Trang mặc định sẽ hiển thị tab **"Sách đang đọc"**
- Mỗi sách sẽ hiển thị:
  - Ảnh bìa và tên sách
  - **Thanh tiến độ** (đã đọc bao nhiêu %)
  - Vị trí đọc cuối cùng
  - Thời gian đọc gần nhất
  - Trạng thái: "Đang đọc", "Đã hoàn thành", "Chưa bắt đầu"
- Click **"Đọc tiếp"** để mở sách tại vị trí đã lưu

### Xem Sách Đã Mua
- Click vào tab **"Sách đã mua"**
- Hoặc truy cập: `/user/library?tab=purchased`
- Hiển thị tất cả sách đã thanh toán thành công
- Click **"Đọc ngay"** để bắt đầu đọc
- Click **"Chi tiết"** để xem thông tin sách

### Thống Kê
Ngay đầu trang hiển thị 2 con số quan trọng:
- 📖 **Sách đang đọc**: Tổng số sách bạn đang theo dõi
- 🛒 **Sách đã mua**: Tổng số sách bạn sở hữu

## 💡 Mẹo Sử Dụng

### Để đọc một cuốn sách
```
Thư viện → Tab "Sách đã mua" → Chọn sách → Click "Đọc ngay"
```

### Để tiếp tục đọc sách dở dang
```
Thư viện → Tab "Sách đang đọc" → Tìm sách → Click "Đọc tiếp"
```

### Nếu có nhiều sách
- Sử dụng **phân trang** ở cuối trang để duyệt qua các trang
- Mỗi trang hiển thị tối đa 12 cuốn sách

## 🔗 Liên Kết với Tính Năng Khác

### Sau khi mua sách (Flow 03)
```
Thanh toán thành công 
    ↓
Sách tự động xuất hiện trong tab "Sách đã mua" 
    ↓
Có thể đọc ngay
```

### Khi đọc sách (Flow 07)
```
Click "Đọc tiếp" từ Thư viện 
    ↓
Mở trình đọc PDF/EPUB 
    ↓
Hệ thống tự động lưu tiến độ 
    ↓
Cập nhật thanh progress trong "Sách đang đọc"
```

## ❓ Câu Hỏi Thường Gặp

### Q: Tại sao sách tôi mới mua không hiện?
**A**: Kiểm tra lại:
- Đơn hàng đã thanh toán **thành công** chưa?
- Vào `/user/orders` để xem trạng thái đơn hàng
- Nếu đơn hàng `COMPLETED` → Sách sẽ tự động hiện

### Q: Làm sao để xóa sách khỏi "Đang đọc"?
**A**: Hiện tại chưa có tính năng xóa. Danh sách "Đang đọc" tự động cập nhật khi bạn mở sách.

### Q: Sách miễn phí có hiện trong "Sách đã mua" không?
**A**: Không. Tab "Sách đã mua" chỉ hiển thị sách đã **thanh toán**. Sách miễn phí chỉ hiện trong "Sách đang đọc" khi bạn bắt đầu đọc.

### Q: Tiến độ đọc có đồng bộ giữa các thiết bị không?
**A**: Có! Tiến độ đọc được lưu trên server, nên bạn có thể tiếp tục đọc trên bất kỳ thiết bị nào.

## 🎨 Giao Diện

### Desktop
```
┌─────────────────────────────────────────┐
│  📊 Sách đang đọc: 15  │  🛒 Sách đã mua: 50  │
├─────────────────────────────────────────┤
│  [ Sách đang đọc ]  [ Sách đã mua ]     │
├─────────────────────────────────────────┤
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│  │ Sách │ │ Sách │ │ Sách │ │ Sách │  │
│  │  01  │ │  02  │ │  03  │ │  04  │  │
│  └──────┘ └──────┘ └──────┘ └──────┘  │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│  │ Sách │ │ Sách │ │ Sách │ │ Sách │  │
│  │  05  │ │  06  │ │  07  │ │  08  │  │
│  └──────┘ └──────┘ └──────┘ └──────┘  │
├─────────────────────────────────────────┤
│        ◄ 1 2 [3] 4 5 ►                  │
└─────────────────────────────────────────┘
```

### Mobile
- Responsive design
- Các card xếp theo chiều dọc
- Touch-friendly buttons

## 🚀 Điểm Nổi Bật

### So với /user/reading-history
| Tính năng | /user/reading-history | /user/library ⭐ |
|-----------|----------------------|------------------|
| Sách đang đọc | ✅ | ✅ |
| Sách đã mua | ❌ | ✅ |
| Thống kê tổng quan | ❌ | ✅ |
| Tab switching | ❌ | ✅ |
| Giao diện hiện đại | ⚠️ | ✅ |

### So với /user/orders
| Tính năng | /user/orders | /user/library ⭐ |
|-----------|--------------|------------------|
| Lịch sử đơn hàng | ✅ | ❌ |
| Xem chi tiết order | ✅ | ❌ |
| Thư viện sách | ❌ | ✅ |
| Focus vào việc đọc | ❌ | ✅ |

## 📱 Navigation Quick Reference

```
Navbar → Avatar Dropdown → "Thư viện"
                         → "Đơn hàng"
                         → "Lịch sử đọc" (legacy)
```

### Đề xuất sử dụng
- ✅ Dùng `/user/library` cho việc **quản lý và đọc sách**
- ✅ Dùng `/user/orders` cho việc **kiểm tra đơn hàng và hoá đơn**
- ⚠️ `/user/reading-history` là phiên bản cũ, nên dùng `/user/library`

## 🔧 Technical Info (cho Developers)

### Backend
- **Controller**: `UserController.java`
- **Method**: `library()`
- **Services**: `ReadingProgressService`, `OrderService`, `OrderItemService`
- **Entities**: `ReadingProgress`, `Order`, `OrderItem`, `Book`

### Frontend
- **Template**: `user/library.html`
- **CSS**: Bootstrap 5 + Custom styles
- **JavaScript**: Bootstrap bundle (for tabs, navigation)

### Endpoints liên quan
```
GET /user/library              -> Thư viện (mặc định: sách đang đọc)
GET /user/library?tab=reading  -> Sách đang đọc
GET /user/library?tab=purchased -> Sách đã mua
GET /reading/book/{bookId}     -> Mở sách để đọc
GET /books/view/{bookId}       -> Xem chi tiết sách
```

---

## ✨ Tóm Tắt

**Thư viện của tôi** là trung tâm quản lý sách cá nhân với 2 tab chính:

1. **Sách đang đọc** 📖
   - Theo dõi tiến độ đọc
   - Resume từ vị trí đã lưu
   - Xem lịch sử đọc

2. **Sách đã mua** 🛒
   - Toàn bộ sách đã sở hữu
   - Đọc bất cứ lúc nào
   - Không giới hạn số lần đọc

**URL**: `/user/library`  
**Trạng thái**: ✅ Đã hoàn thành 100%  
**Ngày tạo**: 13/12/2025

---
*Happy Reading! 📚*

