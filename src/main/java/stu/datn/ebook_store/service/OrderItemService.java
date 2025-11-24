package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.OrderItem;

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
}

