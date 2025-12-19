package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.*;
import stu.datn.ebook_store.repository.CartRepository;
import stu.datn.ebook_store.service.CartItemService;
import stu.datn.ebook_store.service.CartService;
import stu.datn.ebook_store.service.OrderItemService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    private final CartRepository cartRepository;
    private final CartItemService cartItemService;
    private final OrderItemService orderItemService;

    @Autowired
    public CartServiceImpl(CartRepository cartRepository,
                          CartItemService cartItemService,
                          OrderItemService orderItemService) {
        this.cartRepository = cartRepository;
        this.cartItemService = cartItemService;
        this.orderItemService = orderItemService;
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

    @Override
    public boolean isCartValidForCheckout(Cart cart, User user) {
        List<String> errors = getCartValidationErrors(cart, user);
        return errors.isEmpty();
    }

    @Override
    public List<String> getCartValidationErrors(Cart cart, User user) {
        List<String> errors = new ArrayList<>();

        // Kiểm tra cart trống
        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
        if (cartItems.isEmpty()) {
            errors.add("Giỏ hàng trống");
            return errors;
        }

        // Kiểm tra sách đã mua
        List<String> duplicateTitles = findDuplicateBookTitles(cart, user);
        if (!duplicateTitles.isEmpty()) {
            errors.add("Bạn đã sở hữu: " + String.join(", ", duplicateTitles));
        }

        return errors;
    }

    @Override
    public BigDecimal calculateCartTotal(Cart cart) {
        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
        return cartItems.stream()
                .map(item -> {
                    BigDecimal price = item.getBook().getPrice();
                    return price != null ? price : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<String> findDuplicateBookTitles(Cart cart, User user) {
        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);

        // Lấy danh sách sách đã mua
        Set<String> purchasedBookIds = new HashSet<>(
                orderItemService.getPurchasedBookIds(
                        user.getUserId(),
                        Order.OrderType.BOOK,
                        PAID_STATUSES,
                        RETAIL_ACCESS_TYPES
                )
        );

        // Tìm sách trùng
        return cartItems.stream()
                .filter(item -> purchasedBookIds.contains(item.getBook().getBookId()))
                .map(item -> item.getBook().getTitle())
                .collect(Collectors.toList());
    }

    @Override
    public void clearCart(Cart cart) {
        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
        for (CartItem item : cartItems) {
            CartItemId id = new CartItemId(cart.getCartId(), item.getBook().getBookId());
            cartItemService.deleteCartItem(id);
        }
    }

    @Override
    public void clearCartForUser(User user) {
        Optional<Cart> cartOpt = getCartByUser(user);
        cartOpt.ifPresent(this::clearCart);
    }

    private String generateCartId() {
        long count = cartRepository.count();
        return "cart_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}
