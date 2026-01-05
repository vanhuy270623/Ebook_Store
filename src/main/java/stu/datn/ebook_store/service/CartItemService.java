package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.CartItem;
import stu.datn.ebook_store.entity.Cart;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.CartItemId;

import java.util.List;
import java.util.Optional;

/**
 * Cart Item Service Interface
 * REFACTORED: Removed 3 unused methods
 */
public interface CartItemService {
    Optional<CartItem> getCartItemById(CartItemId cartItemId);
    CartItem saveCartItem(CartItem cartItem);
    void deleteCartItem(CartItemId cartItemId);
    List<CartItem> getCartItemsByCart(Cart cart);
    Optional<CartItem> getCartItemByCartAndBook(Cart cart, Book book); // GIỮ LẠI - validation duplicate
    void clearCart(Cart cart);
}

