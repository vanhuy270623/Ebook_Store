package stu.datn.ebook_store.dto;

import lombok.Getter;
import lombok.Setter;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;

import java.time.LocalDateTime;

/**
 * DTO wrapper cho user subscription dựa trên Order entity
 * Subscription được lưu trong bảng orders với order_type = 'SUBSCRIPTION'
 */
@Getter
@Setter
public class UserSubscription {

    public enum Status {
        ACTIVE,      // Đang hoạt động
        EXPIRED,     // Đã hết hạn
        CANCELLED,   // Đã hủy
        PENDING      // Chờ thanh toán
    }

    private String userSubscriptionId; // orderId
    private User user;
    private Subscription subscription;
    private Order order;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Order.PaymentStatus paymentStatus;
    private boolean isActive;
    private Status status;
    private LocalDateTime createdAt;

    public UserSubscription(Order order) {
        if (order.getOrderType() != Order.OrderType.SUBSCRIPTION) {
            throw new IllegalArgumentException("Order must be of type SUBSCRIPTION");
        }
        this.userSubscriptionId = order.getOrderId();
        this.user = order.getUser();
        this.subscription = order.getSubscription();
        this.order = order;
        this.startDate = order.getStartDate();
        this.endDate = order.getEndDate();
        this.paymentStatus = order.getPaymentStatus();
        this.createdAt = order.getCreatedAt();

        // Xác định trạng thái
        if (order.getPaymentStatus() == Order.PaymentStatus.CANCELLED) {
            this.status = Status.CANCELLED;
            this.isActive = false;
        } else if (order.getPaymentStatus() == Order.PaymentStatus.PENDING ||
                   order.getPaymentStatus() == Order.PaymentStatus.WAITING_APPROVAL) {
            this.status = Status.PENDING;
            this.isActive = false;
        } else if ((order.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                    order.getPaymentStatus() == Order.PaymentStatus.PAID) &&
                   order.getEndDate() != null &&
                   order.getEndDate().isAfter(LocalDateTime.now())) {
            this.status = Status.ACTIVE;
            this.isActive = true;
        } else if (order.getEndDate() != null &&
                   order.getEndDate().isBefore(LocalDateTime.now())) {
            this.status = Status.EXPIRED;
            this.isActive = false;
        } else {
            this.status = Status.PENDING;
            this.isActive = false;
        }
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean isPending() {
        return paymentStatus == Order.PaymentStatus.PENDING ||
               paymentStatus == Order.PaymentStatus.WAITING_APPROVAL;
    }
}

