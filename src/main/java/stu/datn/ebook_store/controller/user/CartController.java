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
    public String viewCart(Model model) {
        // TODO: Implement get current user session
        // For now, return empty cart view
        model.addAttribute("cartItems", List.of());
        model.addAttribute("cartTotal", BigDecimal.ZERO);
        model.addAttribute("cartItemCount", 0);

        return "user/cart/view";
    }

    /**
     * Thêm sách vào giỏ hàng
     */
    @PostMapping("/add/{bookId}")
    public String addToCart(
            @PathVariable String bookId,
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
                return "redirect:/books/view/" + bookId;
            }

            // Lấy hoặc tạo giỏ hàng
            Cart cart = cartService.getCartByUser(currentUser)
                    .orElseGet(() -> cartService.createCartForUser(currentUser));

            // Tạo CartItem (composite key: cart + book)
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBook(book);
            cartItemService.saveCartItem(newItem);
            redirectAttributes.addFlashAttribute("success", "Thêm sách vào giỏ thành công");

            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/books/view/" + bookId;
        }
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
                // Xóa toàn bộ items
                // TODO: Implement when CartItemService has getAllByCart method
                redirectAttributes.addFlashAttribute("success", "Giỏ hàng đã được xóa");
            }

            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/cart";
        }
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

