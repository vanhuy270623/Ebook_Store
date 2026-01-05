package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing order items
 * REFACTORED: Removed 4 unused methods, createOrderItem→private helper
 */
public interface OrderItemService {

    /**
     * Get all order items for a specific order
     * @param orderId The order ID
     * @return List of order items
     */
    List<OrderItem> getOrderItemsByOrderId(String orderId);

    /**
     * Save an order item
     * @param orderItem The order item to save
     * @return The saved order item
     */
    OrderItem saveOrderItem(OrderItem orderItem);

    /**
     * Check if a user has purchased a book
     * @param userId The user ID
     * @param bookId The book ID
     * @return true if the user has purchased the book
     */
    boolean hasUserPurchasedBook(String userId, String bookId);

    /**
     * Get total sales count for a book - GIỮ LẠI cho statistics future
     * @param bookId The book ID
     * @return Total number of sales
     */
    long getBookSalesCount(String bookId);

    /**
     * Get a list of purchased book IDs by a user with specific order type, payment statuses, and access types
     * @param userId The user ID
     * @param orderType The order type
     * @param paymentStatuses The collection of payment statuses
     * @param accessTypes The collection of access types
     * @return List of purchased book IDs
     */
    List<String> getPurchasedBookIds(String userId,
                                     Order.OrderType orderType,
                                     Collection<Order.PaymentStatus> paymentStatuses,
                                     Collection<Book.AccessType> accessTypes);
}
