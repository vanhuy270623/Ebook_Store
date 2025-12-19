package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Cart;
import stu.datn.ebook_store.entity.CartItem;
import stu.datn.ebook_store.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý Cart và business logic liên quan
 */
public interface CartService {

    // ========== CRUD Operations ==========
    List<Cart> getAllCarts();
    Optional<Cart> getCartById(String cartId);
    Optional<Cart> getCartByUser(User user);
    Cart saveCart(Cart cart);
    void deleteCart(String cartId);
    Cart createCartForUser(User user);

    // ========== Business Logic Methods ==========

    /**
     * Validate cart có hợp lệ để checkout không
     * @param cart Cart cần validate
     * @param user User owner
     * @return true nếu cart hợp lệ
     */
    boolean isCartValidForCheckout(Cart cart, User user);

    /**
     * Lấy danh sách lỗi validation (nếu có)
     * @param cart Cart cần validate
     * @param user User owner
     * @return List các thông báo lỗi
     */
    List<String> getCartValidationErrors(Cart cart, User user);

    /**
     * Tính tổng tiền giỏ hàng
     * @param cart Cart cần tính
     * @return Tổng tiền
     */
    BigDecimal calculateCartTotal(Cart cart);

    /**
     * Kiểm tra sách trong cart đã mua chưa (duplicate)
     * @param cart Cart cần kiểm tra
     * @param user User owner
     * @return List tên sách đã mua
     */
    List<String> findDuplicateBookTitles(Cart cart, User user);

    /**
     * Xóa toàn bộ items trong giỏ hàng
     * @param cart Cart cần xóa
     */
    void clearCart(Cart cart);

    /**
     * Xóa giỏ hàng của user
     * @param user User owner
     */
    void clearCartForUser(User user);
}

