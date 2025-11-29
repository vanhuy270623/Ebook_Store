# API Documentation - Ebook Store

## Base URL

```
http://localhost:2706
```

## Authentication

**Type**: Session-based authentication with Spring Security

**Session Cookie**: `JSESSIONID`

**Access Control**:
- **Public**: Không cần đăng nhập
- **Authenticated**: Yêu cầu đăng nhập (USER hoặc ADMIN)
- **USER**: Role USER hoặc ADMIN
- **ADMIN**: Chỉ role ADMIN

---

## API Endpoints

### 🔐 Authentication APIs

#### 1. Login Page
```http
GET /auth/login
```
**Access**: Public  
**Response**: HTML login page

#### 2. Process Login
```http
POST /auth/login
Content-Type: application/x-www-form-urlencoded
```
**Access**: Public  
**Request Body**:
```
username={username}&password={password}
```
**Success Response**: Redirect to `/home` or `/admin`  
**Error Response**: Redirect to `/auth/login?error=true`

#### 3. Register Page
```http
GET /auth/register
```
**Access**: Public  
**Response**: HTML register page

#### 4. Process Register
```http
POST /auth/register
Content-Type: application/x-www-form-urlencoded
```
**Access**: Public  
**Request Body**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string",
  "phone": "string"
}
```
**Success**: Redirect to `/auth/login?registered=true`  
**Error**: Redirect to `/auth/register?error=true`

#### 5. Logout
```http
POST /auth/logout
```
**Access**: Authenticated  
**Response**: Redirect to `/auth/login?logout=true`

---

### 🏠 Public APIs

#### 1. Homepage
```http
GET /
GET /home
```
**Access**: Public  
**Response**: HTML homepage with featured books

#### 2. Book Details (Public Preview)
```http
GET /books/{bookId}
```
**Access**: Public  
**Response**: Book information (without download links)

---

### 👤 User APIs

#### 1. User Dashboard
```http
GET /user
GET /user/dashboard
```
**Access**: USER, ADMIN  
**Response**: User dashboard HTML

#### 2. User Library
```http
GET /user/library
```
**Access**: USER, ADMIN  
**Response**: Purchased books and subscribed content

#### 3. User Profile
```http
GET /user/profile
```
**Access**: USER, ADMIN  
**Response**: User profile information

#### 4. Update Profile
```http
POST /user/profile
Content-Type: multipart/form-data
```
**Access**: USER, ADMIN  
**Request Body**:
```json
{
  "fullName": "string",
  "email": "string",
  "phone": "string",
  "address": "string",
  "dateOfBirth": "yyyy-MM-dd",
  "avatar": "file"
}
```

#### 5. Change Password
```http
POST /user/change-password
```
**Access**: USER, ADMIN  
**Request Body**:
```json
{
  "currentPassword": "string",
  "newPassword": "string",
  "confirmPassword": "string"
}
```

#### 6. View Cart
```http
GET /user/cart
```
**Access**: USER, ADMIN  
**Response**: Cart items

#### 7. Add to Cart
```http
POST /user/cart/add
Content-Type: application/json
```
**Access**: USER, ADMIN  
**Request Body**:
```json
{
  "bookId": "string"
}
```
**Response**:
```json
{
  "success": true,
  "message": "Added to cart"
}
```

#### 8. Remove from Cart
```http
DELETE /user/cart/remove/{bookId}
```
**Access**: USER, ADMIN

#### 9. Checkout
```http
POST /user/checkout
Content-Type: application/json
```
**Access**: USER, ADMIN  
**Request Body**:
```json
{
  "paymentMethod": "COD|VNPAY|MOMO",
  "couponCode": "string (optional)"
}
```

#### 10. Order History
```http
GET /user/orders
```
**Access**: USER, ADMIN  
**Response**: List of user orders

#### 11. Order Details
```http
GET /user/orders/{orderId}
```
**Access**: USER, ADMIN

#### 12. Reading Progress
```http
GET /user/reading/{bookId}
POST /user/reading/{bookId}
```
**Access**: USER, ADMIN  
**POST Body**:
```json
{
  "currentPage": 150,
  "totalPages": 300
}
```

#### 13. Submit Review
```http
POST /user/review
Content-Type: application/json
```
**Access**: USER, ADMIN  
**Request Body**:
```json
{
  "bookId": "string",
  "rating": 5,
  "comment": "string"
}
```

---

### 🛡️ Admin APIs

#### 1. Admin Dashboard
```http
GET /admin
GET /admin/dashboard
```
**Access**: ADMIN  
**Response**: Dashboard with statistics

#### 2. Statistics API
```http
GET /admin/api/statistics
```
**Access**: ADMIN  
**Response**:
```json
{
  "totalUsers": 1234,
  "totalBooks": 567,
  "totalOrders": 890,
  "totalRevenue": 12345678.00,
  "newUsersThisMonth": 45,
  "booksAddedThisMonth": 12,
  "ordersThisMonth": 234
}
```

---

### 📚 Admin - Book Management

#### 1. List Books
```http
GET /admin/books
GET /admin/books?page={page}&size={size}&keyword={keyword}
```
**Access**: ADMIN  
**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Items per page (default: 10)
- `keyword`: Search keyword (optional)

**Response**: Paginated book list

#### 2. Book Details (Admin View)
```http
GET /admin/books/{bookId}
```
**Access**: ADMIN

#### 3. Create Book Form
```http
GET /admin/books/create
```
**Access**: ADMIN  
**Response**: HTML form

#### 4. Create Book
```http
POST /admin/books/create
Content-Type: multipart/form-data
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "title": "string",
  "description": "string",
  "price": 100000,
  "categoryId": "string",
  "authorIds": ["author_1", "author_2"],
  "publisher": "string",
  "publicationYear": 2023,
  "language": "vi",
  "pages": 300,
  "isbn": "string",
  "accessType": "PURCHASE|FREE|SUBSCRIPTION|BOTH",
  "isDownloadable": true,
  "coverImage": "file",
  "sourceFile": "file (PDF/EPUB)"
}
```

#### 5. Edit Book Form
```http
GET /admin/books/edit/{bookId}
```
**Access**: ADMIN

#### 6. Update Book
```http
POST /admin/books/update/{bookId}
Content-Type: multipart/form-data
```
**Access**: ADMIN  
**Request Body**: Same as Create Book

#### 7. Delete Book
```http
POST /admin/books/delete/{bookId}
DELETE /admin/books/{bookId}
```
**Access**: ADMIN  
**Note**: CSRF token ignored for this endpoint

**Response**:
```json
{
  "success": true,
  "message": "Book deleted successfully"
}
```

#### 8. Bulk Delete Books
```http
POST /admin/books/bulk-delete
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "bookIds": ["book_01", "book_02", "book_03"]
}
```

---

### 👥 Admin - User Management

#### 1. List Users
```http
GET /admin/users
GET /admin/users?page={page}&size={size}&keyword={keyword}
```
**Access**: ADMIN

#### 2. User Details
```http
GET /admin/users/{userId}
```
**Access**: ADMIN

#### 3. Create User
```http
POST /admin/users/create
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string",
  "phone": "string",
  "roleId": "role_user|role_admin",
  "isActive": true
}
```

#### 4. Update User
```http
POST /admin/users/update/{userId}
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**: Same as Create User (password optional)

#### 5. Delete User
```http
DELETE /admin/users/{userId}
```
**Access**: ADMIN

#### 6. Activate/Deactivate User
```http
POST /admin/users/{userId}/toggle-status
```
**Access**: ADMIN

---

### 📖 Admin - Author Management

#### 1. List Authors
```http
GET /admin/authors
```
**Access**: ADMIN

#### 2. Create Author
```http
POST /admin/authors/create
Content-Type: multipart/form-data
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "name": "string",
  "biography": "string",
  "avatar": "file"
}
```

#### 3. Update Author
```http
POST /admin/authors/update/{authorId}
Content-Type: multipart/form-data
```
**Access**: ADMIN

#### 4. Delete Author
```http
DELETE /admin/authors/{authorId}
```
**Access**: ADMIN

---

### 🏷️ Admin - Category Management

#### 1. List Categories
```http
GET /admin/categories
```
**Access**: ADMIN

#### 2. Create Category
```http
POST /admin/categories/create
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "categoryName": "string",
  "description": "string",
  "displayOrder": 1,
  "isActive": true
}
```

#### 3. Update Category
```http
POST /admin/categories/update/{categoryId}
```
**Access**: ADMIN

#### 4. Delete Category
```http
DELETE /admin/categories/{categoryId}
```
**Access**: ADMIN

---

### 🎟️ Admin - Coupon Management

#### 1. List Coupons
```http
GET /admin/coupons
```
**Access**: ADMIN

#### 2. Create Coupon
```http
POST /admin/coupons/create
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "code": "BLACKFRIDAY2023",
  "description": "Black Friday Sale",
  "discountType": "PERCENTAGE|FIXED_AMOUNT",
  "discountValue": 20,
  "minOrderAmount": 100000,
  "maxDiscount": 50000,
  "usageLimit": 100,
  "startDate": "2023-11-20T00:00:00",
  "endDate": "2023-11-30T23:59:59",
  "isActive": true
}
```

#### 3. Update Coupon
```http
POST /admin/coupons/update/{couponId}
```
**Access**: ADMIN

#### 4. Delete Coupon
```http
DELETE /admin/coupons/{couponId}
```
**Access**: ADMIN

---

### 📦 Admin - Order Management

#### 1. List Orders
```http
GET /admin/orders
GET /admin/orders?status={status}&page={page}
```
**Access**: ADMIN  
**Query Parameters**:
- `status`: PENDING|CONFIRMED|COMPLETED|CANCELLED

#### 2. Order Details
```http
GET /admin/orders/{orderId}
```
**Access**: ADMIN

#### 3. Update Order Status
```http
POST /admin/orders/{orderId}/status
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "status": "CONFIRMED|COMPLETED|CANCELLED"
}
```

---

### ⭐ Admin - Review Management

#### 1. List Reviews
```http
GET /admin/reviews
GET /admin/reviews?approved={true|false}
```
**Access**: ADMIN

#### 2. Approve Review
```http
POST /admin/reviews/{reviewId}/approve
```
**Access**: ADMIN

#### 3. Reject Review
```http
POST /admin/reviews/{reviewId}/reject
```
**Access**: ADMIN

#### 4. Delete Review
```http
DELETE /admin/reviews/{reviewId}
```
**Access**: ADMIN

---

### 🎨 Admin - Banner Management

#### 1. List Banners
```http
GET /admin/banners
```
**Access**: ADMIN

#### 2. Create Banner
```http
POST /admin/banners/create
Content-Type: multipart/form-data
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "title": "string",
  "position": "HOME|CATEGORY|DETAIL",
  "targetUrl": "string",
  "image": "file",
  "isActive": true
}
```

#### 3. Update Banner
```http
POST /admin/banners/update/{bannerId}
```
**Access**: ADMIN

#### 4. Delete Banner
```http
DELETE /admin/banners/{bannerId}
```
**Access**: ADMIN

---

### 📝 Admin - Post Management

#### 1. List Posts
```http
GET /admin/posts
```
**Access**: ADMIN

#### 2. Create Post
```http
POST /admin/posts/create
Content-Type: multipart/form-data
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "title": "string",
  "content": "string (HTML)",
  "categoryId": "string",
  "thumbnail": "file",
  "isPublished": true
}
```

#### 3. Update Post
```http
POST /admin/posts/update/{postId}
```
**Access**: ADMIN

#### 4. Delete Post
```http
DELETE /admin/posts/{postId}
```
**Access**: ADMIN

---

### 💎 Admin - Subscription Management

#### 1. List Subscriptions
```http
GET /admin/subscriptions
```
**Access**: ADMIN

#### 2. Create Subscription Plan
```http
POST /admin/subscriptions/create
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "planName": "VIP|PREMIUM",
  "price": 99000,
  "durationDays": 30,
  "description": "string",
  "features": ["feature1", "feature2"]
}
```

#### 3. Assign Subscription to User
```http
POST /admin/subscriptions/assign
Content-Type: application/json
```
**Access**: ADMIN  
**Request Body**:
```json
{
  "userId": "string",
  "planName": "VIP",
  "durationDays": 30
}
```

---

## Response Formats

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* response data */ }
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error message",
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    }
  ]
}
```

### Paginated Response
```json
{
  "content": [ /* array of items */ ],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
  "last": false,
  "first": true
}
```

---

## Error Codes

| HTTP Code | Description |
|-----------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (not logged in) |
| 403 | Forbidden (no permission) |
| 404 | Not Found |
| 409 | Conflict (duplicate resource) |
| 500 | Internal Server Error |

---

## File Upload

### Endpoints
- Book cover: `/admin/books/create`, `/admin/books/update/{id}`
- Book source: `/admin/books/create`, `/admin/books/update/{id}`
- Author avatar: `/admin/authors/create`, `/admin/authors/update/{id}`
- User avatar: `/user/profile`
- Banner image: `/admin/banners/create`, `/admin/banners/update/{id}`

### Constraints
- **Max file size**: 50MB
- **Allowed types**:
  - Images: JPG, PNG, GIF
  - Ebooks: PDF, EPUB
- **Upload directory**: `F:/datn_uploads/book_asset/`

### File URL Format
```
/book_asset/image/covers/{category}/{filename}
/book_asset/image/authors/{filename}
/book_asset/image/avatars/{filename}
/book_asset/source/{category}/{filename}
```

---

## Static Resources

### Public Access
```
GET /static/**
GET /user_template/**
GET /admin_template/**
GET /shared/**
GET /book_asset/image/**
```

### Protected Access (Authenticated Only)
```
GET /book_asset/source/**
```

---

## CSRF Protection

**Cookie-based CSRF tokens** (except for delete endpoints)

**Header**: `X-CSRF-TOKEN`

**Cookie**: `XSRF-TOKEN`

**Exempted endpoints**:
- `/admin/books/delete/**`

---

## Rate Limiting

*Not implemented yet - to be added*

---

## Versioning

**Current Version**: v1 (no versioning in URL yet)

**Future**: `/api/v1/...`

---

**Tài liệu liên quan**:
- [Cấu trúc dự án](PROJECT_STRUCTURE.md)
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [Security Configuration](SECURITY_CONFIG.md)

