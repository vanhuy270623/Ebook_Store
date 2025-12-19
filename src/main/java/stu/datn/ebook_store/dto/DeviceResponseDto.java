package stu.datn.ebook_store.dto;

import lombok.Data;
import stu.datn.ebook_store.entity.UserDevice;
import java.time.LocalDateTime;

/**
 * DTO để trả về thông tin device cho client
 */
@Data
public class DeviceResponseDto {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String browserName;
    private String osName;
    private String ipAddress;
    private Boolean isTrusted;
    private Boolean isActive;
    private Boolean isCurrentDevice;
    private Integer trustScore;
    private Integer loginCount;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private Long daysSinceLastLogin;

    public static DeviceResponseDto fromEntity(UserDevice device, boolean isCurrentDevice) {
        DeviceResponseDto dto = new DeviceResponseDto();
        dto.setDeviceId(device.getDeviceId());
        dto.setDeviceName(device.getDeviceName());
        dto.setDeviceType(device.getDeviceType() != null ? device.getDeviceType().name() : "WEB");
        dto.setBrowserName(device.getBrowserName());
        dto.setOsName(device.getOsName());
        dto.setIpAddress(device.getIpAddress());
        dto.setIsTrusted(device.getIsTrusted());
        dto.setIsActive(device.getIsActive());
        dto.setIsCurrentDevice(isCurrentDevice);
        dto.setTrustScore(device.getTrustScore());
        dto.setLoginCount(device.getLoginCount());
        dto.setLastLogin(device.getLastLogin());
        dto.setCreatedAt(device.getCreatedAt());

        if (device.getLastLogin() != null) {
            long days = java.time.Duration.between(device.getLastLogin(), LocalDateTime.now()).toDays();
            dto.setDaysSinceLastLogin(days);
        }

        return dto;
    }
}

