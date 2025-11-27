package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Subscription package response (Admin view)
 * Mapped from table: subscriptions
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String subscriptionId;
    private String packageName;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private String features;      // JSON string from DB
    private Integer maxDevices;
    private Boolean hasAds;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private Integer totalSubscribers; // Calculated field - số người đăng ký

    // Constructor from Entity
    public SubscriptionResponse(Subscription subscription) {
        this.subscriptionId = subscription.getSubscriptionId();
        this.packageName = subscription.getPackageName() != null ?
            subscription.getPackageName().name() : null;
        this.description = subscription.getDescription();
        this.price = subscription.getPrice();
        this.durationDays = subscription.getDurationDays();
        this.features = subscription.getFeatures();
        this.maxDevices = subscription.getMaxDevices();
        this.hasAds = subscription.getHasAds();
        this.isActive = subscription.getIsActive();
        this.displayOrder = subscription.getDisplayOrder();
        this.createdAt = subscription.getCreatedAt();
    }

    // Constructor with subscriber count
    public SubscriptionResponse(Subscription subscription, Integer totalSubscribers) {
        this(subscription);
        this.totalSubscribers = totalSubscribers;
    }
}

