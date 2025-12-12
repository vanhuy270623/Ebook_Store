package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing order items
 */
public interface OrderItemService {

    /**
     * Get all order items for a specific order
     * @param orderId The order ID
     * @return List of order items
     */
    List<OrderItem> getOrderItemsByOrderId(String orderId);

    /**
     * Get all order items for a specific order by Order entity
     * @param order The order entity
     * @return List of order items
     */
    List<OrderItem> getOrderItemsByOrder(Order order);

    /**
     * Get an order item by ID
     * @param orderItemId The order item ID
     * @return Optional containing the order item if found
     */
    Optional<OrderItem> getOrderItemById(String orderItemId);

    /**
     * Create a new order item
     * @param orderItem The order item to create
     * @return The created order item
     */
    OrderItem createOrderItem(OrderItem orderItem);

    /**
     * Save an order item (alias for createOrderItem)
     * @param orderItem The order item to save
     * @return The saved order item
     */
    OrderItem saveOrderItem(OrderItem orderItem);

    /**
     * Delete an order item
     * @param orderItemId The order item ID
     */
    void deleteOrderItem(String orderItemId);

    /**
     * Get order items by book ID
     * @param bookId The book ID
     * @return List of order items
     */
    List<OrderItem> getOrderItemsByBookId(String bookId);

    /**
     * Check if a user has purchased a book
     * @param userId The user ID
     * @param bookId The book ID
     * @return true if the user has purchased the book
     */
    boolean hasUserPurchasedBook(String userId, String bookId);

    /**
     * Get total sales count for a book
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
