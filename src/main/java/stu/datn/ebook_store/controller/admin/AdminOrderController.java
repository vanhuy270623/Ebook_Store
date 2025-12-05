package stu.datn.ebook_store.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller quản lý đơn hàng (Admin)
 * Endpoints: /admin/orders/*
 * Chức năng: Xem, quản lý, cập nhật trạng thái đơn hàng
 */
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final String REDIRECT_ORDERS = "redirect:/admin/orders";

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @Autowired
    public AdminOrderController(OrderService orderService, OrderItemService orderItemService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Lấy thông tin user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // ============================= VIEW OPERATIONS =============================

    /**
     * Hiển thị danh sách đơn hàng với filter
     * GET /admin/orders
     */
    @GetMapping
    public String ordersList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication,
            Model model) {

        List<Order> orders;

        // Apply filters
        if (status != null && !status.trim().isEmpty() && !status.equals("ALL")) {
            Order.PaymentStatus paymentStatus = Order.PaymentStatus.valueOf(status);
            orders = orderService.getOrdersByPaymentStatus(paymentStatus);
        } else if (orderType != null && !orderType.trim().isEmpty() && !orderType.equals("ALL")) {
            Order.OrderType type = Order.OrderType.valueOf(orderType);
            orders = orderService.getOrdersByOrderType(type);
        } else if (startDate != null && endDate != null) {
            orders = orderService.getOrdersBetweenDates(startDate, endDate);
        } else {
            orders = orderService.getAllOrders();
        }

        // Sort by date descending
        orders = orders.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedOrderType", orderType);
        model.addAttribute("paymentStatuses", Order.PaymentStatus.values());
        model.addAttribute("orderTypes", Order.OrderType.values());

        // Quick statistics
        long pendingOrders = orders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PENDING)
                .count();
        long completedOrders = orders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .count();
        long failedOrders = orders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.FAILED)
                .count();

        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("failedOrders", failedOrders);

        return "admin/orders/list";
    }

    /**
     * Xem chi tiết đơn hàng
     * GET /admin/orders/view/{id}
     */
    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderService.getOrderById(id);

        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return REDIRECT_ORDERS;
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);

        // Get order items if order type is BOOK
        if (order.getOrderType() == Order.OrderType.BOOK) {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(order.getOrderId());
            model.addAttribute("orderItems", orderItems);
        }

        // Payment statuses for status update dropdown
        model.addAttribute("paymentStatuses", Order.PaymentStatus.values());

        return "admin/orders/view";
    }

    /**
     * Cập nhật trạng thái đơn hàng
     * POST /admin/orders/update-status/{id}
     */
    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
                return REDIRECT_ORDERS;
            }

            Order.PaymentStatus newStatus = Order.PaymentStatus.valueOf(status);
            orderService.updateOrderStatus(id, newStatus);

            redirectAttributes.addFlashAttribute("success",
                "Cập nhật trạng thái đơn hàng thành công!");
            return "redirect:/admin/orders/view/" + id;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
            return REDIRECT_ORDERS;
        }
    }

    /**
     * Xem thống kê đơn hàng và doanh thu
     * GET /admin/orders/statistics
     */
    @GetMapping("/statistics")
    public String viewStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {

        // Default to current month if no dates provided
        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<Order> allOrders = orderService.getAllOrders();
        List<Order> periodOrders = orderService.getOrdersBetweenDates(startDate, endDate);

        // Overall statistics
        long totalOrders = allOrders.size();
        long pendingOrders = orderService.countOrdersByPaymentStatus(Order.PaymentStatus.PENDING);
        long completedOrders = orderService.countOrdersByPaymentStatus(Order.PaymentStatus.COMPLETED);
        long failedOrders = orderService.countOrdersByPaymentStatus(Order.PaymentStatus.FAILED);
        long cancelledOrders = orderService.countOrdersByPaymentStatus(Order.PaymentStatus.CANCELLED);

        // Revenue statistics
        Double totalRevenue = orderService.getTotalRevenueBetweenDates(startDate, endDate);
        Double completedRevenue = periodOrders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum();

        // Order type statistics
        long bookOrders = allOrders.stream()
                .filter(o -> o.getOrderType() == Order.OrderType.BOOK)
                .count();
        long subscriptionOrders = allOrders.stream()
                .filter(o -> o.getOrderType() == Order.OrderType.SUBSCRIPTION)
                .count();

        // Payment method statistics
        Map<String, Long> paymentMethodStats = allOrders.stream()
                .filter(o -> o.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(
                    o -> o.getPaymentMethod().name(),
                    Collectors.counting()
                ));

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("failedOrders", failedOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        model.addAttribute("completedRevenue", completedRevenue);
        model.addAttribute("bookOrders", bookOrders);
        model.addAttribute("subscriptionOrders", subscriptionOrders);
        model.addAttribute("paymentMethodStats", paymentMethodStats);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("periodOrders", periodOrders);

        return "admin/orders/statistics";
    }

    // ============================= AJAX ENDPOINTS =============================

    /**
     * Lấy thông tin đơn hàng (AJAX)
     * GET /admin/orders/api/{id}
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderInfo(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();

        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng");
            return ResponseEntity.ok(response);
        }

        Order order = orderOpt.get();
        response.put("success", true);
        response.put("orderId", order.getOrderId());
        response.put("status", order.getPaymentStatus().name());
        response.put("totalAmount", order.getTotalAmount());
        response.put("createdAt", order.getCreatedAt().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật trạng thái nhanh (AJAX)
     * POST /admin/orders/api/quick-update/{id}
     */
    @PostMapping("/api/quick-update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickUpdateStatus(
            @PathVariable String id,
            @RequestParam String status) {

        Map<String, Object> response = new HashMap<>();

        try {
            Order.PaymentStatus newStatus = Order.PaymentStatus.valueOf(status);
            orderService.updateOrderStatus(id, newStatus);

            response.put("success", true);
            response.put("message", "Cập nhật thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}

