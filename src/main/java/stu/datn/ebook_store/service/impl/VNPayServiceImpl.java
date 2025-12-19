package stu.datn.ebook_store.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.service.VNPayService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation của VNPayService
 * Xử lý toàn bộ logic giao tiếp với VNPay Payment Gateway
 */
@Service
public class VNPayServiceImpl implements VNPayService {

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayUrl;

    @Value("${vnpay.tmn_code:9CB3LH80}")
    private String vnpayTmnCode;

    @Value("${vnpay.hash_secret:UDN2E28HUBUULOWK5KAGTA3GVU523HPK}")
    private String vnpayHashSecret;

    @Value("${vnpay.return_url:http://localhost:2706/payment/vnpay/return}")
    private String vnpayReturnUrl;

    @Override
    public String createPaymentUrl(Order order, HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();

        // Thông tin cơ bản
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayTmnCode);

        // Số tiền (VNPay yêu cầu nhân 100)
        long amount = order.getTotalAmount()
                .multiply(new java.math.BigDecimal(100))
                .longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));

        // Thông tin đơn hàng
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderId());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
        vnpParams.put("vnp_IpAddr", getClientIp(request));

        // Thời gian
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Thời gian hết hạn (15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Build query string và hash
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

    @Override
    public boolean validateCallback(Map<String, String> params) {
        try {
            // Lấy và remove secure hash
            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
                return false;
            }

            // Clone params để không ảnh hưởng map gốc
            Map<String, String> clonedParams = new HashMap<>(params);
            clonedParams.remove("vnp_SecureHash");
            clonedParams.remove("vnp_SecureHashType");

            // Tính toán hash
            String signValue = getSignatureData(clonedParams);
            String calculatedHash = hmacSHA512(vnpayHashSecret, signValue);

            return calculatedHash.equals(vnpSecureHash);
        } catch (Exception e) {
            return false;
        }
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

    @Override
    public String getClientIp(HttpServletRequest request) {
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

        return ipAddress;
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
}

