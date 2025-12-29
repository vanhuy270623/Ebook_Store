# Tính năng hiển thị bài viết (Posts) cho User

## Tổng quan
Đã hoàn thành việc triển khai tính năng hiển thị và đọc bài viết cho người dùng cuối (User). Tính năng này cho phép người dùng xem danh sách bài viết, tìm kiếm, lọc theo danh mục và đọc chi tiết bài viết.

## Các thành phần đã triển khai

### 1. Controller
**File:** `src/main/java/stu/datn/ebook_store/controller/user/UserPostController.java`
- **URL Mapping:** `/posts`
- **Chức năng:**
  - `GET /posts` - Hiển thị danh sách bài viết
  - `GET /posts?category={id}` - Lọc bài viết theo danh mục
  - `GET /posts?search={keyword}` - Tìm kiếm bài viết
  - `GET /posts/{slug}` - Hiển thị chi tiết bài viết theo slug
- **Tính năng:**
  - Tự động tăng lượt xem khi người dùng đọc bài viết
  - Hiển thị bài viết liên quan (cùng danh mục)
  - Hỗ trợ cả người dùng đã đăng nhập và khách

### 2. Service Layer
**File:** `src/main/java/stu/datn/ebook_store/service/CategoryService.java`
**File:** `src/main/java/stu/datn/ebook_store/service/impl/CategoryServiceImpl.java`
- Service mới được tạo để quản lý danh mục bài viết (Category)
- Phân biệt với BookCategoryService (danh mục sách)
- Các phương thức:
  - `getAllCategories()` - Lấy tất cả danh mục
  - `getCategoryById(String)` - Lấy danh mục theo ID
  - `getCategoryByName(String)` - Lấy danh mục theo tên
  - `saveCategory(Category)` - Lưu danh mục
  - `deleteCategory(String)` - Xóa danh mục

### 3. Templates (Views)

#### a. Trang danh sách bài viết
**File:** `src/main/resources/templates/user/posts/list.html`
**Tính năng:**
- Hiển thị grid các bài viết (2 cột trên desktop)
- Thanh tìm kiếm bài viết
- Sidebar với:
  - Danh sách danh mục (có highlight khi đang chọn)
  - Bài viết xem nhiều nhất (Top 10)
  - Bài viết mới nhất (Top 10)
- Hiển thị thumbnail, tiêu đề, mô tả ngắn, ngày đăng, số lượt xem
- Badge danh mục trên mỗi bài viết
- Responsive design
- Hover effect trên card

#### b. Trang chi tiết bài viết
**File:** `src/main/resources/templates/user/posts/detail.html`
**Tính năng:**
- Header gradient với breadcrumb
- Hiển thị đầy đủ thông tin:
  - Avatar và tên tác giả
  - Ngày đăng
  - Danh mục
  - Số lượt xem
  - Thumbnail lớn
  - Tóm tắt (nếu có)
  - Nội dung đầy đủ (HTML)
- Nút chia sẻ:
  - Facebook
  - Twitter
  - Sao chép link
- Sidebar với:
  - Thông tin tác giả
  - Thông tin danh mục
  - Link quay lại danh sách
- Phần bài viết liên quan (tối đa 5 bài)
- Responsive design

### 4. Tích hợp vào trang chủ
**File:** `src/main/java/stu/datn/ebook_store/controller/user/UserDashboardController.java`
- Đã thêm `PostService` dependency
- Lấy 6 bài viết mới nhất để hiển thị trên trang chủ

**File:** `src/main/resources/templates/user/index.html`
- Thêm section "Bài viết mới nhất" với:
  - Grid 3 cột (responsive)
  - Card với thumbnail, tiêu đề, mô tả ngắn
  - Badge danh mục
  - Ngày đăng và số lượt xem
  - Link "Xem tất cả" đến trang danh sách bài viết

### 5. Navigation Menu
**File:** `src/main/resources/templates/user/layout/navbar.html`
- Thêm menu item "Bài viết" vào navigation bar
- Active state khi đang ở trang bài viết

## Cấu trúc Database
Sử dụng các bảng có sẵn:
- **post** - Lưu trữ bài viết
  - post_id (PK)
  - user_id (FK -> users)
  - category_id (FK -> category)
  - title, slug, excerpt, content
  - thumbnail_url
  - is_published (chỉ hiển thị bài đã publish)
  - view_count
  - created_at, updated_at

- **category** - Danh mục bài viết (khác với book_category)
  - category_id (PK)
  - category_name
  
  Dữ liệu mẫu:
  - pcat_1: Tin tức
  - pcat_2: Review Sách

## Tính năng chính

### 1. Danh sách bài viết
- Hiển thị tất cả bài viết đã xuất bản
- Sắp xếp theo ngày mới nhất
- Lọc theo danh mục
- Tìm kiếm theo từ khóa (tìm trong tiêu đề, mô tả, nội dung)

### 2. Chi tiết bài viết
- URL thân thiện với SEO (sử dụng slug)
- Tự động tăng view count
- Hiển thị bài viết liên quan
- Chia sẻ lên mạng xã hội
- Breadcrumb navigation

### 3. Tích hợp
- Link từ navigation menu
- Hiển thị trên trang chủ
- Responsive trên mọi thiết bị

## Cách sử dụng

### Truy cập trang bài viết:
1. **Trang chủ:** `http://localhost:8080/user/index` -> Click "Bài viết" trên menu
2. **Danh sách:** `http://localhost:8080/posts`
3. **Theo danh mục:** `http://localhost:8080/posts?category=pcat_1`
4. **Tìm kiếm:** `http://localhost:8080/posts?search=sách`
5. **Chi tiết:** `http://localhost:8080/posts/chao-mung` (slug)

### Dữ liệu mẫu có sẵn:
- Bài viết 1: "Chào mừng Ebook Store" (slug: chao-mung)
- Bài viết 2: "Top 10 sách hay 2025" (slug: top-10-sach)

## Design Pattern & Best Practices
- **Controller-Service-Repository pattern**
- **DTO pattern** (sẵn sàng cho tương lai)
- **Separation of concerns** (Category vs BookCategory)
- **Responsive design** với Bootstrap 5
- **SEO-friendly URLs** với slug
- **Security** - Chỉ hiển thị bài viết đã publish
- **Performance** - Lazy loading cho relationships

## Các file liên quan
```
Controller:
- src/main/java/stu/datn/ebook_store/controller/user/UserPostController.java

Service:
- src/main/java/stu/datn/ebook_store/service/CategoryService.java
- src/main/java/stu/datn/ebook_store/service/impl/CategoryServiceImpl.java
- src/main/java/stu/datn/ebook_store/service/PostService.java (đã có)
- src/main/java/stu/datn/ebook_store/service/impl/PostServiceImpl.java (đã có)

Templates:
- src/main/resources/templates/user/posts/list.html
- src/main/resources/templates/user/posts/detail.html

Updated Files:
- src/main/java/stu/datn/ebook_store/controller/user/UserDashboardController.java
- src/main/resources/templates/user/index.html
- src/main/resources/templates/user/layout/navbar.html
```

## Trạng thái: ✅ Hoàn thành
- [x] Controller cho user
- [x] Service layer cho Category
- [x] Template danh sách bài viết
- [x] Template chi tiết bài viết
- [x] Tích hợp vào trang chủ
- [x] Thêm vào navigation menu
- [x] Compile thành công
- [x] Responsive design
- [x] SEO-friendly URLs

## Hướng phát triển tiếp theo (Optional)
- [ ] Phân trang cho danh sách bài viết
- [ ] Bình luận bài viết
- [ ] Đánh giá bài viết
- [ ] Tags cho bài viết
- [ ] Related posts thông minh hơn (dựa trên tags)
- [ ] Social share counter
- [ ] Reading time estimate
- [ ] Bookmark/Save post

