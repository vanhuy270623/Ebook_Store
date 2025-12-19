package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
 * Controller xử lý đơn hàng và thanh toán
 * Endpoints: /order/*
 */
@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final CartService cartService;
    private final CartItemService cartItemService;

    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    @Autowired
    public OrderController(OrderService orderService, OrderItemService orderItemService,
                          CartService cartService, CartItemService cartItemService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.cartService = cartService;
        this.cartItemService = cartItemService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * Trang checkout
     */
    @GetMapping("/checkout")
    public String showCheckout(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán");
            return "redirect:/auth/login";
        }

        Cart cart = cartService.getCartByUser(currentUser).orElse(null);
        if (cart == null) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        Set<String> purchasedBookIds = new HashSet<>(orderItemService.getPurchasedBookIds(
                currentUser.getUserId(), Order.OrderType.BOOK, PAID_STATUSES, RETAIL_ACCESS_TYPES));
        List<CartItem> duplicateItems = cartItems.stream()
                .filter(item -> purchasedBookIds.contains(item.getBook().getBookId()))
                .collect(Collectors.toList());

        if (!duplicateItems.isEmpty()) {
            String titles = duplicateItems.stream()
                    .map(item -> item.getBook().getTitle())
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("error",
                    "Bạn đã mua các ebook: " + titles + ". Vui lòng truy cập thư viện hoặc gỡ khỏi giỏ hàng.");
            return "redirect:/cart";
        }

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getBook().getPrice() != null ? item.getBook().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("user", currentUser);
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
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        try {
            Cart cart = cartService.getCartByUser(currentUser)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

            List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống");
            }

            Set<String> purchasedBookIds = new HashSet<>(orderItemService.getPurchasedBookIds(
                    currentUser.getUserId(), Order.OrderType.BOOK, PAID_STATUSES, RETAIL_ACCESS_TYPES));
            List<String> duplicateTitles = cartItems.stream()
                    .filter(item -> purchasedBookIds.contains(item.getBook().getBookId()))
                    .map(item -> item.getBook().getTitle())
                    .collect(Collectors.toList());

            if (!duplicateTitles.isEmpty()) {
                throw new RuntimeException("Bạn đã sở hữu: " + String.join(", ", duplicateTitles));
            }

            BigDecimal totalAmount = cartItems.stream()
                    .map(item -> item.getBook().getPrice() != null ? item.getBook().getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            Order order = new Order();
            order.setUser(currentUser);
            order.setOrderType(Order.OrderType.BOOK);
            order.setTotalAmount(totalAmount);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setPaymentMethod(Order.PaymentMethod.valueOf(paymentMethod));
            order.setCreatedAt(LocalDateTime.now());

            Order savedOrder = orderService.saveOrder(order);

            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setBook(cartItem.getBook());
                orderItem.setPriceAtPurchase(cartItem.getBook().getPrice());
                orderItemService.saveOrderItem(orderItem);
            }

            for (CartItem item : cartItems) {
                CartItemId id = new CartItemId(cart.getCartId(), item.getBook().getBookId());
                cartItemService.deleteCartItem(id);
            }

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
    @GetMapping("/{orderId}")
    public String viewOrder(@PathVariable String orderId, Authentication authentication,
                           Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/user/orders";
        }

        // Kiểm tra quyền truy cập
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn hàng này");
            return "redirect:/user/orders";
        }

        // Lấy order items
        List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(order.getOrderId());

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("user", currentUser);

        return "user/order/order-detail";
    }

    /**
     * Hủy đơn hàng
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable String orderId, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        try {
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Kiểm tra quyền
            if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
            }

            // Chỉ hủy được đơn PENDING
            if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
                throw new RuntimeException("Không thể hủy đơn hàng đã thanh toán");
            }

            order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
            orderService.saveOrder(order);

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
            @RequestParam String orderId,
            Authentication authentication) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return response;
            }

            orderService.getOrderById(orderId).ifPresentOrElse(
                    order -> {
                        // Check permission
                        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
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
