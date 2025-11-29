# Tài Liệu Kỹ Thuật Tổng Hợp - Ebook Store

## 📚 Danh Mục Tài Liệu

Dự án Ebook Store được tài liệu hóa đầy đủ qua các file markdown sau:

### 1. [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)
**Cấu trúc dự án tổng thể**
- Tổng quan dự án
- Cấu trúc thư mục
- Package organization
- Công nghệ sử dụng
- Cấu hình build và run

### 2. [ARCHITECTURE.md](ARCHITECTURE.md)
**Kiến trúc hệ thống**
- Layered Architecture
- Design Patterns
- Data Flow
- Transaction Management
- Scalability considerations

### 3. [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)
**Schema cơ sở dữ liệu**
- Entity Relationship Diagram
- 19 bảng database
- Relationships & Constraints
- Indexes và Foreign Keys
- Sample data

### 4. [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
**Tài liệu API endpoints**
- Authentication APIs
- Public APIs
- User APIs
- Admin APIs
- Request/Response formats
- Error codes

### 5. [SECURITY_CONFIG.md](SECURITY_CONFIG.md)
**Cấu hình bảo mật**
- Spring Security configuration
- Authentication & Authorization flow
- Role-based access control
- CSRF protection
- Password security
- Session management

### 6. [SERVICE_LAYER.md](SERVICE_LAYER.md)
**Tầng Business Logic**
- 18 service interfaces
- Service responsibilities
- Transaction management
- Exception handling
- Data transformation

### 7. [FRONTEND_STRUCTURE.md](FRONTEND_STRUCTURE.md)
**Cấu trúc Frontend**
- Thymeleaf templates
- Layout structure
- Common patterns
- Static resources
- AJAX integration

---

## 🎯 Quick Start Guide

### Yêu Cầu Hệ Thống

- **Java**: 17 hoặc cao hơn
- **Maven**: 3.6+ hoặc sử dụng Maven wrapper
- **MySQL**: 8.0+ (recommend 9.1.0)
- **IDE**: IntelliJ IDEA, Eclipse, hoặc VS Code

### Cài Đặt

#### 1. Clone hoặc mở project
```bash
cd C:\Projects\Ebook_Store
```

#### 2. Cấu hình Database
```sql
CREATE DATABASE ebook_store CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Import schema:
```bash
mysql -u root -p ebook_store < DB/ebook_store.sql
```

#### 3. Cấu hình application.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ebook_store
spring.datasource.username=root
spring.datasource.password=your_password
```

#### 4. Tạo thư mục upload
```powershell
New-Item -ItemType Directory -Path "F:\datn_uploads\book_asset" -Force
```

#### 5. Build project
```bash
mvnw clean install
```

#### 6. Run application
```bash
mvnw spring-boot:run
```

#### 7. Access application
- Homepage: http://localhost:2706
- Admin Panel: http://localhost:2706/admin
- Login: http://localhost:2706/auth/login

### Default Users

**Admin Account**:
- Username: `admin`
- Password: (check database)

**User Account**:
- Username: `user_normal_01`
- Password: (check database)

---

## 🏗️ Kiến Trúc Tổng Quan

```
┌─────────────────────────────────────────────────────┐
│                    CLIENT LAYER                      │
│              (Browser / Mobile App)                  │
└───────────────────┬─────────────────────────────────┘
                    │ HTTP/HTTPS
                    ▼
┌─────────────────────────────────────────────────────┐
│              PRESENTATION LAYER                      │
│    Controllers + Thymeleaf Views + Static Assets    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │  Admin   │  │   User   │  │   Auth   │         │
│  │Controller│  │Controller│  │Controller│         │
│  └──────────┘  └──────────┘  └──────────┘         │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│               SECURITY LAYER                         │
│         Spring Security 6 (Session-based)            │
│  Authentication │ Authorization │ CSRF Protection    │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│                SERVICE LAYER                         │
│              (Business Logic)                        │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐           │
│  │ User │  │ Book │  │Order │  │ Cart │           │
│  │Service│ │Service│ │Service│ │Service│          │
│  └──────┘  └──────┘  └──────┘  └──────┘           │
│     + 14 more services...                           │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│             REPOSITORY LAYER                         │
│         Spring Data JPA Repositories                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │   User   │  │   Book   │  │  Order   │         │
│  │   Repo   │  │   Repo   │  │   Repo   │         │
│  └──────────┘  └──────────┘  └──────────┘         │
│     + 16 more repositories...                       │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│              DATABASE LAYER                          │
│            MySQL 9.1.0 (InnoDB)                      │
│  19 tables with relationships and constraints        │
└─────────────────────────────────────────────────────┘
```

---

## 📦 Core Entities

### Main Business Entities

| Entity | Description | Key Relationships |
|--------|-------------|-------------------|
| **User** | Người dùng hệ thống | → Role, Cart, Orders, Reviews |
| **Book** | Sách điện tử | → Category, Authors, Assets |
| **Author** | Tác giả | ↔ Books (Many-to-Many) |
| **Category** | Danh mục sách | → Books |
| **Cart** | Giỏ hàng | → CartItems → Books |
| **Order** | Đơn hàng | → OrderItems → Books |
| **Review** | Đánh giá | → User, Book |
| **Coupon** | Mã giảm giá | → Orders |
| **Subscription** | Gói VIP | → User |

---

## 🔐 Security Overview

### Authentication
- **Type**: Session-based with Spring Security
- **Password**: BCrypt hashing
- **Session**: JSESSIONID cookie

### Authorization (Role-Based)

| Role | Access |
|------|--------|
| **PUBLIC** | Homepage, Login, Register, Static resources |
| **USER** | User dashboard, Cart, Orders, Reading |
| **ADMIN** | Full system access, Management panels |

### CSRF Protection
- Cookie-based CSRF tokens
- Required for all POST/PUT/DELETE requests

---

## 🛣️ Common User Flows

### 1. User Registration & Login
```
Register → Verify email → Login → Home/Dashboard
```

### 2. Browse & Purchase Books
```
Browse books → View details → Add to cart → Checkout → Payment → Access book
```

### 3. Admin Manage Books
```
Login as admin → Books management → Create/Edit book → Upload files → Save
```

### 4. User Read Book
```
Login → My Library → Select book → Read online or Download
```

---

## 📊 Key Features

### For Users (Readers)
✅ Browse books by category  
✅ Search books  
✅ View book details and reviews  
✅ Add to cart  
✅ Purchase books  
✅ Subscribe to VIP  
✅ Read online / Download  
✅ Track reading progress  
✅ Submit reviews  
✅ Manage profile  

### For Admin
✅ Dashboard with statistics  
✅ Manage books (CRUD)  
✅ Manage users  
✅ Manage orders  
✅ Manage categories & authors  
✅ Manage coupons  
✅ Manage reviews (approve/reject)  
✅ Manage banners  
✅ View reports  

---

## 🗂️ Project Statistics

### Code Metrics
- **Total Entities**: 22 classes
- **Total Repositories**: 19 interfaces
- **Total Services**: 18 interfaces + implementations
- **Total Controllers**: 6+ classes
- **Total DTOs**: 30+ classes (Request + Response)
- **Total Exceptions**: 8 custom exceptions

### Database
- **Total Tables**: 19
- **Sample Data**: ~50+ records across tables

### Frontend
- **Templates**: 20+ HTML files
- **Layouts**: Admin + User + Auth
- **Static Assets**: CSS, JS, Images

---

## 🚀 Deployment Checklist

### Development Environment
- [x] Database running locally
- [x] Application properties configured
- [x] Upload directory created
- [x] Build successful
- [x] Application running on port 2706

### Production Environment (TODO)
- [ ] Database backup strategy
- [ ] HTTPS configuration
- [ ] Secure session management
- [ ] Redis for session store
- [ ] CDN for static resources
- [ ] Environment variables for secrets
- [ ] Logging configuration
- [ ] Monitoring (Actuator endpoints)
- [ ] Rate limiting
- [ ] Database connection pooling

---

## 🧪 Testing

### Manual Testing
- Login/Logout functionality
- CRUD operations for all entities
- File upload/download
- Search and filter
- Cart and checkout flow
- Payment integration (if implemented)
- Access control (roles)
- CSRF protection

### Automated Testing (Recommended)
- Unit tests for services
- Integration tests for repositories
- Controller tests with MockMvc
- Security tests

---

## 📈 Performance Considerations

### Database
- Indexes on frequently queried columns
- Pagination for large datasets
- Lazy loading for relationships
- Connection pooling

### File Storage
- Separate storage drive (F:)
- Direct file serving (not through app)
- File size limits

### Caching (Future)
- Redis for session
- Query result caching
- Static resource caching

---

## 🐛 Common Issues & Solutions

### Issue 1: Database connection failed
**Solution**: Check MySQL is running, verify credentials in `application.properties`

### Issue 2: Upload directory not found
**Solution**: Create directory `F:\datn_uploads\book_asset\` manually

### Issue 3: Port 2706 already in use
**Solution**: Change port in `application.properties` or kill process using the port

### Issue 4: Cannot access book source files
**Solution**: Verify user is logged in and has purchased/subscribed to the book

### Issue 5: CSRF token error
**Solution**: Ensure CSRF meta tags are present and token is included in AJAX requests

---

## 📚 Learning Resources

### Spring Boot
- Official Docs: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- Spring Data JPA: https://spring.io/projects/spring-data-jpa

### Thymeleaf
- Official Docs: https://www.thymeleaf.org/documentation.html

### MySQL
- Official Docs: https://dev.mysql.com/doc/

---

## 👥 Contact & Support

**Project**: Ebook Store (DATN - Đồ Án Tốt Nghiệp)  
**Version**: 0.0.1-SNAPSHOT  
**Group ID**: stu.datn  
**Base Package**: stu.datn.ebook_store

---

## 📝 Changelog

### Version 0.0.1-SNAPSHOT (Current)
- Initial project setup
- Core entities and relationships
- Authentication and authorization
- Admin panel (basic CRUD)
- User dashboard
- Book management
- Order system
- Cart functionality
- File upload system
- Review system
- Coupon system
- Subscription system

---

## 🔜 Roadmap (Future Enhancements)

### Phase 2
- [ ] Payment gateway integration (VNPAY, MOMO)
- [ ] Email notifications
- [ ] Advanced search with filters
- [ ] Recommendation engine
- [ ] Reading statistics
- [ ] Social sharing

### Phase 3
- [ ] Mobile app (React Native / Flutter)
- [ ] RESTful API for mobile
- [ ] Microservices architecture
- [ ] Real-time notifications (WebSocket)
- [ ] Advanced analytics

### Phase 4
- [ ] AI-powered recommendations
- [ ] Multi-language support
- [ ] Cloud deployment (AWS, Azure, GCP)
- [ ] CDN integration
- [ ] Advanced security features (2FA, biometric)

---

## 📄 License

This project is part of a graduation thesis (DATN). All rights reserved.

---

**Last Updated**: November 28, 2025

**Documentation Version**: 1.0

---

## 📖 How to Use This Documentation

1. **New Team Member**: Start with `PROJECT_STRUCTURE.md` to understand overall structure
2. **Backend Developer**: Read `ARCHITECTURE.md` and `SERVICE_LAYER.md`
3. **Frontend Developer**: Focus on `FRONTEND_STRUCTURE.md`
4. **DBA**: Study `DATABASE_SCHEMA.md`
5. **Security Audit**: Review `SECURITY_CONFIG.md`
6. **API Consumer**: Check `API_DOCUMENTATION.md`

**Tip**: Use Markdown preview in your IDE for better readability!

---

Happy Coding! 🚀

