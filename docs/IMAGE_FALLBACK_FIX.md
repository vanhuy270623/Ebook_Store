# Fix Image Fallback và Error Handling

## Vấn Đề

### 1. Đường dẫn image fallback không tồn tại
- Đã dùng: `/user_template/images/default-book.jpg` ❌ KHÔNG TỒN TẠI
- Thực tế: Ảnh được serve từ `F:/datn_uploads/book_asset/image/covers/`

### 2. Backend lỗi không hiển thị trong Thymeleaf
```html
<!-- ❌ SAI - Không xử lý khi book = null từ backend -->
<div th:if="${book}">
    <span th:text="${book.title}"></span>
</div>

<!-- ✅ ĐÚNG - Controller Optional.map() guarantee book tồn tại -->
<span th:text="${book.title}">Title</span>
```

## Giải Pháp

### Option 1: Dùng Data URL (Inline SVG)
```html
<img th:src="${book.coverImageUrl}"
     onerror="this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22200%22 height=%22300%22%3E%3Crect fill=%22%23e0e0e0%22 width=%22200%22 height=%22300%22/%3E%3Ctext fill=%22%23999%22 font-size=%2220%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22%3ENo Image%3C/text%3E%3C/svg%3E';">
```

### Option 2: Dùng ảnh có sẵn
```html
<img th:src="${book.coverImageUrl ?: '/shared/images/book.png'}"
     onerror="this.src='/shared/images/book.png';">
```

### Option 3: Tạo placeholder mới
Tạo file: `src/main/resources/static/images/book-placeholder.svg`

## Implement

Sẽ dùng **Option 2** vì đơn giản và có file thật.

