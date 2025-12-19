package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.OrderItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserOrderController - Quản lý đơn hàng
 * Chịu trách nhiệm hiển thị lịch sử mua hàng.
 * 
 * Endpoints:
 * - GET /user/orders           : Danh sách đơn hàng
 * - GET /user/orders/{orderId} : Chi tiết đơn hàng
 */
@Controller
@RequestMapping("/user")
public class UserOrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @Autowired
    public UserOrderController(OrderService orderService, OrderItemService orderItemService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Lịch sử đơn hàng
     */
    @GetMapping("/orders")
    public String orderHistory(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        User currentUser = getCurrentUser(authentication);
        List<Order> orders = orderService.getOrdersByUser(currentUser).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        // Phân trang
        int pageSize = 10;
        int totalOrders = orders.size();
        int totalPages = (int) Math.ceil((double) totalOrders / pageSize);
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = Math.max(0, page * pageSize);
        int endIndex = Math.min(startIndex + pageSize, totalOrders);
        List<Order> pagedOrders = orders.subList(startIndex, endIndex);

        // Load order items for each order
        Map<String, List<OrderItem>> orderItemsMap = new HashMap<>();
        for (Order order : pagedOrders) {
            List<OrderItem> items = orderItemService.getOrderItemsByOrderId(order.getOrderId());
            orderItemsMap.put(order.getOrderId(), items);
        }

        model.addAttribute("orders", pagedOrders);
        model.addAttribute("orderItemsMap", orderItemsMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", totalOrders);

        return "user/order/orders";
    }

    /**
     * Chi tiết đơn hàng
     */
    @GetMapping("/orders/{orderId}")
    public String orderDetail(
            @PathVariable String orderId,
            Model model,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.getOrderById(orderId).orElse(null);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/user/orders";
        }

        // Load order items for book orders
        if (order.getOrderType() == Order.OrderType.BOOK) {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
            model.addAttribute("orderItems", orderItems);
        }

        model.addAttribute("order", order);

        return "user/order/order-detail";
    }
}
