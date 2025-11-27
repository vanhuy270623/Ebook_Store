package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Order response (Admin view)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private String userId;
    private String username;
    private String userEmail;
    private Order.OrderType orderType;
    private BigDecimal totalAmount;
    private Order.PaymentStatus paymentStatus;
    private Order.PaymentMethod paymentMethod;
    private String transactionId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    // Constructor from Entity
    public OrderResponse(Order order) {
        this.orderId = order.getOrderId();
        this.orderType = order.getOrderType();
        this.totalAmount = order.getTotalAmount();
        this.paymentStatus = order.getPaymentStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.transactionId = order.getTransactionId();
        this.startDate = order.getStartDate();
        this.endDate = order.getEndDate();
        this.createdAt = order.getCreatedAt();

        if (order.getUser() != null) {
            this.userId = order.getUser().getUserId();
            this.username = order.getUser().getUsername();
            this.userEmail = order.getUser().getEmail();
        }
    }
}

