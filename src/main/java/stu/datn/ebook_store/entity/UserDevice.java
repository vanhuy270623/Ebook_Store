package stu.datn.ebook_store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_devices")
public class UserDevice {
    @Id
    @Column(name = "device_id", length = 50)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType = DeviceType.WEB;

    @Column(name = "device_token", unique = true, length = 500)
    private String deviceToken;

    // Device Fingerprinting & Security
    @Column(name = "device_fingerprint", length = 64)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "browser_name", length = 100)
    private String browserName;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Column(name = "is_trusted")
    private Boolean isTrusted = false;

    @Column(name = "trust_score")
    private Integer trustScore = 100;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Tracking
    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "suspicious_login_count")
    private Integer suspiciousLoginCount = 0;

    @Column(name = "last_ip", length = 45)
    private String lastIp;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (loginCount == null) loginCount = 0;
        if (suspiciousLoginCount == null) suspiciousLoginCount = 0;
        if (trustScore == null) trustScore = 100;
        if (isTrusted == null) isTrusted = false;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void incrementLoginCount() {
        this.loginCount++;
    }

    public void incrementSuspiciousLoginCount() {
        this.suspiciousLoginCount++;
        this.trustScore = Math.max(0, this.trustScore - 10);
    }

    public void updateTrustScore(int change) {
        this.trustScore = Math.max(0, Math.min(100, this.trustScore + change));
    }

    public enum DeviceType {
        WEB, MOBILE, TABLET, DESKTOP
    }
}

