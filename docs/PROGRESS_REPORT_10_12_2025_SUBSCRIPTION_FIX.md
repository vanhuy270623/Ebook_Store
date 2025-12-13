# Báo Cáo Tiến Độ: Tạo Trang Gói Đăng Ký và Thanh Toán
**Ngày**: 10/12/2025  
**Người thực hiện**: Developer  
**Tính năng**: Subscription Plans & Payment Flow

---

## 📋 MỤC TIÊU YÊU CẦU

User yêu cầu:
1. ✅ Tạo trang gói đăng ký cho user (`/subscribe`)
2. ✅ Tạo các trang liên quan thanh toán gói
3. ✅ Đảm bảo tuân thủ đúng theo SQL database schema
4. ✅ Chỉnh đường dẫn trang home và index để navbar hoạt động đúng
5. ✅ Fix link "Gói VIP" trong navbar: `<a class="nav-link" href="/subscribe">Gói VIP</a>`

---

## 🐛 LỖI XUẤT HIỆN

### Lỗi 1: Template Fragment Not Found (NGHIÊM TRỌNG)
```
org.thymeleaf.exceptions.TemplateInputException: 
Error resolving fragment: "~{'user/layout/navbar' :: navbar}": 
template or fragment could not be resolved 
(template: "user/subscription/plans" - line 182, col 10)
```

**Nguyên nhân**:
- File `plans.html` trong thư mục `/user/subscription/` đang cố gọi fragment navbar
- Fragment reference không đúng cấu trúc thư mục dự án
- File `navbar.html` tồn tại tại `user/layout/navbar.html` nhưng cách gọi sai

**Hậu quả**:
- Trang `/subscribe` hoặc `/subscription/plans` không load được
- Gây lỗi 500 Internal Server Error
- User không thể xem các gói đăng ký

---

### Lỗi 2: Không Hiển Thị Đầy Đủ Các Gói

**Triệu chứng**:
- User chỉ thấy gói đang ký (FREE) trong trang plans
- Không hiện các gói khác (BASIC, PREMIUM, VIP)
- Bảng `subscriptions` trong DB có đầy đủ 4 gói nhưng UI không hiển thị

**Dữ liệu trong DB**:
```sql
-- 4 gói trong bảng subscriptions
sub_free    | FREE     | 0đ      | 3650 days
sub_basic   | BASIC    | 59,000đ | 30 days  
sub_premium | PREMIUM  | 79,000đ | 30 days
sub_vip     | VIP      | 99,000đ | 30 days
```

**Nguyên nhân có thể**:
- Controller `SubscriptionController.showSubscriptionPlans()` không truyền đủ data
- Template `plans.html` có logic filter sai
- Thymeleaf loop không render đúng

---

### Lỗi 3: Thiếu Đường Dẫn Xem Gói Đăng Ký Bản Thân

**Mô tả**:
- User không có link để xem subscription hiện tại của mình
- Cần có trang "My Subscriptions" hoặc trong profile
- Thiếu endpoint như `/subscription/my-subscriptions`

---

## 🏗️ CẤU TRÚC DỰ ÁN HIỆN TẠI

### Database Schema (Tuân Thủ)
```
subscriptions (Bảng gói)
├── subscription_id (PK)
├── package_name (FREE/BASIC/PREMIUM/VIP)
├── price
├── duration_days
├── description
├── features (JSON)
├── max_devices
├── has_ads
├── is_active
└── display_order

orders (Lưu subscription của user)
├── order_id (PK)
├── user_id (FK)
├── subscription_id (FK) - NULL nếu mua sách lẻ
├── order_type (BOOK/SUBSCRIPTION)
├── payment_status (PENDING/PAID/COMPLETED/CANCELLED)
├── payment_method (VNPAY/BANK_TRANSFER/COD)
├── start_date
├── end_date
└── created_at
```

### Folder Structure
```
templates/user/
├── layout/
│   ├── navbar.html       ✅ Tồn tại
│   ├── footer.html
│   ├── header.html
│   └── main-layout.html
├── subscription/
│   └── plans.html        ⚠️ Có lỗi template
├── payment/
│   └── bank-transfer.html ✅ Hoạt động tốt
└── index.html            ✅ Home page
```

### Controllers
```java
SubscriptionController
├── /subscription/plans         → Hiển thị tất cả gói
├── /subscription/my-subscriptions → Gói của user
└── /subscription/cancel        → Hủy gói

PaymentController  
├── /payment/initiate           → Khởi tạo thanh toán
├── /payment/vnpay/callback     → VNPay callback
└── /payment/bank-transfer      → QR chuyển khoản
```

---

## ✅ GIẢI PHÁP ĐỀ XUẤT

### 1. Fix Template Fragment Error

**Bước 1: Kiểm tra file `navbar.html`**
```bash
# Đường dẫn đúng
src/main/resources/templates/user/layout/navbar.html
```

**Bước 2: Sửa cách gọi fragment trong `plans.html`**

❌ **SAI** (Cách hiện tại):
```html
<div th:replace="~{'user/layout/navbar' :: navbar}"></div>
```

✅ **ĐÚNG** (Phải sửa thành):
```html
<!-- Option 1: Tuyệt đối -->
<div th:replace="~{user/layout/navbar :: navbar}"></div>

<!-- Option 2: Hoặc dùng layout chung -->
<div th:replace="~{user/layout/main-layout :: layout}"></div>
```

**Bước 3: Đảm bảo navbar.html có fragment definition**
```html
<!-- user/layout/navbar.html -->
<nav th:fragment="navbar" class="navbar navbar-expand-lg">
    <!-- Navbar content -->
</nav>
```

---

### 2. Fix "Không Hiển Thị Đủ Gói"

**Kiểm tra SubscriptionController**:
```java
@GetMapping("/plans")
public String showSubscriptionPlans(Authentication auth, Model model) {
    // ✅ ĐẢM BẢO: Lấy TẤT CẢ gói active
    List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
    
    // 🔍 DEBUG: Log ra xem có bao nhiêu gói
    System.out.println("Found " + subscriptions.size() + " subscriptions");
    subscriptions.forEach(s -> 
        System.out.println("- " + s.getPackageName() + ": " + s.getPrice())
    );
    
    model.addAttribute("subscriptions", subscriptions);
    
    // Kiểm tra gói hiện tại của user
    User user = getCurrentUser(auth);
    if (user != null) {
        Optional<UserSubscription> active = getActiveSubscription(user.getUserId());
        model.addAttribute("currentSubscription", active.orElse(null));
    }
    
    return "user/subscription/plans";
}
```

**Fix Thymeleaf Template**:
```html
<!-- plans.html -->
<div class="row">
    <!-- ✅ Loop qua TẤT CẢ subscriptions -->
    <div th:each="sub : ${subscriptions}" 
         class="col-md-3">
        <div class="card">
            <h3 th:text="${sub.packageName}">FREE</h3>
            <p class="price" th:text="${#numbers.formatDecimal(sub.price, 0, 'COMMA', 0, 'POINT')} + 'đ'">0đ</p>
            
            <!-- Features list -->
            <ul th:if="${sub.features != null}">
                <li th:each="feature : ${sub.features}" 
                    th:text="${feature}"></li>
            </ul>
            
            <!-- Button -->
            <a th:if="${sub.price > 0}" 
               th:href="@{/payment/initiate(subscriptionId=${sub.subscriptionId})}"
               class="btn btn-primary">
                Đăng ký ngay
            </a>
            <span th:if="${sub.price == 0}" class="badge">Đang dùng</span>
        </div>
    </div>
</div>
```

---

### 3. Thêm Trang "My Subscriptions"

**Controller Method**:
```java
@GetMapping("/my-subscriptions")
public String showMySubscriptions(Authentication auth, Model model) {
    User user = getCurrentUser(auth);
    if (user == null) {
        return "redirect:/login";
    }
    
    // Gói hiện tại
    Optional<UserSubscription> active = getActiveSubscription(user.getUserId());
    model.addAttribute("activeSubscription", active.orElse(null));
    
    // Lịch sử subscriptions
    List<UserSubscription> history = getUserSubscriptionHistory(user.getUserId());
    model.addAttribute("subscriptionHistory", history);
    
    return "user/subscription/my-subscriptions";
}
```

---

### 4. Thống Nhất Flow Thanh Toán

**Nguyên tắc**:
- ✅ `SubscriptionController`: CHỈ hiển thị thông tin gói
- ✅ `PaymentController`: XỬ LÝ TẤT CẢ thanh toán (gói + sách lẻ)

**Flow chuẩn**:
```
1. User vào /subscription/plans
   ↓
2. Click "Đăng ký" gói PREMIUM
   ↓
3. Redirect đến /payment/initiate?subscriptionId=sub_premium
   ↓
4. PaymentController xử lý:
   - Tạo order với order_type = SUBSCRIPTION
   - Chọn payment method (VNPAY/BANK_TRANSFER)
   ↓
5a. VNPAY: Redirect đến VNPay gateway
5b. Bank Transfer: Hiển thị QR code
   ↓
6. Callback/Confirm → Update order status
   ↓
7. User có quyền truy cập sách theo gói
```

---

## 📝 CHECKLIST THỰC HIỆN

### Phase 1: Fix Lỗi Nghiêm Trọng
- [ ] Sửa fragment reference trong `plans.html`
- [ ] Test trang `/subscription/plans` load thành công
- [ ] Verify navbar hiển thị đúng

### Phase 2: Fix Hiển Thị Gói
- [ ] Debug `SubscriptionController.showSubscriptionPlans()`
- [ ] Verify `subscriptionService.getActiveSubscriptions()` trả về 4 gói
- [ ] Fix Thymeleaf loop trong `plans.html`
- [ ] Test hiển thị đủ 4 gói: FREE, BASIC, PREMIUM, VIP

### Phase 3: Thêm Tính Năng
- [ ] Tạo endpoint `/subscription/my-subscriptions`
- [ ] Tạo template `my-subscriptions.html`
- [ ] Thêm link vào navbar/profile menu
- [ ] Test user xem được gói hiện tại

### Phase 4: Thống Nhất Payment Flow
- [ ] Review `PaymentController` xử lý cả gói + sách
- [ ] Đảm bảo link "Đăng ký" gọi `/payment/initiate`
- [ ] Test flow: plans → payment → callback → success
- [ ] Test cả VNPay và Bank Transfer

### Phase 5: Tuân Thủ Cấu Trúc
- [ ] Đảm bảo subscription templates dùng `user_template` layout
- [ ] Không tự tạo biến tấu CSS/JS riêng
- [ ] Tất cả payment pages trong `/user/payment/`
- [ ] Consistency với các module khác (cart, order, reading)

---

## 🧪 TEST CASES

### TC1: Hiển Thị Plans
```
Điều kiện: User chưa đăng nhập
Bước:
1. Vào /subscription/plans
2. Verify: Hiển thị 4 gói (FREE, BASIC, PREMIUM, VIP)
3. Verify: Mỗi gói có đủ thông tin (tên, giá, features)
4. Verify: Button "Đăng ký" với gói price > 0
```

### TC2: Đăng Ký Gói PREMIUM
```
Điều kiện: User đã đăng nhập, đang dùng FREE
Bước:
1. Vào /subscription/plans
2. Click "Đăng ký" gói PREMIUM
3. Verify redirect đến /payment/initiate?subscriptionId=sub_premium
4. Chọn VNPay → Thanh toán
5. Callback thành công
6. Verify: User có gói PREMIUM trong my-subscriptions
7. Verify: User đọc được sách PREMIUM
```

### TC3: Xem Gói Hiện Tại
```
Điều kiện: User có gói VIP active
Bước:
1. Vào /subscription/my-subscriptions
2. Verify: Hiển thị gói VIP
3. Verify: Hiển thị ngày bắt đầu, ngày hết hạn
4. Verify: Button "Gia hạn" hoặc "Hủy gói"
```

---

## 📊 TRẠNG THÁI HIỆN TẠI

| Tính năng | Trạng thái | Ghi chú |
|-----------|-----------|---------|
| Database Schema | ✅ Hoàn thành | Đúng cấu trúc như yêu cầu |
| SubscriptionController | ⚠️ Cần fix | Logic đúng nhưng template lỗi |
| PaymentController | ✅ Hoạt động | Bank transfer với QR code OK |
| plans.html | ❌ Lỗi | Fragment reference sai |
| navbar link | ⚠️ Cần cập nhật | Cần đổi `/subscribe` → `/subscription/plans` |
| Hiển thị đủ gói | ❌ Lỗi | Chỉ thấy gói đang dùng |
| my-subscriptions | ❌ Chưa có | Cần tạo mới |

---

## 🎯 KẾT LUẬN

### Lỗi Chính:
1. **Template Fragment Error** - Blocking issue, cần fix ngay
2. **Không hiển thị đủ gói** - Logic hoặc template render issue
3. **Thiếu trang My Subscriptions** - Missing feature

### Hành Động Tiếp Theo:
1. **Ưu tiên cao**: Fix template fragment trong `plans.html`
2. **Ưu tiên trung bình**: Debug controller để hiển thị đủ 4 gói
3. **Ưu tiên thấp**: Thêm tính năng my-subscriptions

### Timeline Dự Kiến:
- **Fix lỗi nghiêm trọng**: 30 phút
- **Fix hiển thị gói**: 1 giờ
- **Thêm my-subscriptions**: 2 giờ
- **Testing toàn bộ flow**: 1 giờ

**Tổng thời gian**: ~4-5 giờ

---

## 📂 FILES LIÊN QUAN

### Controllers:
- `src/main/java/stu/datn/ebook_store/controller/user/SubscriptionController.java`
- `src/main/java/stu/datn/ebook_store/controller/user/PaymentController.java`

### Templates:
- `src/main/resources/templates/user/subscription/plans.html` ⚠️
- `src/main/resources/templates/user/layout/navbar.html`
- `src/main/resources/templates/user/payment/bank-transfer.html` ✅
- `src/main/resources/templates/user/index.html`

### Services:
- `src/main/java/stu/datn/ebook_store/service/SubscriptionService.java`
- `src/main/java/stu/datn/ebook_store/service/OrderService.java`
- `src/main/java/stu/datn/ebook_store/service/PaymentService.java`

### Database:
- `DB/ebook_store.sql` - Bảng `subscriptions` và `orders`

---

**Người tạo báo cáo**: GitHub Copilot  
**Ngày tạo**: 2025-12-10  
**Version**: 1.0

