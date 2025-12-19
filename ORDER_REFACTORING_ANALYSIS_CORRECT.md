# PHÂN TÍCH ORDERCONTROLLER VÀ USERORDERCONTROLLER

## 📊 CHỨC NĂNG TRÙNG LẶP

### ❌ TRÙNG HOÀN TOÀN: Xem Chi Tiết Đơn Hàng

**OrderController:**
```java
@GetMapping("/{orderId}")
public String viewOrder(@PathVariable String orderId) {
    // Redirect to UserOrderController
    return "redirect:/user/orders/" + orderId;
}
```

**UserOrderController:**
```java
@GetMapping("/orders/{orderId}")
public String orderDetail(@PathVariable String orderId, ...) {
    // Load order and order items
    // Display order detail
}
```

**✅ ĐÃ GIẢI QUYẾT:** OrderController giờ redirect sang UserOrderController

---

## 🔍 LOGIC NGHIỆP VỤ TRONG ORDERCONTROLLER

### ❌ SAI: Đang dùng OrderProcessingService (không tồn tại trong yêu cầu ban đầu)

OrderController hiện tại:
- ✅ Inject `OrderProcessingService`  
- ❌ Nhưng logic cần tách xuống **CartService** và **OrderService** (theo yêu cầu)

---

## 📋 LOGIC CẦN TÁCH XUỐNG SERVICE

### 1️⃣ Logic cần tách vào **CartService**

#### A. Validate Cart Trước Checkout
```java
// ❌ ĐANG Ở: OrderController (qua OrderProcessingService)
CheckoutValidationResult validation = orderProcessingService.validateCheckout(currentUser, cart);

// ✅ NÊN CHUYỂN SANG: CartService
boolean isCartValid = cartService.validateCart(cart);
List<String> errors = cartService.getCartValidationErrors(cart, currentUser);
```

**Logic cần chuyển:**
- Kiểm tra giỏ hàng trống
- Kiểm tra sách đã mua (duplicate)
- Tính tổng tiền giỏ hàng

#### B. Lấy Cart Items với Validation
```java
// ❌ HIỆN TẠI: Gọi trực tiếp cartItemService
List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);

// ✅ NÊN: CartService cung cấp method với validation
List<CartItem> cartItems = cartService.getValidCartItems(cart, currentUser);
```

#### C. Tính Tổng Tiền Giỏ Hàng
```java
// ❌ ĐANG Ở: OrderProcessingService.calculateTotalAmount()

// ✅ NÊN: CartService
BigDecimal total = cartService.calculateCartTotal(cart);
```

#### D. Xóa Giỏ Hàng Sau Checkout
```java
// ❌ ĐANG Ở: OrderProcessingService (clearCart private method)

// ✅ NÊN: CartService
cartService.clearCart(cart);
// HOẶC
cartService.clearCartForUser(currentUser);
```

---

### 2️⃣ Logic cần tách vào **OrderService**

#### A. Tạo Order Từ Cart
```java
// ❌ ĐANG Ở: OrderProcessingService.createOrderFromCart()
Order order = orderProcessingService.createOrderFromCart(currentUser, cart, method);

// ✅ NÊN: OrderService
Order order = orderService.createOrderFromCart(currentUser, cart, paymentMethod);
```

**Logic bao gồm:**
- Tạo Order entity
- Tạo OrderItems từ CartItems
- Lưu Order và OrderItems
- Xóa cart (gọi CartService)
- Transaction management

#### B. Hủy Đơn Hàng
```java
// ❌ ĐANG Ở: OrderProcessingService.cancelOrder()
orderProcessingService.cancelOrder(orderId, currentUser);

// ✅ NÊN: OrderService
orderService.cancelOrder(orderId, currentUser);
```

**Logic bao gồm:**
- Kiểm tra quyền user
- Kiểm tra trạng thái order (chỉ PENDING)
- Cập nhật status thành CANCELLED

#### C. Kiểm Tra Quyền Truy Cập Order
```java
// ❌ ĐANG Ở: OrderProcessingService.canUserAccessOrder()
boolean canAccess = orderProcessingService.canUserAccessOrder(order, currentUser);

// ✅ NÊN: OrderService
boolean canAccess = orderService.canUserAccessOrder(order, currentUser);
```

---

## 🎯 KẾ HOẠCH REFACTOR

### Phase 1: Cập nhật CartService

```java
// CartService.java - Thêm methods
public interface CartService {
    // Existing
    Optional<Cart> getCartByUser(User user);
    Optional<Cart> getCartById(String cartId);
    Cart createCartForUser(User user);
    Cart saveCart(Cart cart);
    
    // ✅ MỚI - Methods cần thêm
    
    /**
     * Validate cart có hợp lệ để checkout không
     */
    boolean isCartValidForCheckout(Cart cart, User user);
    
    /**
     * Lấy danh sách lỗi validation (nếu có)
     */
    List<String> getCartValidationErrors(Cart cart, User user);
    
    /**
     * Tính tổng tiền giỏ hàng
     */
    BigDecimal calculateCartTotal(Cart cart);
    
    /**
     * Lấy cart items với validation (loại bỏ items đã mua)
     */
    List<CartItem> getValidCartItems(Cart cart, User user);
    
    /**
     * Kiểm tra sách trong cart đã mua chưa
     */
    List<String> findDuplicateBookTitles(Cart cart, User user);
    
    /**
     * Xóa toàn bộ giỏ hàng
     */
    void clearCart(Cart cart);
    
    /**
     * Xóa giỏ hàng của user
     */
    void clearCartForUser(User user);
}
```

### Phase 2: Cập nhật OrderService

```java
// OrderService.java - Thêm methods
public interface OrderService {
    // Existing methods...
    
    // ✅ MỚI - Methods cần thêm
    
    /**
     * Tạo order từ giỏ hàng
     * @return Order đã tạo
     * @throws RuntimeException nếu validation fail
     */
    Order createOrderFromCart(User user, Cart cart, Order.PaymentMethod paymentMethod);
    
    /**
     * Hủy đơn hàng
     * @return true nếu hủy thành công
     * @throws RuntimeException nếu không thể hủy
     */
    boolean cancelOrder(String orderId, User user);
    
    /**
     * Kiểm tra user có quyền xem order không
     */
    boolean canUserAccessOrder(Order order, User user);
    
    /**
     * Lấy orders với phân trang
     */
    List<Order> getOrdersByUserPaged(User user, int page, int pageSize);
}
```

### Phase 3: Cập nhật OrderController

```java
@Controller
@RequestMapping("/order")
public class OrderController extends BaseController {
    
    // ✅ CHỈ inject các service cơ bản
    private final OrderService orderService;
    private final CartService cartService;
    
    // ❌ XÓA OrderProcessingService
    
    @GetMapping("/checkout")
    public String showCheckout(...) {
        Cart cart = cartService.getCartByUser(currentUser).orElse(null);
        
        // ✅ Validate qua CartService
        if (!cartService.isCartValidForCheckout(cart, currentUser)) {
            List<String> errors = cartService.getCartValidationErrors(cart, currentUser);
            // handle errors
        }
        
        // ✅ Tính tổng qua CartService
        BigDecimal total = cartService.calculateCartTotal(cart);
        
        // ✅ Lấy items qua CartService
        List<CartItem> items = cartService.getValidCartItems(cart, currentUser);
    }
    
    @PostMapping("/create")
    public String createOrder(...) {
        Cart cart = cartService.getCartByUser(currentUser).orElseThrow();
        
        // ✅ Tạo order qua OrderService
        Order order = orderService.createOrderFromCart(
            currentUser, 
            cart, 
            Order.PaymentMethod.valueOf(paymentMethod)
        );
        
        // Redirect...
    }
    
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(...) {
        // ✅ Hủy qua OrderService
        orderService.cancelOrder(orderId, currentUser);
    }
    
    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, Object> checkOrderStatus(...) {
        Order order = orderService.getOrderById(orderId).orElse(null);
        
        // ✅ Check permission qua OrderService
        if (!orderService.canUserAccessOrder(order, currentUser)) {
            // unauthorized
        }
    }
}
```

---

## 📊 SO SÁNH

### HIỆN TẠI (SAI)
```
OrderController
    ↓
OrderProcessingService ← ❌ Service thừa, logic phân tán
    ↓
OrderService, CartService
```

### MỤC TIÊU (ĐÚNG)
```
OrderController
    ↓ ↓
    ↓ CartService ← ✅ Logic cart validation, tính tổng
    ↓     ↓
    OrderService ← ✅ Logic order creation, cancel
```

---

## ✅ ACTION ITEMS

### 1. Xóa/Không dùng OrderProcessingService
- [ ] Không inject vào OrderController
- [ ] Có thể giữ file nhưng không dùng (hoặc xóa)

### 2. Thêm methods vào CartService
- [ ] `isCartValidForCheckout()`
- [ ] `getCartValidationErrors()`
- [ ] `calculateCartTotal()`
- [ ] `getValidCartItems()`
- [ ] `findDuplicateBookTitles()`
- [ ] `clearCart()`

### 3. Thêm methods vào OrderService
- [ ] `createOrderFromCart()`
- [ ] `cancelOrder()`
- [ ] `canUserAccessOrder()`

### 4. Refactor OrderController
- [ ] Remove OrderProcessingService injection
- [ ] Use CartService for validation
- [ ] Use OrderService for order operations

### 5. Giữ UserOrderController đơn giản
- [ ] Chỉ hiển thị orders
- [ ] Không có business logic

---

## 🎯 KẾT LUẬN

**Vấn đề hiện tại:**
- ❌ Đã tạo OrderProcessingService (không cần thiết)
- ❌ Logic đúng nhưng nằm sai chỗ
- ✅ Đã giải quyết chức năng trùng (redirect viewOrder)

**Cần làm:**
1. Chuyển logic từ OrderProcessingService vào CartService và OrderService
2. OrderController chỉ gọi CartService và OrderService
3. Giữ code đơn giản, rõ ràng

**Không cần làm:**
- ❌ Tạo thêm DTO phức tạp
- ❌ Tạo thêm service layer
- ✅ Chỉ dùng CartService và OrderService có sẵn

