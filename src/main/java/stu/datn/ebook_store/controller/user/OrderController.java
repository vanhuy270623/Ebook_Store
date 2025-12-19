package stu.datn.ebook_store.controller.user;

import org.springframework.security.core.Authentication;
import stu.datn.ebook_store.controller.BaseController;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.*;
import stu.datn.ebook_store.service.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AdminDashboardController xử lý đơn hàng và thanh toán
 * Endpoints: /order/*
 */
@Controller
@RequestMapping("/order")
public class OrderController extends BaseController {

    private final OrderService orderService;
    private final CartService cartService;
    private final CartItemService cartItemService;

    @Autowired
    public OrderController(OrderService orderService,
                          CartService cartService,
                          CartItemService cartItemService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.cartItemService = cartItemService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }


    /**
     * Trang checkout
     * Business logic validation đã chuyển sang CartService
     */
    @GetMapping("/checkout")
    public String showCheckout(Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán");
            return "redirect:/auth/login";
        }

        Cart cart = cartService.getCartByUser(currentUser).orElse(null);
        if (cart == null) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        // Validate cart qua CartService
        if (!cartService.isCartValidForCheckout(cart, currentUser)) {
            List<String> errors = cartService.getCartValidationErrors(cart, currentUser);
            redirectAttributes.addFlashAttribute("error", String.join(", ", errors));
            return "redirect:/cart";
        }

        // Lấy cart items và tổng tiền qua CartService
        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
        BigDecimal totalAmount = cartService.calculateCartTotal(cart);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("itemCount", cartItems.size());

        return "user/order/checkout";
    }

    /**
     * Xử lý tạo đơn hàng
     */
    @PostMapping("/create")
    public String createOrder(
            @RequestParam String paymentMethod,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        try {
            Cart cart = cartService.getCartByUser(currentUser)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

            // Tạo order qua OrderService (business logic đã chuyển xuống service)
            Order.PaymentMethod method = Order.PaymentMethod.valueOf(paymentMethod);
            Order savedOrder = orderService.createOrderFromCart(currentUser, cart, method);

            // Redirect theo phương thức thanh toán
            if ("VNPAY".equals(paymentMethod)) {
                return "redirect:/payment/vnpay?orderId=" + savedOrder.getOrderId();
            } else if ("BANK_TRANSFER".equals(paymentMethod)) {
                return "redirect:/payment/bank-transfer?orderId=" + savedOrder.getOrderId();
            } else {
                redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: " + savedOrder.getOrderId());
                return "redirect:/user/orders";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo đơn hàng: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }

    /**
     * Xem chi tiết đơn hàng
     */
    /**
     * Xem chi tiết đơn hàng
     * DEPRECATED: Redirect to /user/orders/{orderId} for RESTful consistency
     */
    @GetMapping("/{orderId}")
    public String viewOrder(@PathVariable String orderId) {
        // Redirect to UserOrderController để tránh duplicate code
        return "redirect:/user/orders/" + orderId;
    }

    /**
     * Hủy đơn hàng
     * Business logic đã chuyển sang OrderService
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable String orderId,
                             RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        try {
            // Hủy order qua OrderService
            orderService.cancelOrder(orderId, currentUser);
            redirectAttributes.addFlashAttribute("success", "Hủy đơn hàng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/user/orders";
    }

    /**
     * API endpoint để check order status (cho auto-refresh trong waiting page)
     */
    @GetMapping("/api/status")
    @ResponseBody
    public java.util.Map<String, Object> checkOrderStatus(
            @RequestParam String orderId) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return response;
            }

            orderService.getOrderById(orderId).ifPresentOrElse(
                    order -> {
                        // Check permission qua OrderService
                        if (!orderService.canUserAccessOrder(order, currentUser)) {
                            response.put("success", false);
                            response.put("message", "Unauthorized");
                        } else {
                            response.put("success", true);
                            response.put("orderId", order.getOrderId());
                            response.put("paymentStatus", order.getPaymentStatus().toString());
                            response.put("paymentMethod", order.getPaymentMethod().toString());
                            response.put("totalAmount", order.getTotalAmount());
                        }
                    },
                    () -> {
                        response.put("success", false);
                        response.put("message", "Order not found");
                    }
            );

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }
}
