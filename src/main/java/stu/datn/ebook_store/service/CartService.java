package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Cart;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface CartService {
    List<Cart> getAllCarts();
    Optional<Cart> getCartById(String cartId);
    Optional<Cart> getCartByUser(User user);
    Cart saveCart(Cart cart);
    void deleteCart(String cartId);
    Cart createCartForUser(User user);
}

