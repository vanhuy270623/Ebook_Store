# 🏦 FLOW 21: BANK TRANSFER PAYMENT (Thanh Toán Chuyển Khoản Ngân Hàng)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 21.1: Khởi Tạo Thanh Toán Chuyển Khoản](#flow-211-khởi-tạo-thanh-toán-chuyển-khoản)
3. [Flow 21.2: Hiển Thị QR Code & Thông Tin Ngân Hàng](#flow-212-hiển-thị-qr-code--thông-tin-ngân-hàng)
4. [Flow 21.3: User Xác Nhận Đã Chuyển Khoản](#flow-213-user-xác-nhận-đã-chuyển-khoản)
5. [Flow 21.4: Admin Duyệt Đơn Hàng](#flow-214-admin-duyệt-đơn-hàng)
6. [Flow 21.5: Admin Từ Chối Đơn Hàng](#flow-215-admin-từ-chối-đơn-hàng)
7. [Configuration & Setup](#configuration--setup)
8. [Security & Validation](#security--validation)
9. [Error Handling](#error-handling)

---

## Tổng Quan

### Payment Flow Overview
```
┌──────────────────┐
│ User Select      │
│ BANK_TRANSFER    │
│ at Checkout      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Create Order     │
│ Status: PENDING  │
│ Method: BANK_    │
│       TRANSFER   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Redirect to      │
│ /payment/bank-   │
│    transfer      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Generate QR Code │
│ via VietQR API   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Display Bank     │
│ Info & QR Code   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ User Scans QR    │
│ & Transfers      │
│ Money via App    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ User Clicks      │
│ "Tôi đã chuyển   │
│      khoản"      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Update Status to │
│ WAITING_APPROVAL │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Redirect to      │
│ Waiting Page     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Admin Reviews    │
│ Bank Statement   │
└────────┬─────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│ Approve│ │ Reject │
│ PAID   │ │ FAILED │
└────────┘ └────────┘
    │         │
    └────┬────┘
         │
         ▼
┌──────────────────┐
│ User Receives    │
│ Notification     │
│ (via email/UI)   │
└──────────────────┘
```

### Components
- **Controller**: `PaymentController.java`
- **Service**: `BankTransferService.java`, `BankTransferServiceImpl.java`, `OrderService.java`
- **DTO**: `BankTransferInfo.java`
- **Entity**: `Order.java` (PaymentMethod.BANK_TRANSFER, PaymentStatus)
- **Templates**: 
  - `bank-transfer.html` - Trang thanh toán
  - `waiting-approval.html` - Trang chờ duyệt

### Payment Statuses
```java
public enum PaymentStatus {
    PENDING,           // Đơn hàng mới tạo
    WAITING_APPROVAL,  // User đã xác nhận chuyển khoản, chờ admin duyệt
    PAID,             // Admin đã duyệt, thanh toán thành công
    COMPLETED,        // Đơn hàng hoàn tất (giao sách)
    FAILED,           // Admin từ chối
    CANCELLED         // User hủy
}
```

### URLs

**Payment Flow:**
- `POST /order/create?paymentMethod=BANK_TRANSFER` - Create order (from OrderController)
- `GET /payment/bank-transfer?orderId={orderId}` - Display bank info & QR code
- `POST /payment/bank-transfer/confirm?orderId={orderId}` - User confirms transfer

**Result Pages:**
- `GET /payment/bank-transfer/waiting?orderId={orderId}` - Waiting for admin approval
- `GET /payment/success?orderId={orderId}` - Payment approved by admin
- `GET /payment/error?orderId={orderId}` - Payment rejected by admin

**Admin Actions (in AdminOrderController):**
- `POST /admin/orders/{orderId}/approve` - Admin approves payment
- `POST /admin/orders/{orderId}/reject` - Admin rejects payment
- `GET /admin/orders` - List orders pending approval

**Important Architecture Notes:**
- ✅ QR Code generated using VietQR API
- ✅ Bank info stored in `application.properties`
- ✅ No automatic webhook verification (manual approval by admin)
- ✅ Order status flow: PENDING → WAITING_APPROVAL → PAID/FAILED

---

## Flow 21.1: Khởi Tạo Thanh Toán Chuyển Khoản

### Sequence Diagram
```
User → Browser → OrderController → OrderService → PaymentController → BankTransferService
  │       │            │                │               │                    │
  │  Select "Chuyển khoản" at checkout                                       │
  │       │            │                │               │                    │
  │ ──────────────POST /order/checkout─────────────────►│                    │
  │       │            │                │               │                    │
  │       │            │──createOrderFromCart()────────►│                    │
  │       │            │                │               │                    │
  │       │            │◄─── Order (PENDING) ──────────│                    │
  │       │            │                │               │                    │
  │       │            │──redirect /payment/bank-transfer?orderId=xxx───────►│
  │       │            │                │               │                    │
  │       │◄────── 302 Redirect ────────────────────────┘                    │
  │       │            │                │                                    │
  │ ──────────GET /payment/bank-transfer?orderId=xxx───────────────────────►│
  │       │            │                │                                    │
  │       │            │                │                   ┌─── generateQRCodeUrl()
  │       │            │                │                   │
  │       │            │                │                   ├─── generateTransferContent()
  │       │            │                │                   │
  │       │            │                │                   └─── getBankInfo()
  │       │            │                │                                    │
  │       │◄────────── Render bank-transfer.html ──────────────────────────┘
  │       │            (with QR code & bank info)
  │       │
  │◄──────┘
  │
Display QR Code & Bank Info
```

### Step 1: User Selects Payment Method at Checkout

**Frontend (checkout.html):**
```html
<!-- Phương thức thanh toán chuyển khoản -->
<input class="form-check-input" type="radio" 
       name="paymentMethod" 
       id="bankTransfer" 
       value="BANK_TRANSFER">
<label class="form-check-label" for="bankTransfer">
    <i class="fas fa-university"></i> Chuyển khoản ngân hàng
</label>
```

**Request:**
```http
POST /order/checkout HTTP/1.1
Content-Type: application/x-www-form-urlencoded

paymentMethod=BANK_TRANSFER
```

### Step 2: Create Order with BANK_TRANSFER Method

**File:** `OrderController.java`
```java
@PostMapping("/checkout")
public String checkout(
        @RequestParam String paymentMethod,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
    
    try {
        Cart cart = cartService.getCartByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        // Tạo order qua OrderService
        Order.PaymentMethod method = Order.PaymentMethod.valueOf(paymentMethod);
        Order savedOrder = orderService.createOrderFromCart(currentUser, cart, method);

        // Redirect theo phương thức thanh toán
        if ("BANK_TRANSFER".equals(paymentMethod)) {
            return "redirect:/payment/bank-transfer?orderId=" + savedOrder.getOrderId();
        }
        // ... other payment methods
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi tạo đơn hàng: " + e.getMessage());
        return "redirect:/order/checkout";
    }
}
```

**Order Created:**
```json
{
  "orderId": "ORD-20250125-ABC123",
  "userId": "USER001",
  "orderType": "BOOK",
  "totalAmount": 150000,
  "paymentStatus": "PENDING",
  "paymentMethod": "BANK_TRANSFER",
  "transactionId": null,
  "createdAt": "2025-01-25T10:30:00"
}
```

### Step 3: Redirect to Bank Transfer Page

**Redirect URL:**
```
/payment/bank-transfer?orderId=ORD-20250125-ABC123
```

---

## Flow 21.2: Hiển Thị QR Code & Thông Tin Ngân Hàng

### Sequence Diagram
```
Browser → PaymentController → BankTransferService → VietQR API
   │            │                      │                  │
   │ GET /payment/bank-transfer?orderId=xxx              │
   │            │                      │                  │
   │────────────►│                      │                  │
   │            │                      │                  │
   │            │──getOrderById()─────►OrderService       │
   │            │                      │                  │
   │            │──verify user owns order                 │
   │            │                      │                  │
   │            │──generateTransferContent()─────────────►│
   │            │  (returns "EBOOKSTORE ORD-xxx")         │
   │            │                      │                  │
   │            │──generateQRCodeUrl()────────────────────►│
   │            │  (bank_code, account, amount, content)  │
   │            │                      │                  │
   │            │◄─── QR Code URL ────────────────────────┘
   │            │  (https://img.vietqr.io/image/...)      │
   │            │                      │                  │
   │            │──getBankInfo()──────►│                  │
   │            │◄─── BankTransferInfo─┤                  │
   │            │                      │                  │
   │◄───────────┤ Render bank-transfer.html               │
   │            │ (QR + Bank Info)                        │
   │            │                      │                  │
Display QR & Info                     │                  │
```

### Step 1: Load Bank Transfer Page

**File:** `PaymentController.java`
```java
@GetMapping("/bank-transfer")
public String initiateBankTransfer(
        @RequestParam String orderId,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    try {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        // Lấy order
        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Bạn không có quyền thanh toán đơn hàng này");
        }

        // Cập nhật phương thức thanh toán
        order.setPaymentMethod(Order.PaymentMethod.BANK_TRANSFER);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        orderService.saveOrder(order);

        // Tạo nội dung chuyển khoản
        String transferContent = bankTransferService.generateTransferContent(orderId);

        // Tạo URL QR code
        String qrUrl = bankTransferService.generateQRCodeUrl(order, transferContent);

        // Lấy thông tin ngân hàng
        BankTransferInfo bankInfo = bankTransferService.getBankInfo();

        // Lấy danh sách order items
        List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

        // Thêm thông tin vào model
        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("qrCodeUrl", qrUrl);
        model.addAttribute("bankName", bankInfo.getBankName());
        model.addAttribute("bankAccountNumber", bankInfo.getAccountNumber());
        model.addAttribute("bankAccountName", bankInfo.getAccountName());
        model.addAttribute("bankBranch", bankInfo.getBranch());
        model.addAttribute("transferContent", transferContent);

        return "user/payment/bank-transfer";

    } catch (Exception e) {
        logger.error("Error initiating bank transfer for order {}: {}", orderId, e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán: " + e.getMessage());
        return "redirect:/order/checkout";
    }
}
```

### Step 2: Generate QR Code

**File:** `BankTransferServiceImpl.java`
```java
@Service
public class BankTransferServiceImpl implements BankTransferService {

    @Value("${bank.name:TPbank}")
    private String bankName;

    @Value("${bank.account_number:79992706999}")
    private String bankAccountNumber;

    @Value("${bank.account_name:CONG TY EBOOK STORE}")
    private String bankAccountName;

    @Value("${bank.branch:Chi nhanh TP.HCM}")
    private String bankBranch;

    @Value("${bank.code:TPbank}")
    private String bankCode;

    @Value("${bank.qr_template:https://img.vietqr.io/image/{bank_code}-{account_number}-{template}.png?amount={amount}&addInfo={content}&accountName={account_name}}")
    private String qrTemplate;

    @Override
    public String generateQRCodeUrl(Order order, String transferContent) {
        try {
            String url = qrTemplate
                    .replace("{bank_code}", bankCode)
                    .replace("{account_number}", bankAccountNumber)
                    .replace("{template}", "compact2")
                    .replace("{amount}", String.valueOf(order.getTotalAmount().longValue()))
                    .replace("{content}", URLEncoder.encode(transferContent, StandardCharsets.UTF_8))
                    .replace("{account_name}", URLEncoder.encode(bankAccountName, StandardCharsets.UTF_8));

            logger.info("Generated QR URL: {}", url);
            return url;
            
        } catch (Exception e) {
            logger.error("Error generating QR code URL: {}", e.getMessage(), e);
            return generateFallbackQRCode(order);
        }
    }

    @Override
    public String generateTransferContent(String orderId) {
        return "EBOOKSTORE " + orderId;
    }

    @Override
    public BankTransferInfo getBankInfo() {
        return new BankTransferInfo(
                bankName,
                bankAccountNumber,
                bankAccountName,
                bankBranch,
                bankCode
        );
    }
}
```

### Step 3: Display Bank Transfer Page

**File:** `bank-transfer.html`

**QR Code Section:**
```html
<div class="qr-code-container">
    <h5 class="mb-3">
        <i class="fas fa-mobile-alt me-2"></i>Quét mã QR để thanh toán
    </h5>

    <!-- QR Code Image -->
    <div th:if="${qrCodeUrl != null and !qrCodeUrl.isEmpty()}">
        <img th:src="${qrCodeUrl}"
             alt="QR Code"
             class="qr-code-img"
             id="qrCodeImage">
    </div>

    <p class="text-muted mt-3 small">
        <i class="fas fa-info-circle me-1"></i>
        Sử dụng ứng dụng ngân hàng để quét mã
    </p>
</div>
```

**Bank Information Section:**
```html
<div class="bank-info">
    <h5 class="mb-3">
        <i class="fas fa-university me-2"></i>Thông tin chuyển khoản
    </h5>

    <div class="info-row">
        <span class="text-muted">Ngân hàng:</span>
        <strong>
            <span th:text="${bankName}">TPbank</span>
            <i class="fas fa-copy copy-btn" 
               onclick="copyToClipboard(this, '[[${bankName}]]')" 
               title="Sao chép"></i>
        </strong>
    </div>

    <div class="info-row">
        <span class="text-muted">Số tài khoản:</span>
        <strong>
            <span th:text="${bankAccountNumber}">79992706999</span>
            <i class="fas fa-copy copy-btn" 
               onclick="copyToClipboard(this, '[[${bankAccountNumber}]]')" 
               title="Sao chép"></i>
        </strong>
    </div>

    <div class="info-row">
        <span class="text-muted">Chủ tài khoản:</span>
        <strong>
            <span th:text="${bankAccountName}">CONG TY EBOOK STORE</span>
        </strong>
    </div>

    <div class="info-row">
        <span class="text-muted">Số tiền:</span>
        <strong class="text-danger">
            <span th:text="${#numbers.formatDecimal(order.totalAmount, 0, 'COMMA', 0, 'POINT')} + ' đ'">150,000 đ</span>
            <i class="fas fa-copy copy-btn" 
               onclick="copyToClipboard(this, '[[${order.totalAmount}]]')" 
               title="Sao chép"></i>
        </strong>
    </div>

    <div class="info-row">
        <span class="text-muted">Nội dung CK:</span>
        <strong>
            <span th:text="${transferContent}">EBOOKSTORE ORD001</span>
            <i class="fas fa-copy copy-btn" 
               onclick="copyToClipboard(this, '[[${transferContent}]]')" 
               title="Sao chép"></i>
        </strong>
    </div>
</div>
```

**Confirm Button:**
```html
<div class="d-grid gap-2 mt-4">
    <button type="button"
            class="btn btn-success btn-lg"
            id="confirmBtn"
            onclick="confirmTransfer()">
        <i class="fas fa-check-circle me-2"></i>Tôi đã chuyển khoản
    </button>
</div>
```

### Example Generated QR Code URL
```
https://img.vietqr.io/image/TPbank-79992706999-compact2.png?
  amount=150000&
  addInfo=EBOOKSTORE%20ORD-20250125-ABC123&
  accountName=CONG%20TY%20EBOOK%20STORE
```

**QR Code Display:**
- Width: 300x300 pixels
- Format: PNG
- Service: VietQR API
- Auto-filled: Bank code, account number, amount, transfer content

---

## Flow 21.3: User Xác Nhận Đã Chuyển Khoản

### Sequence Diagram
```
User → Browser → PaymentController → OrderService → Database
  │       │             │                  │              │
  │ Click "Tôi đã chuyển khoản"           │              │
  │       │             │                  │              │
  │───────┼────POST /payment/bank-transfer/confirm───────►│
  │       │             │                  │              │
  │       │             │──getOrderById()─►│              │
  │       │             │                  │              │
  │       │             │──verify ownership│              │
  │       │             │                  │              │
  │       │             │──check status == PENDING        │
  │       │             │                  │              │
  │       │             │──setPaymentStatus(WAITING_APPROVAL)
  │       │             │                  │              │
  │       │             │──saveOrder()─────┼─────────────►│
  │       │             │                  │              │
  │       │◄────JSON response (success)────┘              │
  │       │  {redirectUrl: "/payment/bank-transfer/waiting"}
  │       │             │                  │              │
  │◄──────┤ JavaScript redirect                          │
  │       │             │                  │              │
  │───────┼────GET /payment/bank-transfer/waiting?orderId=xxx
  │       │             │                  │              │
  │       │◄────Render waiting-approval.html              │
  │◄──────┤             │                  │              │
```

### Step 1: User Clicks Confirm Button

**JavaScript (bank-transfer.html):**
```javascript
function confirmTransfer() {
    const orderId = '[[${order.orderId}]]';
    
    // Hiển thị loading
    Swal.fire({
        title: 'Đang xử lý...',
        text: 'Vui lòng đợi',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    // Gửi request xác nhận
    fetch('/payment/bank-transfer/confirm', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'orderId=' + encodeURIComponent(orderId)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            Swal.fire({
                icon: 'success',
                title: 'Thành công!',
                text: data.message,
                confirmButtonText: 'OK'
            }).then(() => {
                window.location.href = data.redirectUrl;
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Lỗi!',
                text: data.message
            });
        }
    })
    .catch(error => {
        Swal.fire({
            icon: 'error',
            title: 'Lỗi!',
            text: 'Có lỗi xảy ra: ' + error.message
        });
    });
}
```

### Step 2: Update Order Status to WAITING_APPROVAL

**File:** `PaymentController.java`
```java
@PostMapping("/bank-transfer/confirm")
@ResponseBody
public Map<String, Object> confirmBankTransfer(@RequestParam String orderId) {
    
    Map<String, Object> response = new HashMap<>();

    try {
        logger.info("=== Confirm Bank Transfer ===");
        logger.info("Received orderId: {}", orderId);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return response;
        }

        // Lấy order
        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này");
        }

        // Kiểm tra trạng thái hiện tại
        if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
            response.put("success", false);
            response.put("message", "Đơn hàng đã được xác nhận trước đó");
            return response;
        }

        // Cập nhật trạng thái thành WAITING_APPROVAL
        order.setPaymentStatus(Order.PaymentStatus.WAITING_APPROVAL);
        orderService.saveOrder(order);

        logger.info("Order {} updated to WAITING_APPROVAL", orderId);

        response.put("success", true);
        response.put("message", "Đã xác nhận chuyển khoản. Đơn hàng đang chờ admin duyệt.");
        response.put("redirectUrl", "/payment/bank-transfer/waiting?orderId=" + orderId);

    } catch (Exception e) {
        logger.error("ERROR in confirmBankTransfer: {}", e.getMessage(), e);
        response.put("success", false);
        response.put("message", "Lỗi: " + e.getMessage());
    }

    return response;
}
```

**Response:**
```json
{
  "success": true,
  "message": "Đã xác nhận chuyển khoản. Đơn hàng đang chờ admin duyệt.",
  "redirectUrl": "/payment/bank-transfer/waiting?orderId=ORD-20250125-ABC123"
}
```

### Step 3: Redirect to Waiting Page

**File:** `PaymentController.java`
```java
@GetMapping("/bank-transfer/waiting")
public String bankTransferWaiting(
        @RequestParam String orderId,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    try {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }

        List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);

        return "user/payment/waiting-approval";

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        return "redirect:/order/history";
    }
}
```

**File:** `waiting-approval.html`
```html
<div class="container my-5">
    <div class="text-center">
        <i class="fas fa-clock fa-5x text-warning mb-3"></i>
        <h2>Đơn hàng đang chờ xác nhận</h2>
        
        <div class="alert alert-info mt-4">
            <i class="fas fa-info-circle me-2"></i>
            Đơn hàng của bạn đang được admin kiểm tra và xác nhận.
            Quá trình này thường mất <strong>1-24 giờ</strong>.
        </div>

        <div class="card mt-4">
            <div class="card-body">
                <p><strong>Mã đơn hàng:</strong> <span th:text="${order.orderId}">ORD001</span></p>
                <p><strong>Tổng tiền:</strong> 
                   <span class="text-danger" th:text="${#numbers.formatDecimal(order.totalAmount, 0, 'COMMA', 0, 'POINT')} + ' đ'">150,000 đ</span>
                </p>
                <p><strong>Trạng thái:</strong> 
                   <span class="badge bg-warning">Chờ duyệt</span>
                </p>
            </div>
        </div>

        <div class="mt-4">
            <a href="/user/orders" class="btn btn-primary">
                <i class="fas fa-list me-2"></i>Xem danh sách đơn hàng
            </a>
        </div>
    </div>
</div>
```

---

## Flow 21.4: Admin Duyệt Đơn Hàng

### Sequence Diagram
```
Admin → Browser → AdminOrderController → OrderService → Database → (Email Service)
  │        │              │                    │             │              │
  │ View Order List                            │             │              │
  │        │              │                    │             │              │
  │───────►│──GET /admin/orders?status=WAITING_APPROVAL     │              │
  │        │              │                    │             │              │
  │        │◄─────Render list.html (filter by status)       │              │
  │◄───────┤              │                    │             │              │
  │        │              │                    │             │              │
  │ Click "Duyệt" button                       │             │              │
  │        │              │                    │             │              │
  │───────►│──POST /admin/orders/approve/{id}──►│            │              │
  │        │              │                    │             │              │
  │        │              │──getOrderById()────►│            │              │
  │        │              │                    │             │              │
  │        │              │──check status == WAITING_APPROVAL              │
  │        │              │                    │             │              │
  │        │              │──setPaymentStatus(PAID)          │              │
  │        │              │                    │             │              │
  │        │              │──saveOrder()───────┼────────────►│              │
  │        │              │                    │             │              │
  │        │              │──(TODO: send email)──────────────┼─────────────►│
  │        │              │                    │             │              │
  │        │◄────JSON response (success)───────┘             │              │
  │◄───────┤              │                                  │              │
  │        │              │                                  │              │
  │ Show success message & reload table                     │              │
```

### Step 1: Admin Views Pending Orders

**URL:**
```
GET /admin/orders?status=WAITING_APPROVAL
```

**File:** `AdminOrderController.java`
```java
@GetMapping("")
public String listOrders(
        @RequestParam(required = false, defaultValue = "ALL") String status,
        @RequestParam(required = false, defaultValue = "ALL") String orderType,
        Model model) {
    
    List<Order> orders;
    
    if ("ALL".equals(status)) {
        orders = orderService.getAllOrders();
    } else {
        Order.PaymentStatus paymentStatus = Order.PaymentStatus.valueOf(status);
        orders = orderService.getOrdersByPaymentStatus(paymentStatus);
    }
    
    // Filter by order type if needed
    if (!"ALL".equals(orderType)) {
        Order.OrderType type = Order.OrderType.valueOf(orderType);
        orders = orders.stream()
                .filter(o -> o.getOrderType() == type)
                .collect(Collectors.toList());
    }
    
    model.addAttribute("orders", orders);
    model.addAttribute("paymentStatuses", Order.PaymentStatus.values());
    model.addAttribute("orderTypes", Order.OrderType.values());
    model.addAttribute("selectedStatus", status);
    model.addAttribute("selectedOrderType", orderType);
    
    return "admin/orders/list";
}
```

**File:** `list.html` (Order List with Filter)**
```html
<form method="get" action="/admin/orders" class="form-inline">
    <div class="form-group">
        <label>Trạng thái:</label>
        <select name="status" class="form-control">
            <option value="ALL">Tất cả</option>
            <option th:each="st : ${paymentStatuses}"
                    th:value="${st}"
                    th:text="${st.name()}"
                    th:selected="${st.name() == selectedStatus}">
            </option>
        </select>
    </div>
    <button type="submit" class="btn btn-primary">
        <i class="fa fa-filter"></i> Lọc
    </button>
</form>

<!-- Order Table -->
<table class="table table-hover" id="ordersTable">
    <thead>
        <tr>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>Tổng tiền</th>
            <th>Phương thức</th>
            <th>Trạng thái</th>
            <th>Ngày tạo</th>
            <th>Hành động</th>
        </tr>
    </thead>
    <tbody>
        <tr th:each="order : ${orders}">
            <td th:text="${order.orderId}">ORD001</td>
            <td th:text="${order.user.username}">user01</td>
            <td th:text="${#numbers.formatDecimal(order.totalAmount, 0, 'COMMA', 0, 'POINT')} + ' đ'">150,000 đ</td>
            <td>
                <span th:if="${order.paymentMethod == T(stu.datn.ebook_store.entity.Order.PaymentMethod).BANK_TRANSFER}"
                      class="badge bg-info">Chuyển khoản</span>
                <span th:if="${order.paymentMethod == T(stu.datn.ebook_store.entity.Order.PaymentMethod).VNPAY}"
                      class="badge bg-primary">VNPay</span>
            </td>
            <td>
                <span th:if="${order.paymentStatus == T(stu.datn.ebook_store.entity.Order.PaymentStatus).WAITING_APPROVAL}"
                      class="badge bg-warning">Chờ duyệt</span>
                <span th:if="${order.paymentStatus == T(stu.datn.ebook_store.entity.Order.PaymentStatus).PAID}"
                      class="badge bg-success">Đã thanh toán</span>
                <span th:if="${order.paymentStatus == T(stu.datn.ebook_store.entity.Order.PaymentStatus).FAILED}"
                      class="badge bg-danger">Thất bại</span>
            </td>
            <td th:text="${#temporals.format(order.createdAt, 'dd/MM/yyyy HH:mm')}">25/01/2025 10:30</td>
            <td>
                <a th:href="@{/admin/orders/view/{id}(id=${order.orderId})}" 
                   class="btn btn-sm btn-info">
                    <i class="fa fa-eye"></i> Chi tiết
                </a>
                
                <!-- Approve Button (only for WAITING_APPROVAL) -->
                <button th:if="${order.paymentStatus == T(stu.datn.ebook_store.entity.Order.PaymentStatus).WAITING_APPROVAL}"
                        class="btn btn-sm btn-success"
                        onclick="approveOrder('[[${order.orderId}]]')">
                    <i class="fa fa-check"></i> Duyệt
                </button>
                
                <!-- Reject Button (only for WAITING_APPROVAL) -->
                <button th:if="${order.paymentStatus == T(stu.datn.ebook_store.entity.Order.PaymentStatus).WAITING_APPROVAL}"
                        class="btn btn-sm btn-danger"
                        onclick="rejectOrder('[[${order.orderId}]]')">
                    <i class="fa fa-times"></i> Từ chối
                </button>
            </td>
        </tr>
    </tbody>
</table>
```

### Step 2: Admin Clicks "Duyệt" Button

**JavaScript (list.html):**
```javascript
function approveOrder(orderId) {
    Swal.fire({
        title: 'Xác nhận duyệt đơn hàng?',
        text: 'Đơn hàng sẽ được đánh dấu là đã thanh toán',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Duyệt',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            // Show loading
            Swal.fire({
                title: 'Đang xử lý...',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Send approve request
            fetch('/admin/orders/approve/' + orderId, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Thành công!',
                        text: data.message
                    }).then(() => {
                        location.reload();
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi!',
                        text: data.message
                    });
                }
            })
            .catch(error => {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi!',
                    text: 'Có lỗi xảy ra: ' + error.message
                });
            });
        }
    });
}
```

### Step 3: Update Order Status to PAID

**File:** `AdminOrderController.java`
```java
@PostMapping("/approve/{id}")
@ResponseBody
public ResponseEntity<Map<String, Object>> approveOrder(
        @PathVariable String id,
        Authentication authentication) {
    
    Map<String, Object> response = new HashMap<>();

    try {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng");
            return ResponseEntity.ok(response);
        }

        Order order = orderOpt.get();

        // Kiểm tra trạng thái hiện tại
        if (order.getPaymentStatus() != Order.PaymentStatus.WAITING_APPROVAL) {
            response.put("success", false);
            response.put("message", "Đơn hàng không ở trạng thái chờ duyệt");
            return ResponseEntity.ok(response);
        }

        // Cập nhật trạng thái thành PAID
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        orderService.saveOrder(order);

        // TODO: Gửi email thông báo cho khách hàng (nếu có email service)
        // emailService.sendPaymentApprovedEmail(order);

        response.put("success", true);
        response.put("message", "Đã duyệt đơn hàng thành công");
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        return ResponseEntity.ok(response);
    }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Đã duyệt đơn hàng thành công"
}
```

---

## Flow 21.5: Admin Từ Chối Đơn Hàng

### Sequence Diagram
```
Admin → Browser → AdminOrderController → OrderService → Database
  │        │              │                    │             │
  │ Click "Từ chối" button                     │             │
  │        │              │                    │             │
  │───────►│──Show confirmation dialog          │             │
  │        │  (with reason input)               │             │
  │◄───────┤              │                    │             │
  │        │              │                    │             │
  │ Enter reason & confirm                     │             │
  │        │              │                    │             │
  │───────►│──POST /admin/orders/reject/{id}───►│            │
  │        │  (with reason parameter)           │            │
  │        │              │                    │             │
  │        │              │──getOrderById()────►│            │
  │        │              │                    │             │
  │        │              │──check status == WAITING_APPROVAL
  │        │              │                    │             │
  │        │              │──setPaymentStatus(FAILED)        │
  │        │              │                    │             │
  │        │              │──saveOrder()───────┼────────────►│
  │        │              │                    │             │
  │        │◄────JSON response (success)───────┘             │
  │◄───────┤              │                                  │
  │        │              │                                  │
  │ Show success & reload                                   │
```

### Step 1: Admin Clicks "Từ chối" Button

**JavaScript (list.html):**
```javascript
function rejectOrder(orderId) {
    Swal.fire({
        title: 'Từ chối đơn hàng',
        text: 'Vui lòng nhập lý do từ chối:',
        input: 'textarea',
        inputPlaceholder: 'Nhập lý do từ chối...',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Từ chối',
        cancelButtonText: 'Hủy',
        inputValidator: (value) => {
            if (!value) {
                return 'Vui lòng nhập lý do từ chối!';
            }
        }
    }).then((result) => {
        if (result.isConfirmed) {
            const reason = result.value;

            // Show loading
            Swal.fire({
                title: 'Đang xử lý...',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Send reject request
            fetch('/admin/orders/reject/' + orderId, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: 'reason=' + encodeURIComponent(reason)
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Thành công!',
                        text: data.message
                    }).then(() => {
                        location.reload();
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi!',
                        text: data.message
                    });
                }
            })
            .catch(error => {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi!',
                    text: 'Có lỗi xảy ra: ' + error.message
                });
            });
        }
    });
}
```

### Step 2: Update Order Status to FAILED

**File:** `AdminOrderController.java`
```java
@PostMapping("/reject/{id}")
@ResponseBody
public ResponseEntity<Map<String, Object>> rejectOrder(
        @PathVariable String id,
        @RequestParam(required = false) String reason,
        Authentication authentication) {
    
    Map<String, Object> response = new HashMap<>();

    try {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng");
            return ResponseEntity.ok(response);
        }

        Order order = orderOpt.get();

        // Kiểm tra trạng thái hiện tại
        if (order.getPaymentStatus() != Order.PaymentStatus.WAITING_APPROVAL) {
            response.put("success", false);
            response.put("message", "Đơn hàng không ở trạng thái chờ duyệt");
            return ResponseEntity.ok(response);
        }

        // Cập nhật trạng thái thành FAILED
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        orderService.saveOrder(order);

        // TODO: Gửi email thông báo cho khách hàng (kèm lý do)
        // emailService.sendPaymentRejectedEmail(order, reason);

        response.put("success", true);
        response.put("message", "Đã từ chối đơn hàng");
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        return ResponseEntity.ok(response);
    }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Đã từ chối đơn hàng"
}
```

---

## Configuration & Setup

### 1. Application Properties

**File:** `application.properties`
```properties
# Bank Transfer QR Payment Configuration
bank.name=TPbank
bank.code=TPbank
bank.account_number=79992706999
bank.account_name=CONG TY EBOOK STORE
bank.branch=Chi nhanh TP.HCM
bank.qr_template=https://img.vietqr.io/image/{bank_code}-{account_number}-{template}.png?amount={amount}&addInfo={content}&accountName={account_name}
```

### 2. Database Schema

**Order Table:**
```sql
CREATE TABLE orders (
    order_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    subscription_id VARCHAR(50),
    order_type ENUM('BOOK', 'SUBSCRIPTION') NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    payment_status ENUM('PENDING', 'WAITING_APPROVAL', 'COMPLETED', 'PAID', 'FAILED', 'CANCELLED'),
    payment_method ENUM('VNPAY', 'BANK_TRANSFER', 'CREDIT_CARD'),
    transaction_id VARCHAR(255),
    start_date DATETIME,
    end_date DATETIME,
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(subscription_id)
);
```

### 3. VietQR API Configuration

**Supported Banks:**
- TPbank (Code: `TPbank`)
- Vietcombank (Code: `VCB`)
- Techcombank (Code: `TCB`)
- VietinBank (Code: `CTG`)
- BIDV (Code: `BIDV`)
- Agribank (Code: `ABB`)
- MBBank (Code: `MB`)
- ACB (Code: `ACB`)
- ... and more

**QR Template Parameters:**
- `{bank_code}`: Mã ngân hàng
- `{account_number}`: Số tài khoản
- `{template}`: Loại template QR (compact, compact2, print, qr_only)
- `{amount}`: Số tiền cần chuyển
- `{content}`: Nội dung chuyển khoản
- `{account_name}`: Tên chủ tài khoản

**Example URL:**
```
https://img.vietqr.io/image/TPbank-79992706999-compact2.png?
  amount=150000&
  addInfo=EBOOKSTORE ORD-20250125-ABC123&
  accountName=CONG TY EBOOK STORE
```

### 4. Payment Status Flow

```
PENDING → WAITING_APPROVAL → PAID
   │              │             │
   │              │             └─► COMPLETED (after book delivery)
   │              │
   │              └─────────────► FAILED (admin rejects)
   │
   └──────────────────────────► CANCELLED (user cancels)
```

---

## Security & Validation

### 1. User Authentication & Authorization

**Kiểm tra đăng nhập:**
```java
User currentUser = getCurrentUser();
if (currentUser == null) {
    redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
    return "redirect:/auth/login";
}
```

**Kiểm tra quyền sở hữu đơn hàng:**
```java
if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
    throw new RuntimeException("Bạn không có quyền thanh toán đơn hàng này");
}
```

### 2. Order Status Validation

**Kiểm tra trạng thái trước khi xác nhận:**
```java
if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
    response.put("success", false);
    response.put("message", "Đơn hàng đã được xác nhận trước đó");
    return response;
}
```

**Kiểm tra trạng thái trước khi duyệt:**
```java
if (order.getPaymentStatus() != Order.PaymentStatus.WAITING_APPROVAL) {
    response.put("success", false);
    response.put("message", "Đơn hàng không ở trạng thái chờ duyệt");
    return response;
}
```

### 3. Input Validation

**Order ID validation:**
```java
Order order = orderService.getOrderById(orderId)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
```

**URL Encoding for QR Code:**
```java
String url = qrTemplate
        .replace("{content}", URLEncoder.encode(transferContent, StandardCharsets.UTF_8))
        .replace("{account_name}", URLEncoder.encode(bankAccountName, StandardCharsets.UTF_8));
```

### 4. Admin Role Check

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/approve/{id}")
public ResponseEntity<Map<String, Object>> approveOrder(@PathVariable String id) {
    // ...
}
```

---

## Error Handling

### 1. QR Code Generation Failures

**Fallback QR Code:**
```java
private String generateFallbackQRCode(Order order) {
    try {
        String data = String.format(
                "Thanh toan don hang: %s\nSo tien: %s VND\nTK: %s - %s",
                order.orderId,
                order.getTotalAmount(),
                bankAccountNumber,
                bankName
        );

        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
               URLEncoder.encode(data, StandardCharsets.UTF_8);
    } catch (Exception e) {
        logger.error("Error generating fallback QR code", e);
        return "";
    }
}
```

### 2. Order Not Found

**Response:**
```json
{
  "success": false,
  "message": "Không tìm thấy đơn hàng với ID: ORD-xxx"
}
```

### 3. Unauthorized Access

**Response:**
```json
{
  "success": false,
  "message": "Bạn không có quyền thanh toán đơn hàng này"
}
```

### 4. Invalid Status Transition

**Response:**
```json
{
  "success": false,
  "message": "Đơn hàng đã được xác nhận trước đó"
}
```

### 5. Database Errors

```java
try {
    order.setPaymentStatus(Order.PaymentStatus.WAITING_APPROVAL);
    orderService.saveOrder(order);
} catch (Exception e) {
    logger.error("ERROR in confirmBankTransfer: {}", e.getMessage(), e);
    response.put("success", false);
    response.put("message", "Lỗi: " + e.getMessage());
}
```

---

## Testing Scenarios

### 1. Happy Path - Complete Flow

```
1. User adds books to cart
2. User goes to checkout
3. User selects "Chuyển khoản ngân hàng"
4. System creates order (PENDING)
5. System redirects to /payment/bank-transfer
6. System generates QR code
7. User scans QR and transfers money
8. User clicks "Tôi đã chuyển khoản"
9. System updates status to WAITING_APPROVAL
10. Admin reviews and approves
11. System updates status to PAID
12. User can download books
```

### 2. User Cancels Before Transfer

```
1. User selects bank transfer
2. Order created (PENDING)
3. User closes browser
4. Order remains PENDING (can be cancelled later)
```

### 3. Admin Rejects Transfer

```
1. User confirms transfer (WAITING_APPROVAL)
2. Admin reviews bank statement
3. Admin finds no matching transfer
4. Admin clicks "Từ chối" with reason
5. System updates status to FAILED
6. User notified
```

### 4. Duplicate Confirmation

```
1. User clicks "Tôi đã chuyển khoản"
2. Status changes to WAITING_APPROVAL
3. User clicks button again
4. System returns error: "Đơn hàng đã được xác nhận trước đó"
```

---

## TODO & Future Enhancements

### 1. Automatic Payment Verification
- [ ] Integrate with bank API để tự động kiểm tra giao dịch
- [ ] Tự động duyệt đơn khi phát hiện chuyển khoản khớp
- [ ] Webhook notification từ ngân hàng

### 2. Email Notifications
- [ ] Gửi email khi user xác nhận chuyển khoản
- [ ] Gửi email khi admin duyệt đơn
- [ ] Gửi email khi admin từ chối (kèm lý do)

### 3. SMS Notifications
- [ ] SMS thông báo khi đơn được duyệt
- [ ] SMS nhắc nhở admin có đơn chờ duyệt

### 4. Payment Reminder
- [ ] Tự động nhắc user nếu đơn PENDING quá 24h
- [ ] Tự động hủy đơn nếu không thanh toán sau 48h

### 5. Bank Statement Upload
- [ ] Cho phép user upload ảnh chụp màn hình chuyển khoản
- [ ] Admin xem ảnh khi duyệt đơn

### 6. Multiple Bank Support
- [ ] Cho phép cấu hình nhiều tài khoản ngân hàng
- [ ] User chọn ngân hàng khi thanh toán

---

## Related Flows

- **FLOW 03**: Shopping Cart & Checkout - Tạo đơn hàng
- **FLOW 05**: VNPay Payment - Phương thức thanh toán khác
- **FLOW 08**: Admin Order Management - Quản lý đơn hàng

---

## References

- VietQR API: https://www.vietqr.io/
- QR Code Generator: https://api.qrserver.com/
- Bank Codes: https://www.vietqr.io/danh-sach-ma-ngan-hang-tren-vietqr

---

**Last Updated:** 20/12/2025  
**Version:** 1.0  
**Author:** Development Team

