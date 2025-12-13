# Hướng Dẫn Fix Lỗi Hiển Thị EPUB Tràn Màn Hình Mobile

## 📱 Vấn Đề
Khi xem sách EPUB trên điện thoại (mobile), nội dung bị tràn ra ngoài không vừa với khung màn hình.

## ✅ Giải Pháp Đã Áp Dụng

### 1. **Cập Nhật CSS (reading.css)**

#### A. Ngăn Overflow Toàn Cục
```css
html, body {
    overflow-x: hidden !important;
    max-width: 100vw !important;
}

.reader-container {
    max-width: 100vw;
    overflow-x: hidden;
}

#epub-viewer {
    overflow-x: hidden !important;
    box-sizing: border-box;
}
```

#### B. CSS cho EPUB Iframe
```css
#epub-viewer iframe {
    max-width: 100% !important;
    overflow-x: hidden !important;
}

#epub-viewer * {
    max-width: 100% !important;
    box-sizing: border-box !important;
}

#epub-viewer img {
    max-width: 100% !important;
    height: auto !important;
}
```

#### C. Responsive Mobile (@media max-width: 768px)
- **Header**: Hiển thị theo cột thay vì hàng
- **Sidebar**: Ẩn mặc định, hiển thị overlay khi bật
- **EPUB Viewer**: padding giảm xuống 10px 5px
- **Navigation buttons**: Thu nhỏ xuống 35px x 35px
- **Settings panel**: Full width trừ 20px margin

#### D. Mobile Nhỏ (@media max-width: 480px)
- **Padding**: Giảm xuống tối thiểu (5px 3px)
- **Navigation buttons**: 30px x 30px
- **Font sizes**: Giảm xuống 11-13px

### 2. **Cập Nhật JavaScript (epub-viewer.html)**

#### A. Thêm CSS vào Rendition Themes
```javascript
rendition.themes.default({
    'body': {
        'overflow-x': 'hidden !important',
        'max-width': '100% !important'
    },
    'img': {
        'max-width': '100% !important',
        'height': 'auto !important'
    },
    // ... thêm nhiều quy tắc khác
});
```

#### B. Cập Nhật toggleSidebar()
- Phát hiện mobile (window.innerWidth <= 768)
- Dùng class 'show' cho mobile, 'hidden' cho desktop

#### C. Cập Nhật changePageWidth()
- Trên mobile: luôn dùng 100% width
- Thêm `overflowX: 'hidden'`

#### D. Thêm Window Resize Listener
```javascript
window.addEventListener('resize', function() {
    if (rendition) {
        const isMobile = window.innerWidth <= 768;
        if (isMobile) {
            container.style.maxWidth = '100%';
            container.style.padding = '10px 5px';
        }
        rendition.resize();
    }
});
```

### 3. **Cập Nhật Meta Tags**
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, shrink-to-fit=no">
<meta name="mobile-web-app-capable" content="yes">
```

## 🧪 Cách Test

### Test trên Chrome DevTools:
1. Mở trang epub-viewer
2. Nhấn F12 để mở DevTools
3. Nhấn Ctrl+Shift+M để bật Device Toolbar
4. Chọn thiết bị: iPhone SE, iPhone 12 Pro, Galaxy S8+, etc.
5. Kiểm tra:
   - ✅ Không có scroll ngang (horizontal scroll)
   - ✅ Nội dung vừa với màn hình
   - ✅ Hình ảnh tự động scale
   - ✅ Navigation buttons không che khuất nội dung
   - ✅ Sidebar có thể toggle

### Test trên Thiết Bị Thật:
1. Mở trên điện thoại thật
2. Kiểm tra ở chế độ portrait (dọc)
3. Kiểm tra ở chế độ landscape (ngang)
4. Test zoom in/out (nếu cần)

## 📊 Breakpoints Responsive

| Kích thước | Target Device | Thay đổi chính |
|------------|--------------|----------------|
| > 768px | Desktop/Tablet | Layout đầy đủ với sidebar |
| ≤ 768px | Tablet/Phone | Sidebar ẩn, padding giảm |
| ≤ 480px | Small Phone | Padding tối thiểu, buttons nhỏ hơn |

## 🔧 Các File Đã Sửa

1. **C:\Projects\Ebook_Store\src\main\resources\static\user_template\css\reading.css**
   - Thêm overflow control
   - Thêm responsive CSS
   - Fix iframe và image sizing

2. **C:\Projects\Ebook_Store\src\main\resources\templates\user\reading\epub-viewer.html**
   - Cập nhật rendition config
   - Thêm themes CSS
   - Cập nhật toggleSidebar()
   - Cập nhật changePageWidth()
   - Thêm resize listener
   - Cập nhật meta viewport

## 🚀 Không Cần Rebuild

Vì chỉ sửa CSS và HTML template, bạn **KHÔNG CẦN** rebuild project:
- CSS và HTML được load trực tiếp
- Chỉ cần refresh browser (Ctrl+F5 để clear cache)
- Nếu dùng Spring Boot DevTools, tự động reload

## ⚠️ Lưu Ý

### Nếu Vẫn Bị Tràn:
1. **Clear browser cache**: Ctrl+F5
2. **Kiểm tra EPUB file**: Một số EPUB có CSS riêng override
3. **Kiểm tra console**: F12 > Console tab để xem lỗi
4. **Tăng !important**: Nếu cần, thêm !important vào CSS

### Tùy Chỉnh Thêm:
```css
/* Trong reading.css, thêm vào @media (max-width: 768px) */
#epub-viewer {
    padding: 5px !important; /* Giảm padding hơn nữa */
}

.nav-btn {
    width: 25px !important; /* Buttons nhỏ hơn */
    height: 25px !important;
}
```

## 📝 Checklist Sau Khi Fix

- [x] CSS đã thêm overflow-x: hidden
- [x] Responsive CSS cho mobile
- [x] JavaScript xử lý resize
- [x] Meta viewport đã cập nhật
- [x] Rendition themes đã thêm
- [x] Sidebar toggle cho mobile
- [ ] Test trên Chrome DevTools
- [ ] Test trên điện thoại thật
- [ ] Test với nhiều file EPUB khác nhau

## 🆘 Troubleshooting

### Vấn đề: Vẫn có scroll ngang
**Giải pháp**: Thêm vào CSS
```css
* {
    max-width: 100vw !important;
    overflow-x: hidden !important;
}
```

### Vấn đề: Hình ảnh vẫn to
**Giải pháp**: Kiểm tra EPUB có CSS inline không, thêm:
```javascript
rendition.themes.override('img', {
    'max-width': '100% !important',
    'height': 'auto !important'
});
```

### Vấn đề: Font chữ quá nhỏ trên mobile
**Giải pháp**: Tăng font size mặc định cho mobile:
```javascript
if (window.innerWidth <= 768) {
    rendition.themes.fontSize('18px');
}
```

---

**Ngày cập nhật**: 13/12/2024  
**Phiên bản**: 1.0  
**Tác giả**: GitHub Copilot

