# TODO List - Ebook Store Project

**Cập nhật:** 28/11/2025  
**Tiến độ tổng thể:** 65% ⬆️ (+25% so với 24/11)  
**Priority:** 🔴 High | 🟡 Medium | 🟢 Low

---

## ✅ HOÀN THÀNH GẦN ĐÂY

### Sprint 28/11/2025
- ✅ Hoàn thành 100% DTOs Layer (35 DTOs)
  - 18 Request DTOs
  - 14 Response DTOs
  - 3 Common DTOs
- ✅ Hoàn thành tất cả validation annotations
- ✅ Dọn dẹp documentation (73% reduction, 44→14 files)
- ✅ Tạo PROJECT_STATUS_SUMMARY.md
- ✅ Tạo CLEANUP_REPORT_28_11_2025.md
- ✅ Tạo QUICK_START.md và INDEX.md
- ✅ Update README.md lên v2.0

### Sprint 24/11/2025
- ✅ Hoàn thành FileStorageService (File upload system)
- ✅ Hoàn thành BookAssetService (PDF/EPUB management)
- ✅ Hoàn thành OrderItemService (Order items management)
- ✅ Hoàn thành UserDeviceService (Device tracking & limits)
- ✅ Cập nhật tất cả Repositories với missing methods

### Sprint 21-23/11/2025
- ✅ Hoàn thành tất cả Service implementations (17/17)
- ✅ Hoàn thành Spring Security configuration (100%)
- ✅ Hoàn thành AdminBookController với full CRUD
- ✅ Hoàn thành File Upload System
- ✅ Tạo comprehensive documentation
- ✅ AdminUserController cơ bản (60%)

---

## 🚀 Phase 1: Core Backend (IN PROGRESS - 95% complete) ✅

### 1. Database Layer ✅ COMPLETED
**Timeline:** ~~3-4 days~~ → DONE  
**Status:** ✅ 100% Complete

- [x] Database schema design ✅
- [x] 20 Entity classes ✅
- [x] 19 Repository interfaces ✅
- [x] 87+ custom query methods ✅
- [x] Relationships & constraints ✅

---

### 2. Service Layer ✅ COMPLETED
**Timeline:** ~~5-7 days~~ → DONE  
**Status:** ✅ 100% Complete (17/17 services)

#### Core Services
- [x] UserService + UserServiceImpl ✅
- [x] BookService + BookServiceImpl ✅
- [x] AuthorService + AuthorServiceImpl ✅
- [x] CategoryService + CategoryServiceImpl ✅
- [x] CartService + CartServiceImpl ✅
- [x] CartItemService + CartItemServiceImpl ✅
- [x] OrderService + OrderServiceImpl ✅
- [x] OrderItemService + OrderItemServiceImpl ✅
- [x] ReviewService + ReviewServiceImpl ✅
- [x] ReadingProgressService + ReadingProgressServiceImpl ✅
- [x] SubscriptionService + SubscriptionServiceImpl ✅
- [x] PostService + PostServiceImpl ✅
- [x] BannerService + BannerServiceImpl ✅
- [x] CouponService + CouponServiceImpl ✅
- [x] FileStorageService + FileStorageServiceImpl ✅
- [x] BookAssetService + BookAssetServiceImpl ✅
- [x] UserDeviceService + UserDeviceServiceImpl ✅

**Features Completed:**
- ✅ @Transactional support
- ✅ Business logic implementation
- ✅ Exception handling
- ✅ File upload integration (images, PDF, EPUB)
- ✅ Password encryption (BCrypt)
- ✅ Device limit enforcement
- ✅ Book asset management
- ✅ Order item tracking

---

### 3. DTOs (Data Transfer Objects) ✅ COMPLETED
**Timeline:** ~~2-3 days~~ → DONE  
**Status:** ✅ 100% Complete (35 DTOs)

#### Request DTOs (18) ✅
- [x] LoginDto ✅
- [x] RegisterDto ✅
- [x] BookDTO ✅
- [x] UserCreateRequest ✅
- [x] UserUpdateRequest ✅
- [x] BookCreateRequest ✅
- [x] BookUpdateRequest ✅
- [x] AuthorCreateRequest ✅
- [x] AuthorUpdateRequest ✅
- [x] CategoryCreateRequest ✅
- [x] CategoryUpdateRequest ✅
- [x] BannerCreateRequest ✅
- [x] BannerUpdateRequest ✅
- [x] PostCreateRequest ✅
- [x] PostUpdateRequest ✅
- [x] CouponCreateRequest ✅
- [x] CouponUpdateRequest ✅
- [x] SubscriptionCreateRequest ✅
- [x] SubscriptionUpdateRequest ✅
- [x] OrderStatusUpdateRequest ✅
- [x] ReviewApprovalRequest ✅

#### Response DTOs (14) ✅
- [x] ApiResponse<T> ✅
- [x] PageResponse<T> ✅
- [x] UserResponse ✅
- [x] BookResponse ✅
- [x] AuthorResponse ✅
- [x] CategoryResponse ✅
- [x] BannerResponse ✅
- [x] PostResponse ✅
- [x] ReviewResponse ✅
- [x] CouponResponse ✅
- [x] SubscriptionResponse ✅
- [x] OrderResponse ✅
- [x] OrderDetailResponse ✅
- [x] StatisticsResponse ✅

#### Common DTOs (3) ✅
- [x] BookAuthorDto ✅
- [x] OrderItemDto ✅
- [x] CartItemDto ✅

**Features Completed:**
- ✅ Bean Validation annotations (@NotNull, @NotBlank, @Email, @Size, @Pattern)
- ✅ Custom validators
- ✅ Nested DTOs
- ✅ Generic wrappers
- ✅ Documentation complete

---

### 4. Controllers 🔄
**Timeline:** 1 week  
**Status:** 🔄 In Progress (50%)

#### Completed Controllers ✅
- [x] AuthController ✅ (login, register, logout)
- [x] HomeController ✅ (public pages)
- [x] AdminController ✅ (dashboard)
- [x] AdminBookController ✅ (full CRUD + upload)
- [x] AdminUserController 🔄 (60% - list, add, edit, delete)

#### Controllers Đang Làm 🔄
- [ ] UserController 🔴
  - [ ] User dashboard
  - [ ] Profile management
  - [ ] Reading history
  
- [ ] UserBookController 🔴
  - [ ] Browse books
  - [ ] Book details
  - [ ] Search & filter
  
- [ ] CartController 🔴
  - [ ] View cart
  - [ ] Add items
  - [ ] Update quantities
  - [ ] Remove items

#### Controllers Cần Làm
- [ ] OrderController 🔴
  - [ ] Checkout
  - [ ] Create order
  - [ ] Order history
  - [ ] Order details
  
- [ ] PaymentController 🔴
  - [ ] VNPay integration
  - [ ] Payment callback
  
- [ ] ReviewController 🟡
  - [ ] Write review
  - [ ] Edit review
  - [ ] Delete review
  
- [ ] ReadingController 🟡
  - [ ] Open book reader
  - [ ] Save progress
  - [ ] Bookmark

- [ ] AdminAuthorController 🟡
  - [ ] CRUD authors
  
- [ ] AdminCategoryController 🟡
  - [ ] CRUD categories
  
- [ ] AdminOrderController 🟡
  - [ ] View orders
  - [ ] Update order status
  - [ ] Order statistics
  
- [ ] AdminReviewController 🟡
  - [ ] Approve/reject reviews
  
- [ ] AdminPostController 🟡
  - [ ] CRUD blog posts
  
- [ ] AdminBannerController 🟡
  - [ ] CRUD banners
  
- [ ] AdminCouponController 🟡
  - [ ] CRUD coupons

#### Exception Handling
- [ ] GlobalExceptionHandler - @ControllerAdvice 🔴
- [ ] Custom exceptions: ResourceNotFoundException, UnauthorizedException, etc. 🔴
- [ ] Standard error response format 🔴

---

### 5. Security Configuration ✅ COMPLETED
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

#### Advanced Features (Phase 6)
- [ ] JWT authentication 🟡
- [ ] OAuth2 (Google, Facebook) 🟢
- [ ] Two-factor authentication 🟢

---

### 6. File Upload System ✅ COMPLETED
**Timeline:** ~~2 days~~ → DONE  
**Status:** ✅ 100% Complete

#### Implemented ✅
- [x] FileStorageService ✅
- [x] File validation (images, PDF, EPUB) ✅
- [x] Directory structure ✅
- [x] Resource handlers ✅
- [x] Cover image upload ✅
- [x] Auto-create directories ✅
- [x] Author avatar upload ✅
- [x] Ebook file upload (PDF/EPUB) ✅
- [x] Banner upload ✅
- [x] Path: F:\datn_uploads\book_asset\ ✅

#### Future Enhancements
- [ ] Cloud storage (AWS S3) 🟢
- [ ] Image compression/optimization 🟢
- [ ] CDN integration 🟢

---

## 🎨 Phase 2: Frontend Development (IN PROGRESS - 45%)

### 7. User Interface 🔄
**Timeline:** 7-10 days  
**Status:** 🔄 In Progress (45%)

#### Completed Pages ✅
- [x] Login page ✅
- [x] Register page ✅
- [x] Home page ✅
- [x] User dashboard ✅
- [x] Admin dashboard ✅
- [x] Admin book management (list, add, edit, delete) ✅
- [x] Admin user management (list, add, edit, delete) ✅
- [x] Layout templates (header, footer, sidebar, navbar) ✅

#### Pages Cần Làm 🔴
**User Pages:**
- [ ] Book listing with filters 🔴
- [ ] Book detail page 🔴
- [ ] Search functionality 🔴
- [ ] Cart page 🔴
- [ ] Checkout flow 🔴
- [ ] Order history 🔴
- [ ] Profile management 🔴
- [ ] Reading history 🔴
- [ ] Favorites 🔴

**Reading Interface:**
- [ ] PDF viewer (PDF.js) 🔴
- [ ] EPUB reader (ePub.js) 🔴
- [ ] Progress tracking 🟡
- [ ] Bookmarks 🟡
- [ ] Night mode 🟡

**Admin Pages:**
- [ ] Statistics dashboard 🟡
- [ ] Charts (sales, users, views) 🟡
- [ ] Authors management 🟡
- [ ] Categories management 🟡
- [ ] Orders management 🟡
- [ ] Reviews management 🟡
- [ ] Posts/Blog management 🟡
- [ ] Banners management 🟡
- [ ] Coupons management 🟡

**Auth Pages:**
- [ ] Forgot password 🟡
- [ ] Email verification 🟡
- [ ] Password reset 🟡

---

## 💳 Phase 3: Payment Integration (NOT STARTED - 0%)

### 8. VNPay Integration 🔴
**Timeline:** 2-3 days  
**Status:** ❌ Not Started (0%)

- [ ] VNPayPaymentService 🔴
- [ ] Generate payment URL 🔴
- [ ] Handle return URL 🔴
- [ ] Verify hash 🔴
- [ ] Update order status 🔴
- [ ] Test with sandbox 🔴

### 9. Payment Management 🟡
**Timeline:** 1 day

- [ ] Payment webhook handler 🟡
- [ ] Retry failed payments 🟡
- [ ] Refund handling 🟡
- [ ] Payment history 🟡

---

## 🔐 Phase 4: REST API Development (NOT STARTED - 0%)

### 10. REST API Controllers 🔴
**Timeline:** 1 week  
**Status:** ❌ Not Started (0%)

#### Public APIs
- [ ] REST AuthController - /api/auth/* 🔴
- [ ] REST BookController - /api/books/* 🔴
- [ ] REST CategoryController - /api/categories 🔴
- [ ] REST AuthorController - /api/authors 🔴

#### User APIs (Authenticated)
- [ ] REST UserController - /api/user/* 🔴
- [ ] REST CartController - /api/cart/* 🔴
- [ ] REST OrderController - /api/orders/* 🔴
- [ ] REST ReviewController - /api/reviews/* 🔴

#### Admin APIs
- [ ] REST Admin controllers - /api/admin/* 🟡

### 11. API Documentation 🟡
**Timeline:** 1-2 days

- [ ] Swagger/OpenAPI setup 🟡
- [ ] Annotate controllers with @ApiOperation 🟡
- [ ] Example requests/responses 🟡
- [ ] Postman collection 🟡

---

## 📚 Phase 5: Advanced Features (NOT STARTED - 0%)

### 12. Email Service 🟡
**Timeline:** 1-2 days

- [ ] EmailService using JavaMailSender 🟡
- [ ] Email templates (Thymeleaf) 🟡
- [ ] Welcome email 🟡
- [ ] Email verification 🟡
- [ ] Order confirmation 🟡
- [ ] Password reset 🟡

### 13. Caching 🟢
**Timeline:** 1-2 days

- [ ] Redis integration 🟢
- [ ] Cache book details 🟢
- [ ] Cache categories 🟢
- [ ] Cache statistics 🟢

### 14. Search Enhancement 🟢
**Timeline:** 2-3 days

- [ ] Elasticsearch integration 🟢
- [ ] Full-text search 🟢
- [ ] Fuzzy matching 🟢
- [ ] Search suggestions 🟢

---

## 🧪 Phase 6: Testing & Quality (NOT STARTED - 0%)

### 15. Unit Tests 🟢
**Timeline:** 3-4 days  
**Status:** ❌ Not Started (0%)

- [ ] Repository tests (@DataJpaTest) 🟢
- [ ] Service tests with Mockito 🟢
- [ ] Controller tests (@WebMvcTest) 🟢
- [ ] Test coverage > 70% 🟢

### 16. Integration Tests 🟢
**Timeline:** 2-3 days

- [ ] API integration tests (@SpringBootTest) 🟢
- [ ] Database integration tests 🟢
- [ ] Payment integration tests (mocked) 🟢
- [ ] End-to-end scenarios 🟢

---

## 📊 Phase 7: Analytics & Reporting (NOT STARTED - 0%)

### 17. Admin Analytics 🟡
**Timeline:** 3-4 days

- [ ] Revenue reports (daily, monthly, yearly) 🟡
- [ ] Top selling books 🟡
- [ ] User growth chart 🟡
- [ ] Popular categories 🟡
- [ ] Export to Excel/PDF 🟡

### 18. User Analytics 🟢
**Timeline:** 1-2 days

- [ ] Reading time tracking 🟢
- [ ] Books completed 🟢
- [ ] Reading streak 🟢
- [ ] Achievements/Badges 🟢

---

## 🚀 Phase 8: Deployment & DevOps (NOT STARTED - 0%)

### 19. Docker 🟡
**Timeline:** 1-2 days

- [ ] Dockerfile for Spring Boot app 🟡
- [ ] docker-compose.yml (app + mysql + redis) 🟡
- [ ] Multi-stage build 🟡
- [ ] Environment variables 🟡

### 20. CI/CD Pipeline 🟡
**Timeline:** 2-3 days

- [ ] GitHub Actions workflow 🟡
- [ ] Auto build on push 🟡
- [ ] Run tests 🟡
- [ ] Deploy to staging 🟡

### 21. Monitoring & Logging 🟢
**Timeline:** 1-2 days

- [ ] Logging configuration (Logback) 🟢
- [ ] Spring Boot Actuator 🟢
- [ ] Health checks 🟢
- [ ] Error tracking 🟢

### 22. Production Setup 🟢
**Timeline:** 1-2 days

- [ ] SSL certificate 🟢
- [ ] Domain setup 🟢
- [ ] Database backup strategy 🟢
- [ ] Rate limiting 🟢

---

## 🔧 Phase 9: Optimization (NOT STARTED - 0%)

### 23. Performance 🟢

- [ ] Database query optimization 🟢
- [ ] N+1 query fix 🟢
- [ ] Add database indexes 🟢
- [ ] Connection pooling (HikariCP) 🟢
- [ ] Response compression 🟢
- [ ] CDN integration 🟢

### 24. Security Hardening 🟡

- [ ] SQL injection prevention 🟡
- [ ] XSS protection 🟡
- [ ] Input validation 🟡
- [ ] Secure headers 🟡
- [ ] Security audit 🟡

---

## 🐛 Known Issues & Bugs

### High Priority 🔴
- [ ] None currently

### Medium Priority 🟡
- [ ] GlobalExceptionHandler chưa implement
- [ ] CORS configuration chưa setup

### Low Priority 🟢
- [ ] IDE warnings về BookAuthorId visibility (có thể ignore)

---

## 💡 Future Enhancements (Backlog)

### Features
- [ ] Book recommendations (AI-based) 🟢
- [ ] Social features (share, comments) 🟢
- [ ] Book clubs / Reading groups 🟢
- [ ] Gift books to friends 🟢
- [ ] Audiobook support 🟢
- [ ] Multi-language support (i18n) 🟢
- [ ] Dark mode toggle 🟢
- [ ] Progressive Web App (PWA) 🟢
- [ ] Mobile apps (iOS, Android) 🟢

### Business
- [ ] Affiliate program 🟢
- [ ] Author dashboard 🟢
- [ ] Publisher accounts 🟢
- [ ] Loyalty points program 🟢
- [ ] Referral program 🟢

---

## 📅 Timeline Summary

| Phase | Duration | Status | Progress |
|-------|----------|--------|----------|
| Phase 1: Core Backend | 2 weeks | ✅ Complete | 95% |
| Phase 2: Frontend | 2-3 weeks | 🔄 In Progress | 45% |
| Phase 3: Payment | 1 week | ❌ Not Started | 0% |
| Phase 4: REST API | 1 week | ❌ Not Started | 0% |
| Phase 5: Advanced | 2 weeks | ❌ Not Started | 0% |
| Phase 6: Testing | 1 week | ❌ Not Started | 0% |
| Phase 7: Analytics | 1 week | ❌ Not Started | 0% |
| Phase 8: Deployment | 1 week | ❌ Not Started | 0% |
| Phase 9: Optimization | Ongoing | ❌ Not Started | 0% |

**Total Estimated Time:** 10-12 weeks  
**Current Overall Progress:** **65%** ⬆️

---

## 🎯 IMMEDIATE PRIORITIES (Next Sprint)

### Week 1-2 (Controllers & Frontend) 🔴
1. **Hoàn thiện Controllers còn lại (50% → 80%)**
   - UserBookController (browse, detail, search)
   - CartController (full CRUD)
   - OrderController (checkout, history)
   - GlobalExceptionHandler

2. **Frontend User Pages (45% → 70%)**
   - Book listing & detail pages
   - Cart & Checkout UI
   - User profile & reading history

### Week 3-4 (REST API & Payment) 🔴
3. **REST API Development**
   - REST controllers cho mobile/external apps
   - API documentation (Swagger)

4. **Payment Integration**
   - VNPay integration
   - Payment flow testing

---

## ✅ Major Completed Milestones

### Infrastructure ✅
- [x] Database schema design (18 tables)
- [x] Entity classes (20 entities)
- [x] Repository interfaces (19 repos, 87+ methods)
- [x] Service layer (17 services with full implementation)
- [x] DTOs layer (35 DTOs with validation)
- [x] Spring Security (Form-based auth, RBAC)
- [x] File upload system (Multi-type support)
- [x] Project structure & configuration

### Documentation ✅
- [x] PROGRESS_REPORT series (5 files)
- [x] FLOW_AUTHENTICATION.md
- [x] COMPLETE_REQUEST_FLOWS.md
- [x] DETAILED_FLOWS.md
- [x] SYSTEM_FLOWS.md
- [x] PROJECT_STATUS_SUMMARY.md
- [x] CLEANUP_REPORT_28_11_2025.md
- [x] QUICK_START.md
- [x] INDEX.md
- [x] README.md (v2.0)
- [x] PROJECT_PROGRESS.md
- [x] TODO.md (this file)

---

## 📈 Progress Chart

```
Backend Core:     ████████████████████████░ 95%
DTOs Layer:       █████████████████████████ 100%
Controllers:      ████████████░░░░░░░░░░░░░ 50%
Frontend:         ██████████░░░░░░░░░░░░░░░ 45%
REST API:         ░░░░░░░░░░░░░░░░░░░░░░░░░ 0%
Payment:          ░░░░░░░░░░░░░░░░░░░░░░░░░ 0%
Testing:          ░░░░░░░░░░░░░░░░░░░░░░░░░ 0%
Deployment:       ░░░░░░░░░░░░░░░░░░░░░░░░░ 0%
─────────────────────────────────────────────
Overall:          ████████████████░░░░░░░░░ 65%
```

---

**Last Updated:** 28/11/2025  
**Next Review:** Sau khi hoàn thành Controllers (dự kiến 05/12/2025)  
**Current Focus:** Controllers & Frontend User Pages

**🎯 Goal:** Đạt 75% overall vào cuối tuần tới (05/12/2025)

