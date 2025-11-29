# Kiến Trúc Hệ Thống Ebook Store

## Tổng Quan Kiến Trúc

Ebook Store được xây dựng theo kiến trúc **Layered Architecture** (Kiến trúc phân lớp) kết hợp với **MVC Pattern**, tuân theo các nguyên tắc **SOLID** và **Clean Architecture**.

## Sơ Đồ Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│                    (Controllers + Views)                     │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Admin      │  │     User     │  │     Auth     │      │
│  │ Controllers  │  │ Controllers  │  │  Controller  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                  │                  │             │
│         └──────────────────┴──────────────────┘             │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       SERVICE LAYER                          │
│                    (Business Logic)                          │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   User   │  │   Book   │  │  Order   │  │   Cart   │   │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Author  │  │ Category │  │  Review  │  │  Coupon  │   │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                          │
│                  (Data Access Layer)                         │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   User   │  │   Book   │  │  Order   │  │   Cart   │   │
│  │   Repo   │  │   Repo   │  │   Repo   │  │   Repo   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  └────┬────────────┬─────────────┬──────────────┬─────────  │
└───────┼────────────┼─────────────┼──────────────┼───────────┘
        │            │             │              │
        └────────────┴─────────────┴──────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                          │
│                    MySQL Database                            │
├─────────────────────────────────────────────────────────────┤
│  Tables: users, roles, books, authors, categories,          │
│          orders, carts, reviews, subscriptions, etc.         │
└─────────────────────────────────────────────────────────────┘
```

## Chi Tiết Các Layer

### 1. Presentation Layer (Tầng Trình Diễn)

**Thành phần**:
- Controllers
- DTOs (Request/Response)
- Thymeleaf Templates
- Static Resources (CSS, JS, Images)

**Trách nhiệm**:
- Nhận HTTP requests từ client
- Validate input data
- Gọi Service layer để xử lý business logic
- Trả về responses (HTML views hoặc JSON)
- Xử lý exception và error handling

**Controllers**:

```
controller/
├── admin/
│   ├── AdminController.java           # Dashboard, tổng quan
│   ├── AdminBookController.java       # CRUD books, quản lý sách
│   └── AdminUserController.java       # CRUD users, quản lý người dùng
├── user/
│   └── UserController.java            # Trang người dùng, thư viện cá nhân
├── AuthController.java                # Login, Register, Logout
└── HomeController.java                # Trang chủ công khai
```

### 2. Service Layer (Tầng Business Logic)

**Thành phần**:
- Service interfaces
- Service implementations (trong package `impl`)

**Trách nhiệm**:
- Xử lý business logic
- Transaction management
- Orchestrate giữa nhiều repositories
- Validation logic phức tạp
- Data transformation (Entity ↔ DTO)

**Core Services**:

```
service/
├── UserService.java              # Quản lý users, authentication
├── BookService.java              # Quản lý books, search, filter
├── OrderService.java             # Đặt hàng, thanh toán
├── CartService.java              # Giỏ hàng
├── CartItemService.java          # Items trong giỏ hàng
├── AuthorService.java            # Quản lý tác giả
├── CategoryService.java          # Quản lý danh mục
├── ReviewService.java            # Đánh giá sách
├── CouponService.java            # Mã giảm giá
├── SubscriptionService.java      # Gói đăng ký premium
├── PostService.java              # Bài viết, blog
├── BannerService.java            # Quảng cáo, banners
├── BookAssetService.java         # Quản lý file sách (PDF, EPUB)
├── FileStorageService.java       # Upload, lưu trữ files
├── ReadingProgressService.java   # Tiến độ đọc sách
├── OrderItemService.java         # Items trong đơn hàng
└── UserDeviceService.java        # Quản lý thiết bị đăng nhập
```

### 3. Repository Layer (Tầng Truy Cập Dữ Liệu)

**Thành phần**:
- Spring Data JPA Repositories

**Trách nhiệm**:
- CRUD operations
- Custom queries (JPQL, Native SQL)
- Database interactions
- Query methods

**Repository Pattern**:
```java
public interface BookRepository extends JpaRepository<Book, String> {
    List<Book> findByTitleContaining(String keyword);
    Page<Book> findByCategoryId(String categoryId, Pageable pageable);
    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :min AND :max")
    List<Book> findByPriceRange(Double min, Double max);
}
```

### 4. Domain Layer (Tầng Domain)

**Thành phần**:
- Entity classes (JPA entities)
- Composite keys
- Enums

**Core Entities**:

| Entity | Mô tả |
|--------|-------|
| **User** | Người dùng hệ thống |
| **Role** | Vai trò (USER, ADMIN) |
| **Book** | Sách điện tử |
| **Author** | Tác giả |
| **Category** | Danh mục sách |
| **BookAuthor** | Relationship Book ↔ Author (Many-to-Many) |
| **BookCategory** | Relationship Book ↔ Category (Many-to-Many) |
| **BookAsset** | File sách (PDF, EPUB, preview) |
| **Cart** | Giỏ hàng |
| **CartItem** | Sản phẩm trong giỏ |
| **Order** | Đơn hàng |
| **OrderItem** | Sản phẩm trong đơn hàng |
| **Review** | Đánh giá sách |
| **Coupon** | Mã giảm giá |
| **Subscription** | Gói đăng ký |
| **ReadingProgress** | Tiến độ đọc |
| **Post** | Bài viết |
| **Banner** | Banner quảng cáo |
| **UserDevice** | Thiết bị đăng nhập |

## Cross-Cutting Concerns

### 1. Security Configuration

**File**: `config/SecurityConfig.java`

**Chức năng**:
- Cấu hình Spring Security
- Role-based access control
- CSRF protection
- Session management
- Authentication & Authorization

**Access Control**:
```
Public:      /, /home, /auth/**, /static/**, /book_asset/image/**
Protected:   /book_asset/source/** (Authenticated users only)
User:        /user/** (Role: USER, ADMIN)
Admin:       /admin/** (Role: ADMIN only)
```

### 2. Exception Handling

**File**: `exception/GlobalExceptionHandler.java`

**Custom Exceptions**:
- `BusinessException` - Lỗi logic nghiệp vụ
- `ResourceNotFoundException` - Không tìm thấy resource
- `DuplicateResourceException` - Trùng lặp dữ liệu
- `UnauthorizedException` - Chưa đăng nhập
- `ForbiddenException` - Không có quyền
- `FileStorageException` - Lỗi upload/storage
- `PaymentException` - Lỗi thanh toán

### 3. DTO Pattern

**Request DTOs** (`dto/request`):
- Validate input từ client
- Data binding
- Annotation-based validation (@NotNull, @Size, @Email, etc.)

**Response DTOs** (`dto/response`):
- Format data trả về client
- Hide sensitive information
- Customize output structure

**Common Responses**:
- `ApiResponse<T>` - Standard API response wrapper
- `PageResponse<T>` - Paginated response
- `ErrorResponse` - Error details

## Design Patterns Được Sử Dụng

### 1. **MVC Pattern** (Model-View-Controller)
- **Model**: Entities
- **View**: Thymeleaf templates
- **Controller**: Controllers

### 2. **Repository Pattern**
- Abstraction over data access
- Spring Data JPA implementations

### 3. **Service Layer Pattern**
- Separation of business logic
- Transaction boundaries

### 4. **DTO Pattern**
- Data transfer between layers
- Input validation
- Output formatting

### 5. **Dependency Injection**
- Constructor injection (recommended)
- Spring IoC container

### 6. **Factory Pattern**
- Response object creation
- Entity to DTO conversion

## Data Flow

### Request Flow (User → System):
```
1. HTTP Request
   ↓
2. Spring Security Filter
   ↓
3. Controller (Validate input)
   ↓
4. Service (Business logic)
   ↓
5. Repository (Database query)
   ↓
6. Entity (Domain model)
```

### Response Flow (System → User):
```
1. Entity (from database)
   ↓
2. DTO Conversion (in Service)
   ↓
3. Response object creation
   ↓
4. Controller (prepare view)
   ↓
5. Thymeleaf (render HTML)
   ↓
6. HTTP Response
```

## Transaction Management

**Boundaries**: Service layer methods

**Propagation**:
- `@Transactional` - Service methods
- Read operations: Read-only transactions
- Write operations: Read-write transactions

## File Storage Architecture

### Upload Process:
```
1. Client uploads file
   ↓
2. Controller receives MultipartFile
   ↓
3. FileStorageService validates file
   ↓
4. Save to F:/datn_uploads/book_asset/
   ↓
5. Store file path in database
   ↓
6. Return file URL to client
```

### Access Control:
- **Public**: `/book_asset/image/**` (covers, avatars, icons)
- **Protected**: `/book_asset/source/**` (PDF, EPUB files)

## Caching Strategy

**Potential Cache Layers** (có thể implement):
- Session cache (Spring Session)
- Query cache (Hibernate 2nd level cache)
- Static resource cache (Browser cache, CDN)

## Security Measures

1. **Authentication**: Session-based với Spring Security
2. **Password**: BCrypt hashing
3. **CSRF Protection**: Cookie-based CSRF tokens
4. **XSS Prevention**: Thymeleaf auto-escaping
5. **SQL Injection**: JPA/Hibernate parameterized queries
6. **File Upload**: Type validation, size limits

## Performance Considerations

1. **Database**:
   - Indexes on frequently queried columns
   - Lazy loading for relationships
   - Pagination for large result sets

2. **File Storage**:
   - Separate disk for uploads (F: drive)
   - Direct file serving (không qua application)

3. **Static Resources**:
   - Browser caching headers
   - Minified CSS/JS

## Scalability

**Current**: Monolithic application

**Future Enhancements**:
- Microservices architecture
- Separate file storage service
- API Gateway
- Load balancing
- Database replication

## Monitoring & Logging

**Spring Boot Actuator**:
- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Application info: `/actuator/info`

**Logging**:
- SQL logging: `spring.jpa.show-sql=true`
- Formatted SQL: `spring.jpa.properties.hibernate.format_sql=true`

---

**Tài liệu liên quan**:
- [Cấu trúc dự án](PROJECT_STRUCTURE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Security Configuration](SECURITY_CONFIG.md)

