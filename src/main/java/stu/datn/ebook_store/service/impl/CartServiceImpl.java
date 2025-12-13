package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Cart;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.CartRepository;
import stu.datn.ebook_store.service.CartService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Autowired
    public CartServiceImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @Override
    public Optional<Cart> getCartById(String cartId) {
        return cartRepository.findById(cartId);
    }

    @Override
    public Optional<Cart> getCartByUser(User user) {
        return cartRepository.findByUser(user);
    }

    @Override
    public Cart saveCart(Cart cart) {
        if (cart.getCartId() == null || cart.getCartId().isEmpty()) {
            cart.setCartId(generateCartId());
        }
        return cartRepository.save(cart);
    }

    @Override
    public void deleteCart(String cartId) {
        cartRepository.deleteById(cartId);
    }

    @Override
    public Cart createCartForUser(User user) {
        // Check if cart already exists for user
        Optional<Cart> existingCart = cartRepository.findByUser(user);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        // Create new cart
        Cart cart = new Cart();
        cart.setCartId(generateCartId());
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private String generateCartId() {
        long count = cartRepository.count();
        return "cart_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

