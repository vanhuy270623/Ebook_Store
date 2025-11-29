# Cấu Trúc Dự Án Ebook Store

## Tổng Quan

Dự án **Ebook Store** là một ứng dụng web quản lý và bán sách điện tử được xây dựng bằng **Spring Boot 3.5.7** với Java 17. Hệ thống hỗ trợ hai loại người dùng chính: **Admin** (quản trị viên) và **User** (người dùng thông thường).

## Thông Tin Cơ Bản

- **Tên dự án**: DATN (Đồ Án Tốt Nghiệp)
- **Group ID**: `stu.datn`
- **Artifact ID**: `Ebook_store`
- **Version**: 0.0.1-SNAPSHOT
- **Java Version**: 17
- **Spring Boot Version**: 3.5.7
- **Server Port**: 2706
- **Database**: MySQL (localhost:3306/ebook_store)

## Cấu Trúc Thư Mục Chính

```
Ebook_Store/
├── DB/                          # Database scripts
│   └── ebook_store.sql         # SQL schema và initial data
├── docs/                        # Tài liệu dự án
│   ├── PROJECT_STRUCTURE.md    # Cấu trúc dự án (file này)
│   ├── ARCHITECTURE.md         # Kiến trúc hệ thống
│   ├── DATABASE_SCHEMA.md      # Schema database
│   ├── API_DOCUMENTATION.md    # API endpoints
│   └── ... (các docs khác)
├── src/                        # Source code
│   ├── main/
│   │   ├── java/
│   │   │   └── stu/datn/ebook_store/
│   │   └── resources/
│   └── test/
├── target/                     # Build output
├── pom.xml                     # Maven configuration
└── mvnw, mvnw.cmd             # Maven wrapper
```

## Cấu Trúc Source Code (src/main/java)

```
stu.datn.ebook_store/
├── EbookStoreApplication.java  # Main application class
├── config/                     # Configuration classes
│   ├── SecurityConfig.java    # Spring Security configuration
│   └── WebMvcConfig.java      # Web MVC configuration
├── controller/                 # Controllers (MVC)
│   ├── admin/                 # Admin controllers
│   │   ├── AdminController.java
│   │   ├── AdminBookController.java
│   │   └── AdminUserController.java
│   ├── user/                  # User controllers
│   │   └── UserController.java
│   ├── AuthController.java    # Authentication controller
│   └── HomeController.java    # Home page controller
├── dto/                       # Data Transfer Objects
│   ├── request/              # Request DTOs
│   │   ├── BookCreateRequest.java
│   │   ├── UserUpdateRequest.java
│   │   └── ... (16 request classes)
│   ├── response/             # Response DTOs
│   │   ├── BookResponse.java
│   │   ├── ApiResponse.java
│   │   └── ... (13 response classes)
│   ├── LoginDto.java
│   └── RegisterDto.java
├── entity/                    # JPA Entities (Database models)
│   ├── User.java
│   ├── Role.java
│   ├── Book.java
│   ├── Author.java
│   ├── Category.java
│   ├── Order.java
│   ├── Cart.java
│   └── ... (22 entity classes)
├── repository/                # Spring Data JPA Repositories
│   ├── UserRepository.java
│   ├── BookRepository.java
│   ├── OrderRepository.java
│   └── ... (19 repository interfaces)
├── service/                   # Business logic services
│   ├── impl/                 # Service implementations
│   ├── UserService.java
│   ├── BookService.java
│   ├── OrderService.java
│   ├── FileStorageService.java
│   └── ... (18 service interfaces)
├── exception/                 # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   └── ... (8 exception classes)
└── util/                      # Utility classes
    └── PasswordEncoderUtil.java
```

## Cấu Trúc Resources (src/main/resources)

```
resources/
├── application.properties     # Application configuration
├── static/                   # Static resources (CSS, JS, images)
│   ├── admin_template/      # Admin UI template assets
│   │   ├── css/
│   │   ├── js/
│   │   ├── images/
│   │   ├── fonts/
│   │   └── plugins/
│   ├── user_template/       # User UI template assets
│   │   ├── css/
│   │   ├── js/
│   │   ├── images/
│   │   └── scss/
│   └── shared/              # Shared resources
│       ├── css/
│       ├── js/
│       └── images/
└── templates/               # Thymeleaf templates
    ├── home.html           # Homepage
    ├── admin/              # Admin pages
    │   ├── dashboard.html
    │   ├── books/
    │   ├── users/
    │   └── layout/
    ├── user/               # User pages
    │   ├── index.html
    │   └── layout/
    ├── auth/               # Authentication pages
    │   ├── login.html
    │   └── register.html
    └── error/              # Error pages
```

## Cấu Trúc Thư Mục Upload (F:\datn_uploads)

```
datn_uploads/
└── book_asset/              # Book-related files
    ├── image/              # Images
    │   ├── covers/        # Book cover images
    │   │   ├── khoahoc-vientuong/
    │   │   ├── kienthuc-hocthuat/
    │   │   ├── kinhte-quanly/
    │   │   ├── tamly-kynangsong/
    │   │   └── tieuthuyet-vanhoc/
    │   ├── authors/       # Author avatars
    │   ├── avatars/       # User avatars
    │   ├── banners/       # Banner images
    │   ├── icons/         # Icon images
    │   └── posts/         # Post images
    ├── preview/           # Book preview files
    └── source/            # Book source files (PDF, EPUB) - Protected
        ├── khoahoc-vientuong/
        ├── kienthuc-hocthuat/
        ├── kinhte-quanly/
        ├── tamly-kynangsong/
        └── tieuthuyet-vanhoc/
```

## Các Thành Phần Chính

### 1. **Backend Layers**

- **Controller Layer**: Xử lý HTTP requests và responses
- **Service Layer**: Business logic
- **Repository Layer**: Data access layer (Spring Data JPA)
- **Entity Layer**: Database models
- **DTO Layer**: Data transfer objects

### 2. **Frontend**

- **Template Engine**: Thymeleaf
- **Admin Template**: Dashboard quản trị với Bootstrap
- **User Template**: Giao diện người dùng responsive

### 3. **Security**

- **Spring Security 6**: Authentication & Authorization
- **BCrypt**: Password encryption
- **Role-based Access Control**: USER, ADMIN roles

### 4. **File Storage**

- **Upload Directory**: F:/datn_uploads
- **Max File Size**: 50MB
- **Supported Formats**: PDF, EPUB cho ebooks; JPG, PNG cho images

### 5. **Database**

- **RDBMS**: MySQL 9.1.0
- **ORM**: Hibernate (JPA)
- **Schema Management**: Validated mode (không tự động thay đổi schema)

## Package Naming Convention

- **Base Package**: `stu.datn.ebook_store`
- **Controllers**: `controller.admin`, `controller.user`
- **Services**: `service` và `service.impl`
- **Repositories**: `repository`
- **Entities**: `entity`
- **DTOs**: `dto.request`, `dto.response`
- **Exceptions**: `exception`
- **Config**: `config`
- **Utils**: `util`

## Các Công Nghệ Sử Dụng

### Core Framework
- Spring Boot 3.5.7
- Spring Web MVC
- Spring Data JPA
- Spring Security 6
- Spring Validation

### Template & View
- Thymeleaf
- Thymeleaf Security Extras

### Database
- MySQL Connector
- Hibernate

### Utilities
- Lombok (Reduce boilerplate code)
- Spring Boot Actuator (Monitoring)
- Jakarta Validation API

### Build Tool
- Maven

## File Uploads Configuration

**Location**: `F:/datn_uploads/book_asset/`

**Settings**:
- Max file size: 50MB
- Max request size: 50MB
- File size threshold: 2KB
- Multipart enabled: true

**Static Resource Mapping**:
- `/book_asset/**` → `F:/datn_uploads/book_asset/`
- `/uploads/**` → `F:/datn_uploads/`

## Port và URL

- **Application Port**: 2706
- **Base URL**: http://localhost:2706
- **Admin URL**: http://localhost:2706/admin
- **User URL**: http://localhost:2706/user
- **Auth URL**: http://localhost:2706/auth

## Lưu Ý Quan Trọng

1. **Database Connection**: Đảm bảo MySQL đang chạy trên port 3306
2. **Upload Directory**: Thư mục `F:/datn_uploads` phải tồn tại và có quyền ghi
3. **Java Version**: Cần Java 17 hoặc cao hơn
4. **Maven**: Sử dụng Maven wrapper (`mvnw`) hoặc Maven cài đặt sẵn

## Build và Run

### Build project:
```bash
mvnw clean install
```

### Run application:
```bash
mvnw spring-boot:run
```

### Access application:
- Homepage: http://localhost:2706
- Admin: http://localhost:2706/admin
- Login: http://localhost:2706/auth/login

---

**Tài liệu liên quan**:
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Security Configuration](SECURITY_CONFIG.md)

