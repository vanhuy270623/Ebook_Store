# 📖 GIẢI THÍCH TỔNG QUAN DỰ ÁN EBOOK STORE

**Ngày tạo:** 23/11/2025  
**Tác giả:** Development Team

---

## 🎯 GIỚI THIỆU DỰ ÁN

### Tên dự án
**Hệ Thống Quản Lý Cửa Hàng Sách Điện Tử (Ebook Store)**

### Mục đích
Xây dựng một nền tảng bán và quản lý sách điện tử (ebook) trực tuyến, cho phép:
- **Người dùng:** Mua, đọc, và quản lý thư viện sách điện tử
- **Admin:** Quản lý sách, tác giả, đơn hàng, người dùng
- **Tác giả:** Upload và bán sách của mình (tương lai)

---

## 🏗️ KIẾN TRÚC HỆ THỐNG

### Công nghệ sử dụng

```
┌─────────────────────────────────────────────────────────┐
│                    TECHNOLOGY STACK                      │
├─────────────────────────────────────────────────────────┤
│ Backend:      Spring Boot 3.5.7                         │
│ Database:     MySQL 8.0                                 │
│ ORM:          Spring Data JPA (Hibernate)               │
│ Security:     Spring Security 6.x                       │
│ Template:     Thymeleaf                                 │
│ Build:        Maven                                     │
│ Frontend:     Bootstrap 5, jQuery, DataTables           │
│ Payment:      VNPay, Momo (planned)                     │
└─────────────────────────────────────────────────────────┘
```

### Cấu trúc Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                      │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Controllers  │  │  Thymeleaf   │  │   Static    │  │
│  │  (@Controller)│  │  Templates   │  │ Resources   │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   BUSINESS LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │   Services   │  │     DTOs     │  │ Validators  │  │
│  │  (@Service)  │  │              │  │             │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                DATA ACCESS LAYER                         │
│  ┌──────────────┐  ┌──────────────┐                    │
│  │ Repositories │  │   Entities   │                    │
│  │ (@Repository)│  │   (@Entity)  │                    │
│  └──────────────┘  └──────────────┘                    │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   DATABASE (MySQL)                       │
│                    18 Tables                             │
└─────────────────────────────────────────────────────────┘
```

**Giải thích từng layer:**

1. **Presentation Layer** - Tầng giao diện:
   - **Controllers:** Nhận HTTP requests, xử lý routing
   - **Templates:** Render HTML với Thymeleaf
   - **Static Resources:** CSS, JS, images

2. **Business Layer** - Tầng logic nghiệp vụ:
   - **Services:** Chứa business logic
   - **DTOs:** Truyền dữ liệu giữa các tầng
   - **Validators:** Validate input data

3. **Data Access Layer** - Tầng truy cập dữ liệu:
   - **Repositories:** CRUD operations với database
   - **Entities:** Map với database tables

4. **Database** - Cơ sở dữ liệu MySQL

---

## 📊 CẤU TRÚC DATABASE

### Tổng quan
- **18 bảng chính**
- **87+ custom query methods**
- **Relationships:** One-to-Many, Many-to-Many
- **Indexes:** Optimized for performance

### Các module chính

#### 1. **User Management Module**
```
users (Người dùng)
  ├── roles (Vai trò: ADMIN, USER)
  └── user_devices (Thiết bị truy cập - DRM)
```

**Mục đích:**
- Quản lý tài khoản người dùng
- Phân quyền (RBAC)
- Tracking thiết bị để bảo vệ bản quyền

#### 2. **Book Management Module**
```
books (Sách)
  ├── book_category (Danh mục sách: Văn học, Khoa học...)
  ├── authors (Tác giả)
  ├── book_authors (Many-to-Many: Sách ↔ Tác giả)
  └── bookassets (File PDF/EPUB)
```

**Mục đích:**
- Quản lý thông tin sách
- Phân loại theo danh mục
- Liên kết với tác giả (1 sách có nhiều tác giả)
- Lưu trữ file ebook

#### 3. **E-commerce Module**
```
carts (Giỏ hàng)
  └── cart_items (Sản phẩm trong giỏ)

orders (Đơn hàng)
  └── order_items (Sách trong đơn hàng)

subscriptions (Gói đăng ký Premium)
```

**Mục đích:**
- Giỏ hàng: Lưu sách người dùng muốn mua
- Đơn hàng: Lưu lịch sử mua hàng
- Gói đăng ký: Đọc không giới hạn với phí tháng/năm

#### 4. **User Interaction Module**
```
reviews (Đánh giá sách)
  └── rating (1-5 sao) + comment

reading_progress (Tiến độ đọc)
  └── last_page, last_position, completion %
```

**Mục đích:**
- Đánh giá: Người dùng review và rate sách
- Tiến độ: Lưu trang đã đọc, tự động bookmark

#### 5. **CMS Module**
```
posts (Bài viết blog)
  └── category (Danh mục bài viết)

banners (Banner quảng cáo)
```

**Mục đích:**
- Blog: Viết bài review, tin tức sách
- Banner: Quảng cáo sách mới, khuyến mãi

---

## 🔄 CÁC LUỒNG XỬ LÝ CHÍNH

### 1. Luồng Đăng Ký & Đăng Nhập

```
User → Register Form → Validate → Hash Password → Save to DB
                                                      ↓
User → Login Form → Spring Security → Verify → Create Session
                                                      ↓
                                          Redirect by Role:
                                          - ADMIN → /admin/dashboard
                                          - USER → /user/index
```

**Giải thích:**
- **Đăng ký:** Tạo tài khoản mới, mặc định role = USER
- **Đăng nhập:** Xác thực bằng Spring Security
- **Session:** Lưu thông tin đăng nhập 30 phút

### 2. Luồng Quản Lý Sách (Admin)

```
Admin → Add Book Form → Upload Cover → Link Authors → Save to DB
Admin → Edit Book → Update Info → Save Changes
Admin → Delete Book → Check Constraints → Soft/Hard Delete
```

**Giải thích:**
- **Add:** Tạo sách mới, upload ảnh bìa
- **Edit:** Cập nhật thông tin, thay ảnh
- **Delete:** Kiểm tra trước khi xóa (có đơn hàng không?)

### 3. Luồng Mua Sách (User)

```
User → Browse Books → Add to Cart → Checkout
                                        ↓
                             Choose Payment Method:
                             - COD (Ship tiền mặt)
                             - VNPay (Thanh toán online)
                             - Momo
                                        ↓
                              Create Order → Payment
                                        ↓
                              Payment Success → Order Confirmed
                                        ↓
                              User can download/read book
```

**Giải thích:**
- **Cart:** Giỏ hàng tạm thời, có thể thêm/xóa
- **Checkout:** Tạo đơn hàng chính thức
- **Payment:** Tích hợp cổng thanh toán
- **Access:** Sau khi thanh toán, có quyền đọc sách

### 4. Luồng Đọc Sách

```
User → Click "Read Book"
          ↓
Check Access Rights:
- FREE book → Allow
- PURCHASED book → Check order exists?
- SUBSCRIPTION book → Check active subscription?
          ↓
Load Ebook File (PDF/EPUB)
          ↓
Open Reader (PDF.js / EPUB.js)
          ↓
Auto-save Reading Progress every 30s
```

**Giải thích:**
- **Access Control:** Kiểm tra quyền truy cập
- **Reader:** Mở file trong browser
- **Progress:** Tự động lưu trang đã đọc

### 5. Luồng Review Sách

```
User → Click "Write Review"
          ↓
Check: User purchased this book?
          ↓
Show Review Form (Rating 1-5 + Comment)
          ↓
Submit → Save to DB
          ↓
Auto Update Book's Average Rating
```

**Giải thích:**
- **Validation:** Chỉ người mua mới review được
- **Rating:** Tự động tính rating trung bình
- **Display:** Hiển thị review trên trang sách

---

## 🔐 HỆ THỐNG BẢO MẬT

### Spring Security Configuration

```
┌─────────────────────────────────────────────────────────┐
│              URL AUTHORIZATION RULES                     │
├─────────────────────────────────────────────────────────┤
│ Public URLs (No login required):                        │
│   - /                      (Home page)                  │
│   - /home                  (Home page)                  │
│   - /auth/**               (Login, Register)            │
│   - /Book_Asset/**         (Static files)               │
│                                                          │
│ Admin URLs (ROLE_ADMIN required):                       │
│   - /admin/**              (All admin pages)            │
│                                                          │
│ User URLs (ROLE_USER or ROLE_ADMIN):                    │
│   - /user/**               (User dashboard, cart...)    │
│                                                          │
│ All other URLs: Require authentication                  │
└─────────────────────────────────────────────────────────┘
```

### Authentication Flow

```
1. User enters username + password
   ↓
2. Spring Security intercept
   ↓
3. UserDetailsService loads user from DB
   ↓
4. Compare password (BCrypt)
   ↓
5. Success → Create Authentication object
   ↓
6. Store in SecurityContext (session)
   ↓
7. Redirect based on role
```

### Password Encryption

- **Algorithm:** BCrypt
- **Strength:** 10 rounds (default)
- **Storage:** Only hashed password in database
- **Verification:** BCrypt.matches(plain, hashed)

---

## 📁 QUẢN LÝ FILE UPLOAD

### Cấu trúc thư mục

```
F:/datn_uploads/
└── book_asset/
    ├── image/
    │   ├── covers/          # Ảnh bìa sách
    │   ├── authors/         # Avatar tác giả
    │   └── banners/         # Banner
    └── files/
        └── ebooks/          # File PDF/EPUB
```

### URL Mapping

```
Database URL: /Book_Asset/image/covers/book1.jpg
       ↓
Browser URL: http://localhost:8080/Book_Asset/image/covers/book1.jpg
       ↓
Physical Path: F:/datn_uploads/book_asset/image/covers/book1.jpg
```

**Giải thích:**
- Database lưu URL tương đối
- Spring MVC map URL → physical path
- Browser truy cập như static resource

---

## 🎨 GIAO DIỆN NGƯỜI DÙNG

### Admin Panel
- **Template:** AdminLTE 3.x
- **Features:**
  - Dashboard với thống kê
  - Quản lý sách (CRUD)
  - Quản lý user
  - Quản lý đơn hàng
  - Báo cáo doanh thu

### User Interface
- **Template:** Bootstrap 5 custom
- **Features:**
  - Trang chủ với sách mới, sách hot
  - Tìm kiếm, filter sách
  - Chi tiết sách, preview
  - Giỏ hàng, checkout
  - Thư viện cá nhân
  - Lịch sử mua hàng

---

## 📈 TIẾN ĐỘ DỰ ÁN

### Đã hoàn thành (60% Backend)

✅ **Database Layer (100%)**
- 18 tables với relationships
- Indexes và constraints
- Sample data

✅ **Entity Layer (100%)**
- 19 entities
- JPA annotations
- Relationships mapping

✅ **Repository Layer (100%)**
- 18 repositories
- 87+ custom query methods
- Derived queries

✅ **Service Layer (70%)**
- Core services implemented
- Business logic
- File upload

🔄 **Controller Layer (40%)**
- Auth controller ✅
- Admin book controller ✅
- User controller (in progress)

🔄 **Frontend (30%)**
- Admin templates ✅
- Auth pages ✅
- User templates (in progress)

### Đang phát triển

🔄 **DTOs (10%)**
- LoginDto, RegisterDto ✅
- BookDTO ✅
- Other DTOs needed

🔄 **Payment Integration (0%)**
- VNPay planned
- Momo planned

🔄 **Email Service (0%)**
- Order confirmation
- Password reset

### Chưa bắt đầu

❌ **Testing (0%)**
- Unit tests
- Integration tests

❌ **Deployment (0%)**
- Production config
- CI/CD pipeline

---

## 📚 TÀI LIỆU THAM KHẢO

### Trong dự án
- `docs/SYSTEM_FLOWS.md` - Luồng xử lý chi tiết
- `docs/FLOW_AUTHENTICATION.md` - Luồng authentication
- `docs/PROJECT_PROGRESS.md` - Tiến độ dự án
- `docs/TODO.md` - Danh sách công việc

### External
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Security Docs](https://spring.io/projects/spring-security)
- [Thymeleaf Docs](https://www.thymeleaf.org/)

---

## 🚀 HƯỚNG DẪN CHẠY DỰ ÁN

### 1. Requirements
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- IDE (IntelliJ IDEA / Eclipse)

### 2. Setup Database
```sql
-- Create database
CREATE DATABASE ebook_store;

-- Import schema
source DB/ebook_store.sql;
```

### 3. Configure Application
```properties
# src/main/resources/application.properties

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ebook_store
spring.datasource.username=root
spring.datasource.password=your_password

# Upload directory
file.upload-dir=F:/datn_uploads/
```

### 4. Run Application
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

### 5. Access Application
- **Home:** http://localhost:8080
- **Login:** http://localhost:8080/auth/login
- **Admin:** http://localhost:8080/admin/dashboard

### Default Accounts
```
Admin:
  username: admin
  password: admin123

User:
  username: user1
  password: user123
```

---

## 🤝 ĐÓNG GÓP

### Quy trình làm việc
1. Tạo branch mới từ `develop`
2. Implement feature theo `TODO.md`
3. Update documentation nếu cần
4. Commit với message rõ ràng
5. Push và tạo Pull Request
6. Review code và merge

### Code Standards
- Follow Java naming conventions
- Use Lombok to reduce boilerplate
- Write meaningful comments
- Update javadoc for public methods

---

## 📞 LIÊN HỆ

- **Project Lead:** [Name]
- **Backend Team:** [Names]
- **Frontend Team:** [Names]

---

**Last updated:** 23/11/2025  
**Version:** 1.0-SNAPSHOT

