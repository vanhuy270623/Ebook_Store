# 📚 EBOOK STORE - FLOWS DOCUMENTATION

> **Hướng dẫn đầy đủ các luồng xử lý nghiệp vụ với debugging endpoints**
> 
> **Last Updated**: 30/11/2025  
> **Version**: 2.0

---

## 📋 Mục Lục

### Core Flows (Luồng Chính)

1. **[FLOW 01: Authentication](./FLOW_01_AUTHENTICATION.md)** 🔐
   - Đăng ký tài khoản
   - Đăng nhập
   - Đăng xuất
   - Session management
   - **Debug endpoints**: Test registration, login, session check

2. **[FLOW 02: Admin Book Management](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md)** 📚
   - Danh sách sách
   - Thêm sách mới (với upload files)
   - Sửa thông tin sách
   - Xóa sách
   - Quản lý file (cover, source)
   - **Debug endpoints**: Book detail, file upload test

3. **[FLOW 03: Shopping Cart & Checkout](./FLOW_03_SHOPPING_CART_CHECKOUT.md)** 🛒
   - Browse & search books
   - Add to cart
   - View & update cart
   - Apply coupon
   - Checkout process
   - Payment integration (COD, VNPAY, MOMO)
   - **Debug endpoints**: Cart info, coupon validation, order tracking

4. **[FLOW 04: User Account Management](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md)** 👤
   - View profile
   - Update profile
   - Change password
   - Upload avatar
   - Order history
   - Reading history
   - **Debug endpoints**: User info, password test, avatar upload

---

## 🎯 Quick Navigation

### By User Role

#### **User (Khách hàng)**
- [Đăng ký/Đăng nhập](./FLOW_01_AUTHENTICATION.md)
- [Tìm kiếm và mua sách](./FLOW_03_SHOPPING_CART_CHECKOUT.md)
- [Quản lý tài khoản](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md)

#### **Admin (Quản trị viên)**
- [Quản lý sách](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md)
- [Quản lý đơn hàng](./ADMIN_ENDPOINTS_REFERENCE.md)
- [Quản lý categories, authors](./ADMIN_ENDPOINTS_REFERENCE.md)

### By Feature

#### **Authentication & Security**
- [User Registration Flow](./FLOW_01_AUTHENTICATION.md#flow-11-đăng-ký)
- [User Login Flow](./FLOW_01_AUTHENTICATION.md#flow-12-đăng-nhập)
- [Session Management](./FLOW_01_AUTHENTICATION.md#flow-13-đăng-xuất)

#### **Book Management**
- [List & Search Books](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-21-list-books)
- [Create Book](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-22-create-book)
- [Update Book](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-23-edit-book)
- [File Upload](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md#flow-25-upload-files)

#### **Shopping & Checkout**
- [Browse Books](./FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-31-browse--search-books)
- [Cart Management](./FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-32-add-to-cart)
- [Coupon System](./FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-35-apply-coupon)
- [Checkout Process](./FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-36-checkout)
- [Payment Integration](./FLOW_03_SHOPPING_CART_CHECKOUT.md#flow-37-payment-processing)

#### **User Account**
- [Profile Management](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-42-update-profile)
- [Password Change](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-43-change-password)
- [Avatar Upload](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-44-update-avatar)
- [Order History](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md#flow-45-order-history)

---

## 🔧 Debugging Guide

### Global Debug Endpoints

All flows include debugging endpoints. Access them at:

```
http://localhost:8080/debug/{endpoint}
```

### Common Debug Endpoints

| Endpoint | Description | Flow Reference |
|----------|-------------|----------------|
| `/debug/current-user` | Check authenticated user | [Flow 01](./FLOW_01_AUTHENTICATION.md#3-check-current-user-session) |
| `/debug/sessions` | Session information | [Flow 01](./FLOW_01_AUTHENTICATION.md#5-session-debugging) |
| `/debug/book/{id}` | Book details | [Flow 02](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md#3-debug-endpoint---check-book-details) |
| `/debug/upload-test` | Test file upload | [Flow 02](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md#4-debug-file-upload) |
| `/debug/cart` | Cart information | [Flow 03](./FLOW_03_SHOPPING_CART_CHECKOUT.md#1-check-cart-items) |
| `/debug/coupon/{code}` | Coupon details | [Flow 03](./FLOW_03_SHOPPING_CART_CHECKOUT.md#3-test-apply-coupon) |
| `/debug/user-info` | User profile info | [Flow 04](./FLOW_04_USER_ACCOUNT_MANAGEMENT.md#1-check-user-profile-data) |

### Quick Debug with cURL

**Check if logged in**:
```bash
curl -X GET http://localhost:8080/debug/current-user \
  -H "Cookie: JSESSIONID=xxx" \
  -L
```

**Check cart**:
```bash
curl -X GET http://localhost:8080/debug/cart \
  -H "Cookie: JSESSIONID=xxx"
```

**Check book details**:
```bash
curl -X GET http://localhost:8080/debug/book/1
```

### Logging Configuration

Add to `application.properties`:
```properties
# Enable all debug logging
logging.level.com.example.ebook_store=DEBUG

# SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG

# Security logging
logging.level.org.springframework.security=DEBUG

# Web logging
logging.level.org.springframework.web=DEBUG
```

---

## 📊 Architecture Overview

### Request Flow Architecture
```
Browser → Controller → Service → Repository → Database
   ↓          ↓           ↓
 View ← Model ← DTO/Entity
```

### Key Components

| Layer | Responsibility | Files |
|-------|---------------|-------|
| **Controller** | Handle HTTP requests | `*Controller.java` |
| **Service** | Business logic | `*Service.java`, `*ServiceImpl.java` |
| **Repository** | Database access | `*Repository.java` |
| **Entity** | Data model | `*.java` in `entity` package |
| **DTO** | Data transfer | `*Dto.java` |
| **Config** | Configuration | `SecurityConfig.java`, etc. |

### Technology Stack

- **Backend**: Spring Boot 3.x
- **Security**: Spring Security
- **Database**: MySQL 8.0
- **Template Engine**: Thymeleaf
- **File Storage**: Local file system
- **Payment**: VNPAY, MOMO APIs

---

## 🎓 How to Use This Documentation

### For Developers

1. **New Feature**: Read the relevant flow document
2. **Debugging**: Use the debug endpoints provided
3. **Testing**: Follow test scenarios in each flow
4. **Troubleshooting**: Check "Common Issues" section

### For Each Flow Document

Each flow includes:
- ✅ Sequence diagrams
- ✅ Implementation code
- ✅ SQL queries
- ✅ Debug endpoints
- ✅ Test scenarios
- ✅ Common issues & solutions

### Reading Order for New Developers

1. Start with [ARCHITECTURE.md](./ARCHITECTURE.md) for overview
2. Read [FLOW 01: Authentication](./FLOW_01_AUTHENTICATION.md) - understand security
3. Read [FLOW 03: Shopping Cart](./FLOW_03_SHOPPING_CART_CHECKOUT.md) - core business logic
4. Read [FLOW 02: Admin Management](./FLOW_02_ADMIN_BOOK_MANAGEMENT.md) - admin features
5. Read other flows as needed

---

## 🧪 Testing Strategy

### Unit Testing
- Test services in isolation
- Mock repositories
- Verify business logic

### Integration Testing
- Test complete flows
- Use test database
- Verify database state

### Manual Testing
- Use debug endpoints
- Follow test scenarios
- Verify in browser

### Tools
- **Postman**: API testing
- **cURL**: Quick CLI testing
- **Browser DevTools**: Frontend debugging
- **MySQL Workbench**: Database verification

---

## 📝 Additional Documentation

### Technical Documentation
- [Architecture](./ARCHITECTURE.md) - System architecture
- [Database Schema](./DATABASE_SCHEMA.md) - Database design
- [API Documentation](./API_DOCUMENTATION.md) - REST APIs
- [Security Config](./SECURITY_CONFIG.md) - Security setup
- [Frontend Structure](./FRONTEND_STRUCTURE.md) - Template structure

### Admin Reference
- [Admin Endpoints](./ADMIN_ENDPOINTS_REFERENCE.md) - All admin endpoints
- [Service Layer](./SERVICE_LAYER.md) - Service documentation

### Progress & Planning
- [TODO](./TODO.md) - Current tasks
- [Progress Reports](./PROGRESS_REPORT_*.md) - Historical progress
- [Project Progress](./PROJECT_PROGRESS.md) - Overall status

---

## 🆘 Troubleshooting

### Common Issues Across All Flows

| Issue | Possible Cause | Solution |
|-------|---------------|----------|
| 404 Not Found | Wrong URL or controller not mapped | Check `@RequestMapping` annotations |
| 403 Forbidden | CSRF token missing or security config | Add CSRF token in forms |
| 500 Internal Server Error | Exception in code | Check logs, add breakpoints |
| Session expired | Session timeout | Adjust `server.servlet.session.timeout` |
| File upload failed | File size limit | Adjust `spring.servlet.multipart.max-file-size` |
| Database error | Wrong SQL or constraint violation | Check SQL logs, verify data |

### Debug Checklist

- [ ] Check application logs
- [ ] Verify database state with SQL
- [ ] Test with debug endpoints
- [ ] Check browser console for errors
- [ ] Verify session/authentication
- [ ] Check file system for uploaded files
- [ ] Use breakpoints in IDE

### Getting Help

1. Check the specific flow documentation
2. Look at "Common Issues" section
3. Use debug endpoints to gather info
4. Check database state
5. Review logs with DEBUG level enabled

---

## 🔄 Version History

### Version 2.0 (30/11/2025)
- ✅ Restructured all flow documentation
- ✅ Added comprehensive debugging endpoints
- ✅ Added detailed SQL queries for each flow
- ✅ Added test scenarios
- ✅ Removed redundant fix/implementation files
- ✅ Consolidated into 4 main flows

### Version 1.x (Previous)
- Initial documentation
- Separate files for each feature
- Basic flow descriptions

---

## 📞 Contact & Support

For questions or issues:
- Check this documentation first
- Review the TODO list for known issues
- Use debug endpoints to diagnose problems
- Check the progress reports for recent changes

---

**Happy Coding! 🚀**

*This documentation is maintained alongside the codebase. Always refer to the latest version.*

