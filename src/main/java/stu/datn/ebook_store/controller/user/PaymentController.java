package stu.datn.ebook_store.controller.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.UserSubscription;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.SubscriptionService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller xử lý thanh toán cho cả sách lẻ và gói đăng ký
 * Endpoints: /payment/*
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @Autowired
    private SubscriptionService subscriptionService;


    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayUrl;

    @Value("${vnpay.tmn_code:9CB3LH80}")
    private String vnpayTmnCode;

    @Value("${vnpay.hash_secret:UDN2E28HUBUULOWK5KAGTA3GVU523HPK}")
    private String vnpayHashSecret;

    @Value("${vnpay.return_url:http://localhost:2706/payment/vnpay/return}")
    private String vnpayReturnUrl;

    @Value("${bank.name:TPbank}")
    private String bankName;

    @Value("${bank.account_number:79992706999}")
    private String bankAccountNumber;

    @Value("${bank.account_name:CONG TY EBOOK STORE}")
    private String bankAccountName;

    @Value("${bank.branch:Chi nhanh TP.HCM}")
    private String bankBranch;

    @Value("${bank.code:TP}")
    private String bankCode;

    @Value("${bank.qr_template:https://img.vietqr.io/image/{bank_code}-{account_number}-{template}.png?amount={amount}&addInfo={content}&accountName={account_name}}")
    private String qrTemplate;

    @Autowired
    public PaymentController(OrderService orderService, OrderItemService orderItemService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
    }

    /**
     * Lấy user hiện tại
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * Lấy địa chỉ IP của client
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Lấy IP đầu tiên nếu có nhiều IP
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress != null ? ipAddress : "127.0.0.1";
    }

    /**
     * Khởi tạo thanh toán VNPay
     */
    @GetMapping("/vnpay")
    public String initiateVNPayPayment(
            @RequestParam String orderId,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
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

            // Tạo payment URL
            String paymentUrl = createVNPayPaymentUrl(order, request);

            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }

    /**
     * Tạo URL thanh toán VNPay
     */
    private String createVNPayPaymentUrl(Order order, HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();

        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(order.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderId());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
        vnpParams.put("vnp_IpAddr", getClientIp(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Build query string
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
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

        String queryUrl = query.toString();
        String vnpSecureHash = hmacSHA512(vnpayHashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        return vnpayUrl + "?" + queryUrl;
    }

    /**
     * Callback từ VNPay
     */
    @GetMapping("/vnpay/return")
    public String vnpayReturn(
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {

        try {
            // Lấy secure hash
            String vnpSecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");

            // Xác thực chữ ký
            String signValue = getSignatureData(params);
            String calculatedHash = hmacSHA512(vnpayHashSecret, signValue);

            if (!calculatedHash.equals(vnpSecureHash)) {
                redirectAttributes.addFlashAttribute("error", "Chữ ký không hợp lệ");
                return "redirect:/payment/error";
            }

            // Lấy thông tin
            String orderId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");

            Order order = orderService.getOrderById(orderId).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/payment/error";
            }

            // Cập nhật trạng thái
            if ("00".equals(responseCode)) {
                // Thanh toán thành công
                order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
                order.setTransactionId(transactionNo);

                // Nếu là subscription order, kích hoạt subscription
                if (order.getOrderType() == Order.OrderType.SUBSCRIPTION && order.getSubscription() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    order.setStartDate(now);

                    // Tính end_date dựa trên duration của subscription
                    int durationDays = order.getSubscription().getDurationDays();
                    LocalDateTime endDate = now.plusDays(durationDays);
                    order.setEndDate(endDate);

                    logger.info("Subscription activated for order: {}, user: {}, plan: {}, end_date: {}",
                            orderId, order.getUser().getUserId(),
                            order.getSubscription().getPackageName(), endDate);
                }

                orderService.saveOrder(order);

                redirectAttributes.addFlashAttribute("success", "Thanh toán thành công!");
                return "redirect:/payment/success?orderId=" + orderId;
            } else {
                // Thanh toán thất bại
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                orderService.saveOrder(order);

                redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại. Mã lỗi: " + responseCode);
                return "redirect:/payment/error?orderId=" + orderId;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "redirect:/payment/error";
        }
    }

    /**
     * Trang thanh toán thành công
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String orderId, Model model) {
        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order != null) {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
            model.addAttribute("orderItems", orderItems);
        }
        model.addAttribute("order", order);
        return "user/payment/success";
    }

    /**
     * Trang thanh toán thất bại
     */
    @GetMapping("/error")
    public String paymentError(@RequestParam(required = false) String orderId, Model model) {
        if (orderId != null) {
            Order order = orderService.getOrderById(orderId).orElse(null);
            if (order != null) {
                List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
                model.addAttribute("orderItems", orderItems);
            }
            model.addAttribute("order", order);
        }
        return "user/payment/error";
    }

    /**
     * Khởi tạo thanh toán chuyển khoản ngân hàng (QR Code)
     */
    @GetMapping("/bank-transfer")
    public String initiateBankTransferPayment(
            @RequestParam String orderId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
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
            String transferContent = "EBOOKSTORE " + orderId;

            // Tạo URL QR code
            String qrUrl = generateQRCodeUrl(order, transferContent);

            // Lấy danh sách order items
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

            // Thêm thông tin vào model
            model.addAttribute("order", order);
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("qrCodeUrl", qrUrl);
            model.addAttribute("bankName", bankName);
            model.addAttribute("bankAccountNumber", bankAccountNumber);
            model.addAttribute("bankAccountName", bankAccountName);
            model.addAttribute("bankBranch", bankBranch);
            model.addAttribute("transferContent", transferContent);

            return "user/payment/bank-transfer";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }

    /**
     * Tạo URL QR code cho thanh toán ngân hàng
     */
    private String generateQRCodeUrl(Order order, String content) {
        try {
            // Log để debug
            System.out.println("=== Generating QR Code ===");
            System.out.println("Bank Code: " + bankCode);
            System.out.println("Account Number: " + bankAccountNumber);
            System.out.println("Account Name: " + bankAccountName);
            System.out.println("Amount: " + order.getTotalAmount().longValue());
            System.out.println("Content: " + content);
            System.out.println("QR Template: " + qrTemplate);

            String url = qrTemplate
                    .replace("{bank_code}", bankCode)
                    .replace("{account_number}", bankAccountNumber)
                    .replace("{template}", "compact2")
                    .replace("{amount}", String.valueOf(order.getTotalAmount().longValue()))
                    .replace("{content}", URLEncoder.encode(content, StandardCharsets.UTF_8))
                    .replace("{account_name}", URLEncoder.encode(bankAccountName, StandardCharsets.UTF_8));

            System.out.println("Generated QR URL: " + url);

            return url;
        } catch (Exception e) {
            System.err.println("Error generating QR code URL: " + e.getMessage());
            e.printStackTrace();
            // Return a default QR code URL instead of throwing exception
            return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
                   URLEncoder.encode("Thanh toan don hang: " + order.getOrderId(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Xác nhận đã chuyển khoản
     */
    @PostMapping("/bank-transfer/confirm")
    @ResponseBody
    public Map<String, Object> confirmBankTransfer(
            @RequestParam String orderId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Debug logging
            System.out.println("=== Confirm Bank Transfer ===");
            System.out.println("Received orderId: " + orderId);
            System.out.println("Authentication: " + (authentication != null ? "Present" : "Null"));

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                System.out.println("ERROR: Current user is null");
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập");
                return response;
            }

            System.out.println("Current user: " + currentUser.getUserId());

            // Lấy order
            System.out.println("Looking for order: " + orderId);
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

            System.out.println("Order found: " + order.getOrderId());
            System.out.println("Order user: " + order.getUser().getUserId());

            // Kiểm tra quyền
            if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                System.out.println("ERROR: User mismatch - Order user: " + order.getUser().getUserId() + ", Current user: " + currentUser.getUserId());
                throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này");
            }

            // Kiểm tra trạng thái hiện tại
            System.out.println("Current order status: " + order.getPaymentStatus());
            if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
                System.out.println("ERROR: Order status is not PENDING");
                response.put("success", false);
                response.put("message", "Đơn hàng đã được xác nhận trước đó");
                return response;
            }

            // Cập nhật trạng thái thành WAITING_APPROVAL
            order.setPaymentStatus(Order.PaymentStatus.WAITING_APPROVAL);
            orderService.saveOrder(order);

            System.out.println("Order updated to WAITING_APPROVAL");
            System.out.println("=== Confirm Success ===");

            response.put("success", true);
            response.put("message", "Đã xác nhận chuyển khoản. Đơn hàng đang chờ admin duyệt.");
            response.put("redirectUrl", "/payment/bank-transfer/waiting?orderId=" + orderId);

        } catch (Exception e) {
            System.err.println("ERROR in confirmBankTransfer: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }

        return response;
    }

    /**
     * Trang chờ duyệt thanh toán
     */
    @GetMapping("/bank-transfer/waiting")
    public String bankTransferWaiting(
            @RequestParam String orderId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
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



    /**
     * HMAC SHA512
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

    /**
     * Get signature data from params
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

    // ========================================================================
    // SUBSCRIPTION PAYMENT METHODS
    // ========================================================================

    /**
     * Trang thanh toán gói đăng ký
     */
    @GetMapping("/subscription/checkout/{subscriptionId}")
    public String showSubscriptionCheckout(@PathVariable String subscriptionId,
                                          Authentication authentication,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đăng ký gói");
            return "redirect:/auth/login";
        }

        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Gói không tồn tại");
            return "redirect:/subscription/plans";
        }

        Subscription subscription = subscriptionOpt.get();

        // Kiểm tra nếu user đã có gói active
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(currentUser.getUserId(), Order.OrderType.SUBSCRIPTION);
        Optional<UserSubscription> activeSubscription = subscriptionOrders.stream()
                .filter(order -> {
                    if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED &&
                        order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                        return false;
                    }
                    return order.getEndDate() != null &&
                           order.getEndDate().isAfter(java.time.LocalDateTime.now());
                })
                .map(UserSubscription::new)
                .findFirst();

        model.addAttribute("subscription", subscription);
        model.addAttribute("user", currentUser);
        model.addAttribute("currentSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());

        return "user/payment/subscription-checkout";
    }

    /**
     * Xử lý thanh toán gói đăng ký
     */
    @PostMapping("/subscription/process")
    public String processSubscriptionPayment(@RequestParam("subscriptionId") String subscriptionId,
                                            @RequestParam("paymentMethod") String paymentMethod,
                                            Authentication authentication,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Gói không tồn tại");
            return "redirect:/subscription/plans";
        }

        Subscription subscription = subscriptionOpt.get();

        try {
            // Tạo đơn hàng
            Order order = new Order();
            order.setOrderId("SUB_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setUser(currentUser);
            order.setSubscription(subscription);
            order.setOrderType(Order.OrderType.SUBSCRIPTION);
            order.setTotalAmount(subscription.getPrice());
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // Set thời gian gói
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(subscription.getDurationDays());
            order.setStartDate(startDate);
            order.setEndDate(endDate);

            // Set phương thức thanh toán
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                order.setPaymentMethod(Order.PaymentMethod.VNPAY);
            } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                order.setPaymentMethod(Order.PaymentMethod.BANK_TRANSFER);
            } else {
                order.setPaymentMethod(Order.PaymentMethod.CREDIT_CARD);
            }

            // Lưu đơn hàng
            order = orderService.saveOrder(order);

            // Xử lý theo phương thức thanh toán
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                // Chuyển đến VNPAY
                String paymentUrl = createVNPayPaymentUrl(order, request);
                return "redirect:" + paymentUrl;
            } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                // Chuyển đến trang chuyển khoản ngân hàng
                return "redirect:/payment/subscription/bank-transfer?orderId=" + order.getOrderId();
            } else {
                redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ");
                return "redirect:/subscription/plans";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/subscription/plans";
        }
    }

    /**
     * Trang chuyển khoản ngân hàng cho gói đăng ký
     */
    @GetMapping("/subscription/bank-transfer")
    public String showSubscriptionBankTransfer(@RequestParam("orderId") String orderId,
                                              Authentication authentication,
                                              Model model,
                                              RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/subscription/plans";
        }

        Order order = orderOpt.get();

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn hàng này");
            return "redirect:/subscription/plans";
        }

        model.addAttribute("order", order);
        model.addAttribute("bankName", bankName);
        model.addAttribute("bankAccountNumber", bankAccountNumber);
        model.addAttribute("bankAccountName", bankAccountName);
        model.addAttribute("bankBranch", bankBranch);

        return "user/payment/subscription-bank-transfer";
    }

    /**
     * Trang thanh toán thành công cho gói đăng ký
     */
    @GetMapping("/subscription/success")
    public String subscriptionPaymentSuccess(@RequestParam("orderId") String orderId,
                                            Authentication authentication,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/subscription/plans";
        }

        Order order = orderOpt.get();

        // Kiểm tra quyền
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn hàng này");
            return "redirect:/subscription/plans";
        }

        model.addAttribute("order", order);

        return "user/payment/subscription-success";
    }

    /**
     * Trang thanh toán thất bại cho gói đăng ký
     */
    @GetMapping("/subscription/failed")
    public String subscriptionPaymentFailed(@RequestParam(value = "orderId", required = false) String orderId,
                                           @RequestParam(value = "message", required = false) String message,
                                           Authentication authentication,
                                           Model model) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser != null && orderId != null) {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(order -> {
                if (order.getUser().getUserId().equals(currentUser.getUserId())) {
                    model.addAttribute("order", order);
                }
            });
        }

        model.addAttribute("errorMessage", message != null ? message : "Thanh toán không thành công");

        return "user/payment/subscription-failed";
    }
}

