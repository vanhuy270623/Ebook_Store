package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.*;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.CartItemService;
import stu.datn.ebook_store.service.CartService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller xử lý giỏ hàng
 * Endpoints: /cart/*
 * Note: CartItem sử dụng composite key (Cart, Book) không có quantity
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final CartItemService cartItemService;
    private final BookService bookService;

    @Autowired
    public CartController(CartService cartService, CartItemService cartItemService, BookService bookService) {
        this.cartService = cartService;
        this.cartItemService = cartItemService;
        this.bookService = bookService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Xem giỏ hàng
     */
    @GetMapping
    public String viewCart(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        List<CartItem> cartItems = new ArrayList<>();
        BigDecimal cartTotal = BigDecimal.ZERO;

        if (currentUser != null) {
            Optional<Cart> cartOpt = cartService.getCartByUser(currentUser);
            if (cartOpt.isPresent()) {
                Cart cart = cartOpt.get();
                cartItems = cartItemService.getCartItemsByCart(cart);

                // Calculate total
                cartTotal = cartItems.stream()
                        .map(item -> item.getBook().getPrice() != null ? item.getBook().getPrice() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartItemCount", cartItems.size());

        return "user/cart/view";
    }

    /**
     * Thêm sách vào giỏ hàng
     */
    @PostMapping("/add/{bookId}")
    public String addToCart(
            @PathVariable String bookId,
            @RequestParam(value = "redirect", required = false) String redirectUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);

            // Kiểm tra sách tồn tại
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

            // Kiểm tra sách có thể mua được không
            if (book.getAccessType() == Book.AccessType.FREE) {
                redirectAttributes.addFlashAttribute("error", "Sách này không thể thêm vào giỏ (sách miễn phí)");
                return getRedirectPath(redirectUrl, bookId);
            }

            // Lấy hoặc tạo giỏ hàng
            Cart cart = cartService.getCartByUser(currentUser)
                    .orElseGet(() -> cartService.createCartForUser(currentUser));

            // Kiểm tra sách đã có trong giỏ chưa
            CartItemId cartItemId = new CartItemId(cart.getCartId(), bookId);
            Optional<CartItem> existingItem = cartItemService.getCartItemById(cartItemId);

            if (existingItem.isPresent()) {
                redirectAttributes.addFlashAttribute("info", "Sách này đã có trong giỏ hàng");
                return getRedirectPath(redirectUrl, bookId);
            }

            // Tạo CartItem mới (composite key: cart + book)
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBook(book);
            cartItemService.saveCartItem(newItem);

            redirectAttributes.addFlashAttribute("success", "Đã thêm \"" + book.getTitle() + "\" vào giỏ hàng");
            redirectAttributes.addFlashAttribute("cartUpdated", true); // Flag để JS biết cần cập nhật

            return getRedirectPath(redirectUrl, bookId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return getRedirectPath(redirectUrl, bookId);
        }
    }

    /**
     * Helper method để xác định trang redirect
     */
    private String getRedirectPath(String redirectUrl, String bookId) {
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/books/view/" + bookId;
    }

    /**
     * Xóa sách khỏi giỏ hàng
     */
    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam String cartItemId,
            RedirectAttributes redirectAttributes) {

        try {
            // CartItemId là composite key, cần parse từ format
            // Định dạng: cart_id:book_id
            String[] parts = cartItemId.split(":");
            if (parts.length == 2) {
                String cartId = parts[0];
                String bookId = parts[1];

                Optional<Cart> cartOpt = cartService.getCartById(cartId);
                if (cartOpt.isPresent()) {
                    Optional<Book> bookOpt = bookService.getBookById(bookId);
                    if (bookOpt.isPresent()) {
                        CartItemId id = new CartItemId(cartId, bookId);
                        cartItemService.deleteCartItem(id);
                        redirectAttributes.addFlashAttribute("success", "Xóa sách khỏi giỏ thành công");
                    }
                }
            }

            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    @PostMapping("/clear")
    public String clearCart(
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
            Optional<Cart> cartOpt = cartService.getCartByUser(currentUser);

            if (cartOpt.isPresent()) {
                Cart cart = cartOpt.get();
                List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);

                // Xóa từng item
                for (CartItem item : cartItems) {
                    CartItemId id = new CartItemId(cart.getCartId(), item.getBook().getBookId());
                    cartItemService.deleteCartItem(id);
                }

                redirectAttributes.addFlashAttribute("success", "Đã xóa toàn bộ giỏ hàng");
            }

            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * REST API: Lấy số lượng sách trong giỏ
     * Trả về JSON cho AJAX request
     */
    @GetMapping("/count")
    @ResponseBody
    public Map<String, Object> getCartCount(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = getCurrentUser(authentication);
                Optional<Cart> cartOpt = cartService.getCartByUser(currentUser);

                int count = 0;
                if (cartOpt.isPresent()) {
                    Cart cart = cartOpt.get();
                    List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
                    count = cartItems.size();
                }

                response.put("count", count);
                response.put("displayCount", count > 5 ? "5+" : String.valueOf(count));
                response.put("success", true);
            } else {
                response.put("count", 0);
                response.put("displayCount", "0");
                response.put("success", true);
            }
        } catch (Exception e) {
            response.put("count", 0);
            response.put("displayCount", "0");
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * Kiểm tra giỏ hàng (AJAX)
     */
    @GetMapping("/check")
    @ResponseBody
    public Map<String, Object> checkCart(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser(authentication);
            Optional<Cart> cartOpt = cartService.getCartByUser(currentUser);

            if (cartOpt.isEmpty()) {
                response.put("itemCount", 0);
                response.put("total", BigDecimal.ZERO);
            } else {
                response.put("itemCount", 0); // TODO: Calculate from CartItems
                response.put("total", BigDecimal.ZERO);
            }
        } catch (Exception e) {
            response.put("itemCount", 0);
            response.put("total", BigDecimal.ZERO);
        }
        return response;
    }
}
