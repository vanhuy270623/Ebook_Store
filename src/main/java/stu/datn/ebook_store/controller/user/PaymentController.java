package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.OrderService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller xử lý thanh toán VNPay và MoMo
 * Endpoints: /payment/*
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final OrderService orderService;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayUrl;

    @Value("${vnpay.tmn_code:YOUR_TMN_CODE}")
    private String vnpayTmnCode;

    @Value("${vnpay.hash_secret:YOUR_HASH_SECRET}")
    private String vnpayHashSecret;

    @Value("${vnpay.return_url:http://localhost:8080/payment/vnpay/return}")
    private String vnpayReturnUrl;

    @Autowired
    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
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
     * Khởi tạo thanh toán VNPay
     */
    @GetMapping("/vnpay")
    public String initiateVNPayPayment(
            @RequestParam String orderId,
            Authentication authentication,
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
            String paymentUrl = createVNPayPaymentUrl(order);

            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khởi tạo thanh toán: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }

    /**
     * Tạo URL thanh toán VNPay
     */
    private String createVNPayPaymentUrl(Order order) throws UnsupportedEncodingException {
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
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

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
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

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
            Model model,
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
            model.addAttribute("order", order);
        }
        return "user/payment/error";
    }

    /**
     * Khởi tạo thanh toán MoMo (placeholder)
     */
    @GetMapping("/momo")
    public String initiateMoMoPayment(
            @RequestParam String orderId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // TODO: Implement MoMo payment integration
        redirectAttributes.addFlashAttribute("error", "MoMo payment chưa được tích hợp");
        return "redirect:/order/checkout";
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
    private String getSignatureData(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        return hashData.toString();
    }
}

