# 🛒 FLOW 03: SHOPPING CART & CHECKOUT (Giỏ Hàng & Thanh Toán)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 3.1: Browse & Search Books](#flow-31-browse--search-books)
3. [Flow 3.2: Add to Cart](#flow-32-add-to-cart)
4. [Flow 3.3: View Cart](#flow-33-view-cart)
5. [Flow 3.4: Update Cart](#flow-34-update-cart)
6. [Flow 3.5: Apply Coupon](#flow-35-apply-coupon)
7. [Flow 3.6: Checkout](#flow-36-checkout)
8. [Flow 3.7: Payment Processing](#flow-37-payment-processing)
9. [Debugging Endpoints](#debugging-endpoints)

---

## Tổng Quan

### Complete Shopping Journey
```
┌──────────────┐
│ Browse Books │
│ /user/books  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ View Details │
│ /user/books/ │
│    /{id}     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Add to Cart │
│ POST /cart   │
│    /add      │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  View Cart   │
│  /user/cart  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Apply Coupon? │◄──── Optional
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Checkout    │
│/user/checkout│
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Select Payment│
│   Method     │
└──────┬───────┘
       │
       ├──► COD
       ├──► VNPAY
       └──► MOMO
       │
       ▼
┌──────────────┐
│Create Order  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Clear Cart   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Confirmation  │
└──────────────┘
```

### Components
- **Controllers**: `UserBookController.java`, `CartController.java`, `OrderController.java`
- **Services**: `BookService.java`, `CartService.java`, `OrderService.java`, `CouponService.java`
- **Entities**: `Book.java`, `CartItem.java`, `Order.java`, `OrderItem.java`, `Coupon.java`

---

## Flow 3.1: Browse & Search Books

### Sequence Diagram
```
User → Browser → UserBookController → BookService → BookRepository → Database
  │       │             │                  │             │             │
  │  GET /user/books?keyword=java&category=1                          │
  │──────────────────────►│                                            │
  │       │               │ searchBooks()                              │
  │       │               ├─────────────────►│                         │
  │       │               │                  │ findByFilter()          │
  │       │               │                  ├────────────►│           │
  │       │               │                  │             │ SELECT *  │
  │       │               │                  │             ├──────────►│
  │       │               │                  │             │◄──────────┤
  │       │               │                  │◄────────────┤           │
  │       │               │◄─────────────────┤                         │
  │◄──────────────────────┤ (return user/books/list.html)             │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/books")
public String browseBooks(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false) String sortBy,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size,
    Model model) {
    
    Pageable pageable = PageRequest.of(page, size);
    Page<Book> books;
    
    if (keyword != null && !keyword.isEmpty()) {
        books = bookService.searchBooks(keyword, pageable);
    } else if (categoryId != null) {
        books = bookService.getBooksByCategory(categoryId, pageable);
    } else {
        books = bookService.getAllAvailableBooks(pageable);
    }
    
    model.addAttribute("books", books);
    model.addAttribute("categories", categoryService.getAllCategories());
    return "user/books/list";
}
```

**SQL Query**:
```sql
SELECT b.*, bc.category_name,
       GROUP_CONCAT(a.author_name) as authors,
       AVG(r.rating) as avg_rating,
       COUNT(r.review_id) as review_count
FROM books b
LEFT JOIN book_category bc ON b.book_category_id = bc.book_category_id
LEFT JOIN book_authors ba ON b.book_id = ba.book_id
LEFT JOIN authors a ON ba.author_id = a.author_id
LEFT JOIN reviews r ON b.book_id = r.book_id
WHERE b.is_available = true
  AND b.stock_quantity > 0
  AND (? IS NULL OR b.title LIKE CONCAT('%', ?, '%') 
       OR a.author_name LIKE CONCAT('%', ?, '%'))
  AND (? IS NULL OR b.book_category_id = ?)
GROUP BY b.book_id
ORDER BY 
  CASE WHEN ? = 'price_asc' THEN b.price END ASC,
  CASE WHEN ? = 'price_desc' THEN b.price END DESC,
  CASE WHEN ? = 'newest' THEN b.created_at END DESC,
  CASE WHEN ? = 'popular' THEN COUNT(r.review_id) END DESC
LIMIT ? OFFSET ?;
```

---

## Flow 3.2: Add to Cart

### Sequence Diagram
```
User → Browser → CartController → CartService → CartItemRepository → Database
  │       │           │               │                │                │
  │ Click "Add to Cart" (bookId=1, quantity=2)                         │
  │──────────────────────►│                                             │
  │       │               │ addToCart()                                 │
  │       │               ├─────────────►│                              │
  │       │               │               │ findByUserAndBook()         │
  │       │               │               ├────────────────►│           │
  │       │               │               │                 │ SELECT *  │
  │       │               │               │                 ├──────────►│
  │       │               │               │                 │◄──────────┤
  │       │               │               │◄────────────────┤           │
  │       │               │               │ if exists:                  │
  │       │               │               │   update quantity           │
  │       │               │               │ else:                       │
  │       │               │               │   create new                │
  │       │               │               │ save()                      │
  │       │               │               ├────────────────►│           │
  │       │               │               │                 │ INSERT/   │
  │       │               │               │                 │ UPDATE    │
  │       │               │               │                 ├──────────►│
  │       │               │               │                 │◄──────────┤
  │       │               │               │◄────────────────┤           │
  │       │               │◄─────────────┤                              │
  │◄──────────────────────┤ redirect:/user/cart                        │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/cart/add")
public String addToCart(
    @RequestParam Long bookId,
    @RequestParam(defaultValue = "1") Integer quantity,
    Authentication authentication,
    RedirectAttributes redirectAttributes) {
    
    try {
        // 1. Get current user
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        
        // 2. Get book
        Book book = bookService.getBookById(bookId);
        
        // 3. Validate stock
        if (book.getStockQuantity() < quantity) {
            throw new RuntimeException("Không đủ hàng trong kho");
        }
        
        // 4. Add to cart
        cartService.addToCart(user, book, quantity);
        
        redirectAttributes.addFlashAttribute("success", 
            "Đã thêm vào giỏ hàng!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    
    return "redirect:/user/cart";
}
```

**Service Logic**:
```java
@Override
@Transactional
public CartItem addToCart(User user, Book book, Integer quantity) {
    // 1. Check if item already in cart
    Optional<CartItem> existingItem = cartItemRepository
        .findByUserAndBook(user, book);
    
    if (existingItem.isPresent()) {
        // 2. Update quantity
        CartItem item = existingItem.get();
        int newQuantity = item.getQuantity() + quantity;
        
        // Validate stock
        if (newQuantity > book.getStockQuantity()) {
            throw new RuntimeException("Vượt quá số lượng trong kho");
        }
        
        item.setQuantity(newQuantity);
        item.setSubtotal(book.getDiscountPrice() * newQuantity);
        return cartItemRepository.save(item);
        
    } else {
        // 3. Create new cart item
        CartItem item = new CartItem();
        item.setUser(user);
        item.setBook(book);
        item.setQuantity(quantity);
        item.setPrice(book.getDiscountPrice());
        item.setSubtotal(book.getDiscountPrice() * quantity);
        return cartItemRepository.save(item);
    }
}
```

**SQL Queries**:
```sql
-- Check existing cart item
SELECT * FROM cart_items
WHERE user_id = ? AND book_id = ?

-- Update existing
UPDATE cart_items
SET quantity = ?,
    subtotal = ?,
    updated_at = NOW()
WHERE cart_item_id = ?

-- Insert new
INSERT INTO cart_items (user_id, book_id, quantity, price, subtotal, created_at)
VALUES (?, ?, ?, ?, ?, NOW())
```

---

## Flow 3.3: View Cart

### Sequence Diagram
```
User → Browser → CartController → CartService → CartItemRepository → Database
  │       │           │               │                │                │
  │  GET /user/cart                                                     │
  │──────────────────────►│                                             │
  │       │               │ getCartItems()                              │
  │       │               ├─────────────►│                              │
  │       │               │               │ findByUser()                │
  │       │               │               ├────────────────►│           │
  │       │               │               │                 │ SELECT *  │
  │       │               │               │                 ├──────────►│
  │       │               │               │                 │◄──────────┤
  │       │               │               │◄────────────────┤           │
  │       │               │◄─────────────┤                              │
  │       │               │ calculateTotal()                            │
  │◄──────────────────────┤ (return user/cart/view.html)               │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/cart")
public String viewCart(Authentication authentication, Model model) {
    String username = authentication.getName();
    User user = userService.findByUsername(username);
    
    // Get cart items
    List<CartItem> cartItems = cartService.getCartItems(user);
    
    // Calculate totals
    BigDecimal subtotal = cartItems.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal discount = BigDecimal.ZERO;
    if (model.containsAttribute("appliedCoupon")) {
        Coupon coupon = (Coupon) model.getAttribute("appliedCoupon");
        discount = cartService.calculateDiscount(subtotal, coupon);
    }
    
    BigDecimal total = subtotal.subtract(discount);
    
    model.addAttribute("cartItems", cartItems);
    model.addAttribute("subtotal", subtotal);
    model.addAttribute("discount", discount);
    model.addAttribute("total", total);
    model.addAttribute("itemCount", cartItems.size());
    
    return "user/cart/view";
}
```

**SQL Query**:
```sql
SELECT ci.*, b.title, b.cover_image_url, b.discount_price, b.stock_quantity
FROM cart_items ci
INNER JOIN books b ON ci.book_id = b.book_id
WHERE ci.user_id = ?
ORDER BY ci.created_at DESC;
```

---

## Flow 3.4: Update Cart

### Implementation Details

**Remove Item**:
```java
@PostMapping("/cart/remove/{itemId}")
public String removeFromCart(@PathVariable Long itemId,
                            RedirectAttributes redirectAttributes) {
    try {
        cartService.removeFromCart(itemId);
        redirectAttributes.addFlashAttribute("success", "Đã xóa khỏi giỏ hàng");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/user/cart";
}
```

**Update Quantity**:
```java
@PostMapping("/cart/update/{itemId}")
public String updateCartItem(@PathVariable Long itemId,
                            @RequestParam Integer quantity,
                            RedirectAttributes redirectAttributes) {
    try {
        cartService.updateQuantity(itemId, quantity);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật số lượng");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/user/cart";
}
```

**SQL**:
```sql
-- Update quantity
UPDATE cart_items
SET quantity = ?,
    subtotal = price * ?,
    updated_at = NOW()
WHERE cart_item_id = ?

-- Delete item
DELETE FROM cart_items WHERE cart_item_id = ?
```

---

## Flow 3.5: Apply Coupon

### Sequence Diagram
```
User → Browser → CartController → CouponService → CouponRepository → Database
  │       │           │                 │               │               │
  │  POST /cart/apply-coupon (code="SUMMER2024")                       │
  │──────────────────────►│                                             │
  │       │               │ validateCoupon()                            │
  │       │               ├────────────────►│                           │
  │       │               │                 │ findByCode()              │
  │       │               │                 ├──────────────►│           │
  │       │               │                 │                │ SELECT * │
  │       │               │                 │                ├─────────►│
  │       │               │                 │                │◄─────────┤
  │       │               │                 │◄──────────────┤           │
  │       │               │                 │ validate()                │
  │       │               │                 │ - check active            │
  │       │               │                 │ - check dates             │
  │       │               │                 │ - check usage             │
  │       │               │                 │ - check min order         │
  │       │               │◄────────────────┤                           │
  │       │               │ calculateDiscount()                         │
  │       │               │ saveToSession()                             │
  │◄──────────────────────┤ redirect:/user/cart                        │
```

### Implementation Details

**Controller**:
```java
@PostMapping("/cart/apply-coupon")
public String applyCoupon(@RequestParam String couponCode,
                         Authentication authentication,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
    try {
        User user = userService.findByUsername(authentication.getName());
        List<CartItem> cartItems = cartService.getCartItems(user);
        
        BigDecimal subtotal = cartItems.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Validate and apply coupon
        Coupon coupon = couponService.validateAndGetCoupon(
            couponCode, user, subtotal
        );
        
        // Save to session
        session.setAttribute("appliedCoupon", coupon);
        
        redirectAttributes.addFlashAttribute("success", 
            "Đã áp dụng mã giảm giá!");
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    
    return "redirect:/user/cart";
}
```

**Service Logic**:
```java
@Override
public Coupon validateAndGetCoupon(String code, User user, BigDecimal orderTotal) {
    // 1. Find coupon
    Coupon coupon = couponRepository.findByCode(code)
        .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));
    
    // 2. Check active
    if (!coupon.getIsActive()) {
        throw new RuntimeException("Mã giảm giá đã bị vô hiệu hóa");
    }
    
    // 3. Check dates
    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
        throw new RuntimeException("Mã giảm giá đã hết hạn");
    }
    
    // 4. Check usage limit
    if (coupon.getUsageCount() >= coupon.getMaxUsage()) {
        throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
    }
    
    // 5. Check minimum order
    if (orderTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
        throw new RuntimeException(
            "Đơn hàng tối thiểu: " + coupon.getMinimumOrderAmount()
        );
    }
    
    // 6. Check user usage (if applicable)
    if (coupon.getMaxUsagePerUser() != null) {
        int userUsage = couponUsageRepository
            .countByUserAndCoupon(user, coupon);
        if (userUsage >= coupon.getMaxUsagePerUser()) {
            throw new RuntimeException(
                "Bạn đã sử dụng hết lượt giảm giá này"
            );
        }
    }
    
    return coupon;
}

@Override
public BigDecimal calculateDiscount(BigDecimal subtotal, Coupon coupon) {
    BigDecimal discount;
    
    if ("PERCENTAGE".equals(coupon.getDiscountType())) {
        // Percentage discount
        discount = subtotal.multiply(coupon.getDiscountValue())
            .divide(BigDecimal.valueOf(100));
        
        // Apply max discount
        if (coupon.getMaxDiscountAmount() != null 
            && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
    } else {
        // Fixed amount discount
        discount = coupon.getDiscountValue();
    }
    
    // Discount cannot exceed subtotal
    if (discount.compareTo(subtotal) > 0) {
        discount = subtotal;
    }
    
    return discount;
}
```

**SQL Queries**:
```sql
-- Find coupon by code
SELECT * FROM coupons
WHERE coupon_code = ?
AND is_active = true
AND start_date <= NOW()
AND end_date >= NOW()

-- Check user usage
SELECT COUNT(*) FROM coupon_usage
WHERE user_id = ? AND coupon_id = ?
```

---

## Flow 3.6: Checkout

### Sequence Diagram
```
User → Browser → OrderController → OrderService → CartService → Database
  │       │           │                │              │            │
  │  GET /user/checkout                                            │
  │──────────────────────►│                                        │
  │       │               │ getCartItems()                         │
  │       │               │ getUserAddresses()                     │
  │◄──────────────────────┤ (return checkout.html)                │
  │       │               │                                        │
  │  POST /user/checkout (CheckoutDto)                            │
  │──────────────────────────►│                                    │
  │       │                   │ createOrder()                      │
  │       │                   ├───────────────►│                   │
  │       │                   │                │ validateStock()   │
  │       │                   │                │ createOrder()     │
  │       │                   │                │ createOrderItems()│
  │       │                   │                │ updateStock()     │
  │       │                   │                │ clearCart()       │
  │       │                   │                ├──────────────────►│
  │       │                   │                │◄──────────────────┤
  │       │                   │◄───────────────┤                   │
  │◄──────────────────────────┤ redirect based on payment         │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/checkout")
public String showCheckoutForm(Authentication authentication, Model model) {
    User user = userService.findByUsername(authentication.getName());
    
    // Get cart items
    List<CartItem> cartItems = cartService.getCartItems(user);
    if (cartItems.isEmpty()) {
        return "redirect:/user/cart";
    }
    
    // Calculate totals
    BigDecimal subtotal = cartItems.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Get applied coupon from session
    Coupon coupon = (Coupon) session.getAttribute("appliedCoupon");
    BigDecimal discount = coupon != null 
        ? couponService.calculateDiscount(subtotal, coupon) 
        : BigDecimal.ZERO;
    
    BigDecimal total = subtotal.subtract(discount);
    
    model.addAttribute("cartItems", cartItems);
    model.addAttribute("subtotal", subtotal);
    model.addAttribute("discount", discount);
    model.addAttribute("total", total);
    model.addAttribute("user", user);
    model.addAttribute("checkout", new CheckoutDto());
    
    return "user/checkout/form";
}

@PostMapping("/checkout")
public String processCheckout(
    @Valid @ModelAttribute("checkout") CheckoutDto checkoutDto,
    Authentication authentication,
    HttpSession session,
    RedirectAttributes redirectAttributes) {
    
    try {
        User user = userService.findByUsername(authentication.getName());
        Coupon coupon = (Coupon) session.getAttribute("appliedCoupon");
        
        // Create order
        Order order = orderService.createOrder(user, checkoutDto, coupon);
        
        // Clear cart
        cartService.clearCart(user);
        
        // Clear coupon from session
        session.removeAttribute("appliedCoupon");
        
        // Redirect based on payment method
        if ("VNPAY".equals(checkoutDto.getPaymentMethod())) {
            return "redirect:/payment/vnpay?orderId=" + order.getOrderId();
        } else if ("MOMO".equals(checkoutDto.getPaymentMethod())) {
            return "redirect:/payment/momo?orderId=" + order.getOrderId();
        } else {
            // COD
            redirectAttributes.addFlashAttribute("success", 
                "Đặt hàng thành công!");
            return "redirect:/user/orders/" + order.getOrderId();
        }
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/user/checkout";
    }
}
```

**Service Logic**:
```java
@Override
@Transactional
public Order createOrder(User user, CheckoutDto checkoutDto, Coupon coupon) {
    // 1. Get cart items
    List<CartItem> cartItems = cartService.getCartItems(user);
    if (cartItems.isEmpty()) {
        throw new RuntimeException("Giỏ hàng trống");
    }
    
    // 2. Validate stock
    for (CartItem item : cartItems) {
        Book book = item.getBook();
        if (book.getStockQuantity() < item.getQuantity()) {
            throw new RuntimeException(
                "Sách '" + book.getTitle() + "' không đủ hàng trong kho"
            );
        }
    }
    
    // 3. Calculate totals
    BigDecimal subtotal = cartItems.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal discount = coupon != null 
        ? couponService.calculateDiscount(subtotal, coupon) 
        : BigDecimal.ZERO;
    
    BigDecimal total = subtotal.subtract(discount);
    
    // 4. Create order
    Order order = new Order();
    order.setUser(user);
    order.setOrderNumber(generateOrderNumber());
    order.setSubtotalAmount(subtotal);
    order.setDiscountAmount(discount);
    order.setTotalAmount(total);
    order.setOrderStatus("PENDING");
    order.setPaymentMethod(checkoutDto.getPaymentMethod());
    order.setPaymentStatus("UNPAID");
    
    // Shipping info
    order.setShippingName(checkoutDto.getShippingName());
    order.setShippingPhone(checkoutDto.getShippingPhone());
    order.setShippingAddress(checkoutDto.getShippingAddress());
    order.setShippingCity(checkoutDto.getShippingCity());
    order.setShippingDistrict(checkoutDto.getShippingDistrict());
    order.setShippingWard(checkoutDto.getShippingWard());
    order.setNote(checkoutDto.getNote());
    
    // Applied coupon
    if (coupon != null) {
        order.setCoupon(coupon);
    }
    
    order = orderRepository.save(order);
    
    // 5. Create order items & update stock
    for (CartItem cartItem : cartItems) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setBook(cartItem.getBook());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setPrice(cartItem.getPrice());
        orderItem.setSubtotal(cartItem.getSubtotal());
        orderItemRepository.save(orderItem);
        
        // Update stock
        Book book = cartItem.getBook();
        book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
        bookRepository.save(book);
    }
    
    // 6. Update coupon usage
    if (coupon != null) {
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);
        
        // Record user usage
        CouponUsage usage = new CouponUsage();
        usage.setUser(user);
        usage.setCoupon(coupon);
        usage.setOrder(order);
        usage.setUsedAt(LocalDateTime.now());
        couponUsageRepository.save(usage);
    }
    
    return order;
}

private String generateOrderNumber() {
    return "ORD" + System.currentTimeMillis();
}
```

**SQL Queries**:
```sql
-- Insert order
INSERT INTO orders (
    user_id, order_number, subtotal_amount, discount_amount, 
    total_amount, order_status, payment_method, payment_status,
    shipping_name, shipping_phone, shipping_address,
    shipping_city, shipping_district, shipping_ward, note,
    coupon_id, created_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())

-- Insert order items
INSERT INTO order_items (order_id, book_id, quantity, price, subtotal)
VALUES (?, ?, ?, ?, ?)

-- Update book stock
UPDATE books
SET stock_quantity = stock_quantity - ?
WHERE book_id = ?

-- Update coupon usage
UPDATE coupons
SET usage_count = usage_count + 1
WHERE coupon_id = ?

-- Insert coupon usage record
INSERT INTO coupon_usage (user_id, coupon_id, order_id, used_at)
VALUES (?, ?, ?, NOW())

-- Clear cart
DELETE FROM cart_items WHERE user_id = ?
```

---

## Flow 3.7: Payment Processing

### COD (Cash on Delivery)
```
User submits order → Order created with PENDING status → Confirmation page
```

**No additional processing needed**

### VNPAY Integration
```java
@GetMapping("/payment/vnpay")
public String vnpayPayment(@RequestParam Long orderId, HttpServletRequest request) {
    Order order = orderService.getOrderById(orderId);
    
    // Build VNPAY payment URL
    String vnpayUrl = vnpayService.createPaymentUrl(order, request);
    
    return "redirect:" + vnpayUrl;
}

@GetMapping("/payment/vnpay/callback")
public String vnpayCallback(@RequestParam Map<String, String> params,
                           RedirectAttributes redirectAttributes) {
    try {
        // Verify signature
        boolean isValid = vnpayService.verifySignature(params);
        if (!isValid) {
            throw new RuntimeException("Invalid signature");
        }
        
        String orderNumber = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        
        Order order = orderService.getOrderByOrderNumber(orderNumber);
        
        if ("00".equals(responseCode)) {
            // Success
            order.setPaymentStatus("PAID");
            order.setOrderStatus("CONFIRMED");
            orderRepository.save(order);
            
            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công!");
        } else {
            // Failed
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);
            
            redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại!");
        }
        
        return "redirect:/user/orders/" + order.getOrderId();
        
    } catch (Exception e) {
        return "redirect:/user/orders";
    }
}
```

---

## Debugging Endpoints

### 1. Check Cart Items

**Debug Controller**:
```java
@GetMapping("/debug/cart")
@ResponseBody
public Map<String, Object> debugCart(Authentication authentication) {
    User user = userService.findByUsername(authentication.getName());
    List<CartItem> cartItems = cartService.getCartItems(user);
    
    Map<String, Object> result = new HashMap<>();
    result.put("userId", user.getUserId());
    result.put("username", user.getUsername());
    result.put("itemCount", cartItems.size());
    result.put("items", cartItems.stream().map(item -> {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("cartItemId", item.getCartItemId());
        itemData.put("bookId", item.getBook().getBookId());
        itemData.put("bookTitle", item.getBook().getTitle());
        itemData.put("quantity", item.getQuantity());
        itemData.put("price", item.getPrice());
        itemData.put("subtotal", item.getSubtotal());
        return itemData;
    }).collect(Collectors.toList()));
    
    BigDecimal subtotal = cartItems.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    result.put("subtotal", subtotal);
    
    return result;
}
```

**Test URL**: `http://localhost:8080/debug/cart`

### 2. Test Add to Cart

**cURL**:
```bash
curl -X POST http://localhost:8080/user/cart/add \
  -H "Cookie: JSESSIONID=xxx" \
  -d "bookId=1&quantity=2" \
  -L
```

**Postman**:
- Method: POST
- URL: `http://localhost:8080/user/cart/add`
- Body (form-data):
  - bookId: 1
  - quantity: 2

### 3. Test Apply Coupon

**Debug Controller**:
```java
@GetMapping("/debug/coupon/{code}")
@ResponseBody
public Map<String, Object> debugCoupon(@PathVariable String code) {
    Coupon coupon = couponRepository.findByCode(code)
        .orElseThrow(() -> new RuntimeException("Coupon not found"));
    
    Map<String, Object> result = new HashMap<>();
    result.put("couponId", coupon.getCouponId());
    result.put("code", coupon.getCouponCode());
    result.put("discountType", coupon.getDiscountType());
    result.put("discountValue", coupon.getDiscountValue());
    result.put("minOrderAmount", coupon.getMinimumOrderAmount());
    result.put("maxDiscountAmount", coupon.getMaxDiscountAmount());
    result.put("isActive", coupon.getIsActive());
    result.put("usageCount", coupon.getUsageCount());
    result.put("maxUsage", coupon.getMaxUsage());
    result.put("startDate", coupon.getStartDate());
    result.put("endDate", coupon.getEndDate());
    return result;
}
```

### 4. Test Order Creation

**SQL Verify**:
```sql
-- Check order created
SELECT * FROM orders WHERE order_id = ?;

-- Check order items
SELECT oi.*, b.title
FROM order_items oi
INNER JOIN books b ON oi.book_id = b.book_id
WHERE oi.order_id = ?;

-- Check stock updated
SELECT book_id, title, stock_quantity
FROM books
WHERE book_id IN (SELECT book_id FROM order_items WHERE order_id = ?);

-- Check coupon usage
SELECT * FROM coupon_usage WHERE order_id = ?;

-- Check cart cleared
SELECT COUNT(*) FROM cart_items WHERE user_id = ?;
```

### 5. Common Issues & Solutions

| Issue | Debug Method | Solution |
|-------|-------------|----------|
| Cart empty after logout | Check session | Use database cart instead of session |
| Stock not updated | Check transaction | Ensure @Transactional on service |
| Coupon not applied | Check validation | Verify all coupon conditions |
| Order total wrong | Check calculation | Debug discount logic |
| Payment redirect failed | Check URL params | Verify payment gateway config |

### 6. Logging Configuration

```properties
# Cart & Order logging
logging.level.com.example.ebook_store.controller.CartController=DEBUG
logging.level.com.example.ebook_store.controller.OrderController=DEBUG
logging.level.com.example.ebook_store.service.CartService=DEBUG
logging.level.com.example.ebook_store.service.OrderService=DEBUG
logging.level.com.example.ebook_store.service.CouponService=DEBUG

# Payment logging
logging.level.com.example.ebook_store.service.VNPayService=DEBUG

# Transaction logging
logging.level.org.springframework.transaction=DEBUG
```

### 7. Breakpoint Locations

**CartController**:
- Line: `cartService.addToCart(user, book, quantity)`

**OrderController**:
- Line: `Order order = orderService.createOrder(user, checkoutDto, coupon)`

**OrderService**:
- Line: Stock validation loop
- Line: `order = orderRepository.save(order)`
- Line: Order items creation loop
- Line: Stock update

---

## Test Scenarios

### Scenario 1: Complete shopping flow (COD)
1. Browse books
2. Add 3 items to cart
3. View cart
4. Apply coupon
5. Proceed to checkout
6. Fill shipping info
7. Select COD payment
8. Submit order
9. Verify: Order created
10. Verify: Stock updated
11. Verify: Cart cleared

### Scenario 2: Add same book twice
1. Add book A (qty=1)
2. Add book A (qty=2)
3. Verify: Quantity = 3 (not 2 items)

### Scenario 3: Coupon validation
1. Try invalid coupon code
2. Try expired coupon
3. Try coupon with min order not met
4. Try valid coupon
5. Verify: Discount applied correctly

### Scenario 4: Stock validation
1. Add book with qty > stock
2. Verify: Error shown
3. Update cart with qty > stock
4. Verify: Error shown

### Scenario 5: Payment integration
1. Complete checkout with VNPAY
2. Redirect to payment gateway
3. Complete payment
4. Return to callback
5. Verify: Order status = PAID

---

**Last Updated**: 30/11/2025
**Version**: 2.0

