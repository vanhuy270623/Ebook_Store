package stu.datn.ebook_store.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO để nhận thông tin device từ client khi login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoDto {
    private String deviceFingerprint;  // SHA256 hash từ client
    private String deviceName;         // Tên thiết bị (VD: "Chrome on Windows")
    private String deviceType;         // WEB, MOBILE, TABLET, DESKTOP
    private String userAgent;          // Browser User-Agent
    private String browserName;        // Chrome, Firefox, Safari...
    private String browserVersion;
    private String osName;             // Windows, Mac, Linux, Android, iOS
    private String osVersion;
    private String screenResolution;
    private String timezone;
    private String language;
}

