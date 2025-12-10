# 💳 FLOW 05: PAYMENT PROCESSING - VNPAY (Xử Lý Thanh Toán VNPay)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 5.1: Khởi Tạo Thanh Toán VNPay](#flow-51-khởi-tạo-thanh-toán-vnpay)
3. [Flow 5.2: Xử Lý Callback VNPay](#flow-52-xử-lý-callback-vnpay)
4. [Flow 5.3: IPN (Instant Payment Notification)](#flow-53-ipn-instant-payment-notification)
5. [Flow 5.4: Tra Cứu Giao Dịch](#flow-54-tra-cứu-giao-dịch)
6. [Error Handling](#error-handling)
7. [Security & Validation](#security--validation)

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
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Generate HMAC │
│  Signature   │
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
└──────┬───────┘
       │
       ▼
┌──────────────┐
│VNPay Callback│
│   Return     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Verify Secure │
│     Hash     │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│Update Order  │
│    Status    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Show Success │
│Confirmation  │
└──────────────┘
```

### Components
- **Controller**: `PaymentController.java`
- **Service**: `OrderService.java`, `OrderItemService.java`
- **Entity**: `Order.java`, `OrderItem.java`
- **Config**: `application.properties` (VNPay credentials)

### VNPay Configuration
```properties
# VNPay Settings
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.tmn_code=ICCSTETD
vnpay.hash_secret=E5DSMU678G8NJL64IO7PD9HUAK52F54P
vnpay.return_url=http://localhost:2706/payment/vnpay/return
vnpay.api_url=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
```

### URLs
- `GET /payment/vnpay?orderId={orderId}` - Khởi tạo thanh toán
- `GET /payment/vnpay/return` - Callback từ VNPay
- `POST /payment/vnpay/ipn` - IPN endpoint (webhook)

---

## Flow 5.1: Khởi Tạo Thanh Toán VNPay

### Sequence Diagram
```
User → Browser → PaymentController → OrderService → VNPay Gateway
  │       │              │                 │              │
  │ Click "Thanh toán VNPay"                             │
  │───────────────────────►│                              │
  │       │                │ getOrderById()               │
  │       │                ├────────────────►│            │
  │       │                │◄────────────────┤            │
  │       │                │ validateOrder()               │
  │       │                │ buildVNPayParams()           │
  │       │                │ generateSecureHash()         │
  │       │                │ buildPaymentUrl()            │
  │◄───────────────────────┤ (redirect to VNPay)         │
  │       │                                               │
  │ Redirect to VNPay Gateway                            │
  │──────────────────────────────────────────────────────►│
  │       │                                               │
  │ Enter card info & confirm                            │
  │◄──────────────────────────────────────────────────────┤
```

### Implementation Details

**Controller Method**:
```java
@GetMapping("/vnpay")
public String initiateVNPayPayment(
        @RequestParam String orderId,
        Authentication authentication,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes) {
    
    try {
        // 1. Validate user
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        // 2. Get order
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại");
            return "redirect:/user/orders";
        }

        // 3. Validate order ownership
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Không có quyền truy cập");
            return "redirect:/user/orders";
        }

        // 4. Check order status
        if (!"PENDING".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng đã được xử lý");
            return "redirect:/user/orders";
        }

        // 5. Build VNPay payment parameters
        Map<String, String> vnpParams = buildVNPayParams(order, request);
        
        // 6. Generate secure hash
        String queryUrl = buildQueryUrl(vnpParams);
        String secureHash = hmacSHA512(vnpayHashSecret, queryUrl);
        
        // 7. Build full payment URL
        String paymentUrl = vnpayUrl + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
        
        // 8. Redirect to VNPay
        return "redirect:" + paymentUrl;
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán");
        return "redirect:/user/orders";
    }
}
```

**Build VNPay Parameters**:
```java
private Map<String, String> buildVNPayParams(Order order, HttpServletRequest request) {
    Map<String, String> vnpParams = new TreeMap<>();
    
    // Basic parameters
    vnpParams.put("vnp_Version", "2.1.0");
    vnpParams.put("vnp_Command", "pay");
    vnpParams.put("vnp_TmnCode", vnpayTmnCode);
    
    // Amount (VNPay requires amount in VND * 100)
    long amount = order.getTotalAmount().multiply(new BigDecimal(100)).longValue();
    vnpParams.put("vnp_Amount", String.valueOf(amount));
    
    // Currency
    vnpParams.put("vnp_CurrCode", "VND");
    
    // Transaction reference (Order ID)
    vnpParams.put("vnp_TxnRef", order.getOrderId());
    
    // Order description
    String orderInfo = "Thanh toan don hang " + order.getOrderId();
    vnpParams.put("vnp_OrderInfo", orderInfo);
    
    // Order type (bookstore = other)
    vnpParams.put("vnp_OrderType", "other");
    
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

