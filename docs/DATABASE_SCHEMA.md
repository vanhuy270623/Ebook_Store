# Database Schema - Ebook Store

## Tổng Quan Database

**Database Name**: `ebook_store`  
**Database Engine**: MySQL 9.1.0  
**Character Set**: utf8mb4  
**Collation**: utf8mb4_0900_ai_ci  
**Total Tables**: 19 tables

## Entity Relationship Diagram (ERD) - Text Format

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│  users   │◄───────►│  roles   │         │  carts   │
└────┬─────┘         └──────────┘         └────┬─────┘
     │                                          │
     │                                    ┌─────▼──────────┐
     │                                    │   cart_items   │
     │                                    └─────┬──────────┘
     │                                          │
     ├──────────┐                               │
     │          │                               │
┌────▼─────┐ ┌─▼────────┐              ┌───────▼──────┐
│ orders   │ │ reviews  │              │    books     │
└────┬─────┘ └──────────┘              └───────┬──────┘
     │                                          │
┌────▼─────────┐                        ┌──────┴─────────┐
│ order_items  │                        │                │
└──────────────┘                  ┌─────▼──────┐  ┌─────▼──────────┐
                                  │ book_      │  │  book_         │
                                  │ authors    │  │  category      │
                                  └─────┬──────┘  └─────┬──────────┘
                                        │               │
                                  ┌─────▼──────┐  ┌─────▼──────────┐
                                  │  authors   │  │  book_category │
                                  └────────────┘  └────────────────┘
```

## Core Tables

### 1. users (Người dùng)

**Mô tả**: Lưu thông tin người dùng hệ thống

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| user_id | VARCHAR(50) | PK | ID người dùng |
| username | VARCHAR(100) | UNIQUE | Tên đăng nhập |
| email | VARCHAR(255) | UNIQUE | Email |
| password_hash | VARCHAR(255) | | Mật khẩu mã hóa BCrypt |
| full_name | VARCHAR(255) | | Họ tên |
| avatar_url | VARCHAR(500) | | URL avatar |
| phone | VARCHAR(20) | | Số điện thoại |
| address | TEXT | | Địa chỉ |
| date_of_birth | DATE | | Ngày sinh |
| gender | ENUM | | Nam/Nữ/Khác |
| is_active | TINYINT(1) | | Trạng thái kích hoạt |
| created_at | DATETIME | | Ngày tạo |
| updated_at | DATETIME | | Ngày cập nhật |
| role_id | VARCHAR(50) | FK | Vai trò (USER/ADMIN) |

**Indexes**:
- PRIMARY KEY: `user_id`
- UNIQUE: `username`, `email`
- FOREIGN KEY: `role_id` → `roles(role_id)`

---

### 2. roles (Vai trò)

**Mô tả**: Phân quyền hệ thống

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| role_id | VARCHAR(50) | PK | ID vai trò |
| role_name | VARCHAR(50) | UNIQUE | Tên vai trò (USER, ADMIN) |
| description | TEXT | | Mô tả vai trò |

**Sample Data**:
```sql
INSERT INTO roles VALUES 
('role_user', 'USER', 'Người dùng thông thường'),
('role_admin', 'ADMIN', 'Quản trị viên');
```

---

### 3. books (Sách)

**Mô tả**: Thông tin sách điện tử

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| book_id | VARCHAR(50) | PK | ID sách |
| book_category_id | VARCHAR(50) | FK | Danh mục sách |
| title | VARCHAR(255) | | Tiêu đề |
| description | TEXT | | Mô tả |
| price | DECIMAL(15,2) | | Giá bán |
| cover_image_url | VARCHAR(500) | | Ảnh bìa |
| publisher | VARCHAR(255) | | Nhà xuất bản |
| publication_year | INT | | Năm xuất bản |
| language | VARCHAR(10) | | Ngôn ngữ (vi, en) |
| pages | INT | | Số trang |
| isbn | VARCHAR(20) | | Mã ISBN |
| access_type | ENUM | | FREE, PURCHASE, SUBSCRIPTION, BOTH |
| is_downloadable | TINYINT(1) | | Có tải về được không |
| average_rating | FLOAT | | Điểm đánh giá TB |
| total_reviews | INT | | Số lượt đánh giá |
| view_count | INT | | Số lượt xem |
| created_at | DATETIME | | Ngày tạo |
| updated_at | DATETIME | | Ngày cập nhật |

**Access Types**:
- `FREE`: Miễn phí
- `PURCHASE`: Mua một lần
- `SUBSCRIPTION`: Yêu cầu gói VIP
- `BOTH`: Cả mua và subscription

---

### 4. authors (Tác giả)

**Mô tả**: Thông tin tác giả

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| author_id | VARCHAR(50) | PK | ID tác giả |
| name | VARCHAR(255) | | Tên tác giả |
| biography | TEXT | | Tiểu sử |
| avatar_url | VARCHAR(500) | | Ảnh đại diện |
| created_at | DATETIME | | Ngày tạo |

**Sample Authors**:
- Nguyễn Nhật Ánh
- Dale Carnegie
- Aoyama Gosho (Conan)
- Fujiko F. Fujio (Doraemon)
- Paulo Coelho
- Haruki Murakami

---

### 5. book_authors (Many-to-Many)

**Mô tả**: Quan hệ Sách - Tác giả (nhiều-nhiều)

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| book_id | VARCHAR(50) | PK, FK | ID sách |
| author_id | VARCHAR(50) | PK, FK | ID tác giả |

**Composite Primary Key**: (`book_id`, `author_id`)

---

### 6. book_category (Danh mục sách)

**Mô tả**: Phân loại sách

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| book_category_id | VARCHAR(50) | PK | ID danh mục |
| category_name | VARCHAR(255) | | Tên danh mục |
| description | TEXT | | Mô tả |
| icon_url | VARCHAR(500) | | Icon danh mục |
| display_order | INT | | Thứ tự hiển thị |
| is_active | TINYINT(1) | | Trạng thái |
| created_at | DATETIME | | Ngày tạo |

**Categories**:
1. Tiểu thuyết
2. Kỹ năng sống
3. Truyện tranh
4. Kinh tế
5. Thiếu nhi
6. Giáo khoa
7. Tâm lý

---

### 7. bookassets (File sách)

**Mô tả**: File nguồn sách (PDF, EPUB)

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| book_asset_id | VARCHAR(50) | PK | ID asset |
| book_id | VARCHAR(50) | FK | ID sách |
| file_type | ENUM('PDF','EPUB') | | Loại file |
| file_url | VARCHAR(500) | | Đường dẫn file |
| file_size | BIGINT | | Kích thước (bytes) |
| preview_url | VARCHAR(500) | | URL xem trước |
| created_at | DATETIME | | Ngày tạo |

---

### 8. carts (Giỏ hàng)

**Mô tả**: Giỏ hàng của người dùng

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| cart_id | VARCHAR(50) | PK | ID giỏ hàng |
| user_id | VARCHAR(50) | FK, UNIQUE | ID người dùng |
| updated_at | DATETIME | | Ngày cập nhật |

**Relationship**: 1 user có 1 cart

---

### 9. cart_items (Sản phẩm trong giỏ)

**Mô tả**: Sách trong giỏ hàng

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| cart_id | VARCHAR(50) | PK, FK | ID giỏ hàng |
| book_id | VARCHAR(50) | PK, FK | ID sách |
| added_at | DATETIME | | Ngày thêm |

**Composite Primary Key**: (`cart_id`, `book_id`)

---

### 10. orders (Đơn hàng)

**Mô tả**: Đơn hàng mua sách

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| order_id | VARCHAR(50) | PK | ID đơn hàng |
| user_id | VARCHAR(50) | FK | ID người mua |
| total_amount | DECIMAL(15,2) | | Tổng tiền |
| discount_amount | DECIMAL(15,2) | | Tiền giảm giá |
| final_amount | DECIMAL(15,2) | | Tiền thanh toán |
| payment_method | ENUM | | COD, VNPAY, MOMO |
| payment_status | ENUM | | PENDING, PAID, FAILED |
| order_status | ENUM | | PENDING, CONFIRMED, COMPLETED, CANCELLED |
| coupon_id | VARCHAR(50) | FK | Mã giảm giá |
| order_date | DATETIME | | Ngày đặt |
| payment_date | DATETIME | | Ngày thanh toán |
| completed_date | DATETIME | | Ngày hoàn thành |

**Order Status Flow**:
```
PENDING → CONFIRMED → COMPLETED
   ↓
CANCELLED
```

---

### 11. order_items (Sản phẩm trong đơn hàng)

**Mô tả**: Chi tiết sách trong đơn hàng

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| order_item_id | VARCHAR(50) | PK | ID order item |
| order_id | VARCHAR(50) | FK | ID đơn hàng |
| book_id | VARCHAR(50) | FK | ID sách |
| price | DECIMAL(15,2) | | Giá tại thời điểm mua |

---

### 12. reviews (Đánh giá)

**Mô tả**: Đánh giá sách

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| review_id | VARCHAR(50) | PK | ID review |
| book_id | VARCHAR(50) | FK | ID sách |
| user_id | VARCHAR(50) | FK | ID người đánh giá |
| rating | INT | | Điểm (1-5) |
| comment | TEXT | | Nhận xét |
| is_approved | TINYINT(1) | | Đã duyệt? |
| created_at | DATETIME | | Ngày tạo |

**Constraint**: UNIQUE(`book_id`, `user_id`) - 1 user chỉ review 1 lần/sách

---

### 13. coupons (Mã giảm giá)

**Mô tả**: Mã khuyến mãi

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| coupon_id | VARCHAR(50) | PK | ID coupon |
| code | VARCHAR(50) | UNIQUE | Mã code |
| description | TEXT | | Mô tả |
| discount_type | ENUM | | PERCENTAGE, FIXED_AMOUNT |
| discount_value | DECIMAL(15,2) | | Giá trị giảm |
| min_order_amount | DECIMAL(15,2) | | Đơn tối thiểu |
| max_discount | DECIMAL(15,2) | | Giảm tối đa |
| usage_limit | INT | | Số lần dùng tối đa |
| used_count | INT | | Đã dùng bao nhiêu lần |
| start_date | DATETIME | | Ngày bắt đầu |
| end_date | DATETIME | | Ngày kết thúc |
| is_active | TINYINT(1) | | Trạng thái |

---

### 14. subscriptions (Gói đăng ký)

**Mô tả**: Gói VIP

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| subscription_id | VARCHAR(50) | PK | ID subscription |
| user_id | VARCHAR(50) | FK | ID người dùng |
| plan_name | VARCHAR(100) | | Tên gói (VIP, PREMIUM) |
| price | DECIMAL(15,2) | | Giá |
| duration_days | INT | | Thời hạn (ngày) |
| start_date | DATETIME | | Ngày bắt đầu |
| end_date | DATETIME | | Ngày hết hạn |
| is_active | TINYINT(1) | | Đang hoạt động? |
| auto_renew | TINYINT(1) | | Tự động gia hạn? |

---

### 15. reading_progress (Tiến độ đọc)

**Mô tả**: Lưu tiến độ đọc sách

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| progress_id | VARCHAR(50) | PK | ID progress |
| user_id | VARCHAR(50) | FK | ID người dùng |
| book_id | VARCHAR(50) | FK | ID sách |
| current_page | INT | | Trang hiện tại |
| total_pages | INT | | Tổng số trang |
| progress_percentage | INT | | % hoàn thành |
| last_read_at | DATETIME | | Lần đọc gần nhất |

**Constraint**: UNIQUE(`user_id`, `book_id`)

---

### 16. posts (Bài viết)

**Mô tả**: Bài viết blog/tin tức

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| post_id | VARCHAR(50) | PK | ID bài viết |
| author_id | VARCHAR(50) | FK | ID tác giả (user) |
| title | VARCHAR(255) | | Tiêu đề |
| content | TEXT | | Nội dung |
| thumbnail_url | VARCHAR(500) | | Ảnh thumbnail |
| category_id | VARCHAR(50) | FK | Danh mục bài viết |
| view_count | INT | | Số lượt xem |
| is_published | TINYINT(1) | | Đã xuất bản? |
| published_at | DATETIME | | Ngày xuất bản |
| created_at | DATETIME | | Ngày tạo |

---

### 17. banner (Banner quảng cáo)

**Mô tả**: Banner trên website

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| banner_id | VARCHAR(50) | PK | ID banner |
| user_id | VARCHAR(50) | FK | ID người tạo |
| title | VARCHAR(255) | | Tiêu đề |
| image_url | VARCHAR(500) | | Ảnh banner |
| target_url | VARCHAR(500) | | Link đích |
| position | ENUM | | HOME, CATEGORY, DETAIL |
| is_active | TINYINT(1) | | Trạng thái |
| created_at | DATETIME | | Ngày tạo |

---

### 18. user_devices (Thiết bị đăng nhập)

**Mô tả**: Quản lý thiết bị đăng nhập

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| device_id | VARCHAR(50) | PK | ID thiết bị |
| user_id | VARCHAR(50) | FK | ID người dùng |
| device_name | VARCHAR(255) | | Tên thiết bị |
| device_type | VARCHAR(50) | | Loại (Mobile, Desktop) |
| ip_address | VARCHAR(45) | | IP address |
| last_login | DATETIME | | Lần đăng nhập cuối |
| is_active | TINYINT(1) | | Đang hoạt động? |

---

### 19. category (Danh mục bài viết)

**Mô tả**: Phân loại bài viết

| Column | Type | Key | Description |
|--------|------|-----|-------------|
| category_id | VARCHAR(50) | PK | ID danh mục |
| category_name | VARCHAR(255) | | Tên danh mục |

## Relationships Summary

### One-to-One
- `users` ↔ `carts` (1:1)

### One-to-Many
- `users` → `orders` (1:N)
- `users` → `reviews` (1:N)
- `users` → `subscriptions` (1:N)
- `users` → `reading_progress` (1:N)
- `users` → `posts` (1:N)
- `books` → `bookassets` (1:N)
- `books` → `reviews` (1:N)
- `books` → `order_items` (1:N)
- `orders` → `order_items` (1:N)
- `book_category` → `books` (1:N)

### Many-to-Many
- `books` ↔ `authors` (N:M) via `book_authors`
- `carts` ↔ `books` (N:M) via `cart_items`

## Indexes

**Primary Keys**: Tất cả tables đều có PK

**Foreign Keys**: 
- All relationship columns have FK constraints với CASCADE actions

**Unique Indexes**:
- `users.username`
- `users.email`
- `roles.role_name`
- `coupons.code`
- `carts.user_id`

## Data Integrity

**Constraints**:
- NOT NULL cho các trường bắt buộc
- DEFAULT values cho timestamps, status fields
- CHECK constraints cho ENUM types
- UNIQUE constraints để tránh duplicate

**Cascading**:
- ON DELETE CASCADE: Xóa parent → xóa child
- ON UPDATE CASCADE: Update parent → update child

## Storage Engine

**InnoDB**: Tất cả tables sử dụng InnoDB
- ACID compliance
- Foreign key support
- Row-level locking
- Transaction support

---

**Tài liệu liên quan**:
- [Cấu trúc dự án](PROJECT_STRUCTURE.md)
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [API Documentation](API_DOCUMENTATION.md)

