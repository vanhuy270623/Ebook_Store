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

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum DeviceType {
        WEB, MOBILE, TABLET, DESKTOP
    }
}

