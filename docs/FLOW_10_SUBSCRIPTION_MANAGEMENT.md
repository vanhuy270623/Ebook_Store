# 📅 FLOW 10: SUBSCRIPTION MANAGEMENT (Quản Lý Gói Đăng Ký)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 10.1: User - View Subscription Plans](#flow-101-user---view-subscription-plans)
3. [Flow 10.2: User - Subscribe to Plan](#flow-102-user---subscribe-to-plan)
4. [Flow 10.3: User - Manage Subscriptions](#flow-103-user---manage-subscriptions)
5. [Flow 10.4: User - Cancel Subscription](#flow-104-user---cancel-subscription)
6. [Flow 10.5: Admin - Manage Plans](#flow-105-admin---manage-plans)
7. [Flow 10.6: Admin - Subscription Analytics](#flow-106-admin---subscription-analytics)
8. [Subscription Lifecycle](#subscription-lifecycle)

---

## Tổng Quan

### Subscription System Architecture
```
┌────────────────────────────────────────────────────────────┐
│                  SUBSCRIPTION SYSTEM                        │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  USER SIDE                  │      ADMIN SIDE              │
│  ─────────                  │      ──────────              │
│                             │                              │
│  📋 View Plans              │   ⚙️  Manage Plans           │
│  💳 Subscribe               │   📊 Analytics               │
│  📊 My Subscriptions        │   👥 Subscriber List         │
│  ❌ Cancel                  │   💰 Revenue Tracking        │
│                             │                              │
└────────────────────────────────────────────────────────────┘
```

### Components

**User Controllers**:
- `SubscriptionController.java` - User subscription views & actions
- `PaymentController.java` - Handles subscription payment

**Admin Controllers**:
- `AdminSubscriptionController.java` - Admin subscription management

**Services**:
- `SubscriptionService.java` - Subscription CRUD operations
- `OrderService.java` - Subscription order tracking

**Entities**:
- `Subscription.java` - Subscription plans
- `Order.java` - Subscription orders (order_type = 'SUBSCRIPTION')

### URLs

**User Endpoints:**
- `GET /subscription/plans` - View all subscription plans (public, no login required)
- `GET /subscription/my-subscriptions` - View user's subscription history (requires login)
- `POST /subscription/cancel/{subscriptionId}` - Cancel subscription (requires login)

**Payment Flow (handled by PaymentController):**
- User selects plan → Creates order with `order_type = 'SUBSCRIPTION'`
- `POST /order/create?paymentMethod=VNPAY` → Creates subscription order
- `GET /payment/vnpay?orderId={id}` → VNPay payment
- After payment success → Subscription auto-activated with `start_date` and `end_date`

**Admin Endpoints:**
- `GET /admin/subscriptions` - List all subscription plans
- `GET /admin/subscriptions/create` - Create plan form
- `POST /admin/subscriptions/create` - Save new plan
- `GET /admin/subscriptions/edit/{id}` - Edit plan form
- `POST /admin/subscriptions/update/{id}` - Update plan
- `POST /admin/subscriptions/delete/{id}` - Soft delete plan
- `POST /admin/subscriptions/toggle-status/{id}` - Toggle active status

**Important Architecture Notes:**
- ✅ Subscriptions stored in `orders` table with `order_type = 'SUBSCRIPTION'`
- ✅ No separate `user_subscriptions` table
- ✅ Active subscription = Order with:
  - `order_type = 'SUBSCRIPTION'`
  - `payment_status IN ('COMPLETED', 'PAID', 'CANCELLED')`
  - `end_date > NOW()`
- ✅ Cancelled subscriptions still valid until `end_date`
- ✅ Payment handled by PaymentController (VNPay, Bank Transfer)
- ✅ Subscription auto-activated after payment success

---

## Flow 10.1: User - View Subscription Plans

### Sequence Diagram
```
User → Browser → SubscriptionController → SubscriptionService → Database
  │       │              │                        │                  │
  │ Visit /subscription/plans                                       │
  │───────────────────►│                                            │
  │       │             │ getActiveSubscriptions()                  │
  │       │             ├────────────────────────►│                 │
  │       │             │                         │ SELECT * FROM   │
  │       │             │                         │ subscriptions   │
  │       │             │                         │ WHERE is_active │
  │       │             │                         ├────────────────►│
  │       │             │◄────────────────────────┤                 │
  │       │             │                                           │
  │       │             │ [If logged in]                            │
  │       │             │ getActiveSubscription(userId)             │
  │       │             ├────────────────────────►│                 │
  │       │             │◄────────────────────────┤                 │
  │       │             │                                           │
  │◄───────────────────┤ subscription/plans.html                   │
  │ Display plans                                                   │
```

### Implementation Details

**Controller**: `UserSubscriptionController.java`

**Important Notes:**
- Controller KHÔNG xử lý payment logic
- Payment được xử lý bởi `PaymentController` và `OrderController`
- Subscriptions được lưu dưới dạng `Order` với `order_type = 'SUBSCRIPTION'`

**Method: showSubscriptionPlans()**
```java
@GetMapping("/plans")
public String showSubscriptionPlans(Model model) {
    // Lấy tất cả gói đang active
    List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
    model.addAttribute("subscriptions", subscriptions);

    // Nếu user đã đăng nhập, kiểm tra gói hiện tại
    User currentUser = getCurrentUser();
    if (currentUser != null) {
        // QUAN TRỌNG: Thêm user vào model để template có thể hiển thị
        model.addAttribute("user", currentUser);

        Optional<UserSubscription> activeSubscription =
            getActiveSubscription(currentUser.getUserId());

        model.addAttribute("currentSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());
    } else {
        model.addAttribute("hasActiveSubscription", false);
    }

    return "user/subscription/plans";
}
```

**Helper Method: getActiveSubscription()**
```java
private Optional<UserSubscription> getActiveSubscription(String userId) {
    List<Order> orders = orderService.getOrdersByUserIdAndType(
        userId, Order.OrderType.SUBSCRIPTION
    );

    return orders.stream()
        .filter(order -> {
            // Phải đã thanh toán HOẶC đã hủy 
            // (CANCELLED vẫn giữ quyền đến hết thời gian đã trả)
            if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED &&
                order.getPaymentStatus() != Order.PaymentStatus.PAID &&
                order.getPaymentStatus() != Order.PaymentStatus.CANCELLED) {
                return false;
            }
            // Phải còn trong thời hạn
            return order.getEndDate() != null && 
                   order.getEndDate().isAfter(LocalDateTime.now());
        })
        .map(UserSubscription::new)
        .findFirst();
}
```

**DTO: UserSubscription**
```java
public class UserSubscription {
    private String orderId;
    private String packageName;
    private BigDecimal price;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Order.PaymentStatus paymentStatus;
    private int durationDays;
    private int maxDevices;
    
    public UserSubscription(Order order) {
        this.orderId = order.getOrderId();
        this.packageName = order.getSubscription().getPackageName();
        this.price = order.getTotalAmount();
        this.startDate = order.getStartDate();
        this.endDate = order.getEndDate();
        this.paymentStatus = order.getPaymentStatus();
        this.durationDays = order.getSubscription().getDurationDays();
        this.maxDevices = order.getSubscription().getMaxDevices();
    }
    
    public boolean isActive() {
        return endDate != null && endDate.isAfter(LocalDateTime.now());
    }
    
    public long getDaysRemaining() {
        if (endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(), endDate
        );
    }
}
```

**Database Query**:
```sql
-- Get active subscription plans
SELECT * FROM subscriptions 
WHERE is_active = true 
ORDER BY price ASC;

-- Get user's active subscription
SELECT o.* FROM orders o
WHERE o.user_id = ?
AND o.order_type = 'SUBSCRIPTION'
AND o.payment_status IN ('PAID', 'COMPLETED')
AND o.end_date > NOW()
ORDER BY o.created_at DESC
LIMIT 1;
```

### Template Example

```html
<!-- subscription/plans.html -->
<div class="subscription-plans">
    <h1>Chọn gói đăng ký phù hợp</h1>
    
    <div class="plans-grid">
        <div th:each="plan : ${subscriptions}" class="plan-card"
             th:classappend="${currentSubscription?.subscriptionId == plan.subscriptionId} ? 'current-plan' : ''">
            
            <!-- Plan Header -->
            <div class="plan-header">
                <h3 th:text="${plan.packageName}">BASIC</h3>
                <div class="plan-price">
                    <span class="amount" th:text="${#numbers.formatDecimal(plan.price, 0, 'COMMA', 0, 'POINT')}">99,000</span>
                    <span class="currency">đ</span>
                    <span class="period" th:text="'/ ' + ${plan.durationDays} + ' ngày'">/ 30 ngày</span>
                </div>
            </div>
            
            <!-- Plan Features -->
            <div class="plan-features">
                <p th:text="${plan.description}">Access to premium books</p>
                <ul>
                    <li>✓ Truy cập sách subscription</li>
                    <li>✓ Đọc trên <span th:text="${plan.maxDevices}">3</span> thiết bị</li>
                    <li>✓ Tải offline</li>
                    <li>✓ Hỗ trợ 24/7</li>
                </ul>
            </div>
            
            <!-- Action Button -->
            <div class="plan-action">
                <!-- If user has this plan -->
                <button th:if="${currentSubscription?.subscriptionId == plan.subscriptionId}" 
                        class="btn btn-success" disabled>
                    Gói hiện tại
                </button>
                
                <!-- If user has different plan -->
                <button th:if="${hasActiveSubscription and currentSubscription?.subscriptionId != plan.subscriptionId}" 
                        class="btn btn-secondary" disabled>
                    Nâng cấp (coming soon)
                </button>
                
                <!-- If user has no plan -->
                <a th:if="${!hasActiveSubscription}" 
                   th:href="@{/subscription/subscribe(subscriptionId=${plan.subscriptionId})}"
                   class="btn btn-primary">
                    Đăng ký ngay
                </a>
            </div>
        </div>
    </div>
</div>
```

---

## Flow 10.2: User - Subscribe to Plan

### Sequence Diagram
```
User → Controller → SubscriptionService → OrderService → PaymentController → VNPay
  │        │               │                    │               │              │
  │ Click "Subscribe"                                                          │
  │────────►│                                                                   │
  │        │ getSubscriptionById()                                             │
  │        ├───────────────►│                                                  │
  │        │◄───────────────┤                                                  │
  │        │                                                                    │
  │        │ createSubscriptionOrder()                                         │
  │        ├───────────────────────────────►│                                  │
  │        │                                │ Generate order_id                │
  │        │                                │ Set order_type = SUBSCRIPTION    │
  │        │                                │ Set end_date = now + duration    │
  │        │                                │ Save order                       │
  │        │◄───────────────────────────────┤                                  │
  │        │                                                                    │
  │        │ redirect to payment                                               │
  │◄────────┤                                                                   │
  │        │                                                                    │
  │ Redirect to /payment/vnpay?orderId=xxx&type=subscription                  │
  │────────────────────────────────────────────────────────────────────────────►│
  │        │                                                                    │
  │ Complete payment flow (see FLOW 05)                                        │
```

### Implementation Details

**Controller Method**:
```java
@GetMapping("/subscribe")
public String subscribeToplan(@RequestParam String subscriptionId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
    try {
        // 1. Validate user
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        // 2. Check if user already has active subscription
        Optional<UserSubscription> activeSubscription = 
            getActiveSubscription(currentUser.getUserId());
        
        if (activeSubscription.isPresent()) {
            redirectAttributes.addFlashAttribute("error", 
                "Bạn đã có gói đăng ký đang hoạt động");
            return "redirect:/subscription/my-subscriptions";
        }

        // 3. Get subscription plan
        Subscription subscription = subscriptionService.getSubscriptionById(subscriptionId);
        if (subscription == null || !subscription.getIsActive()) {
            redirectAttributes.addFlashAttribute("error", "Gói đăng ký không hợp lệ");
            return "redirect:/subscription/plans";
        }

        // 4. Create subscription order
        Order order = createSubscriptionOrder(currentUser, subscription);
        
        // 5. Redirect to payment
        return "redirect:/payment/vnpay?orderId=" + order.getOrderId() + "&type=subscription";
        
    } catch (Exception e) {
        log.error("Error subscribing to plan", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi đăng ký gói. Vui lòng thử lại.");
        return "redirect:/subscription/plans";
    }
}
```

**Create Subscription Order**:
```java
private Order createSubscriptionOrder(User user, Subscription subscription) {
    Order order = new Order();
    
    // Generate order ID
    order.setOrderId("SUB_" + System.currentTimeMillis());
    
    // Set user
    order.setUser(user);
    
    // Set order type
    order.setOrderType(Order.OrderType.SUBSCRIPTION);
    
    // Set amount
    order.setTotalAmount(subscription.getPrice());
    
    // Set dates
    LocalDateTime now = LocalDateTime.now();
    order.setCreatedAt(now);
    order.setStartDate(now);
    order.setEndDate(now.plusDays(subscription.getDurationDays()));
    
    // Set status
    order.setStatus("PENDING");
    order.setPaymentStatus(Order.PaymentStatus.PENDING);
    order.setPaymentMethod(Order.PaymentMethod.VNPAY);
    
    // Set subscription reference
    order.setSubscriptionId(subscription.getSubscriptionId());
    
    // Save order
    return orderService.saveOrder(order);
}
```

**Database Insert**:
```sql
INSERT INTO orders (
    order_id, user_id, order_type, subscription_id,
    total_amount, payment_method, payment_status,
    status, start_date, end_date, created_at
) VALUES (
    'SUB_1733556789', 'user_001', 'SUBSCRIPTION', 'sub_001',
    99000, 'VNPAY', 'PENDING',
    'PENDING', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW()
);
```

---

## Flow 10.3: User - Manage Subscriptions

### Sequence Diagram
```
User → Browser → SubscriptionController → OrderService → Database
  │       │              │                      │             │
  │ Visit /subscription/my-subscriptions                     │
  │───────────────────►│                                     │
  │       │            │ getOrdersByUserIdAndType()          │
  │       │            ├─────────────────────────►│          │
  │       │            │                          │ SELECT   │
  │       │            │                          ├─────────►│
  │       │            │◄─────────────────────────┤          │
  │       │            │ map to UserSubscription              │
  │◄───────────────────┤ my-subscriptions.html               │
  │ Display subscription history                             │
```

### Implementation Details

**Controller Method**:
```java
@GetMapping("/my-subscriptions")
public String mySubscriptions(Authentication authentication, Model model,
                             RedirectAttributes redirectAttributes) {
    // 1. Validate user
    User currentUser = getCurrentUser(authentication);
    if (currentUser == null) {
        redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
        return "redirect:/auth/login";
    }

    // 2. Get active subscription
    Optional<UserSubscription> activeSubscription = 
        getActiveSubscription(currentUser.getUserId());
    
    model.addAttribute("activeSubscription", activeSubscription.orElse(null));
    model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());

    // 3. Get subscription history
    List<UserSubscription> subscriptionHistory = 
        getUserSubscriptionHistory(currentUser.getUserId());
    
    model.addAttribute("subscriptionHistory", subscriptionHistory);

    return "user/subscription/my-subscriptions";
}
```

**UserSubscription DTO**:
```java
public class UserSubscription {
    private String orderId;
    private String subscriptionId;
    private String packageName;
    private BigDecimal price;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String paymentStatus;
    private String status;
    private boolean isActive;
    private long daysRemaining;

    public UserSubscription(Order order) {
        this.orderId = order.getOrderId();
        this.subscriptionId = order.getSubscriptionId();
        this.price = order.getTotalAmount();
        this.startDate = order.getStartDate();
        this.endDate = order.getEndDate();
        this.paymentStatus = order.getPaymentStatus().name();
        this.status = order.getStatus();
        
        // Calculate if active
        this.isActive = order.getEndDate() != null && 
                        order.getEndDate().isAfter(LocalDateTime.now()) &&
                        (order.getPaymentStatus() == Order.PaymentStatus.PAID ||
                         order.getPaymentStatus() == Order.PaymentStatus.COMPLETED);
        
        // Calculate days remaining
        if (this.isActive && order.getEndDate() != null) {
            this.daysRemaining = ChronoUnit.DAYS.between(
                LocalDateTime.now(), order.getEndDate()
            );
        }
        
        // Get subscription details
        // ... fetch from SubscriptionService
    }
    
    // Getters and setters
}
```

### Template Example

```html
<!-- my-subscriptions.html -->
<div class="my-subscriptions">
    
    <!-- Active Subscription -->
    <div th:if="${hasActiveSubscription}" class="active-subscription-card">
        <h2>Gói đăng ký hiện tại</h2>
        <div th:with="sub=${activeSubscription}">
            <h3 th:text="${sub.packageName}">PREMIUM</h3>
            <p>Hết hạn: <span th:text="${#temporals.format(sub.endDate, 'dd/MM/yyyy HH:mm')}"></span></p>
            <p>Còn lại: <span th:text="${sub.daysRemaining} + ' ngày'"></span></p>
            
            <div class="subscription-actions">
                <a th:href="@{/subscription/plans}" class="btn btn-secondary">Nâng cấp</a>
                <button th:onclick="'confirmCancel(\'' + ${sub.orderId} + '\')'" 
                        class="btn btn-danger">Hủy đăng ký</button>
            </div>
        </div>
    </div>
    
    <!-- No Active Subscription -->
    <div th:unless="${hasActiveSubscription}" class="no-subscription">
        <h2>Bạn chưa có gói đăng ký nào</h2>
        <p>Đăng ký ngay để truy cập sách premium!</p>
        <a th:href="@{/subscription/plans}" class="btn btn-primary">Xem các gói</a>
    </div>
    
    <!-- Subscription History -->
    <div class="subscription-history">
        <h2>Lịch sử đăng ký</h2>
        <table class="table">
            <thead>
                <tr>
                    <th>Mã đơn</th>
                    <th>Gói</th>
                    <th>Giá</th>
                    <th>Ngày bắt đầu</th>
                    <th>Ngày kết thúc</th>
                    <th>Trạng thái</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="sub : ${subscriptionHistory}">
                    <td th:text="${sub.orderId}">SUB_001</td>
                    <td th:text="${sub.packageName}">BASIC</td>
                    <td th:text="${#numbers.formatDecimal(sub.price, 0, 'COMMA', 0, 'POINT')} + ' đ'">99,000 đ</td>
                    <td th:text="${#temporals.format(sub.startDate, 'dd/MM/yyyy')}">01/12/2025</td>
                    <td th:text="${#temporals.format(sub.endDate, 'dd/MM/yyyy')}">31/12/2025</td>
                    <td>
                        <span th:if="${sub.isActive}" class="badge badge-success">Đang hoạt động</span>
                        <span th:unless="${sub.isActive}" class="badge badge-secondary">Đã hết hạn</span>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
```

---

## Flow 10.4: User - Cancel Subscription

### Sequence Diagram
```
User → Browser → SubscriptionController → OrderService → Database
  │       │              │                      │             │
  │ Click "Cancel"                                           │
  │───────────────────►│                                     │
  │       │            │ Confirm dialog                      │
  │◄───────────────────┤                                     │
  │ Confirm                                                  │
  │───────────────────►│                                     │
  │       │            │ getOrderById()                      │
  │       │            ├─────────────────────────►│          │
  │       │            │◄─────────────────────────┤          │
  │       │            │ Validate ownership & status         │
  │       │            │ updateOrderStatus(CANCELLED)        │
  │       │            ├─────────────────────────►│          │
  │       │            │                          │ UPDATE   │
  │       │            │                          ├─────────►│
  │       │            │◄─────────────────────────┤          │
  │◄───────────────────┤ Success message                     │
  │ Show confirmation                                        │
```

### Implementation Details

**Controller Method**:
```java
@PostMapping("/cancel/{orderId}")
public String cancelSubscription(@PathVariable String orderId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
    try {
        // 1. Validate user
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        // 2. Get order
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại");
            return "redirect:/subscription/my-subscriptions";
        }

        // 3. Validate ownership
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Không có quyền hủy đơn này");
            return "redirect:/subscription/my-subscriptions";
        }

        // 4. Validate order type
        if (order.getOrderType() != Order.OrderType.SUBSCRIPTION) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không phải subscription");
            return "redirect:/subscription/my-subscriptions";
        }

        // 5. Check if can cancel
        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", 
                "Không thể hủy đơn hàng ở trạng thái này");
            return "redirect:/subscription/my-subscriptions";
        }

        // 6. Cancel order
        order.setStatus("CANCELLED");
        order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
        order.setEndDate(LocalDateTime.now()); // End subscription immediately
        orderService.updateOrder(order);

        redirectAttributes.addFlashAttribute("success", 
            "Đã hủy gói đăng ký thành công");
        
        return "redirect:/subscription/my-subscriptions";
        
    } catch (Exception e) {
        log.error("Error cancelling subscription", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi hủy gói. Vui lòng thử lại.");
        return "redirect:/subscription/my-subscriptions";
    }
}
```

**Database Update**:
```sql
UPDATE orders 
SET status = 'CANCELLED',
    payment_status = 'CANCELLED',
    end_date = NOW()
WHERE order_id = ?
AND order_type = 'SUBSCRIPTION';
```

---

## Flow 10.5: Admin - Manage Plans

### Create Subscription Plan

**Controller Method**:
```java
@GetMapping("/create")
public String createSubscriptionForm(Model model) {
    model.addAttribute("subscription", new SubscriptionCreateRequest());
    model.addAttribute("packageNames", Subscription.PackageName.values());
    model.addAttribute("isEdit", false);
    return "admin/subscription-form";
}

@PostMapping("/create")
public String createSubscription(@Valid @ModelAttribute SubscriptionCreateRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
    // Validate
    if (bindingResult.hasErrors()) {
        return "admin/subscription-form";
    }

    try {
        // Generate ID
        String subscriptionId = generateNextSubscriptionId();
        
        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(subscriptionId);
        subscription.setPackageName(Subscription.PackageName.valueOf(request.getPackageName()));
        subscription.setDescription(request.getDescription());
        subscription.setPrice(request.getPrice());
        subscription.setDurationDays(request.getDurationDays());
        subscription.setMaxDevices(request.getMaxDevices());
        subscription.setIsActive(true);
        subscription.setCreatedAt(LocalDateTime.now());
        
        subscriptionService.saveSubscription(subscription);
        
        redirectAttributes.addFlashAttribute("success", "Tạo gói đăng ký thành công");
        return "redirect:/admin/subscriptions";
        
    } catch (Exception e) {
        log.error("Error creating subscription", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi tạo gói");
        return "redirect:/admin/subscriptions/create";
    }
}
```

### Edit Subscription Plan

**Controller Method**:
```java
@GetMapping("/edit/{id}")
public String editSubscriptionForm(@PathVariable String id, Model model,
                                  RedirectAttributes redirectAttributes) {
    Subscription subscription = subscriptionService.getSubscriptionById(id);
    if (subscription == null) {
        redirectAttributes.addFlashAttribute("error", "Gói không tồn tại");
        return "redirect:/admin/subscriptions";
    }

    SubscriptionUpdateRequest dto = mapToUpdateRequest(subscription);
    model.addAttribute("subscription", dto);
    model.addAttribute("packageNames", Subscription.PackageName.values());
    model.addAttribute("isEdit", true);
    
    return "admin/subscription-form";
}

@PostMapping("/edit/{id}")
public String editSubscription(@PathVariable String id,
                              @Valid @ModelAttribute SubscriptionUpdateRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        return "admin/subscription-form";
    }

    try {
        Subscription subscription = subscriptionService.getSubscriptionById(id);
        if (subscription == null) {
            redirectAttributes.addFlashAttribute("error", "Gói không tồn tại");
            return "redirect:/admin/subscriptions";
        }

        // Update fields
        subscription.setPackageName(Subscription.PackageName.valueOf(request.getPackageName()));
        subscription.setDescription(request.getDescription());
        subscription.setPrice(request.getPrice());
        subscription.setDurationDays(request.getDurationDays());
        subscription.setMaxDevices(request.getMaxDevices());
        subscription.setIsActive(request.getIsActive());
        
        subscriptionService.updateSubscription(subscription);
        
        redirectAttributes.addFlashAttribute("success", "Cập nhật gói thành công");
        return "redirect:/admin/subscriptions";
        
    } catch (Exception e) {
        log.error("Error updating subscription", e);
        redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật gói");
        return "redirect:/admin/subscriptions/edit/" + id;
    }
}
```

### Toggle Status

**Controller Method**:
```java
@PostMapping("/toggle-status/{id}")
@ResponseBody
public ResponseEntity<?> toggleSubscriptionStatus(@PathVariable String id) {
    try {
        Subscription subscription = subscriptionService.getSubscriptionById(id);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }

        subscription.setIsActive(!subscription.getIsActive());
        subscriptionService.updateSubscription(subscription);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isActive", subscription.getIsActive());
        response.put("message", subscription.getIsActive() ? 
            "Đã kích hoạt gói" : "Đã vô hiệu hóa gói");

        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "Lỗi cập nhật trạng thái"
        ));
    }
}
```

---

## Flow 10.6: Admin - Subscription Analytics

### Statistics Methods

**Controller**:
```java
@GetMapping
public String subscriptionsList(Model model) {
    List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();
    model.addAttribute("subscriptions", subscriptions);
    model.addAttribute("totalSubscriptions", subscriptions.size());

    // Quick statistics
    long activeSubscriptions = subscriptions.stream()
        .filter(Subscription::getIsActive)
        .count();
    long inactiveSubscriptions = subscriptions.stream()
        .filter(s -> !s.getIsActive())
        .count();

    model.addAttribute("activeSubscriptions", activeSubscriptions);
    model.addAttribute("inactiveSubscriptions", inactiveSubscriptions);

    // Revenue statistics
    BigDecimal subscriptionRevenue = orderService.getRevenueByOrderType(Order.OrderType.SUBSCRIPTION);
    Long totalSubscribers = orderService.getActiveSubscribersCount();
    
    model.addAttribute("subscriptionRevenue", subscriptionRevenue);
    model.addAttribute("totalSubscribers", totalSubscribers);

    return "admin/subscription-list";
}
```

**Service Methods**:
```java
// OrderService.java
public Long getActiveSubscribersCount() {
    return orderRepository.countActiveSubscribers(LocalDateTime.now());
}

public BigDecimal getRevenueByOrderType(Order.OrderType orderType) {
    return orderRepository.sumTotalAmountByOrderType(orderType);
}

public Map<String, Long> getSubscribersByPlan() {
    List<Object[]> results = orderRepository.countSubscribersByPlan(LocalDateTime.now());
    
    Map<String, Long> subscribersByPlan = new HashMap<>();
    for (Object[] result : results) {
        String planId = (String) result[0];
        Long count = (Long) result[1];
        subscribersByPlan.put(planId, count);
    }
    
    return subscribersByPlan;
}
```

**Repository Queries**:
```java
// OrderRepository.java
@Query("SELECT COUNT(DISTINCT o.user.userId) FROM Order o " +
       "WHERE o.orderType = 'SUBSCRIPTION' " +
       "AND o.paymentStatus IN ('PAID', 'COMPLETED') " +
       "AND o.endDate > :now")
Long countActiveSubscribers(@Param("now") LocalDateTime now);

@Query("SELECT o.subscriptionId, COUNT(DISTINCT o.user.userId) " +
       "FROM Order o " +
       "WHERE o.orderType = 'SUBSCRIPTION' " +
       "AND o.paymentStatus IN ('PAID', 'COMPLETED') " +
       "AND o.endDate > :now " +
       "GROUP BY o.subscriptionId")
List<Object[]> countSubscribersByPlan(@Param("now") LocalDateTime now);
```

---

## Subscription Lifecycle

### Status Flow
```
┌─────────────┐
│   CREATED   │ (New subscription plan created by admin)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   ACTIVE    │ (Available for users to subscribe)
└──────┬──────┘
       │
       ├─────► User subscribes ──────► Order created (PENDING)
       │                                      │
       │                                      ▼
       │                              Payment completed
       │                                      │
       │                                      ▼
       │                              Order status: COMPLETED
       │                              User has access
       │                                      │
       │                              ┌───────┴────────┐
       │                              │                │
       │                         End date     User cancels
       │                          reached           │
       │                              │             ▼
       │                              ▼      Order: CANCELLED
       │                          Expired    Access revoked
       │
       ▼
┌─────────────┐
│  INACTIVE   │ (Admin disables plan, no new subscriptions)
└─────────────┘
```

### Access Control

**Check User Access**:
```java
public boolean hasSubscriptionAccess(String userId, String bookId) {
    // 1. Get book
    Book book = bookRepository.findById(bookId).orElse(null);
    if (book == null) return false;
    
    // 2. Check if book requires subscription
    if (book.getAccessType() != Book.AccessType.SUBSCRIPTION) {
        return true; // Not a subscription book
    }
    
    // 3. Check user's active subscription
    Optional<Order> activeSubscription = orderRepository.findActiveSubscription(
        userId, LocalDateTime.now()
    );
    
    return activeSubscription.isPresent();
}
```

**Repository Query**:
```sql
SELECT * FROM orders
WHERE user_id = ?
AND order_type = 'SUBSCRIPTION'
AND payment_status IN ('PAID', 'COMPLETED')
AND end_date > NOW()
ORDER BY end_date DESC
LIMIT 1;
```

---

## Best Practices

### 1. **Renewal Logic**
```java
// Auto-renewal (future enhancement)
@Scheduled(cron = "0 0 0 * * *") // Daily at midnight
public void processSubscriptionRenewals() {
    // Find subscriptions expiring in 3 days
    LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
    List<Order> expiringSubscriptions = orderRepository.findExpiringSubscriptions(threeDaysFromNow);
    
    for (Order order : expiringSubscriptions) {
        // Send renewal reminder email
        emailService.sendRenewalReminder(order.getUser(), order);
    }
}
```

### 2. **Grace Period**
```java
// Allow 3-day grace period after expiration
public boolean hasSubscriptionAccessWithGrace(String userId) {
    Optional<Order> subscription = orderRepository.findLatestSubscription(userId);
    
    if (subscription.isEmpty()) return false;
    
    Order order = subscription.get();
    LocalDateTime graceEndDate = order.getEndDate().plusDays(3);
    
    return LocalDateTime.now().isBefore(graceEndDate);
}
```

### 3. **Upgrade/Downgrade**
```java
// Allow plan changes (future enhancement)
public void changeSubscriptionPlan(String userId, String newPlanId) {
    // 1. Cancel current subscription
    // 2. Calculate prorated refund
    // 3. Create new subscription order
    // 4. Process payment difference
}
```

### 4. **Subscription Metrics**
```java
// Track key metrics
public Map<String, Object> getSubscriptionMetrics() {
    return Map.of(
        "activeSubscribers", orderService.getActiveSubscribersCount(),
        "churnRate", calculateChurnRate(),
        "averageLifetime", calculateAverageLifetime(),
        "mrr", calculateMonthlyRecurringRevenue(),
        "popularPlan", getMostPopularPlan()
    );
}
```

---

## Error Handling

### Common Errors

**Error 1: Already Subscribed**
```java
if (hasActiveSubscription(userId)) {
    throw new BusinessException("USER_ALREADY_SUBSCRIBED", 
        "Bạn đã có gói đăng ký đang hoạt động");
}
```

**Error 2: Invalid Plan**
```java
if (!subscription.getIsActive()) {
    throw new BusinessException("PLAN_NOT_AVAILABLE",
        "Gói đăng ký không còn khả dụng");
}
```

**Error 3: Payment Failed**
```java
// Handle in PaymentController callback
if (!"00".equals(vnpResponseCode)) {
    order.setPaymentStatus(Order.PaymentStatus.FAILED);
    orderService.updateOrder(order);
    // Don't activate subscription
}
```

---

## Testing

### Unit Tests

```java
@SpringBootTest
class SubscriptionServiceTest {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void testCreateSubscription() {
        Subscription sub = new Subscription();
        sub.setSubscriptionId("sub_test_001");
        sub.setPackageName(Subscription.PackageName.BASIC);
        sub.setPrice(new BigDecimal("99000"));
        sub.setDurationDays(30);
        
        Subscription saved = subscriptionService.saveSubscription(sub);
        
        assertNotNull(saved);
        assertEquals("sub_test_001", saved.getSubscriptionId());
    }
    
    @Test
    void testUserSubscription() {
        // Create user and subscription
        // Subscribe user
        // Verify access
        // Cancel subscription
        // Verify no access
    }
}
```

---

## Related Flows

- **FLOW 03**: Shopping Cart & Checkout - Similar order creation
- **FLOW 05**: Payment VNPay - Subscription payment processing
- **FLOW 07**: Reading Interface - Subscription book access control
- **FLOW 09**: Admin Dashboard - Subscription analytics

---

**Last Updated:** 07/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

