# 📦 FLOW 08: ADMIN ORDER MANAGEMENT (Quản Lý Đơn Hàng - Admin)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 8.1: List Orders](#flow-81-list-orders)
3. [Flow 8.2: View Order Details](#flow-82-view-order-details)
4. [Flow 8.3: Update Order Status](#flow-83-update-order-status)
5. [Flow 8.4: Cancel Order](#flow-84-cancel-order)
6. [Flow 8.5: Process Refund](#flow-85-process-refund)
7. [Flow 8.6: Export Orders](#flow-86-export-orders)
8. [Order Statistics & Analytics](#order-statistics--analytics)

---

## Tổng Quan

### Order Management Flow
```
┌──────────────┐
│ Admin Access │
│   Orders     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Orders List  │
│ with Filters │
└──────┬───────┘
       │
       ├──► View Details
       ├──► Update Status
       ├──► Cancel Order
       ├──► Process Refund
       └──► Export Data
```

### Components
- **Controller**: `AdminOrderController.java`
- **Service**: `OrderService.java`, `OrderItemService.java`
- **Entity**: `Order.java`, `OrderItem.java`, `User.java`, `Book.java`

### Order Status Flow
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
   │          │            │           │
   │          │            │           └──► COMPLETED
   │          │            │
   │          │            └──► CANCELLED
   │          │
   │          └──► CANCELLED
   │
   └──► CANCELLED
```

### Payment Status
- **PENDING**: Chưa thanh toán
- **PAID**: Đã thanh toán
- **FAILED**: Thanh toán thất bại
- **REFUNDED**: Đã hoàn tiền

### URLs
- `GET /admin/orders` - Danh sách orders
- `GET /admin/orders/{id}` - Chi tiết order
- `POST /admin/orders/{id}/status` - Cập nhật status
- `POST /admin/orders/{id}/cancel` - Hủy order
- `POST /admin/orders/{id}/refund` - Hoàn tiền
- `GET /admin/orders/export` - Export CSV

---

## Flow 8.1: List Orders

### Sequence Diagram
```
Admin → Browser → AdminOrderController → OrderService → OrderRepository → Database
  │        │              │                   │              │              │
  │ GET /admin/orders?status=PENDING&date=today                           │
  │────────────────────────►│                                              │
  │        │                │ getOrdersWithFilters()                       │
  │        │                ├──────────────────►│                          │
  │        │                │                   │ findByFilters()          │
  │        │                │                   ├─────────────►│           │
  │        │                │                   │              │ SELECT *  │
  │        │                │                   │              │ WHERE...  │
  │        │                │                   │              ├──────────►│
  │        │                │                   │              │◄──────────┤
  │        │                │                   │◄─────────────┤           │
  │        │                │◄──────────────────┤                          │
  │        │◄────────────────┤ (return orders list)                        │
```

### Implementation Details

**Controller**:
```java
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController extends BaseAdminController {
    
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    
    @GetMapping
    public String ordersList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        List<Order> orders;
        
        // Apply filters
        if (keyword != null && !keyword.isEmpty()) {
            orders = orderService.searchOrders(keyword);
        } else {
            orders = orderService.getOrdersWithFilters(
                status, paymentStatus, dateFrom, dateTo
            );
        }
        
        // Calculate statistics
        Map<String, Object> stats = calculateOrderStatistics(orders);
        
        model.addAttribute("orders", orders);
        model.addAttribute("stats", stats);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPaymentStatus", paymentStatus);
        model.addAttribute("keyword", keyword);
        
        // Status options
        model.addAttribute("orderStatuses", Arrays.asList(
            "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED"
        ));
        model.addAttribute("paymentStatuses", Arrays.asList(
            "PENDING", "PAID", "FAILED", "REFUNDED"
        ));
        
        return "admin/orders/list";
    }
    
    private Map<String, Object> calculateOrderStatistics(List<Order> orders) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalOrders", orders.size());
        
        BigDecimal totalRevenue = orders.stream()
            .filter(o -> "PAID".equals(o.getPaymentStatus()))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);
        
        long pendingOrders = orders.stream()
            .filter(o -> "PENDING".equals(o.getStatus()))
            .count();
        stats.put("pendingOrders", pendingOrders);
        
        long completedOrders = orders.stream()
            .filter(o -> "COMPLETED".equals(o.getStatus()))
            .count();
        stats.put("completedOrders", completedOrders);
        
        long cancelledOrders = orders.stream()
            .filter(o -> "CANCELLED".equals(o.getStatus()))
            .count();
        stats.put("cancelledOrders", cancelledOrders);
        
        return stats;
    }
}
```

**Service**:
```java
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    public List<Order> getOrdersWithFilters(
            String status,
            String paymentStatus,
            String dateFrom,
            String dateTo) {
        
        // Build specification for dynamic query
        Specification<Order> spec = Specification.where(null);
        
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("status"), status));
        }
        
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("paymentStatus"), paymentStatus));
        }
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            LocalDate from = LocalDate.parse(dateFrom);
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }
        
        if (dateTo != null && !dateTo.isEmpty()) {
            LocalDate to = LocalDate.parse(dateTo);
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23, 59, 59)));
        }
        
        return orderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
    
    public List<Order> searchOrders(String keyword) {
        return orderRepository.findByOrderIdContainingOrUser_UsernameContainingOrUser_EmailContaining(
            keyword, keyword, keyword
        );
    }
}
```

**SQL Query**:
```sql
SELECT o.*, 
       u.username, u.email, u.full_name,
       COUNT(oi.order_item_id) as total_items
FROM orders o
LEFT JOIN users u ON o.user_id = u.user_id
LEFT JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.status = ? 
  AND o.payment_status = ?
  AND o.created_at BETWEEN ? AND ?
GROUP BY o.order_id
ORDER BY o.created_at DESC;
```

---

## Flow 8.2: View Order Details

### Sequence Diagram
```
Admin → Browser → AdminOrderController → OrderService → Database
  │        │              │                   │              │
  │ GET /admin/orders/{id}                                  │
  │────────────────────────►│                               │
  │        │                │ getOrderById()                │
  │        │                ├──────────────────►│           │
  │        │                │                   │ SELECT *  │
  │        │                │                   │ WITH items│
  │        │                │                   ├──────────►│
  │        │                │                   │◄──────────┤
  │        │                │◄──────────────────┤           │
  │        │◄────────────────┤ (return detail page)         │
```

**Controller**:
```java
@GetMapping("/{id}")
public String viewOrderDetail(@PathVariable String id, Model model) {
    Order order = orderService.getOrderById(id);
    if (order == null) {
        return "redirect:/admin/orders?error=not_found";
    }
    
    // Get order items with book details
    List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(id);
    
    // Calculate totals
    BigDecimal subtotal = orderItems.stream()
        .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal discount = BigDecimal.ZERO;
    if (order.getCoupon() != null) {
        if (order.getCoupon().getDiscountType().equals("PERCENTAGE")) {
            discount = subtotal.multiply(order.getCoupon().getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = order.getCoupon().getDiscountValue();
        }
    }
    
    model.addAttribute("order", order);
    model.addAttribute("orderItems", orderItems);
    model.addAttribute("subtotal", subtotal);
    model.addAttribute("discount", discount);
    model.addAttribute("total", order.getTotalAmount());
    
    // Status history (if tracked)
    List<OrderStatusHistory> statusHistory = orderService.getOrderStatusHistory(id);
    model.addAttribute("statusHistory", statusHistory);
    
    return "admin/orders/detail";
}
```

**Response Data**:
```json
{
  "order": {
    "orderId": "ord_001",
    "user": {
      "userId": "usr_001",
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe"
    },
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "paymentMethod": "VNPAY",
    "totalAmount": 158000,
    "shippingAddress": "123 Main St, Hanoi",
    "createdAt": "2025-12-06T10:30:00",
    "transactionId": null
  },
  "orderItems": [
    {
      "orderItemId": "oi_001",
      "book": {
        "bookId": "book_001",
        "title": "Đắc Nhân Tâm",
        "coverImageUrl": "/uploads/covers/dacnhantam.jpg"
      },
      "quantity": 2,
      "priceAtPurchase": 79000
    }
  ],
  "subtotal": 158000,
  "discount": 0,
  "total": 158000
}
```

---

## Flow 8.3: Update Order Status

### Sequence Diagram
```
Admin → Browser → AdminOrderController → OrderService → Database
  │        │              │                   │              │
  │ POST /admin/orders/{id}/status                         │
  │────────────────────────►│                               │
  │        │                │ updateOrderStatus()           │
  │        │                ├──────────────────►│           │
  │        │                │                   │ UPDATE    │
  │        │                │                   ├──────────►│
  │        │                │                   │◄──────────┤
  │        │                │                   │           │
  │        │                │ logStatusChange() │           │
  │        │                │ sendNotification()│           │
  │        │                │◄──────────────────┤           │
  │        │◄────────────────┤ redirect with success        │
```

**Controller**:
```java
@PostMapping("/{id}/status")
public String updateOrderStatus(
        @PathVariable String id,
        @RequestParam String newStatus,
        @RequestParam(required = false) String notes,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
    
    try {
        User admin = (User) authentication.getPrincipal();
        Order order = orderService.getOrderById(id);
        
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order không tồn tại");
            return "redirect:/admin/orders";
        }
        
        // Validate status transition
        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            redirectAttributes.addFlashAttribute("error", 
                "Không thể chuyển từ " + order.getStatus() + " sang " + newStatus);
            return "redirect:/admin/orders/" + id;
        }
        
        // Update status
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderService.saveOrder(order);
        
        // Log status change
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(id);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(admin.getUserId());
        history.setNotes(notes);
        history.setChangedAt(LocalDateTime.now());
        orderService.saveStatusHistory(history);
        
        // Send notification to user
        notificationService.sendOrderStatusUpdate(order, newStatus);
        
        redirectAttributes.addFlashAttribute("success", 
            "Cập nhật trạng thái thành công: " + newStatus);
        return "redirect:/admin/orders/" + id;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật trạng thái");
        return "redirect:/admin/orders/" + id;
    }
}

/**
 * Validate status transition
 */
private boolean isValidStatusTransition(String currentStatus, String newStatus) {
    Map<String, List<String>> validTransitions = Map.of(
        "PENDING", Arrays.asList("CONFIRMED", "CANCELLED"),
        "CONFIRMED", Arrays.asList("PROCESSING", "CANCELLED"),
        "PROCESSING", Arrays.asList("SHIPPED", "CANCELLED"),
        "SHIPPED", Arrays.asList("DELIVERED", "CANCELLED"),
        "DELIVERED", Arrays.asList("COMPLETED"),
        "COMPLETED", Arrays.asList(), // Cannot change from COMPLETED
        "CANCELLED", Arrays.asList()  // Cannot change from CANCELLED
    );
    
    return validTransitions.getOrDefault(currentStatus, Arrays.asList())
        .contains(newStatus);
}
```

---

## Flow 8.4: Cancel Order

**Controller**:
```java
@PostMapping("/{id}/cancel")
public String cancelOrder(
        @PathVariable String id,
        @RequestParam String reason,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
    
    try {
        Order order = orderService.getOrderById(id);
        
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order không tồn tại");
            return "redirect:/admin/orders";
        }
        
        // Check if can cancel
        if (Arrays.asList("COMPLETED", "CANCELLED").contains(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", 
                "Không thể hủy order với trạng thái " + order.getStatus());
            return "redirect:/admin/orders/" + id;
        }
        
        // Cancel order
        order.setStatus("CANCELLED");
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        orderService.saveOrder(order);
        
        // If already paid, initiate refund
        if ("PAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("REFUND_PENDING");
            orderService.saveOrder(order);
            
            // TODO: Process refund with payment gateway
            // refundService.initiateRefund(order);
        }
        
        // Restore book quantities if necessary
        restoreBookStock(order);
        
        // Notify user
        notificationService.sendOrderCancellation(order, reason);
        
        redirectAttributes.addFlashAttribute("success", "Hủy đơn hàng thành công");
        return "redirect:/admin/orders/" + id;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi hủy đơn hàng");
        return "redirect:/admin/orders/" + id;
    }
}

private void restoreBookStock(Order order) {
    List<OrderItem> items = orderItemService.getOrderItemsByOrderId(order.getOrderId());
    for (OrderItem item : items) {
        Book book = item.getBook();
        // Only restore if book has stock tracking
        if (book.getStockQuantity() != null) {
            book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
            bookService.saveBook(book);
        }
    }
}
```

---

## Flow 8.5: Process Refund

**Controller**:
```java
@PostMapping("/{id}/refund")
public String processRefund(
        @PathVariable String id,
        @RequestParam BigDecimal refundAmount,
        @RequestParam(required = false) String refundReason,
        RedirectAttributes redirectAttributes) {
    
    try {
        Order order = orderService.getOrderById(id);
        
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order không tồn tại");
            return "redirect:/admin/orders";
        }
        
        // Validate refund
        if (!"PAID".equals(order.getPaymentStatus())) {
            redirectAttributes.addFlashAttribute("error", 
                "Order chưa được thanh toán");
            return "redirect:/admin/orders/" + id;
        }
        
        if (refundAmount.compareTo(order.getTotalAmount()) > 0) {
            redirectAttributes.addFlashAttribute("error", 
                "Số tiền hoàn lại không được lớn hơn tổng đơn hàng");
            return "redirect:/admin/orders/" + id;
        }
        
        // Process refund with payment gateway
        boolean refundSuccess = false;
        
        switch (order.getPaymentMethod()) {
            case "VNPAY":
                refundSuccess = vnpayService.processRefund(order, refundAmount);
                break;
            case "MOMO":
                refundSuccess = momoService.processRefund(order, refundAmount);
                break;
            case "COD":
                // Manual refund
                refundSuccess = true;
                break;
        }
        
        if (refundSuccess) {
            order.setPaymentStatus("REFUNDED");
            order.setRefundAmount(refundAmount);
            order.setRefundReason(refundReason);
            order.setRefundedAt(LocalDateTime.now());
            orderService.saveOrder(order);
            
            // Notify user
            notificationService.sendRefundConfirmation(order, refundAmount);
            
            redirectAttributes.addFlashAttribute("success", 
                "Hoàn tiền thành công: " + refundAmount + " VND");
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "Lỗi xử lý hoàn tiền với cổng thanh toán");
        }
        
        return "redirect:/admin/orders/" + id;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi hoàn tiền");
        return "redirect:/admin/orders/" + id;
    }
}
```

---

## Flow 8.6: Export Orders

**Controller**:
```java
@GetMapping("/export")
public void exportOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        HttpServletResponse response) throws IOException {
    
    List<Order> orders = orderService.getOrdersWithFilters(status, null, dateFrom, dateTo);
    
    // Set response headers
    response.setContentType("text/csv");
    response.setHeader("Content-Disposition", 
        "attachment; filename=orders_" + LocalDate.now() + ".csv");
    
    // Write CSV
    PrintWriter writer = response.getWriter();
    
    // Header
    writer.println("Order ID,User,Email,Status,Payment Status,Payment Method,Total Amount,Created At");
    
    // Data
    for (Order order : orders) {
        writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
            order.getOrderId(),
            order.getUser().getFullName(),
            order.getUser().getEmail(),
            order.getStatus(),
            order.getPaymentStatus(),
            order.getPaymentMethod(),
            order.getTotalAmount(),
            order.getCreatedAt()
        ));
    }
    
    writer.flush();
}
```

---

## Order Statistics & Analytics

**Dashboard Statistics**:
```java
@GetMapping("/statistics")
@ResponseBody
public Map<String, Object> getOrderStatistics(
        @RequestParam(required = false) String period) {
    
    LocalDateTime startDate;
    LocalDateTime endDate = LocalDateTime.now();
    
    switch (period != null ? period : "today") {
        case "week":
            startDate = endDate.minusWeeks(1);
            break;
        case "month":
            startDate = endDate.minusMonths(1);
            break;
        case "year":
            startDate = endDate.minusYears(1);
            break;
        default:
            startDate = endDate.toLocalDate().atStartOfDay();
    }
    
    List<Order> orders = orderService.getOrdersBetweenDates(startDate, endDate);
    
    Map<String, Object> stats = new HashMap<>();
    
    // Total revenue
    BigDecimal totalRevenue = orders.stream()
        .filter(o -> "PAID".equals(o.getPaymentStatus()))
        .map(Order::getTotalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    stats.put("totalRevenue", totalRevenue);
    
    // Order counts by status
    Map<String, Long> ordersByStatus = orders.stream()
        .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    stats.put("ordersByStatus", ordersByStatus);
    
    // Average order value
    BigDecimal avgOrderValue = totalRevenue.divide(
        BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP
    );
    stats.put("avgOrderValue", avgOrderValue);
    
    // Top selling books
    List<Map<String, Object>> topBooks = orderItemService.getTopSellingBooks(startDate, endDate, 10);
    stats.put("topSellingBooks", topBooks);
    
    // Revenue by payment method
    Map<String, BigDecimal> revenueByPaymentMethod = orders.stream()
        .filter(o -> "PAID".equals(o.getPaymentStatus()))
        .collect(Collectors.groupingBy(
            Order::getPaymentMethod,
            Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
        ));
    stats.put("revenueByPaymentMethod", revenueByPaymentMethod);
    
    return stats;
}
```

---

**Last Updated:** 06/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

