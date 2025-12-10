# 🎫 FLOW 16: ADMIN COUPON MANAGEMENT (Quản Lý Mã Giảm Giá)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 16.1: List Coupons](#flow-161-list-coupons)
3. [Flow 16.2: Create Coupon](#flow-162-create-coupon)
4. [Flow 16.3: View Coupon Details](#flow-163-view-coupon-details)
5. [Flow 16.4: Edit Coupon](#flow-164-edit-coupon)
6. [Flow 16.5: Delete Coupon](#flow-165-delete-coupon)
7. [Flow 16.6: Validate Coupon Code](#flow-166-validate-coupon-code)
8. [Flow 16.7: Coupon Usage Tracking](#flow-167-coupon-usage-tracking)
9. [Coupon Types](#coupon-types)
10. [Error Handling](#error-handling)

---

## Tổng Quan

### Components
- **Controller**: `AdminCouponController.java`
- **Service**: `CouponService.java`
- **Repository**: `CouponRepository.java`
- **Entity**: `Coupon.java`, `Order.java`
- **DTOs**: `CouponCreateRequest.java`, `CouponUpdateRequest.java`

### URLs
- `GET /admin/coupons` - Danh sách mã giảm giá
- `GET /admin/coupons/create` - Form tạo mã giảm giá
- `POST /admin/coupons/create` - Xử lý tạo mã giảm giá
- `GET /admin/coupons/view/{id}` - Chi tiết mã giảm giá
- `GET /admin/coupons/edit/{id}` - Form sửa mã giảm giá
- `POST /admin/coupons/edit/{id}` - Xử lý sửa mã giảm giá
- `POST /admin/coupons/delete/{id}` - Xóa mã giảm giá
- `GET /api/coupons/validate` - Validate mã giảm giá

### Coupon Structure
```
Coupon:
  - couponId: String (auto: "coupon_001", "coupon_002", ...)
  - code: String (unique, e.g., "SUMMER2025")
  - discountType: Enum (PERCENTAGE, FIXED_AMOUNT)
  - discountValue: BigDecimal
  - minOrderValue: BigDecimal (minimum order to apply)
  - usageLimit: Integer (max uses, null = unlimited)
  - usedCount: Integer (current usage count)
  - endDate: LocalDateTime (expiration date)
  - createdBy: User
  - createdAt: LocalDateTime
```

### Discount Types
```java
public enum DiscountType {
    PERCENTAGE,    // % discount (e.g., 10% off)
    FIXED_AMOUNT   // Fixed amount (e.g., 50,000 VND off)
}
```

---

## Flow 16.1: List Coupons

### Sequence Diagram
```
Admin → Browser → AdminCouponController → CouponService → CouponRepository → Database
  │       │               │                    │                 │              │
  │  GET /admin/coupons?search={query}&status={active/expired}                 │
  │────────────────────────►│                                                   │
  │       │                 │ searchCoupons() OR getAllCoupons()                │
  │       │                 ├────────────────────►│                             │
  │       │                 │                     │ findByCodeContaining()      │
  │       │                 │                     ├────────────────►│           │
  │       │                 │                     │                 │ SELECT    │
  │       │                 │                     │                 ├──────────►│
  │       │                 │                     │◄────────────────┤           │
  │       │                 │◄────────────────────┤                             │
  │◄────────────────────────┤ (return admin/coupons/list.html)                 │
```

### Implementation Details

**Controller**:
```java
@GetMapping
public String couponsList(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String status,
                         Authentication authentication,
                         Model model) {
    List<Coupon> coupons;
    
    if (search != null && !search.trim().isEmpty()) {
        coupons = couponService.searchCouponsByCode(search);
        model.addAttribute("search", search);
    } else if ("active".equals(status)) {
        coupons = couponService.getActiveCoupons();
    } else if ("expired".equals(status)) {
        coupons = couponService.getExpiredCoupons();
    } else {
        coupons = couponService.getAllCoupons();
    }
    
    model.addAttribute("coupons", coupons);
    model.addAttribute("totalCoupons", coupons.size());
    model.addAttribute("status", status);
    
    // Statistics
    long activeCount = coupons.stream()
        .filter(c -> c.getEndDate().isAfter(LocalDateTime.now()))
        .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
        .count();
    long expiredCount = coupons.size() - activeCount;
    
    model.addAttribute("activeCount", activeCount);
    model.addAttribute("expiredCount", expiredCount);
    
    return "admin/coupons/list";
}
```

**Service**:
```java
@Override
public List<Coupon> getAllCoupons() {
    return couponRepository.findAllByOrderByCreatedAtDesc();
}

@Override
public List<Coupon> getActiveCoupons() {
    LocalDateTime now = LocalDateTime.now();
    return couponRepository.findByEndDateAfterOrderByCreatedAtDesc(now);
}

@Override
public List<Coupon> getExpiredCoupons() {
    LocalDateTime now = LocalDateTime.now();
    return couponRepository.findByEndDateBeforeOrderByCreatedAtDesc(now);
}

@Override
public List<Coupon> searchCouponsByCode(String code) {
    return couponRepository.findByCodeContainingIgnoreCase(code);
}
```

**SQL Query**:
```sql
-- Get all coupons
SELECT c.*, u.full_name as creator_name,
       (CASE WHEN c.end_date > NOW() AND 
             (c.usage_limit IS NULL OR c.used_count < c.usage_limit)
        THEN 'ACTIVE' ELSE 'EXPIRED' END) as status
FROM coupons c
LEFT JOIN users u ON c.created_by = u.user_id
ORDER BY c.created_at DESC;

-- Search coupons
SELECT * FROM coupons
WHERE UPPER(code) LIKE UPPER(CONCAT('%', ?, '%'))
ORDER BY created_at DESC;

-- Get active coupons
SELECT * FROM coupons
WHERE end_date > NOW()
  AND (usage_limit IS NULL OR used_count < usage_limit)
ORDER BY created_at DESC;
```

**Response Data**:
```json
{
  "coupons": [
    {
      "couponId": "coupon_001",
      "code": "SUMMER2025",
      "discountType": "PERCENTAGE",
      "discountValue": 20.00,
      "minOrderValue": 100000.00,
      "usageLimit": 100,
      "usedCount": 45,
      "endDate": "2025-08-31T23:59:59",
      "status": "ACTIVE"
    },
    {
      "couponId": "coupon_002",
      "code": "WELCOME50K",
      "discountType": "FIXED_AMOUNT",
      "discountValue": 50000.00,
      "minOrderValue": 200000.00,
      "usageLimit": null,
      "usedCount": 230,
      "endDate": "2025-12-31T23:59:59",
      "status": "ACTIVE"
    }
  ]
}
```

---

## Flow 16.2: Create Coupon

### Sequence Diagram
```
Admin → Browser → AdminCouponController → CouponService → CouponRepository → Database
  │       │               │                    │                 │              │
  │  GET /admin/coupons/create                                                  │
  │────────────────────────►│                                                   │
  │◄────────────────────────┤ (return form with empty CouponCreateRequest)     │
  │       │                 │                                                   │
  │  POST /admin/coupons/create (CouponCreateRequest)                          │
  │────────────────────────►│                                                   │
  │       │                 │ Validate input                                    │
  │       │                 │ generateNextCouponId()                            │
  │       │                 │ checkCodeDuplicate()                              │
  │       │                 ├────────────────────►│                             │
  │       │                 │                     │ findByCode()                │
  │       │                 │                     ├────────────────►│           │
  │       │                 │◄────────────────────┤                             │
  │       │                 │ saveCoupon()                                      │
  │       │                 ├────────────────────►│                             │
  │       │                 │                     │ save()                      │
  │       │                 │                     ├────────────────────────►│   │
  │       │                 │◄────────────────────┤                             │
  │◄────────────────────────┤ redirect:/admin/coupons                          │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/create")
public String showCreateForm(Model model) {
    model.addAttribute("coupon", new CouponCreateRequest());
    addCommonFormAttributes(model, false);
    return "admin/coupons/form";
}

@PostMapping("/create")
public String createCoupon(@Valid @ModelAttribute("coupon") CouponCreateRequest request,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes,
                          Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, false);
        return "admin/coupons/form";
    }
    
    // Check duplicate code
    if (couponService.getCouponByCode(request.getCode()).isPresent()) {
        result.rejectValue("code", "error.coupon", "Mã giảm giá đã tồn tại");
        addCommonFormAttributes(model, false);
        return "admin/coupons/form";
    }
    
    // Validate discount value
    if (request.getDiscountType().equals("PERCENTAGE")) {
        if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0 || 
            request.getDiscountValue().compareTo(new BigDecimal(100)) > 0) {
            result.rejectValue("discountValue", "error.coupon", 
                "Giá trị giảm giá phần trăm phải từ 0-100");
            addCommonFormAttributes(model, false);
            return "admin/coupons/form";
        }
    }
    
    try {
        User currentUser = getCurrentUser(authentication);
        String couponId = generateNextCouponId();
        
        // Create coupon
        Coupon coupon = new Coupon();
        coupon.setCouponId(couponId);
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountType(Coupon.DiscountType.valueOf(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? 
            request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);
        coupon.setEndDate(request.getValidTo());
        coupon.setCreatedBy(currentUser);
        coupon.setCreatedAt(LocalDateTime.now());
        
        couponService.saveCoupon(coupon);
        
        redirectAttributes.addFlashAttribute("successMessage", "Tạo mã giảm giá thành công!");
        return REDIRECT_COUPONS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_COUPONS;
    }
}
```

**Helper Method**:
```java
private String generateNextCouponId() {
    List<Coupon> allCoupons = couponService.getAllCoupons();
    int nextNumber = allCoupons.size() + 1;
    return String.format("coupon_%03d", nextNumber);
}
```

**SQL Query**:
```sql
-- Check code exists
SELECT * FROM coupons WHERE UPPER(code) = UPPER(?);

-- Insert new coupon
INSERT INTO coupons (coupon_id, code, discount_type, discount_value, 
                     min_order_value, usage_limit, used_count, end_date,
                     created_by, created_at)
VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, NOW());
```

**Request DTO**:
```java
public class CouponCreateRequest {
    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 50, message = "Mã giảm giá không được quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã chỉ chứa chữ in hoa và số")
    private String code;
    
    @NotBlank(message = "Loại giảm giá không được để trống")
    private String discountType; // PERCENTAGE or FIXED_AMOUNT
    
    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị phải > 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.0", message = "Giá trị đơn tối thiểu phải >= 0")
    private BigDecimal minOrderValue;
    
    @Min(value = 1, message = "Số lần sử dụng phải >= 1")
    private Integer usageLimit;
    
    @NotNull(message = "Ngày hết hạn không được để trống")
    @Future(message = "Ngày hết hạn phải ở tương lai")
    private LocalDateTime validTo;
}
```

---

## Flow 16.3: View Coupon Details

### Implementation Details

**Controller**:
```java
@GetMapping("/view/{id}")
public String viewCoupon(@PathVariable String id, Model model, 
                        RedirectAttributes redirectAttributes) {
    Coupon coupon = couponService.getCouponById(id).orElse(null);
    
    if (coupon == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy mã giảm giá!");
        return REDIRECT_COUPONS;
    }
    
    // Get orders using this coupon
    List<Order> ordersWithCoupon = orderService.getOrdersByCoupon(coupon);
    
    // Calculate statistics
    BigDecimal totalDiscount = ordersWithCoupon.stream()
        .map(Order::getDiscountAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    model.addAttribute("coupon", coupon);
    model.addAttribute("orders", ordersWithCoupon);
    model.addAttribute("totalDiscount", totalDiscount);
    model.addAttribute("remainingUses", 
        coupon.getUsageLimit() != null ? coupon.getUsageLimit() - coupon.getUsedCount() : null);
    
    return "admin/coupons/view";
}
```

**SQL Query**:
```sql
-- Get coupon with usage stats
SELECT c.*, 
       COUNT(o.order_id) as order_count,
       SUM(o.discount_amount) as total_discount
FROM coupons c
LEFT JOIN orders o ON c.coupon_id = o.coupon_id
WHERE c.coupon_id = ?
GROUP BY c.coupon_id;
```

---

## Flow 16.4: Edit Coupon

### Implementation Details

**Controller**:
```java
@PostMapping("/edit/{id}")
public String updateCoupon(@PathVariable String id,
                          @Valid @ModelAttribute("coupon") CouponUpdateRequest request,
                          BindingResult result,
                          RedirectAttributes redirectAttributes,
                          Model model) {
    if (result.hasErrors()) {
        addCommonFormAttributes(model, true);
        return "admin/coupons/form";
    }
    
    Coupon coupon = couponService.getCouponById(id).orElse(null);
    if (coupon == null) {
        redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy mã giảm giá!");
        return REDIRECT_COUPONS;
    }
    
    try {
        // Update fields (code cannot be changed)
        coupon.setDiscountType(Coupon.DiscountType.valueOf(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setEndDate(request.getValidTo());
        
        couponService.saveCoupon(coupon);
        
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật mã giảm giá thành công!");
        return REDIRECT_COUPONS;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        return REDIRECT_COUPONS;
    }
}
```

**SQL Query**:
```sql
UPDATE coupons 
SET discount_type = ?, discount_value = ?, min_order_value = ?, 
    usage_limit = ?, end_date = ?
WHERE coupon_id = ?;
```

---

## Flow 16.5: Delete Coupon

### Implementation Details

**Controller**:
```java
@PostMapping("/delete/{id}")
public String deleteCoupon(@PathVariable String id, 
                          RedirectAttributes redirectAttributes) {
    try {
        Coupon coupon = couponService.getCouponById(id).orElse(null);
        if (coupon == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy mã giảm giá!");
            return REDIRECT_COUPONS;
        }
        
        // Check if coupon has been used
        if (coupon.getUsedCount() > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể xóa mã giảm giá đã được sử dụng " + coupon.getUsedCount() + " lần!");
            return REDIRECT_COUPONS;
        }
        
        couponService.deleteCoupon(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa mã giảm giá thành công!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
    }
    
    return REDIRECT_COUPONS;
}
```

**SQL Query**:
```sql
-- Check usage count
SELECT used_count FROM coupons WHERE coupon_id = ?;

-- Delete coupon
DELETE FROM coupons WHERE coupon_id = ? AND used_count = 0;
```

---

## Flow 16.6: Validate Coupon Code

### Sequence Diagram
```
User → Browser → CartController → CouponService → CouponRepository → Database
  │       │            │               │                │              │
  │  POST /cart/apply-coupon (code, orderAmount)                      │
  │──────────────────────►│                                            │
  │       │                │ validateCoupon()                          │
  │       │                ├──────────────────►│                       │
  │       │                │                   │ findByCode()          │
  │       │                │                   ├──────────────►│       │
  │       │                │                   │               │ SELECT│
  │       │                │                   │◄──────────────┤       │
  │       │                │                   │ checkExpired()        │
  │       │                │                   │ checkUsageLimit()     │
  │       │                │                   │ checkMinOrder()       │
  │       │                │◄──────────────────┤                       │
  │◄──────────────────────┤ JSON: {valid, discountAmount}             │
```

### Implementation Details

**Service**:
```java
public CouponValidationResult validateCoupon(String code, BigDecimal orderAmount) {
    // Find coupon
    Optional<Coupon> couponOpt = couponRepository.findByCode(code.toUpperCase());
    if (couponOpt.isEmpty()) {
        return CouponValidationResult.invalid("Mã giảm giá không tồn tại");
    }
    
    Coupon coupon = couponOpt.get();
    
    // Check expiration
    if (coupon.getEndDate().isBefore(LocalDateTime.now())) {
        return CouponValidationResult.invalid("Mã giảm giá đã hết hạn");
    }
    
    // Check usage limit
    if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
        return CouponValidationResult.invalid("Mã giảm giá đã hết lượt sử dụng");
    }
    
    // Check minimum order value
    if (orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
        return CouponValidationResult.invalid(
            "Đơn hàng tối thiểu " + formatCurrency(coupon.getMinOrderValue()) + " để sử dụng mã này");
    }
    
    // Calculate discount
    BigDecimal discountAmount;
    if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
        discountAmount = orderAmount.multiply(coupon.getDiscountValue())
            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
    } else {
        discountAmount = coupon.getDiscountValue();
    }
    
    // Ensure discount doesn't exceed order amount
    if (discountAmount.compareTo(orderAmount) > 0) {
        discountAmount = orderAmount;
    }
    
    return CouponValidationResult.valid(coupon, discountAmount);
}

public void incrementCouponUsage(String couponId) {
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new RuntimeException("Coupon not found"));
    
    coupon.setUsedCount(coupon.getUsedCount() + 1);
    couponRepository.save(coupon);
}
```

**DTO**:
```java
public class CouponValidationResult {
    private boolean valid;
    private String message;
    private Coupon coupon;
    private BigDecimal discountAmount;
    
    public static CouponValidationResult valid(Coupon coupon, BigDecimal discountAmount) {
        CouponValidationResult result = new CouponValidationResult();
        result.valid = true;
        result.coupon = coupon;
        result.discountAmount = discountAmount;
        result.message = "Áp dụng mã giảm giá thành công";
        return result;
    }
    
    public static CouponValidationResult invalid(String message) {
        CouponValidationResult result = new CouponValidationResult();
        result.valid = false;
        result.message = message;
        return result;
    }
}
```

**SQL Query**:
```sql
-- Validate coupon
SELECT * FROM coupons
WHERE UPPER(code) = UPPER(?)
  AND end_date > NOW()
  AND (usage_limit IS NULL OR used_count < usage_limit);

-- Increment usage count
UPDATE coupons 
SET used_count = used_count + 1
WHERE coupon_id = ?;
```

---

## Flow 16.7: Coupon Usage Tracking

### Implementation Details

**Track in Order**:
```java
// When creating order with coupon
Order order = new Order();
order.setCoupon(coupon);
order.setDiscountAmount(discountAmount);
orderService.saveOrder(order);

// Increment coupon usage
couponService.incrementCouponUsage(coupon.getCouponId());
```

**Get Usage Statistics**:
```java
public CouponUsageStats getCouponUsageStats(String couponId) {
    Coupon coupon = getCouponById(couponId)
        .orElseThrow(() -> new RuntimeException("Coupon not found"));
    
    List<Order> orders = orderRepository.findByCoupon(coupon);
    
    BigDecimal totalRevenue = orders.stream()
        .map(Order::getFinalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalDiscount = orders.stream()
        .map(Order::getDiscountAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    return new CouponUsageStats(
        coupon.getUsedCount(),
        orders.size(),
        totalRevenue,
        totalDiscount
    );
}
```

---

## Coupon Types

### 1. Percentage Discount
```
Example: SUMMER20
- discountType: PERCENTAGE
- discountValue: 20.00 (20%)
- Calculation: orderAmount * 20 / 100
```

### 2. Fixed Amount Discount
```
Example: WELCOME50K
- discountType: FIXED_AMOUNT
- discountValue: 50000.00 (50,000 VND)
- Calculation: Fixed 50,000 VND off
```

### Discount Calculation Example
```
Order Amount: 500,000 VND

Case 1: SUMMER20 (20% off, min order 100k)
- Discount: 500,000 * 20 / 100 = 100,000 VND
- Final: 400,000 VND

Case 2: WELCOME50K (50k off, min order 200k)
- Discount: 50,000 VND
- Final: 450,000 VND
```

---

## Error Handling

### Common Errors

**1. Coupon Not Found**
```json
{
  "valid": false,
  "message": "Mã giảm giá không tồn tại"
}
```

**2. Coupon Expired**
```json
{
  "valid": false,
  "message": "Mã giảm giá đã hết hạn"
}
```

**3. Usage Limit Reached**
```json
{
  "valid": false,
  "message": "Mã giảm giá đã hết lượt sử dụng"
}
```

**4. Minimum Order Not Met**
```json
{
  "valid": false,
  "message": "Đơn hàng tối thiểu 200,000 VND để sử dụng mã này"
}
```

**5. Duplicate Code**
```json
{
  "error": "DUPLICATE_CODE",
  "message": "Mã giảm giá 'SUMMER2025' đã tồn tại"
}
```

---

## Best Practices

### 1. Code Format
- Use uppercase letters and numbers only
- Make codes memorable (SUMMER2025, WELCOME50K)
- Avoid confusing characters (0 vs O, 1 vs I)
- Length: 6-20 characters

### 2. Validation Rules
- Always validate expiration date
- Check usage limits before applying
- Verify minimum order amount
- Prevent concurrent usage issues

### 3. Usage Tracking
- Increment usage count atomically
- Track which orders used which coupons
- Calculate total savings per coupon
- Monitor coupon effectiveness

### 4. Expiration Strategy
- Set reasonable expiration dates
- Notify users before expiration
- Allow extending expiration for specific coupons
- Archive expired coupons (don't delete)

### 5. Limiting Abuse
- Set usage limits per coupon
- Consider user-specific limits (future)
- Track IP addresses (future)
- Implement cooldown periods (future)

---

## Security Considerations

### 1. Authorization
```java
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController extends BaseAdminController {
    // ...
}
```

### 2. Code Validation
- Sanitize coupon codes
- Prevent SQL injection
- Validate format and length
- Case-insensitive comparison

### 3. Race Conditions
```java
// Use optimistic locking for concurrent updates
@Version
private Long version;

// Or use pessimistic locking
@Lock(LockModeType.PESSIMISTIC_WRITE)
Coupon findById(String id);
```

### 4. Fraud Prevention
- Monitor unusual usage patterns
- Limit coupons per user (future)
- Blacklist abusive users
- Log all coupon applications

---

## Related Flows
- 🛒 **FLOW 03**: Shopping Cart & Checkout - Apply coupons
- 📦 **FLOW 08**: Admin Order Management - View coupon usage
- 📊 **FLOW 09**: Admin Dashboard - Coupon statistics

---

**Last Updated**: December 7, 2025  
**Version**: 1.0  
**Author**: Ebook Store Development Team

