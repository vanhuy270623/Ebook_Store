package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.CartItem;
import stu.datn.ebook_store.entity.Cart;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.CartItemId;
import stu.datn.ebook_store.repository.CartItemRepository;
import stu.datn.ebook_store.service.CartItemService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;

    @Autowired
    public CartItemServiceImpl(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public List<CartItem> getAllCartItems() {
        return cartItemRepository.findAll();
    }

    @Override
    public Optional<CartItem> getCartItemById(CartItemId cartItemId) {
        return cartItemRepository.findById(cartItemId);
    }

    @Override
    public CartItem saveCartItem(CartItem cartItem) {
        return cartItemRepository.save(cartItem);
    }

    @Override
    public void deleteCartItem(CartItemId cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    public List<CartItem> getCartItemsByCart(Cart cart) {
        return cartItemRepository.findByCart(cart);
    }

    @Override
    public Optional<CartItem> getCartItemByCartAndBook(Cart cart, Book book) {
        return cartItemRepository.findByCartAndBook(cart, book);
    }

    @Override
    public long countCartItemsByCart(Cart cart) {
        return cartItemRepository.countByCart(cart);
    }

    @Override
    public void deleteCartItemByCartAndBook(Cart cart, Book book) {
        cartItemRepository.deleteByCartAndBook(cart, book);
    }

    @Override
    public void clearCart(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        cartItemRepository.deleteAll(cartItems);
    }
}

