package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for Order detail with items (Admin view)
 * Supports both BOOK purchase and SUBSCRIPTION orders
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private String orderId;
    private String userId;
    private String username;
    private String userEmail;
    private String userPhone;

    // Order type: BOOK (mua sách lẻ) or SUBSCRIPTION (đăng ký gói)
    private Order.OrderType orderType;

    private BigDecimal totalAmount;
    private Order.PaymentStatus paymentStatus;
    private Order.PaymentMethod paymentMethod;
    private String transactionId;

    // For SUBSCRIPTION orders only
    private String subscriptionId;
    private String packageName; // FREE, BASIC, PREMIUM, VIP
    private BigDecimal subscriptionPrice;
    private Integer durationDays;

    // Dates (for SUBSCRIPTION: activation period, for BOOK: null)
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    // For BOOK orders only (null for SUBSCRIPTION)
    private List<OrderItemInfo> items;

    // Inner class for order item info
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private String bookId;
        private String bookTitle;
        private String coverImageUrl;
        private BigDecimal priceAtPurchase;
        private Integer quantity; // Always 1 for ebooks
        private BigDecimal subtotal; // Same as priceAtPurchase for ebooks
    }

    // Constructor from Entity
    public OrderDetailResponse(Order order, List<OrderItem> orderItems) {
        this.orderId = order.getOrderId();
        this.orderType = order.getOrderType();
        this.totalAmount = order.getTotalAmount();
        this.paymentStatus = order.getPaymentStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.transactionId = order.getTransactionId();
        this.startDate = order.getStartDate();
        this.endDate = order.getEndDate();
        this.createdAt = order.getCreatedAt();

        // User info
        if (order.getUser() != null) {
            this.userId = order.getUser().getUserId();
            this.username = order.getUser().getUsername();
            this.userEmail = order.getUser().getEmail();
            this.userPhone = order.getUser().getPhone();
        }

        // Subscription info (for SUBSCRIPTION orders)
        if (order.getOrderType() == Order.OrderType.SUBSCRIPTION && order.getSubscription() != null) {
            this.subscriptionId = order.getSubscription().getSubscriptionId();
            this.packageName = order.getSubscription().getPackageName().name();
            this.subscriptionPrice = order.getSubscription().getPrice();
            this.durationDays = order.getSubscription().getDurationDays();
        }

        // Order items (for BOOK orders)
        if (order.getOrderType() == Order.OrderType.BOOK && orderItems != null) {
            this.items = orderItems.stream()
                .map(item -> new OrderItemInfo(
                    item.getBook().getBookId(),
                    item.getBook().getTitle(),
                    item.getBook().getCoverImageUrl(),
                    item.getPriceAtPurchase(),
                    1, // Ebooks quantity is always 1
                    item.getPriceAtPurchase() // Subtotal = price for single item
                ))
                .collect(Collectors.toList());
        }
    }
}

