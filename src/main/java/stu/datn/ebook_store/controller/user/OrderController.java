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
import java.util.List;
import java.util.UUID;

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
    private final CouponService couponService;

    @Autowired
    public OrderController(OrderService orderService, OrderItemService orderItemService,
                          CartService cartService, CartItemService cartItemService,
                          CouponService couponService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.cartService = cartService;
        this.cartItemService = cartItemService;
        this.couponService = couponService;
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

        // Lấy giỏ hàng
        Cart cart = cartService.getCartByUser(currentUser).orElse(null);
        if (cart == null) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        // Lấy danh sách items trong giỏ (cần implement trong CartItemService)
        List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);

        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        // Tính tổng tiền
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
            @RequestParam(required = false) String couponCode,
            @RequestParam String paymentMethod,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        try {
            // Lấy giỏ hàng
            Cart cart = cartService.getCartByUser(currentUser)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

            List<CartItem> cartItems = cartItemService.getCartItemsByCart(cart);
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống");
            }

            // Tính tổng tiền
            BigDecimal totalAmount = cartItems.stream()
                    .map(item -> item.getBook().getPrice() != null ? item.getBook().getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Áp dụng coupon nếu có
            if (couponCode != null && !couponCode.trim().isEmpty()) {
                Coupon appliedCoupon = couponService.getCouponByCode(couponCode).orElse(null);
                if (appliedCoupon != null && appliedCoupon.getUsageLimit() != null && appliedCoupon.getUsageLimit() > 0) {
                    // Tính giảm giá
                    BigDecimal discount = BigDecimal.ZERO;
                    if (appliedCoupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
                        discount = totalAmount.multiply(appliedCoupon.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    } else if (appliedCoupon.getDiscountType() == Coupon.DiscountType.FIXED) {
                        discount = appliedCoupon.getDiscountValue();
                    }

                    totalAmount = totalAmount.subtract(discount);

                    // Giảm số lượng coupon
                    appliedCoupon.setUsageLimit(appliedCoupon.getUsageLimit() - 1);
                    couponService.saveCoupon(appliedCoupon);
                }
            }

            // Tạo order
            Order order = new Order();
            order.setOrderId(UUID.randomUUID().toString());
            order.setUser(currentUser);
            order.setOrderType(Order.OrderType.BOOK);
            order.setTotalAmount(totalAmount);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setPaymentMethod(Order.PaymentMethod.valueOf(paymentMethod));
            order.setCreatedAt(LocalDateTime.now());

            Order savedOrder = orderService.saveOrder(order);

            // Tạo order items
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderItemId(UUID.randomUUID().toString());
                orderItem.setOrder(savedOrder);
                orderItem.setBook(cartItem.getBook());
                orderItem.setPriceAtPurchase(cartItem.getBook().getPrice());
                orderItemService.saveOrderItem(orderItem);
            }

            // Xóa giỏ hàng
            for (CartItem item : cartItems) {
                CartItemId id = new CartItemId(cart.getCartId(), item.getBook().getBookId());
                cartItemService.deleteCartItem(id);
            }

            // Chuyển đến payment gateway hoặc confirmation
            if ("VNPAY".equals(paymentMethod)) {
                return "redirect:/payment/vnpay?orderId=" + savedOrder.getOrderId();
            } else if ("MOMO".equals(paymentMethod)) {
                return "redirect:/payment/momo?orderId=" + savedOrder.getOrderId();
            } else {
                // Thanh toán khác - chuyển về trang xác nhận
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

        return "user/order-detail";
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
}

