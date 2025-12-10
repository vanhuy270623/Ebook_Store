package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.OrderRepository;
import stu.datn.ebook_store.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public Order saveOrder(Order order) {
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId(generateOrderId(order.getOrderType()));
        }
        return orderRepository.save(order);
    }

    @Override
    public void deleteOrder(String orderId) {
        orderRepository.deleteById(orderId);
    }

    @Override
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }

    @Override
    public List<Order> getOrdersByUserSortedByDate(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public Optional<Order> getOrderByTransactionId(String transactionId) {
        return orderRepository.findByTransactionId(transactionId);
    }

    @Override
    public List<Order> getOrdersByPaymentStatus(Order.PaymentStatus paymentStatus) {
        return orderRepository.findByPaymentStatus(paymentStatus);
    }

    @Override
    public List<Order> getOrdersByOrderType(Order.OrderType orderType) {
        return orderRepository.findByOrderType(orderType);
    }

    @Override
    public List<Order> getOrdersByUserAndOrderType(User user, Order.OrderType orderType) {
        return orderRepository.findByUserAndOrderType(user, orderType);
    }

    @Override
    public List<Order> getOrdersByUserAndPaymentStatus(User user, Order.PaymentStatus paymentStatus) {
        return orderRepository.findByUserAndPaymentStatus(user, paymentStatus);
    }

    @Override
    public List<Order> getOrdersBySubscription(Subscription subscription) {
        return orderRepository.findBySubscription(subscription);
    }

    @Override
    public List<Order> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersBetweenDates(startDate, endDate);
    }

    @Override
    public Double getTotalRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderRepository.getTotalRevenueBetweenDates(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public long countOrdersByPaymentStatus(Order.PaymentStatus paymentStatus) {
        return orderRepository.countByPaymentStatus(paymentStatus);
    }

    @Override
    public void updateOrderStatus(String orderId, Order.PaymentStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setPaymentStatus(status);
            orderRepository.save(order);
        }
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findAll().stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public long getTotalOrdersCount() {
        return orderRepository.count();
    }

    @Override
    public Double getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .mapToDouble(order -> order.getTotalAmount().doubleValue())
                .sum();
    }

    @Override
    public long getPendingOrdersCount() {
        return countOrdersByPaymentStatus(Order.PaymentStatus.PENDING);
    }

    @Override
    public long getCompletedOrdersCount() {
        return countOrdersByPaymentStatus(Order.PaymentStatus.COMPLETED);
    }

    @Override
    public long getFailedOrdersCount() {
        return countOrdersByPaymentStatus(Order.PaymentStatus.FAILED);
    }

    @Override
    public long getCancelledOrdersCount() {
        return countOrdersByPaymentStatus(Order.PaymentStatus.CANCELLED);
    }

    @Override
    public Double getRevenueByOrderType(Order.OrderType orderType) {
        return orderRepository.findByOrderType(orderType).stream()
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .mapToDouble(order -> order.getTotalAmount().doubleValue())
                .sum();
    }

    @Override
    public List<Object[]> getMonthlyRevenue(int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Order> orders = orderRepository.findOrdersBetweenDates(startDate, LocalDateTime.now());

        // Group by month and sum revenue
        return orders.stream()
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .collect(java.util.stream.Collectors.groupingBy(
                        order -> order.getCreatedAt().getYear() + "-" +
                                String.format("%02d", order.getCreatedAt().getMonthValue()),
                        java.util.stream.Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ))
                .entrySet().stream()
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .sorted((a, b) -> ((String)a[0]).compareTo((String)b[0]))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Double getTodayRevenue() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return getTotalRevenueBetweenDates(startOfDay, endOfDay);
    }

    @Override
    public Double getThisMonthRevenue() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();
        return getTotalRevenueBetweenDates(startOfMonth, now);
    }

    @Override
    public long getTodayOrdersCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return orderRepository.findOrdersBetweenDates(startOfDay, endOfDay).size();
    }

    @Override
    public List<Order> getOrdersByUserIdAndType(String userId, Order.OrderType orderType) {
        return orderRepository.findByUser_UserIdAndOrderTypeOrderByCreatedAtDesc(userId, orderType);
    }

    @Override
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Generate order ID based on order type
     * Format: order_book_XX for book orders, order_sub_XX for subscription orders
     */
    private String generateOrderId(Order.OrderType orderType) {
        String prefix;
        long typeCount;

        if (orderType == Order.OrderType.SUBSCRIPTION) {
            prefix = "order_sub_";
            typeCount = orderRepository.countByOrderType(Order.OrderType.SUBSCRIPTION);
        } else {
            prefix = "order_book_";
            typeCount = orderRepository.countByOrderType(Order.OrderType.BOOK);
        }

        // Format with 2 digits: 01, 02, 03, etc.
        return prefix + String.format("%02d", typeCount + 1);
    }
}

