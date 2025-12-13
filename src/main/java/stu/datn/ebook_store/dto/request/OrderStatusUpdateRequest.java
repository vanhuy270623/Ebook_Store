package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Order;

/**
 * DTO for updating order payment status (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {

    @NotBlank(message = "ID đơn hàng không được để trống")
    private String orderId;

    private Order.PaymentStatus paymentStatus;

    private String adminNote; // Ghi chú từ admin
}

