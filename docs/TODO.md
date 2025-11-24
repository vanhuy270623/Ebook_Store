# TODO List - Ebook Store Project

**Cập nhật:** 23/11/2025  
**Tiến độ tổng thể:** 35% ⬆️ (+10% so với 21/11)  
**Priority:** 🔴 High | 🟡 Medium | 🟢 Low

---

## ✅ HOÀN THÀNH GẦN ĐÂY

### Sprint 21-23/11/2025
- ✅ Hoàn thành tất cả Service implementations (13/13)
- ✅ Hoàn thành Spring Security configuration (100%)
- ✅ Hoàn thành AdminBookController với full CRUD
- ✅ Hoàn thành File Upload System
- ✅ Tạo comprehensive documentation (7 files)
- ✅ Tổ chức lại tài liệu dự án
- ✅ AdminUserController cơ bản (60%)

---

## 🚀 Phase 1: Core Backend (IN PROGRESS - 70% complete)

### 1. Services Layer ✅ COMPLETED
**Timeline:** ~~2-3 days~~ → DONE  
**Status:** ✅ 100% Complete

#### All Services Implemented ✅
- [x] UserService + UserServiceImpl ✅
- [x] BookService + BookServiceImpl ✅
- [x] AuthorService + AuthorServiceImpl ✅
- [x] CategoryService + CategoryServiceImpl ✅
- [x] CartService + CartServiceImpl ✅
- [x] CartItemService + CartItemServiceImpl ✅
- [x] OrderService + OrderServiceImpl ✅
- [x] ReviewService + ReviewServiceImpl ✅
- [x] ReadingProgressService + ReadingProgressServiceImpl ✅
- [x] SubscriptionService + SubscriptionServiceImpl ✅
- [x] PostService + PostServiceImpl ✅
- [x] BannerService + BannerServiceImpl ✅
- [x] CouponService + CouponServiceImpl ✅

**Features Completed:**
- ✅ @Transactional support
- ✅ Business logic implementation
- ✅ Exception handling
- ✅ File upload integration
- ✅ Password encryption (BCrypt)

---

### 2. DTOs (Data Transfer Objects) 🔄
**Timeline:** 2-3 days  
**Status:** 🔄 In Progress (15%)

#### Request DTOs
- [x] `LoginDto.java` ✅
- [x] `RegisterDto.java` ✅
- [x] `BookDTO.java` ✅
- [ ] `BookCreateRequest.java`
- [ ] `BookUpdateRequest.java`
- [ ] `ReviewCreateRequest.java`
- [ ] `OrderCreateRequest.java`
- [ ] `SubscriptionPurchaseRequest.java`
- [ ] `CartAddItemRequest.java`
- [ ] `PostCreateRequest.java`
- [ ] `BannerCreateRequest.java`
- [ ] `AuthorCreateRequest.java`

#### Response DTOs
- [ ] `UserResponse.java`
- [ ] `BookResponse.java`
- [ ] `BookDetailResponse.java`
- [ ] `AuthorResponse.java`
- [ ] `ReviewResponse.java`
- [ ] `OrderResponse.java`
- [ ] `CartResponse.java`
- [ ] `ReadingProgressResponse.java`
- [ ] `PostResponse.java`
- [ ] `ApiResponse<T>.java` (Generic wrapper)

#### Validators
- [ ] Custom validators cho @Email, @ISBN, @Phone
- [ ] Password strength validator
- [ ] File upload validator (PDF, EPUB, Image)

#### Mappers
- [ ] `UserMapper.java`
- [ ] `BookMapper.java`
- [ ] `OrderMapper.java`
- [ ] `ReviewMapper.java`
- [ ] Or use MapStruct for auto-mapping

---

### 3. Controllers 🔄
**Timeline:** 1 week  
**Status:** 🔄 In Progress (50%)

#### Completed Controllers ✅
- [x] `AuthController.java` ✅ (login, register, logout)
- [x] `HomeController.java` ✅ (public pages)
- [x] `AdminController.java` ✅ (dashboard)
- [x] `AdminBookController.java` ✅ (full CRUD + upload)
- [x] `AdminUserController.java` 🔄 (60% - list, add, edit)
- [x] `UserController.java` 🔄 (40% - dashboard only)

#### Controllers Cần Làm
- [ ] `UserBookController.java` 🔴
  - Browse books
  - Book details
  - Search & filter
  
- [ ] `CartController.java` 🔴
  - View cart
  - Add items
  - Update quantities
  - Remove items
  
- [ ] `OrderController.java` 🔴
  - Checkout
  - Create order
  - Order history
  - Order details
  
- [ ] `PaymentController.java` 🔴
  - VNPay integration
  - Momo integration
  - Payment callback
  
- [ ] `ReviewController.java` 🟡
  - Write review
  - Edit review
  - Delete review
  
- [ ] `ReadingController.java` 🟡
  - Open book reader
  - Save progress
  - Bookmark

---

### 4. Security Configuration ✅ COMPLETED
**Timeline:** ~~3-4 days~~ → DONE  
**Status:** ✅ 100% Complete

#### Implemented Features ✅
- [x] Spring Security configuration ✅
- [x] Form-based authentication ✅
- [x] BCrypt password encoder ✅
- [x] Role-based access control (RBAC) ✅
- [x] URL authorization rules ✅
- [x] CSRF protection ✅
- [x] Session management ✅
- [x] Custom login/logout handlers ✅
- [x] UserDetailsService implementation ✅

#### Advanced Features (Optional)
- [ ] JWT authentication
- [ ] OAuth2 (Google, Facebook)
- [ ] Two-factor authentication

---

### 5. File Upload System ✅ COMPLETED
**Timeline:** ~~2 days~~ → DONE  
**Status:** ✅ 100% Complete

#### Implemented ✅
- [x] Upload service ✅
- [x] File validation ✅
- [x] Directory structure ✅
- [x] Resource handlers ✅
- [x] Cover image upload ✅
- [x] Auto-create directories ✅

#### Future Enhancements
- [ ] Author avatar upload
- [ ] Ebook file upload (PDF/EPUB)
- [ ] Banner upload
- [ ] Cloud storage (AWS S3)

### 2. Service Layer 🔴
**Timeline:** 5-7 days  
**Status:** 🔄 In Progress (~60%)

#### Core Services
- [x] `AuthService` - Login, Register (via UserService) ✅
- [x] `UserService` - Profile, Update, Password change ✅
- [x] `BookService` - CRUD, Search, Filter, Statistics ✅
- [x] `AuthorService` - CRUD authors ✅
- [x] `CategoryService` - Manage categories (via BookCategoryService) ✅
- [x] `CartService` - Add, Remove, Clear cart ✅
- [x] `CartItemService` - Cart item operations ✅
- [x] `OrderService` - Create order, Process payment, History ✅
- [x] `SubscriptionService` - Package management, Subscribe ✅
- [x] `ReviewService` - Submit, Approve, Calculate rating ✅
- [x] `ReadingProgressService` - Track progress, Continue reading ✅
- [x] `PostService` - Blog CRUD, Publish ✅
- [x] `BannerService` - Banner management ✅
- [x] `CouponService` - Coupon management ✅
- [ ] `FileStorageService` - Upload PDF, EPUB, Images

**Đã tạo:** 13/14 services (interface + implementation)

#### Business Logic to Implement
- [ ] **Access Control**: Check user có quyền đọc sách không
- [ ] **Payment Processing**: Integration với MoMo, VNPay
- [ ] **Rating Calculation**: Auto-update khi có review mới
- [ ] **Subscription Expiry**: Check và handle expired subscriptions
- [ ] **Email Service**: Send verification, order confirmation
- [ ] **Device Limit**: Enforce max_devices từ subscription
- [ ] **Download Tracking**: Track downloads, prevent abuse

---

### 3. Controller Layer 🔴
**Timeline:** 3-4 days  
**Status:** 🔄 In Progress (~25%)

**Đã tạo:** 4 controllers cơ bản
- [x] `AuthController` - Login, Register (form-based) ✅
- [x] `HomeController` - Home page ✅
- [x] `AdminController` - Admin dashboard ✅
- [x] `UserController` - User pages ✅

#### Public APIs
- [x] `AuthController` - POST /auth/login, /auth/register (form) ✅
- [ ] `AuthController` - REST API /api/auth/login, /register, /refresh
- [ ] `BookController` - GET /api/books, /books/{id}, /books/search
- [ ] `CategoryController` - GET /api/categories
- [ ] `AuthorController` - GET /api/authors
- [ ] `ReviewController` - GET /api/books/{id}/reviews
- [ ] `PostController` - GET /api/posts, /posts/{slug}

#### User APIs (Authenticated)
- [ ] `UserController` - GET/PUT /api/user/profile
- [ ] `CartController` - GET/POST/DELETE /api/cart
- [ ] `OrderController` - POST /api/orders, GET /api/orders/history
- [ ] `ReviewController` - POST /api/reviews
- [ ] `ReadingController` - GET/PUT /api/reading/progress
- [ ] `FavoriteController` - POST/DELETE /api/favorites
- [ ] `SubscriptionController` - POST /api/subscriptions/subscribe

#### Admin APIs (ADMIN only)
- [ ] `AdminBookController` - POST/PUT/DELETE /api/admin/books
- [ ] `AdminAuthorController` - POST/PUT/DELETE /api/admin/authors
- [ ] `AdminCategoryController` - POST/PUT/DELETE /api/admin/categories
- [ ] `AdminOrderController` - GET /api/admin/orders, /statistics
- [ ] `AdminReviewController` - PUT /api/admin/reviews/{id}/approve
- [ ] `AdminUserController` - GET /api/admin/users, /ban, /unban
- [ ] `AdminPostController` - POST/PUT/DELETE /api/admin/posts
- [ ] `AdminBannerController` - POST/PUT/DELETE /api/admin/banners
- [ ] `AdminDashboardController` - GET /api/admin/dashboard/stats

#### Exception Handling
- [ ] `GlobalExceptionHandler` - @ControllerAdvice
- [ ] Custom exceptions: `ResourceNotFoundException`, `UnauthorizedException`, etc.
- [ ] Standard error response format

---

## 🔐 Phase 2: Security & Authentication (2 weeks)

### 4. JWT Authentication 🔴
**Timeline:** 2-3 days
**Status:** ❌ Not Started (0%)

- [ ] `JwtTokenProvider` - Generate, validate, parse JWT
- [ ] `JwtAuthenticationFilter` - Filter for Bearer token
- [ ] `JwtAuthenticationEntryPoint` - Handle 401 Unauthorized
- [ ] Access token (15 min) + Refresh token (7 days)
- [ ] Store refresh tokens in database
- [ ] Logout: Invalidate refresh token
- [ ] Token blacklist (optional, using Redis)

### 5. Spring Security Config 🔴
**Timeline:** 1-2 days
**Status:** 🔄 In Progress (~50%)

- [x] `SecurityConfig` - Configure HTTP security ✅
- [x] Password encoder (BCrypt) ✅
- [x] CSRF protection ✅
- [x] Public vs Protected endpoints ✅
- [x] Role-based authorization (@PreAuthorize) ✅
- [ ] CORS configuration
- [ ] Method security (@Secured)

### 6. OAuth2 Integration 🟡
**Timeline:** 2-3 days

- [ ] Google OAuth2 login
- [ ] Facebook OAuth2 login
- [ ] Link social accounts to existing users

---

## 💳 Phase 3: Payment Integration (1 week)

### 7. MoMo Integration 🔴
**Timeline:** 2-3 days

- [ ] `MoMoPaymentService`
- [ ] Generate payment URL
- [ ] Handle IPN callback
- [ ] Verify signature
- [ ] Update order status
- [ ] Test with sandbox

### 8. VNPay Integration 🔴
**Timeline:** 2-3 days

- [ ] `VNPayPaymentService`
- [ ] Generate payment URL
- [ ] Handle return URL
- [ ] Verify hash
- [ ] Update order status
- [ ] Test with sandbox

### 9. Payment Management 🟡
**Timeline:** 1 day

- [ ] Webhook handler cho payment notifications
- [ ] Retry failed payments
- [ ] Refund handling
- [ ] Payment history

---

## 🎨 Phase 4: Frontend Development (2-3 weeks)

### 10. User Interface 🟡
**Timeline:** 7-10 days
**Status:** 🔄 In Progress (~30%)

#### Home Page
- [ ] Banner carousel
- [ ] Featured books
- [ ] New arrivals
- [ ] Top rated books
- [ ] Categories grid

#### Book Pages
- [ ] Book listing with filters
- [ ] Search functionality
- [ ] Book detail page
- [ ] Add to cart
- [ ] Reviews section
- [ ] Related books

#### User Dashboard
- [ ] Profile management
- [ ] Reading history
- [ ] Continue reading
- [ ] Favorites
- [ ] Purchased books

#### Cart & Checkout
- [ ] Cart page
- [ ] Checkout flow
- [ ] Payment method selection
- [ ] Order confirmation

#### Reading Interface
- [ ] PDF viewer (PDF.js)
- [ ] EPUB reader (ePub.js)
- [ ] Progress tracking
- [ ] Bookmarks
- [ ] Night mode

#### Auth Pages
- [x] Login page (đã có) ✅
- [x] Register page (đã có) ✅
- [ ] Forgot password
- [ ] Email verification

**Templates đã tạo:**
- [x] `auth/login.html` ✅
- [x] `auth/register.html` ✅
- [x] `user/index.html` ✅
- [x] `user/layout/` (head, header, footer, navbar, scripts, main-layout) ✅
- [x] `home.html` ✅

---

### 11. Admin Interface 🟡
**Timeline:** 5-7 days
**Status:** 🔄 In Progress (~20%)

#### Dashboard
- [ ] Statistics cards (users, books, orders, revenue)
- [ ] Charts (sales, users, views)
- [ ] Recent orders
- [ ] Quick actions

#### Management Pages
- [ ] Users management (list, view, ban, unban)
- [ ] Books management (list, create, edit, delete)
- [ ] Authors management
- [ ] Categories management
- [ ] Orders management (view, update status)
- [ ] Reviews management (approve, reject)
- [ ] Subscriptions management
- [ ] Posts/Blog management
- [ ] Banners management

#### Upload Features
- [ ] Book cover upload
- [ ] PDF/EPUB upload with progress
- [ ] Author avatar upload
- [ ] Post thumbnail upload

**Templates đã tạo:**
- [x] `admin/dashboard.html` ✅
- [x] `admin/layout/` (head, header, footer, sidebar, scripts) ✅

---

## 🧪 Phase 5: Testing & Quality (1 week)

### 12. Unit Tests 🟢
**Timeline:** 3-4 days

- [ ] Repository tests (@DataJpaTest)
- [ ] Service tests with Mockito
- [ ] Controller tests (@WebMvcTest)
- [ ] Test coverage > 70%

### 13. Integration Tests 🟢
**Timeline:** 2-3 days

- [ ] API integration tests (@SpringBootTest)
- [ ] Database integration tests
- [ ] Payment integration tests (mocked)
- [ ] End-to-end scenarios

---

## 📚 Phase 6: Advanced Features (2 weeks)

### 14. Email Service 🟡
**Timeline:** 1-2 days

- [ ] `EmailService` using JavaMailSender
- [ ] Email templates (Thymeleaf)
- [ ] Welcome email
- [ ] Email verification
- [ ] Order confirmation
- [ ] Subscription expiry reminder
- [ ] Password reset

### 15. File Management 🟡
**Timeline:** 2-3 days

- [ ] Local file storage
- [ ] Or AWS S3 integration
- [ ] File upload API
- [ ] Image resizing
- [ ] PDF/EPUB validation
- [ ] Serving static files

### 16. Caching 🟢
**Timeline:** 1-2 days

- [ ] Redis integration
- [ ] Cache book details
- [ ] Cache categories
- [ ] Cache statistics
- [ ] Cache eviction strategy

### 17. Search Enhancement 🟢
**Timeline:** 2-3 days

- [ ] Elasticsearch integration
- [ ] Full-text search
- [ ] Fuzzy matching
- [ ] Search suggestions
- [ ] Advanced filters

### 18. Notification System 🟢
**Timeline:** 2-3 days

- [ ] Firebase Cloud Messaging
- [ ] Push notifications
- [ ] In-app notifications
- [ ] Email notifications
- [ ] Notification preferences

---

## 📊 Phase 7: Analytics & Reporting (1 week)

### 19. Admin Analytics 🟡
**Timeline:** 3-4 days

- [ ] Revenue reports (daily, monthly, yearly)
- [ ] Top selling books
- [ ] User growth chart
- [ ] Popular categories
- [ ] Reading statistics
- [ ] Export to Excel/PDF

### 20. User Analytics 🟢
**Timeline:** 1-2 days

- [ ] Reading time tracking
- [ ] Books completed
- [ ] Reading streak
- [ ] Achievements/Badges

---

## 🚀 Phase 8: Deployment & DevOps (1 week)

### 21. Docker 🟡
**Timeline:** 1-2 days

- [ ] Dockerfile for Spring Boot app
- [ ] docker-compose.yml (app + mysql + redis)
- [ ] Multi-stage build
- [ ] Environment variables
- [ ] Volume mapping

### 22. CI/CD Pipeline 🟡
**Timeline:** 2-3 days

- [ ] GitHub Actions workflow
- [ ] Auto build on push
- [ ] Run tests
- [ ] Build Docker image
- [ ] Deploy to staging
- [ ] Deploy to production (manual approval)

### 23. Monitoring & Logging 🟢
**Timeline:** 1-2 days

- [ ] Logging configuration (Logback)
- [ ] Spring Boot Actuator
- [ ] Health checks
- [ ] Metrics (Prometheus)
- [ ] Error tracking (Sentry)
- [ ] Application monitoring (New Relic / DataDog)

### 24. Production Setup 🟢
**Timeline:** 1-2 days

- [ ] SSL certificate
- [ ] Domain setup
- [ ] Database backup strategy
- [ ] Load balancer (if needed)
- [ ] CDN for static files
- [ ] Rate limiting
- [ ] DDoS protection

---

## 📖 Phase 9: Documentation (Ongoing)

### 25. API Documentation 🟡
**Timeline:** 1-2 days

- [ ] Swagger/OpenAPI setup
- [ ] Annotate controllers with @ApiOperation
- [ ] Example requests/responses
- [ ] Authentication documentation
- [ ] Error codes reference
- [ ] Postman collection

### 26. User Documentation 🟢
**Timeline:** 2-3 days

- [ ] User guide
- [ ] FAQ
- [ ] Video tutorials
- [ ] Troubleshooting

### 27. Developer Documentation 🟢
**Timeline:** 2-3 days

- [ ] Setup guide
- [ ] Architecture overview
- [ ] Code style guide
- [ ] Contributing guidelines
- [ ] Database schema docs (đã có ✅)

---

## 🔧 Phase 10: Optimization (Ongoing)

### 28. Performance 🟢

- [ ] Database query optimization
- [ ] N+1 query fix
- [ ] Add database indexes
- [ ] Lazy loading tuning
- [ ] Connection pooling (HikariCP)
- [ ] Response compression
- [ ] Static file caching
- [ ] CDN integration

### 29. Security Hardening 🟡

- [ ] SQL injection prevention
- [ ] XSS protection
- [ ] CSRF tokens
- [ ] Rate limiting
- [ ] Input validation
- [ ] Secure headers
- [ ] Dependency updates
- [ ] Security audit

---

## 🐛 Known Issues & Bugs

### High Priority 🔴
- [ ] None currently

### Medium Priority 🟡
- [ ] IDE warnings về BookAuthorId visibility (có thể ignore)

### Low Priority 🟢
- [ ] None currently

---

## 💡 Future Enhancements (Backlog)

### Features
- [ ] Book recommendations (AI-based)
- [ ] Social features (share, comments)
- [ ] Book clubs / Reading groups
- [ ] Gift books to friends
- [ ] Audiobook support
- [ ] Multi-language support (i18n)
- [ ] Dark mode toggle
- [ ] Accessibility improvements (WCAG)
- [ ] Progressive Web App (PWA)
- [ ] Mobile apps (iOS, Android)

### Business
- [ ] Affiliate program
- [ ] Author dashboard
- [ ] Publisher accounts
- [ ] Bulk discounts
- [ ] Gift cards
- [ ] Loyalty points program
- [ ] Referral program

---

## 📅 Timeline Summary

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Core Backend | 2 weeks | 🔄 40% |
| Phase 2: Security | 1 week | 🔄 25% |
| Phase 3: Payment | 1 week | ❌ 0% |
| Phase 4: Frontend | 2-3 weeks | 🔄 25% |
| Phase 5: Testing | 1 week | ❌ 0% |
| Phase 6: Advanced | 2 weeks | ❌ 0% |
| Phase 7: Analytics | 1 week | ❌ 0% |
| Phase 8: Deployment | 1 week | ❌ 0% |
| Phase 9: Documentation | Ongoing | 🔄 20% |
| Phase 10: Optimization | Ongoing | ❌ 0% |

**Total Estimated Time:** 10-12 weeks
**Current Overall Progress:** ~25%

---

## ✅ Completed Tasks

### Infrastructure
- [x] Database schema design
- [x] Entity classes (19 entities)
- [x] Repository interfaces (18 repos, 87+ methods)
- [x] Project structure
- [x] Maven dependencies
- [x] Application configuration

### Documentation
- [x] ENTITIES_CHECKLIST.md
- [x] REPOSITORIES_CHECKLIST.md
- [x] DATABASE_SCHEMA.md
- [x] PROJECT_PROGRESS.md
- [x] TODO.md (this file)

---

**Last Updated:** 20/11/2025  
**Next Review:** Sau khi hoàn thành Phase 1

