package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.repository.OrderItemRepository;
import stu.datn.ebook_store.repository.OrderRepository;
import stu.datn.ebook_store.service.OrderItemService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        // Use JOIN FETCH query to eagerly load Book and Authors
        return orderItemRepository.findByOrderIdWithBook(orderId);
    }

    @Override
    public OrderItem saveOrderItem(OrderItem orderItem) {
        return createOrderItem(orderItem);
    }

    /**
     * PRIVATE HELPER - Tạo order item
     * Được gọi từ saveOrderItem
     */
    private OrderItem createOrderItem(OrderItem orderItem) {
        if (orderItem.getOrderItemId() == null || orderItem.getOrderItemId().isEmpty()) {
            orderItem.setOrderItemId(generateOrderItemId());
        }
        return orderItemRepository.save(orderItem);
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

    @Override
    @Transactional(readOnly = true)
    public List<String> getPurchasedBookIds(String userId,
                                            Order.OrderType orderType,
                                            Collection<Order.PaymentStatus> paymentStatuses,
                                            Collection<Book.AccessType> accessTypes) {
        return orderItemRepository.findPurchasedBookIds(userId, orderType, paymentStatuses, accessTypes);
    }

    // Private helper methods

    /**
     * Generate order item ID with sequential numbering
     * Format: item_XX
     */
    private String generateOrderItemId() {
        long count = orderItemRepository.count();
        // Format with 2 digits: 01, 02, 03, etc.
        return "item_" + String.format("%02d", count + 1);
    }
}
