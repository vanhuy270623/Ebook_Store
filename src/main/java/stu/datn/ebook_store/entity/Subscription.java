package stu.datn.ebook_store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @Column(name = "subscription_id", length = 50)
    private String subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_name", nullable = false)
    private PackageName packageName;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "features", columnDefinition = "JSON")
    private String features;

    @Column(name = "max_devices")
    private Integer maxDevices = 1;

    @Column(name = "has_ads")
    private Boolean hasAds = true;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum PackageName {
        FREE, BASIC, PREMIUM, VIP
    }
}

