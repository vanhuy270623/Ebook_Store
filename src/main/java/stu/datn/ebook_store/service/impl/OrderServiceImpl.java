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
            order.setOrderId(generateOrderId());
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

    private String generateOrderId() {
        long count = orderRepository.count();
        return "order_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

