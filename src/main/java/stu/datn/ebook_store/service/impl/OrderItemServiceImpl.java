package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.repository.OrderItemRepository;
import stu.datn.ebook_store.repository.OrderRepository;
import stu.datn.ebook_store.service.OrderItemService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderItemServiceImpl(OrderItemRepository orderItemRepository,
                                OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByOrderId(String orderId) {
        return orderItemRepository.findByOrder_OrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderItem> getOrderItemById(String orderItemId) {
        return orderItemRepository.findById(orderItemId);
    }

    @Override
    public OrderItem createOrderItem(OrderItem orderItem) {
        if (orderItem.getOrderItemId() == null || orderItem.getOrderItemId().isEmpty()) {
            orderItem.setOrderItemId(generateOrderItemId());
        }
        return orderItemRepository.save(orderItem);
    }

    @Override
    public void deleteOrderItem(String orderItemId) {
        orderItemRepository.deleteById(orderItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByBookId(String bookId) {
        return orderItemRepository.findByBook_BookId(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPurchasedBook(String userId, String bookId) {
        // Get all completed orders for the user
        List<Order> userOrders = orderRepository.findByUser_UserIdAndPaymentStatus(userId, Order.PaymentStatus.COMPLETED);

        // Check if any order contains the book
        for (Order order : userOrders) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
            for (OrderItem item : orderItems) {
                if (item.getBook().getBookId().equals(bookId)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public long getBookSalesCount(String bookId) {
        List<OrderItem> orderItems = orderItemRepository.findByBook_BookId(bookId);

        // Count only items from completed orders
        return orderItems.stream()
                .filter(item -> item.getOrder().getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .count();
    }

    // Private helper methods

    private String generateOrderItemId() {
        return "OI" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
    }
}

