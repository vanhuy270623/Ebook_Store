# 📊 FLOW 09: ADMIN DASHBOARD & ANALYTICS (Bảng Điều Khiển Admin)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 9.1: Load Dashboard Statistics](#flow-91-load-dashboard-statistics)
3. [Flow 9.2: Real-time Analytics](#flow-92-real-time-analytics)
4. [Flow 9.3: Revenue Analytics](#flow-93-revenue-analytics)
5. [Flow 9.4: User Activity Tracking](#flow-94-user-activity-tracking)
6. [Dashboard Components](#dashboard-components)

---

## Tổng Quan

### Dashboard Overview
```
┌─────────────────────────────────────────────────────────┐
│                   ADMIN DASHBOARD                        │
├─────────────────────────────────────────────────────────┤
│  📚 Books Stats     👥 Users Stats     💰 Revenue       │
│  • Total: 150       • Total: 1,234     • Today: 2.5M    │
│  • Free: 50         • Active: 980      • Month: 45M     │
│  • Paid: 80         • Verified: 900    • Total: 234M    │
│  • Subscription: 20 • Premium: 120                      │
├─────────────────────────────────────────────────────────┤
│  📦 Orders Stats                                         │
│  • Pending: 15      • Completed: 450                    │
│  • Processing: 8    • Failed: 5                         │
│  • Cancelled: 12                                        │
├─────────────────────────────────────────────────────────┤
│  📈 Revenue Chart (Monthly)                             │
│  [Bar Chart: Last 12 months revenue]                    │
├─────────────────────────────────────────────────────────┤
│  📋 Recent Orders                                        │
│  [Table: Latest 10 orders]                              │
├─────────────────────────────────────────────────────────┤
│  ⭐ Top Rated Books        🆕 Recent Books               │
│  [List: Top 5 books]      [List: Latest 5 books]       │
└─────────────────────────────────────────────────────────┘
```

### Components
- **Controller**: `AdminController.java`
- **Services**: `BookService.java`, `UserService.java`, `OrderService.java`, `ReviewService.java`
- **Repositories**: All entity repositories
- **Templates**: `admin/dashboard.html`

### URL
- `GET /admin/dashboard` - Admin dashboard page

---

## Flow 9.1: Load Dashboard Statistics

### Sequence Diagram
```
Admin → Browser → AdminController → Multiple Services → Database
  │        │             │                  │               │
  │ Access /admin/dashboard                                 │
  │────────────────────►│                                   │
  │        │             │ Check Authentication & Role      │
  │        │             │ (Spring Security)                │
  │        │             │                                  │
  │        │             │ getTotalBooksCount()             │
  │        │             ├──────────────────►│              │
  │        │             │                   │ COUNT books  │
  │        │             │                   ├─────────────►│
  │        │             │◄──────────────────┤              │
  │        │             │                                  │
  │        │             │ getTotalUsersCount()             │
  │        │             ├──────────────────►│              │
  │        │             │                   │ COUNT users  │
  │        │             │                   ├─────────────►│
  │        │             │◄──────────────────┤              │
  │        │             │                                  │
  │        │             │ getTotalOrdersCount()            │
  │        │             ├──────────────────►│              │
  │        │             │                   │ COUNT orders │
  │        │             │                   ├─────────────►│
  │        │             │◄──────────────────┤              │
  │        │             │                                  │
  │        │             │ getTotalRevenue()                │
  │        │             ├──────────────────►│              │
  │        │             │                   │ SUM(total)   │
  │        │             │                   ├─────────────►│
  │        │             │◄──────────────────┤              │
  │        │             │                                  │
  │        │             │ getRecentOrders(10)              │
  │        │             ├──────────────────►│              │
  │        │             │◄──────────────────┤              │
  │        │             │                                  │
  │◄────────────────────┤ dashboard.html                   │
  │ Display statistics                                      │
```

### Implementation Details

**Controller Method**:
```java
@GetMapping("/dashboard")
public String adminDashboard(HttpSession session, Model model) {
    // Debug logging
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("=== ADMIN DASHBOARD ACCESS ===");
    System.out.println("Authentication: " + auth);
    
    // Add username to model
    model.addAttribute("username", session.getAttribute("username"));

    // ===== BOOK STATISTICS =====
    model.addAttribute("totalBooks", bookService.getTotalBooksCount());
    model.addAttribute("freeBooks", bookService.getFreeBooks());
    model.addAttribute("paidBooks", bookService.getPaidBooks());
    model.addAttribute("subscriptionBooks", bookService.getSubscriptionBooks());
    model.addAttribute("recentBooks", bookService.getRecentBooks(5));
    model.addAttribute("topRatedBooks", bookService.getTopRatedBooks(5));

    // ===== USER STATISTICS =====
    model.addAttribute("totalUsers", userService.getTotalUsersCount());
    model.addAttribute("activeUsers", userService.getActiveUsersCount());
    model.addAttribute("verifiedUsers", userService.getVerifiedUsersCount());
    model.addAttribute("recentUsers", userService.getRecentUsers(5));

    // ===== ORDER & REVENUE STATISTICS =====
    model.addAttribute("totalOrders", orderService.getTotalOrdersCount());
    model.addAttribute("totalRevenue", orderService.getTotalRevenue());
    model.addAttribute("pendingOrders", orderService.getPendingOrdersCount());
    model.addAttribute("completedOrders", orderService.getCompletedOrdersCount());
    model.addAttribute("failedOrders", orderService.getFailedOrdersCount());
    model.addAttribute("cancelledOrders", orderService.getCancelledOrdersCount());
    model.addAttribute("recentOrders", orderService.getRecentOrders(10));

    // ===== REVENUE DETAILS =====
    model.addAttribute("todayRevenue", orderService.getTodayRevenue());
    model.addAttribute("thisMonthRevenue", orderService.getThisMonthRevenue());
    model.addAttribute("todayOrders", orderService.getTodayOrdersCount());
    model.addAttribute("bookRevenue", orderService.getRevenueByOrderType(Order.OrderType.BOOK));
    model.addAttribute("subscriptionRevenue", orderService.getRevenueByOrderType(Order.OrderType.SUBSCRIPTION));
    model.addAttribute("monthlyRevenue", orderService.getMonthlyRevenue(12));

    // ===== REVIEW STATISTICS =====
    model.addAttribute("totalReviews", reviewService.getTotalReviewsCount());

    return "admin/dashboard";
}
```

---

## Flow 9.2: Real-time Analytics

### Book Statistics

**SQL Query Examples**:

```sql
-- Total books count
SELECT COUNT(*) FROM books WHERE deleted_at IS NULL;

-- Free books
SELECT COUNT(*) FROM books 
WHERE access_type = 'FREE' AND deleted_at IS NULL;

-- Paid books
SELECT COUNT(*) FROM books 
WHERE access_type = 'PAID' AND deleted_at IS NULL;

-- Subscription books
SELECT COUNT(*) FROM books 
WHERE access_type = 'SUBSCRIPTION' AND deleted_at IS NULL;

-- Recent books (last 5)
SELECT * FROM books 
WHERE deleted_at IS NULL 
ORDER BY created_at DESC 
LIMIT 5;

-- Top rated books
SELECT b.*, AVG(r.rating) as avg_rating, COUNT(r.review_id) as review_count
FROM books b
LEFT JOIN reviews r ON b.book_id = r.book_id
WHERE b.deleted_at IS NULL AND r.is_approved = true
GROUP BY b.book_id
HAVING COUNT(r.review_id) >= 5
ORDER BY avg_rating DESC, review_count DESC
LIMIT 5;
```

**Service Implementation**:
```java
// BookService.java
public Long getTotalBooksCount() {
    return bookRepository.count();
}

public Long getFreeBooks() {
    return bookRepository.countByAccessType(Book.AccessType.FREE);
}

public Long getPaidBooks() {
    return bookRepository.countByAccessType(Book.AccessType.PAID);
}

public Long getSubscriptionBooks() {
    return bookRepository.countByAccessType(Book.AccessType.SUBSCRIPTION);
}

public List<Book> getRecentBooks(int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
    return bookRepository.findAll(pageable).getContent();
}

public List<Book> getTopRatedBooks(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return bookRepository.findTopRatedBooks(pageable);
}
```

---

## Flow 9.3: Revenue Analytics

### Revenue Tracking

**Database Schema**:
```sql
-- Revenue queries use orders table
CREATE TABLE orders (
    order_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50),
    total_amount DECIMAL(10,2),
    order_type ENUM('BOOK', 'SUBSCRIPTION'),
    payment_status ENUM('PENDING', 'PAID', 'COMPLETED', 'FAILED', 'CANCELLED'),
    order_status ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'COMPLETED', 'CANCELLED'),
    created_at TIMESTAMP,
    ...
);
```

**Revenue Queries**:
```sql
-- Total revenue (all completed orders)
SELECT SUM(total_amount) FROM orders 
WHERE payment_status IN ('PAID', 'COMPLETED');

-- Today's revenue
SELECT SUM(total_amount) FROM orders 
WHERE payment_status IN ('PAID', 'COMPLETED')
AND DATE(created_at) = CURDATE();

-- This month's revenue
SELECT SUM(total_amount) FROM orders 
WHERE payment_status IN ('PAID', 'COMPLETED')
AND MONTH(created_at) = MONTH(CURDATE())
AND YEAR(created_at) = YEAR(CURDATE());

-- Monthly revenue (last 12 months)
SELECT 
    DATE_FORMAT(created_at, '%Y-%m') as month,
    SUM(total_amount) as revenue,
    COUNT(*) as order_count
FROM orders
WHERE payment_status IN ('PAID', 'COMPLETED')
AND created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month DESC;

-- Revenue by order type
SELECT 
    order_type,
    SUM(total_amount) as revenue,
    COUNT(*) as order_count,
    AVG(total_amount) as avg_order_value
FROM orders
WHERE payment_status IN ('PAID', 'COMPLETED')
GROUP BY order_type;
```

**Service Implementation**:
```java
// OrderService.java
public BigDecimal getTotalRevenue() {
    return orderRepository.sumTotalAmountByPaymentStatus(
        Arrays.asList(Order.PaymentStatus.PAID, Order.PaymentStatus.COMPLETED)
    );
}

public BigDecimal getTodayRevenue() {
    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    LocalDateTime endOfDay = startOfDay.plusDays(1);
    return orderRepository.sumTotalAmountByDateRange(startOfDay, endOfDay);
}

public BigDecimal getThisMonthRevenue() {
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
    return orderRepository.sumTotalAmountByDateRange(startOfMonth, endOfMonth);
}

public BigDecimal getRevenueByOrderType(Order.OrderType orderType) {
    return orderRepository.sumTotalAmountByOrderType(orderType);
}

public Map<String, BigDecimal> getMonthlyRevenue(int months) {
    LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
    List<Object[]> results = orderRepository.getMonthlyRevenue(startDate);
    
    Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
    for (Object[] result : results) {
        String month = (String) result[0];
        BigDecimal revenue = (BigDecimal) result[1];
        monthlyRevenue.put(month, revenue);
    }
    
    return monthlyRevenue;
}
```

---

## Flow 9.4: User Activity Tracking

### User Statistics

**Queries**:
```sql
-- Total users
SELECT COUNT(*) FROM users WHERE deleted_at IS NULL;

-- Active users (logged in last 30 days)
SELECT COUNT(*) FROM users 
WHERE deleted_at IS NULL 
AND last_login >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- Verified users (email verified)
SELECT COUNT(*) FROM users 
WHERE deleted_at IS NULL 
AND is_email_verified = true;

-- Recent users (last 5 registered)
SELECT * FROM users 
WHERE deleted_at IS NULL 
ORDER BY created_at DESC 
LIMIT 5;

-- Users with active subscriptions
SELECT COUNT(DISTINCT u.user_id) 
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id
WHERE u.deleted_at IS NULL
AND o.order_type = 'SUBSCRIPTION'
AND o.payment_status IN ('PAID', 'COMPLETED')
AND o.end_date > NOW();
```

**Service Implementation**:
```java
// UserService.java
public Long getTotalUsersCount() {
    return userRepository.count();
}

public Long getActiveUsersCount() {
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    return userRepository.countByLastLoginAfter(thirtyDaysAgo);
}

public Long getVerifiedUsersCount() {
    return userRepository.countByIsEmailVerified(true);
}

public List<User> getRecentUsers(int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
    return userRepository.findAll(pageable).getContent();
}
```

---

## Dashboard Components

### 1. Statistics Cards

**Books Card**:
```html
<div class="stat-card">
    <div class="stat-icon">📚</div>
    <div class="stat-content">
        <h3 th:text="${totalBooks}">0</h3>
        <p>Tổng số sách</p>
        <div class="stat-breakdown">
            <span>Free: <strong th:text="${freeBooks}">0</strong></span>
            <span>Paid: <strong th:text="${paidBooks}">0</strong></span>
            <span>Subscription: <strong th:text="${subscriptionBooks}">0</strong></span>
        </div>
    </div>
</div>
```

**Users Card**:
```html
<div class="stat-card">
    <div class="stat-icon">👥</div>
    <div class="stat-content">
        <h3 th:text="${totalUsers}">0</h3>
        <p>Tổng người dùng</p>
        <div class="stat-breakdown">
            <span>Active: <strong th:text="${activeUsers}">0</strong></span>
            <span>Verified: <strong th:text="${verifiedUsers}">0</strong></span>
        </div>
    </div>
</div>
```

**Revenue Card**:
```html
<div class="stat-card">
    <div class="stat-icon">💰</div>
    <div class="stat-content">
        <h3 th:text="${#numbers.formatDecimal(totalRevenue, 0, 'COMMA', 0, 'POINT')}">0</h3>
        <p>Tổng doanh thu (VNĐ)</p>
        <div class="stat-breakdown">
            <span>Hôm nay: <strong th:text="${#numbers.formatDecimal(todayRevenue, 0, 'COMMA', 0, 'POINT')}">0</strong></span>
            <span>Tháng này: <strong th:text="${#numbers.formatDecimal(thisMonthRevenue, 0, 'COMMA', 0, 'POINT')}">0</strong></span>
        </div>
    </div>
</div>
```

**Orders Card**:
```html
<div class="stat-card">
    <div class="stat-icon">📦</div>
    <div class="stat-content">
        <h3 th:text="${totalOrders}">0</h3>
        <p>Tổng đơn hàng</p>
        <div class="stat-breakdown">
            <span>Pending: <strong th:text="${pendingOrders}">0</strong></span>
            <span>Completed: <strong th:text="${completedOrders}">0</strong></span>
            <span>Failed: <strong th:text="${failedOrders}">0</strong></span>
        </div>
    </div>
</div>
```

### 2. Charts

**Monthly Revenue Chart** (using Chart.js):
```javascript
// Monthly Revenue Bar Chart
const monthlyRevenueData = {
    labels: [[${monthlyRevenueLabels}]], // ['2024-01', '2024-02', ...]
    datasets: [{
        label: 'Doanh thu (VNĐ)',
        data: [[${monthlyRevenueValues}]], // [1500000, 2300000, ...]
        backgroundColor: 'rgba(54, 162, 235, 0.5)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 1
    }]
};

const config = {
    type: 'bar',
    data: monthlyRevenueData,
    options: {
        responsive: true,
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    callback: function(value) {
                        return value.toLocaleString('vi-VN') + ' đ';
                    }
                }
            }
        }
    }
};

new Chart(document.getElementById('monthlyRevenueChart'), config);
```

### 3. Recent Orders Table

```html
<div class="recent-orders">
    <h3>Đơn hàng gần đây</h3>
    <table class="table">
        <thead>
            <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Loại</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="order : ${recentOrders}">
                <td th:text="${order.orderId}">ORD_001</td>
                <td th:text="${order.user.fullName}">John Doe</td>
                <td>
                    <span class="badge" 
                          th:classappend="${order.orderType == 'BOOK'} ? 'badge-primary' : 'badge-info'"
                          th:text="${order.orderType}">BOOK</span>
                </td>
                <td th:text="${#numbers.formatDecimal(order.totalAmount, 0, 'COMMA', 0, 'POINT')} + ' đ'">100,000 đ</td>
                <td>
                    <span class="badge" 
                          th:classappend="${order.paymentStatus == 'COMPLETED'} ? 'badge-success' : 
                                         (${order.paymentStatus == 'PENDING'} ? 'badge-warning' : 'badge-danger')"
                          th:text="${order.paymentStatus}">COMPLETED</span>
                </td>
                <td th:text="${#temporals.format(order.createdAt, 'dd/MM/yyyy HH:mm')}">07/12/2025 10:30</td>
            </tr>
        </tbody>
    </table>
</div>
```

### 4. Top Lists

**Top Rated Books**:
```html
<div class="top-books">
    <h3>Sách đánh giá cao nhất</h3>
    <ul class="book-list">
        <li th:each="book : ${topRatedBooks}">
            <img th:src="${book.coverImage}" alt="Cover" class="book-thumbnail">
            <div class="book-info">
                <h4 th:text="${book.title}">Book Title</h4>
                <p th:text="${book.author?.name}">Author Name</p>
                <div class="rating">
                    <span>⭐</span>
                    <span th:text="${#numbers.formatDecimal(book.averageRating, 1, 1)}">4.5</span>
                    <span th:text="'(' + ${book.reviewCount} + ')'"> (120)</span>
                </div>
            </div>
        </li>
    </ul>
</div>
```

**Recent Users**:
```html
<div class="recent-users">
    <h3>Người dùng mới</h3>
    <ul class="user-list">
        <li th:each="user : ${recentUsers}">
            <img th:src="${user.avatar}" alt="Avatar" class="user-avatar">
            <div class="user-info">
                <h4 th:text="${user.fullName}">Full Name</h4>
                <p th:text="${user.email}">email@example.com</p>
                <small th:text="${#temporals.format(user.createdAt, 'dd/MM/yyyy')}">07/12/2025</small>
            </div>
        </li>
    </ul>
</div>
```

---

## Performance Optimization

### Caching Strategy

**Cache Configuration**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("dashboardStats"),
            new ConcurrentMapCache("revenueStats")
        ));
        return cacheManager;
    }
}
```

**Cacheable Methods**:
```java
// Cache dashboard statistics (TTL: 5 minutes)
@Cacheable(value = "dashboardStats", key = "'totalBooks'")
public Long getTotalBooksCount() {
    return bookRepository.count();
}

@Cacheable(value = "revenueStats", key = "'totalRevenue'")
public BigDecimal getTotalRevenue() {
    return orderRepository.sumTotalAmountByPaymentStatus(
        Arrays.asList(Order.PaymentStatus.PAID, Order.PaymentStatus.COMPLETED)
    );
}

// Clear cache when data changes
@CacheEvict(value = "dashboardStats", allEntries = true)
public void clearDashboardCache() {
    // Cache cleared
}
```

### Async Loading

**Async Service Calls**:
```java
@Async
public CompletableFuture<Long> getTotalBooksCountAsync() {
    return CompletableFuture.completedFuture(getTotalBooksCount());
}

@Async
public CompletableFuture<Long> getTotalUsersCountAsync() {
    return CompletableFuture.completedFuture(getTotalUsersCount());
}

// Controller usage
CompletableFuture<Long> booksFuture = bookService.getTotalBooksCountAsync();
CompletableFuture<Long> usersFuture = userService.getTotalUsersCountAsync();

CompletableFuture.allOf(booksFuture, usersFuture).join();

model.addAttribute("totalBooks", booksFuture.get());
model.addAttribute("totalUsers", usersFuture.get());
```

---

## Security

### Access Control

**Spring Security Configuration**:
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/admin/dashboard").hasAnyRole("ADMIN", "SUPER_ADMIN")
            // ...
        );
        return http.build();
    }
}
```

**Role Check in Controller**:
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/dashboard")
public String adminDashboard(Model model) {
    // Method implementation
}
```

---

## Error Handling

**Exception Handling**:
```java
@GetMapping("/dashboard")
public String adminDashboard(Model model, RedirectAttributes redirectAttributes) {
    try {
        // Load statistics
        loadDashboardStatistics(model);
        return "admin/dashboard";
        
    } catch (DataAccessException e) {
        log.error("Database error loading dashboard", e);
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi truy cập cơ sở dữ liệu. Vui lòng thử lại.");
        return "redirect:/admin";
        
    } catch (Exception e) {
        log.error("Unexpected error loading dashboard", e);
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi không xác định. Vui lòng liên hệ quản trị viên.");
        return "redirect:/admin";
    }
}
```

---

## Testing

### Unit Tests

```java
@SpringBootTest
class AdminControllerTest {
    
    @Autowired
    private AdminController adminController;
    
    @MockBean
    private BookService bookService;
    
    @MockBean
    private UserService userService;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void testAdminDashboard() {
        // Given
        when(bookService.getTotalBooksCount()).thenReturn(100L);
        when(userService.getTotalUsersCount()).thenReturn(50L);
        when(orderService.getTotalOrdersCount()).thenReturn(200L);
        
        Model model = new ExtendedModelMap();
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("username")).thenReturn("admin");
        
        // When
        String viewName = adminController.adminDashboard(session, model);
        
        // Then
        assertEquals("admin/dashboard", viewName);
        assertEquals(100L, model.getAttribute("totalBooks"));
        assertEquals(50L, model.getAttribute("totalUsers"));
        assertEquals(200L, model.getAttribute("totalOrders"));
    }
}
```

---

## Best Practices

### 1. **Lazy Loading**
- Load heavy statistics only when needed
- Use AJAX for real-time updates
- Paginate large result sets

### 2. **Performance**
- Cache frequently accessed statistics
- Use database indexes on commonly queried columns
- Optimize complex queries with EXPLAIN

### 3. **User Experience**
- Show loading indicators for slow operations
- Provide drill-down capabilities
- Export data to CSV/Excel

### 4. **Security**
- Always validate user permissions
- Sanitize all inputs
- Log all admin actions for audit trail

### 5. **Monitoring**
- Track dashboard load times
- Monitor database query performance
- Set up alerts for anomalies

---

## Troubleshooting

### Common Issues

**Issue 1: Slow Dashboard Loading**
```
Symptom: Dashboard takes >5 seconds to load
Solution:
1. Enable query logging: spring.jpa.show-sql=true
2. Check for N+1 queries
3. Add database indexes
4. Implement caching
5. Use pagination for large datasets
```

**Issue 2: Incorrect Statistics**
```
Symptom: Statistics don't match actual data
Solution:
1. Check SQL queries for filters (deleted_at, payment_status)
2. Verify timezone settings
3. Clear cache and reload
4. Check for concurrent updates
```

**Issue 3: Memory Issues**
```
Symptom: OutOfMemoryError on dashboard load
Solution:
1. Limit result set sizes
2. Use projection queries (select specific columns)
3. Implement pagination
4. Increase JVM heap size
```

---

## Future Enhancements

### Planned Features

1. **Real-time Updates**
   - WebSocket integration for live statistics
   - Auto-refresh every 30 seconds
   - Push notifications for critical events

2. **Advanced Analytics**
   - User behavior analysis
   - Sales forecasting
   - A/B testing results
   - Conversion funnel tracking

3. **Custom Dashboards**
   - Drag-and-drop widgets
   - Saved dashboard layouts
   - Role-based dashboard views

4. **Export Capabilities**
   - PDF reports
   - Excel exports
   - Scheduled email reports

5. **Mobile Dashboard**
   - Responsive design
   - Native mobile app
   - Push notifications

---

## Related Flows

- **FLOW 02**: Admin Book Management - Book statistics source
- **FLOW 06**: Admin User Management - User statistics source
- **FLOW 08**: Admin Order Management - Order & revenue statistics
- **FLOW 10**: Subscription Management - Subscription analytics

---

**Last Updated:** 07/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

