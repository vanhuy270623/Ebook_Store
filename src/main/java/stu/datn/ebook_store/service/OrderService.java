package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Cart;
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

    // Dashboard statistics methods
    List<Order> getRecentOrders(int limit);
    long getTotalOrdersCount();
    Double getTotalRevenue();
    long getPendingOrdersCount();

    // Additional dashboard statistics
    long getCompletedOrdersCount();
    long getFailedOrdersCount();
    long getCancelledOrdersCount();
    Double getRevenueByOrderType(Order.OrderType orderType);
    List<Object[]> getMonthlyRevenue(int months);
    Double getTodayRevenue();
    Double getThisMonthRevenue();
    long getTodayOrdersCount();

    // Subscription management methods
    List<Order> getOrdersByUserIdAndType(String userId, Order.OrderType orderType);
    Order updateOrder(Order order);

    // ========== Business Logic Methods ==========

    /**
     * Tạo order từ giỏ hàng
     * @param user User tạo order
     * @param cart Cart chứa items
     * @param paymentMethod Phương thức thanh toán
     * @return Order đã được tạo
     * @throws RuntimeException nếu validation thất bại
     */
    Order createOrderFromCart(User user, Cart cart, Order.PaymentMethod paymentMethod);

    /**
     * Hủy đơn hàng
     * @param orderId ID đơn hàng
     * @param user User hủy (để check quyền)
     * @return true nếu hủy thành công
     * @throws RuntimeException nếu không thể hủy
     */
    boolean cancelOrder(String orderId, User user);

    /**
     * Kiểm tra user có quyền xem order không
     * @param order Order cần kiểm tra
     * @param user User hiện tại
     * @return true nếu có quyền
     */
    boolean canUserAccessOrder(Order order, User user);
}
