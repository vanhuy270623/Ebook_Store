# 💳 FLOW 05: PAYMENT PROCESSING - VNPAY (Xử Lý Thanh Toán VNPay)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 5.1: Khởi Tạo Thanh Toán VNPay](#flow-51-khởi-tạo-thanh-toán-vnpay)
3. [Flow 5.2: Xử Lý Callback VNPay](#flow-52-xử-lý-callback-vnpay)
4. [Flow 5.3: Subscription Payment Activation](#flow-53-subscription-payment-activation)
5. [Security & Validation](#security--validation)
6. [Debugging Endpoints](#debugging-endpoints)

---

## Tổng Quan

### Payment Flow Overview
```
┌──────────────┐
│ User Select  │
│ VNPay Method │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Create Payment│
│   Request    │
│(Controller)  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│VNPayService  │
│createPayment │
│    URL()     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Generate HMAC │
│SHA512 Hash   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Redirect to  │
│VNPay Gateway │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│User Complete │
│   Payment    │
│  at VNPay    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│VNPay Callback│
│   /return    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│VNPayService  │
│validateCall  │
│   back()     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Update Order  │
│Status: PAID/ │
│   FAILED     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Activate Sub  │
│(if applicable)│
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Show Success/│
│Error Page    │
└──────────────┘
```

### Components
- **Controller**: `PaymentController.java` (598 lines)
- **Service**: `VNPayService.java`, `VNPayServiceImpl.java` (225 lines)
- **Service**: `OrderService.java`, `OrderItemService.java`, `SubscriptionService.java`
- **Entity**: `Order.java`, `OrderItem.java`, `Subscription.java`
- **Config**: `application.properties` (VNPay credentials)

### VNPay Configuration (from application.properties)
```properties
# VNPay Settings (Sandbox)
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.tmn_code=9CB3LH80
vnpay.hash_secret=UDN2E28HUBUULOWK5KAGTA3GVU523HPK
vnpay.return_url=http://localhost:2706/payment/vnpay/return
```

### URLs
**Payment Initiation:**
- `GET /payment/vnpay?orderId={orderId}` - Khởi tạo thanh toán VNPay (redirect to VNPay gateway)

**Payment Callback:**
- `GET /payment/vnpay/return?vnp_*` - Callback từ VNPay sau khi user thanh toán
  - Query params: `vnp_Amount`, `vnp_BankCode`, `vnp_ResponseCode`, `vnp_TransactionNo`, `vnp_SecureHash`, etc.

**Result Pages:**
- `GET /payment/success?orderId={orderId}` - Trang thanh toán thành công
- `GET /payment/error?orderId={orderId}` - Trang thanh toán thất bại

**Alternative Payment Methods (cùng PaymentController):**
- `GET /payment/bank-transfer?orderId={orderId}` - Thanh toán chuyển khoản
- `POST /payment/bank-transfer/confirm` - Xác nhận đã chuyển khoản

### Key Features
- ✅ **Business Logic in Service Layer**: VNPayService handles all VNPay integration
- ✅ **HMAC SHA512 Signature**: Secure hash generation và validation
- ✅ **15-minute Payment Timeout**: Auto-expire payment requests
- ✅ **Subscription Auto-Activation**: Tự động kích hoạt subscription sau payment
- ✅ **Transaction Tracking**: Lưu `transaction_id` từ VNPay
- ✅ **Response Code Handling**: Xử lý 40+ VNPay response codes

---

## Flow 5.1: Khởi Tạo Thanh Toán VNPay

### Sequence Diagram
```
User → Browser → PaymentController → VNPayService → VNPay Gateway
  │       │              │                 │              │
  │ Click "Thanh toán VNPay"                             │
  │───────────────────────►│                              │
  │       │                │ getOrderById()               │
  │       │                │ validateOrder()              │
  │       │                │ createPaymentUrl()           │
  │       │                ├────────────────►│            │
  │       │                │                 │ buildParams()
  │       │                │                 │ sortParams()
  │       │                │                 │ hmacSHA512()
  │       │                │                 │ buildQuery()
  │       │                │◄────────────────┤            │
  │       │                │ paymentUrl                   │
  │◄───────────────────────┤ (redirect)                  │
  │       │                                               │
  │ Redirect to VNPay Gateway                            │
  │──────────────────────────────────────────────────────►│
  │       │                                               │
  │ User enters card & confirms                          │
  │◄──────────────────────────────────────────────────────┤
```

### Implementation Details

**Controller**: `PaymentController.initiateVNPayPayment()`

```java
@GetMapping("/vnpay")
public String initiateVNPayPayment(
        @RequestParam String orderId,
        HttpServletRequest request,
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

        // ⚡ Tạo payment URL qua VNPayService
        String paymentUrl = vnPayService.createPaymentUrl(order, request);

        return "redirect:" + paymentUrl;

    } catch (Exception e) {
        logger.error("Error initiating VNPay payment for order {}: {}", 
                    orderId, e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi khởi tạo thanh toán: " + e.getMessage());
        return "redirect:/order/checkout";
    }
}
```

**Service**: `VNPayServiceImpl.createPaymentUrl()`

```java
@Override
public String createPaymentUrl(Order order, HttpServletRequest request) {
    Map<String, String> vnpParams = new HashMap<>();

    // 1. Thông tin cơ bản
    vnpParams.put("vnp_Version", "2.1.0");
    vnpParams.put("vnp_Command", "pay");
    vnpParams.put("vnp_TmnCode", vnpayTmnCode); // "9CB3LH80"

    // 2. Số tiền (VNPay yêu cầu nhân 100)
    long amount = order.getTotalAmount()
            .multiply(new BigDecimal(100))
            .longValue();
    vnpParams.put("vnp_Amount", String.valueOf(amount));

    // 3. Thông tin đơn hàng
    vnpParams.put("vnp_CurrCode", "VND");
    vnpParams.put("vnp_TxnRef", order.getOrderId()); // Order ID là transaction reference
    vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderId());
    vnpParams.put("vnp_OrderType", "other"); // Bookstore = "other"
    vnpParams.put("vnp_Locale", "vn"); // Vietnamese
    vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
    vnpParams.put("vnp_IpAddr", getClientIp(request));

    // 4. Thời gian
    Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    String vnpCreateDate = formatter.format(cld.getTime());
    vnpParams.put("vnp_CreateDate", vnpCreateDate);

    // 5. Thời gian hết hạn (15 phút)
    cld.add(Calendar.MINUTE, 15);
    String vnpExpireDate = formatter.format(cld.getTime());
    vnpParams.put("vnp_ExpireDate", vnpExpireDate);

    // 6. Build query string và hash
    List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
    Collections.sort(fieldNames); // ⚡ QUAN TRỌNG: Sort alphabetically

    StringBuilder hashData = new StringBuilder();
    StringBuilder query = new StringBuilder();

    Iterator<String> itr = fieldNames.iterator();
    while (itr.hasNext()) {
        String fieldName = itr.next();
        String fieldValue = vnpParams.get(fieldName);

        if ((fieldValue != null) && (!fieldValue.isEmpty())) {
            // Build hash data
            hashData.append(fieldName);
            hashData.append('=');
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            // Build query
            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
            query.append('=');
            query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (itr.hasNext()) {
                query.append('&');
                hashData.append('&');
            }
        }
    }

    // 7. Generate secure hash
    String queryUrl = query.toString();
    String vnpSecureHash = hmacSHA512(vnpayHashSecret, hashData.toString());
    queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

    // 8. Return full payment URL
    return vnpayUrl + "?" + queryUrl;
}

/**
 * Generate HMAC SHA512 hash
 */
private String hmacSHA512(String key, String data) {
    try {
        Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
        SecretKeySpec secret_key = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"
        );
        sha512_HMAC.init(secret_key);
        byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    } catch (Exception e) {
        throw new RuntimeException("Error generating HMAC SHA512", e);
    }
}

/**
 * Get client IP address (supports proxy headers)
 */
private String getClientIp(HttpServletRequest request) {
    String ipAddress = request.getHeader("X-Forwarded-For");

    if (ipAddress == null || ipAddress.isEmpty() || 
        "unknown".equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getHeader("Proxy-Client-IP");
    }
    if (ipAddress == null || ipAddress.isEmpty() || 
        "unknown".equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ipAddress == null || ipAddress.isEmpty() || 
        "unknown".equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getRemoteAddr();
    }

    // Lấy IP đầu tiên nếu có nhiều IP
    if (ipAddress != null && ipAddress.contains(",")) {
        ipAddress = ipAddress.split(",")[0].trim();
    }

    return ipAddress;
}
```

### VNPay Parameters Explained

**Required Parameters**:
```
vnp_Version: "2.1.0" (VNPay API version)
vnp_Command: "pay" (Payment command)
vnp_TmnCode: "9CB3LH80" (Terminal/Merchant code from VNPay)
vnp_Amount: "5000000" (50,000 VND * 100 = 5,000,000)
vnp_CurrCode: "VND"
vnp_TxnRef: "ORD_001" (Order ID - unique transaction reference)
vnp_OrderInfo: "Thanh toan don hang ORD_001"
vnp_OrderType: "other" (Product category)
vnp_Locale: "vn" (Language: vn/en)
vnp_ReturnUrl: "http://localhost:2706/payment/vnpay/return"
vnp_IpAddr: "192.168.1.100" (Customer IP)
vnp_CreateDate: "20251230120000" (yyyyMMddHHmmss)
vnp_ExpireDate: "20251230121500" (15 minutes from create)
vnp_SecureHash: "a1b2c3d4e5f6..." (HMAC SHA512 hash)
```

### Example Payment URL
```
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?
vnp_Amount=5000000&
vnp_Command=pay&
vnp_CreateDate=20251230120000&
vnp_CurrCode=VND&
vnp_ExpireDate=20251230121500&
vnp_IpAddr=192.168.1.100&
vnp_Locale=vn&
vnp_OrderInfo=Thanh+toan+don+hang+ORD_001&
vnp_OrderType=other&
vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A2706%2Fpayment%2Fvnpay%2Freturn&
vnp_TmnCode=9CB3LH80&
vnp_TxnRef=ORD_001&
vnp_Version=2.1.0&
vnp_SecureHash=a1b2c3d4e5f6789...
```

### Hash Generation Process

1. **Collect parameters** into Map
2. **Remove** `vnp_SecureHash` and `vnp_SecureHashType` if exists
3. **Sort** parameters alphabetically by key
4. **Build hash data**: `key1=value1&key2=value2&...`
5. **URL encode** values
6. **Generate HMAC SHA512** using secret key
7. **Convert** to lowercase hex string

**Example Hash Data**:
```
vnp_Amount=5000000&vnp_Command=pay&vnp_CreateDate=20251230120000&...
```

**Secret Key**: `UDN2E28HUBUULOWK5KAGTA3GVU523HPK`

**Generated Hash**: `a1b2c3d4e5f6789abcdef0123456789...` (128 characters)
    
    // Return URL
    vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
    
    // IP Address
    vnpParams.put("vnp_IpAddr", getClientIp(request));
    
    // Locale
    vnpParams.put("vnp_Locale", "vn");
    
    // Create date (format: yyyyMMddHHmmss)
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    String createDate = formatter.format(new Date());
    vnpParams.put("vnp_CreateDate", createDate);
    
    return vnpParams;
}
```

**Generate Secure Hash (HMAC SHA512)**:
```java
private String hmacSHA512(String key, String data) {
    try {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKey);
        byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder result = new StringBuilder();
        for (byte b : hashBytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
        
    } catch (Exception e) {
        throw new RuntimeException("Error generating HMAC SHA512", e);
    }
}
```

**Build Query URL**:
```java
private String buildQueryUrl(Map<String, String> params) {
    StringBuilder query = new StringBuilder();
    
    for (Map.Entry<String, String> entry : params.entrySet()) {
        if (query.length() > 0) {
            query.append("&");
        }
        query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
        query.append("=");
        query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    
    return query.toString();
}
```

**Example VNPay URL**:
```
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  ?vnp_Amount=7900000
  &vnp_Command=pay
  &vnp_CreateDate=20251206143025
  &vnp_CurrCode=VND
  &vnp_IpAddr=192.168.1.1
  &vnp_Locale=vn
  &vnp_OrderInfo=Thanh+toan+don+hang+ord_001
  &vnp_OrderType=other
  &vnp_ReturnUrl=http://localhost:2706/payment/vnpay/return
  &vnp_TmnCode=ICCSTETD
  &vnp_TxnRef=ord_001
  &vnp_Version=2.1.0
  &vnp_SecureHash=abc123def456...
```

---

## Flow 5.2: Xử Lý Callback VNPay

### Sequence Diagram
```
VNPay → Browser → PaymentController → VNPayService → OrderService → Database
  │       │              │                 │              │            │
  │ Redirect after payment                                            │
  │──────────────────────►│                                            │
  │       │               │ validateCallback()                         │
  │       │               ├────────────────►│                          │
  │       │               │                 │ getSignature()           │
  │       │               │                 │ sortParams()             │
  │       │               │                 │ hmacSHA512()             │
  │       │               │                 │ compare()                │
  │       │               │◄────────────────┤ (valid/invalid)          │
  │       │               │                                            │
  │       │               │ getResponseCode()                          │
  │       │               ├────────────────►│                          │
  │       │               │◄────────────────┤ "00" (success)           │
  │       │               │                                            │
  │       │               │ getOrderById()                             │
  │       │               ├─────────────────────────────►│             │
  │       │               │                              │ SELECT      │
  │       │               │                              ├────────────►│
  │       │               │◄─────────────────────────────┤             │
  │       │               │                                            │
  │       │               │ updateOrderStatus()                        │
  │       │               │ setPaymentStatus(COMPLETED)                │
  │       │               │ setTransactionId()                         │
  │       │               ├─────────────────────────────►│             │
  │       │               │                              │ UPDATE      │
  │       │               │                              ├────────────►│
  │       │               │◄─────────────────────────────┤             │
  │       │               │                                            │
  │       │               │ [IF SUBSCRIPTION ORDER]                    │
  │       │               │ activateSubscription()                     │
  │       │               │ setStartDate(now)                          │
  │       │               │ setEndDate(now + duration)                 │
  │       │               ├─────────────────────────────►│             │
  │       │               │                              │ UPDATE      │
  │       │               │                              ├────────────►│
  │       │               │◄─────────────────────────────┤             │
  │◄──────────────────────┤ redirect:/payment/success                  │
```

### Implementation Details

**Controller**: `PaymentController.vnpayReturn()`

```java
@GetMapping("/vnpay/return")
public String vnpayReturn(
        @RequestParam Map<String, String> params,
        RedirectAttributes redirectAttributes) {

    try {
        // 1. Xác thực chữ ký qua VNPayService
        if (!vnPayService.validateCallback(params)) {
            logger.warn("Invalid VNPay callback signature");
            redirectAttributes.addFlashAttribute("error", "Chữ ký không hợp lệ");
            return "redirect:/payment/error";
        }

        // 2. Lấy thông tin từ callback
        String orderId = vnPayService.getOrderId(params);
        String responseCode = vnPayService.getResponseCode(params);
        String transactionNo = vnPayService.getTransactionId(params);

        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/payment/error";
        }

        // 3. Cập nhật trạng thái theo response code
        if ("00".equals(responseCode)) {
            // ✅ Thanh toán thành công
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setTransactionId(transactionNo);

            // 4. Nếu là subscription order, kích hoạt subscription
            if (order.getOrderType() == Order.OrderType.SUBSCRIPTION && 
                order.getSubscription() != null) {
                
                LocalDateTime now = LocalDateTime.now();
                order.setStartDate(now);

                // Tính end_date dựa trên duration của subscription
                int durationDays = order.getSubscription().getDurationDays();
                LocalDateTime endDate = now.plusDays(durationDays);
                order.setEndDate(endDate);

                logger.info("Subscription activated for order: {}, user: {}, " +
                           "plan: {}, end_date: {}",
                           orderId, order.getUser().getUserId(),
                           order.getSubscription().getPackageName(), endDate);
            }

            orderService.saveOrder(order);

            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công!");
            return "redirect:/payment/success?orderId=" + orderId;
        } else {
            // ❌ Thanh toán thất bại
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderService.saveOrder(order);

            redirectAttributes.addFlashAttribute("error", 
                "Thanh toán thất bại. Mã lỗi: " + responseCode);
            return "redirect:/payment/error?orderId=" + orderId;
        }

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi xử lý thanh toán: " + e.getMessage());
        return "redirect:/payment/error";
    }
}
```

**Service**: `VNPayServiceImpl.validateCallback()`

```java
@Override
public boolean validateCallback(Map<String, String> params) {
    try {
        // 1. Lấy và remove secure hash
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return false;
        }

        // 2. Clone params để không ảnh hưởng map gốc
        Map<String, String> clonedParams = new HashMap<>(params);
        clonedParams.remove("vnp_SecureHash");
        clonedParams.remove("vnp_SecureHashType");

        // 3. Tính toán hash
        String signValue = getSignatureData(clonedParams);
        String calculatedHash = hmacSHA512(vnpayHashSecret, signValue);

        // 4. So sánh hash
        return calculatedHash.equals(vnpSecureHash);
    } catch (Exception e) {
        return false;
    }
}

/**
 * Get signature data from params (sorted alphabetically)
 */
private String getSignatureData(Map<String, String> params) {
    List<String> fieldNames = new ArrayList<>(params.keySet());
    Collections.sort(fieldNames);

    StringBuilder hashData = new StringBuilder();
    Iterator<String> itr = fieldNames.iterator();

    while (itr.hasNext()) {
        String fieldName = itr.next();
        String fieldValue = params.get(fieldName);

        if ((fieldValue != null) && (!fieldValue.isEmpty())) {
            hashData.append(fieldName);
            hashData.append('=');
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (itr.hasNext()) {
                hashData.append('&');
            }
        }
    }

    return hashData.toString();
}

@Override
public String getResponseCode(Map<String, String> params) {
    return params.get("vnp_ResponseCode");
}

@Override
public String getTransactionId(Map<String, String> params) {
    return params.get("vnp_TransactionNo");
}

@Override
public String getOrderId(Map<String, String> params) {
    return params.get("vnp_TxnRef");
}
```

### VNPay Callback Parameters

**Success Callback Example**:
```
http://localhost:2706/payment/vnpay/return?
vnp_Amount=5000000&
vnp_BankCode=NCB&
vnp_BankTranNo=VNP123456789&
vnp_CardType=ATM&
vnp_OrderInfo=Thanh+toan+don+hang+ORD_001&
vnp_PayDate=20251230120530&
vnp_ResponseCode=00&
vnp_TmnCode=9CB3LH80&
vnp_TransactionNo=14567890&
vnp_TransactionStatus=00&
vnp_TxnRef=ORD_001&
vnp_SecureHash=a1b2c3d4e5f6...
```

### VNPay Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| `00` | Giao dịch thành công | Update order to COMPLETED |
| `07` | Trừ tiền thành công. Giao dịch bị nghi ngờ | Manual review |
| `09` | Thẻ chưa đăng ký Internet Banking | Show error |
| `10` | Thẻ hết hạn | Show error |
| `11` | Thẻ bị khóa | Show error |
| `12` | Thẻ chưa đăng ký dịch vụ | Show error |
| `13` | Sai mật khẩu | Show error |
| `24` | Khách hàng hủy giao dịch | Set order to CANCELLED |
| `51` | Tài khoản không đủ số dư | Show error |
| `65` | Tài khoản vượt quá hạn mức | Show error |
| `75` | Ngân hàng thanh toán đang bảo trì | Show error, retry later |
| `79` | Nhập sai mật khẩu quá số lần quy định | Show error |
| Other | Lỗi không xác định | Log & show generic error |

**Full response code list**: https://sandbox.vnpayment.vn/apis/docs/bang-ma-loi/

### Order Status Updates

**Subscription Order** (order_type = 'SUBSCRIPTION'):
```sql
UPDATE orders
SET payment_status = 'COMPLETED',
    transaction_id = '14567890',
    start_date = NOW(),
    end_date = DATE_ADD(NOW(), INTERVAL 30 DAY)  -- Based on subscription duration
WHERE order_id = 'ORD_001';
```

**Book Purchase Order** (order_type = 'BOOK_PURCHASE'):
```sql
UPDATE orders
SET payment_status = 'COMPLETED',
    transaction_id = '14567890'
WHERE order_id = 'ORD_001';
```

### Validation Process

1. **Extract vnp_SecureHash** from callback params
2. **Remove** `vnp_SecureHash` và `vnp_SecureHashType` from params
3. **Sort** remaining params alphabetically
4. **Build signature data**: `field1=value1&field2=value2&...`
5. **Generate HMAC SHA512** with secret key
6. **Compare** calculated hash with received hash
7. **Return** true if match, false otherwise

### Error Handling

```java
// In PaymentController
if (!vnPayService.validateCallback(params)) {
    logger.warn("Invalid signature. Params: {}", params);
    return "redirect:/payment/error";
}

if (!"00".equals(responseCode)) {
    logger.warn("Payment failed. Order: {}, Code: {}", orderId, responseCode);
    order.setPaymentStatus(Order.PaymentStatus.FAILED);
    orderService.saveOrder(order);
    return "redirect:/payment/error?orderId=" + orderId;
}
```

### Sequence Diagram
```
VNPay → Browser → PaymentController → OrderService → Database
  │        │              │                 │             │
  │ Payment Complete                                      │
  │────────►│                                             │
  │        │ GET /payment/vnpay/return?params...         │
  │        ├─────────────►│                               │
  │        │              │ parseReturnParams()           │
  │        │              │ verifySecureHash()            │
  │        │              │ checkResponseCode()           │
  │        │              │                                │
  │        │              │ getOrderById()                │
  │        │              ├────────────────►│              │
  │        │              │◄────────────────┤              │
  │        │              │                                │
  │        │              │ updateOrderStatus()           │
  │        │              ├────────────────►│              │
  │        │              │                 │ UPDATE orders│
  │        │              │                 ├─────────────►│
  │        │              │                 │◄─────────────┤
  │        │              │◄────────────────┤              │
  │        │              │                                │
  │        │              │ clearUserCart()                │
  │        │              │                                │
  │        │◄─────────────┤ (show success page)           │
  │◄────────┤                                              │
```

### Implementation Details

**Controller Method**:
```java
@GetMapping("/vnpay/return")
public String handleVNPayReturn(
        @RequestParam Map<String, String> allParams,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    try {
        // 1. Extract và validate parameters
        String vnpSecureHash = allParams.get("vnp_SecureHash");
        allParams.remove("vnp_SecureHash");
        allParams.remove("vnp_SecureHashType");
        
        // 2. Verify secure hash
        String queryUrl = buildQueryUrl(allParams);
        String calculatedHash = hmacSHA512(vnpayHashSecret, queryUrl);
        
        if (!calculatedHash.equals(vnpSecureHash)) {
            model.addAttribute("error", "Chữ ký không hợp lệ");
            return "user/payment-result";
        }
        
        // 3. Get transaction info
        String orderId = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");
        String transactionNo = allParams.get("vnp_TransactionNo");
        String bankCode = allParams.get("vnp_BankCode");
        String payDate = allParams.get("vnp_PayDate");
        
        // 4. Get order
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            model.addAttribute("error", "Đơn hàng không tồn tại");
            return "user/payment-result";
        }
        
        // 5. Check response code
        if ("00".equals(responseCode)) {
            // SUCCESS
            order.setStatus("PAID");
            order.setPaymentStatus("PAID");
            order.setTransactionId(transactionNo);
            order.setPaymentDate(parseVNPayDate(payDate));
            orderService.saveOrder(order);
            
            // Clear cart
            cartService.clearCart(order.getUser());
            
            model.addAttribute("success", true);
            model.addAttribute("message", "Thanh toán thành công!");
            model.addAttribute("orderId", orderId);
            model.addAttribute("transactionNo", transactionNo);
            model.addAttribute("bankCode", bankCode);
            
        } else {
            // FAILURE
            order.setStatus("PAYMENT_FAILED");
            order.setPaymentStatus("FAILED");
            orderService.saveOrder(order);
            
            model.addAttribute("success", false);
            model.addAttribute("message", getErrorMessage(responseCode));
            model.addAttribute("orderId", orderId);
        }
        
        model.addAttribute("order", order);
        return "user/payment-result";
        
    } catch (Exception e) {
        model.addAttribute("error", "Lỗi xử lý kết quả thanh toán");
        return "user/payment-result";
    }
}
```

**VNPay Response Codes**:
```java
private String getErrorMessage(String responseCode) {
    switch (responseCode) {
        case "00": return "Giao dịch thành công";
        case "07": return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
        case "09": return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
        case "10": return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
        case "11": return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
        case "12": return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
        case "13": return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
        case "24": return "Giao dịch không thành công do: Khách hàng hủy giao dịch";
        case "51": return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
        case "65": return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
        case "75": return "Ngân hàng thanh toán đang bảo trì.";
        case "79": return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
        default: return "Giao dịch không thành công. Mã lỗi: " + responseCode;
    }
}
```

**Parse VNPay Date**:
```java
private Date parseVNPayDate(String vnpayDate) {
    try {
        // Format: yyyyMMddHHmmss
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.parse(vnpayDate);
    } catch (Exception e) {
        return new Date();
    }
}
```

---

## Flow 5.3: IPN (Instant Payment Notification)

### Overview
IPN là webhook endpoint mà VNPay gọi để thông báo kết quả giao dịch. Khác với return URL (callback từ browser), IPN được gọi trực tiếp từ server VNPay.

### Sequence Diagram
```
VNPay Server → PaymentController → OrderService → Database
      │                │                │             │
      │ POST /payment/vnpay/ipn                       │
      ├───────────────►│                              │
      │                │ verifySecureHash()           │
      │                │ checkDuplicate()             │
      │                │                               │
      │                │ getOrderById()               │
      │                ├───────────────►│              │
      │                │◄───────────────┤              │
      │                │                               │
      │                │ updateOrderStatus()          │
      │                ├───────────────►│              │
      │                │                │ UPDATE       │
      │                │                ├─────────────►│
      │                │                │◄─────────────┤
      │                │◄───────────────┤              │
      │                │                               │
      │◄───────────────┤ {"RspCode":"00","Message":"Success"}
```

**Implementation**:
```java
@PostMapping("/vnpay/ipn")
@ResponseBody
public Map<String, String> handleVNPayIPN(@RequestParam Map<String, String> allParams) {
    Map<String, String> response = new HashMap<>();
    
    try {
        // 1. Verify secure hash
        String vnpSecureHash = allParams.get("vnp_SecureHash");
        allParams.remove("vnp_SecureHash");
        allParams.remove("vnp_SecureHashType");
        
        String queryUrl = buildQueryUrl(allParams);
        String calculatedHash = hmacSHA512(vnpayHashSecret, queryUrl);
        
        if (!calculatedHash.equals(vnpSecureHash)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
            return response;
        }
        
        // 2. Get transaction info
        String orderId = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");
        String amount = allParams.get("vnp_Amount");
        String transactionNo = allParams.get("vnp_TransactionNo");
        
        // 3. Check order exists
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            response.put("RspCode", "01");
            response.put("Message", "Order not found");
            return response;
        }
        
        // 4. Check amount
        long expectedAmount = order.getTotalAmount().multiply(new BigDecimal(100)).longValue();
        if (!String.valueOf(expectedAmount).equals(amount)) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid amount");
            return response;
        }
        
        // 5. Check if already processed
        if ("PAID".equals(order.getPaymentStatus())) {
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return response;
        }
        
        // 6. Update order
        if ("00".equals(responseCode)) {
            order.setStatus("PAID");
            order.setPaymentStatus("PAID");
            order.setTransactionId(transactionNo);
            orderService.saveOrder(order);
            
            response.put("RspCode", "00");
            response.put("Message", "Success");
        } else {
            order.setStatus("PAYMENT_FAILED");
            order.setPaymentStatus("FAILED");
            orderService.saveOrder(order);
            
            response.put("RspCode", "00");
            response.put("Message", "Success");
        }
        
        return response;
        
    } catch (Exception e) {
        response.put("RspCode", "99");
        response.put("Message", "Unknown error");
        return response;
    }
}
```

---

## Flow 5.4: Tra Cứu Giao Dịch

### VNPay Query API
```java
@GetMapping("/vnpay/query/{orderId}")
@ResponseBody
public Map<String, Object> queryTransaction(@PathVariable String orderId) {
    try {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return Map.of("success", false, "message", "Order not found");
        }
        
        // Build query parameters
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "querydr");
        params.put("vnp_TmnCode", vnpayTmnCode);
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_OrderInfo", "Query transaction " + orderId);
        params.put("vnp_TransactionDate", order.getCreatedAt().toString());
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("vnp_IpAddr", "127.0.0.1");
        
        // Generate secure hash
        String queryUrl = buildQueryUrl(params);
        String secureHash = hmacSHA512(vnpayHashSecret, queryUrl);
        
        // Call VNPay API
        String apiUrl = vnpayApiUrl + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
        
        // TODO: Make HTTP request to VNPay API
        // RestTemplate or HttpClient
        
        return Map.of("success", true, "data", params);
        
    } catch (Exception e) {
        return Map.of("success", false, "message", e.getMessage());
    }
}
```

---

## Error Handling

### Common Errors

**1. Invalid Signature**
```java
if (!calculatedHash.equals(vnpSecureHash)) {
    throw new SecurityException("Invalid VNPay signature");
}
```

**2. Order Not Found**
```java
if (order == null) {
    throw new ResourceNotFoundException("Order not found: " + orderId);
}
```

**3. Amount Mismatch**
```java
if (!expectedAmount.equals(actualAmount)) {
    throw new PaymentException("Amount mismatch");
}
```

**4. Duplicate Transaction**
```java
if (order.getTransactionId() != null) {
    throw new DuplicateTransactionException("Order already processed");
}
```

---

## Security & Validation

### Security Checklist

✅ **Always verify secure hash**
```java
String calculatedHash = hmacSHA512(vnpayHashSecret, queryUrl);
if (!calculatedHash.equals(vnpSecureHash)) {
    return "error";
}
```

✅ **Validate order ownership**
```java
if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
    throw new AccessDeniedException("Unauthorized");
}
```

✅ **Check order status**
```java
if (!"PENDING".equals(order.getStatus())) {
    throw new InvalidOrderStateException("Order not pending");
}
```

✅ **Validate amount**
```java
if (!expectedAmount.equals(receivedAmount)) {
    throw new PaymentException("Amount mismatch");
}
```

✅ **Prevent replay attacks**
```java
if (order.getTransactionId() != null) {
    throw new DuplicateTransactionException();
}
```

✅ **Use HTTPS in production**
```properties
vnpay.return_url=https://yourdomain.com/payment/vnpay/return
```

---

## Testing

### Test Cases

**1. Successful Payment**
- User selects VNPay
- Redirected to VNPay
- Complete payment
- Return with responseCode=00
- Order status = PAID

**2. Failed Payment**
- User selects VNPay
- Redirected to VNPay
- Cancel payment
- Return with responseCode=24
- Order status = PAYMENT_FAILED

**3. Invalid Signature**
- Modify vnp_SecureHash
- Should reject transaction

**4. Amount Mismatch**
- Modify vnp_Amount
- Should reject transaction

**5. Duplicate Processing**
- Process same order twice
- Second attempt should be rejected

---

## Production Checklist

- [ ] Change VNPay URL to production: `https://vnpayment.vn/paymentv2/vpcpay.html`
- [ ] Update TMN Code (merchant code)
- [ ] Update Hash Secret (production key)
- [ ] Update Return URL to production domain (HTTPS)
- [ ] Set up IPN endpoint
- [ ] Configure firewall to allow VNPay IP addresses
- [ ] Enable transaction logging
- [ ] Set up monitoring & alerts
- [ ] Test with real bank cards (small amounts)
- [ ] Implement refund functionality
- [ ] Configure transaction timeout (default: 15 minutes)

---

## Debugging

### Enable VNPay Logging
```java
@Slf4j
@Controller
@RequestMapping("/payment")
public class PaymentController {
    
    @GetMapping("/vnpay")
    public String initiateVNPayPayment(...) {
        log.info("=== VNPay Payment Initiation ===");
        log.info("Order ID: {}", orderId);
        log.info("Amount: {}", order.getTotalAmount());
        log.info("VNPay Params: {}", vnpParams);
        log.info("Payment URL: {}", paymentUrl);
        // ...
    }
    
    @GetMapping("/vnpay/return")
    public String handleVNPayReturn(...) {
        log.info("=== VNPay Return Callback ===");
        log.info("All params: {}", allParams);
        log.info("Response code: {}", responseCode);
        log.info("Transaction No: {}", transactionNo);
        // ...
    }
}
```

### Common Issues

**Issue 1: Invalid Signature**
- Check hash secret matches VNPay portal
- Verify parameter order (alphabetically sorted)
- Ensure URL encoding is correct

**Issue 2: Amount = 0**
- VNPay requires amount * 100
- Check data type (Long, not Double)

**Issue 3: Timeout**
- Default: 15 minutes
- Configure: `vnp_ExpireDate`

---

**Last Updated:** 06/12/2025  
**Status:** ✅ COMPLETE  
**Version:** 1.0

