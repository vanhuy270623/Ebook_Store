package stu.datn.ebook_store.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class để xử lý Device Fingerprinting và IP detection
 */
@Component
public class DeviceFingerprintUtil {

    /**
     * Lấy IP address thực của client (xử lý proxy, load balancer)
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy cái đầu tiên
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Tạo device fingerprint từ server-side (fallback nếu client không gửi)
     */
    public static String generateServerSideFingerprint(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        // IP Address
        String ip = getClientIpAddress(request);
        sb.append(ip != null ? ip : "unknown").append("|");

        // User-Agent
        String userAgent = request.getHeader("User-Agent");
        sb.append(userAgent != null ? userAgent : "unknown").append("|");

        // Accept-Language
        String language = request.getHeader("Accept-Language");
        sb.append(language != null ? language : "unknown").append("|");

        // Accept-Encoding
        String encoding = request.getHeader("Accept-Encoding");
        sb.append(encoding != null ? encoding : "unknown");

        return sha256Hash(sb.toString());
    }

    /**
     * Tạo SHA256 hash
     */
    public static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Parse browser name từ User-Agent
     */
    public static String parseBrowserName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("edg/") || userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("chrome") && !userAgent.contains("edg")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr/")) {
            return "Opera";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "Internet Explorer";
        } else {
            return "Other";
        }
    }

    /**
     * Parse OS name từ User-Agent
     */
    public static String parseOsName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac os x") || userAgent.contains("macintosh")) {
            return "MacOS";
        } else if (userAgent.contains("linux") && !userAgent.contains("android")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        } else {
            return "Other";
        }
    }

    /**
     * Determine device type từ User-Agent
     */
    public static String determineDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "WEB";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("mobile") || userAgent.contains("iphone") ||
            userAgent.contains("android") && userAgent.contains("mobile")) {
            return "MOBILE";
        } else if (userAgent.contains("ipad") || userAgent.contains("tablet")) {
            return "TABLET";
        } else if (userAgent.contains("electron") || userAgent.contains("desktop")) {
            return "DESKTOP";
        } else {
            return "WEB";
        }
    }

    /**
     * Kiểm tra xem 2 IP có phải từ cùng subnet không (đơn giản hóa)
     */
    public static boolean isSameSubnet(String ip1, String ip2) {
        if (ip1 == null || ip2 == null) {
            return false;
        }

        // Đơn giản: so sánh 3 octet đầu của IPv4
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");

        if (parts1.length >= 3 && parts2.length >= 3) {
            return parts1[0].equals(parts2[0]) &&
                   parts1[1].equals(parts2[1]) &&
                   parts1[2].equals(parts2[2]);
        }

        return ip1.equals(ip2);
    }

    /**
     * Generate unique device ID
     */
    public static String generateDeviceId(String userId, String fingerprint) {
        String input = userId + "_" + fingerprint + "_" + System.currentTimeMillis();
        return "dev_" + sha256Hash(input).substring(0, 32);
    }
}

