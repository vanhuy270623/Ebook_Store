package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> getAllOrders();
    Optional<Order> getOrderById(String orderId);
    Order saveOrder(Order order);
    void deleteOrder(String orderId);
    List<Order> getOrdersByUser(User user);
    List<Order> getOrdersByUserSortedByDate(User user);
    Optional<Order> getOrderByTransactionId(String transactionId);
    List<Order> getOrdersByPaymentStatus(Order.PaymentStatus paymentStatus);
    List<Order> getOrdersByOrderType(Order.OrderType orderType);
    List<Order> getOrdersByUserAndOrderType(User user, Order.OrderType orderType);
    List<Order> getOrdersByUserAndPaymentStatus(User user, Order.PaymentStatus paymentStatus);
    List<Order> getOrdersBySubscription(Subscription subscription);
    List<Order> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate);
    Double getTotalRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate);
    long countOrdersByPaymentStatus(Order.PaymentStatus paymentStatus);
    void updateOrderStatus(String orderId, Order.PaymentStatus status);
}

