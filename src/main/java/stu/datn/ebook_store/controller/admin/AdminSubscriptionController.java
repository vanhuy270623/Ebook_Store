package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.SubscriptionCreateRequest;
import stu.datn.ebook_store.dto.request.SubscriptionUpdateRequest;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.SubscriptionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller quản lý gói subscription (Admin)
 * Endpoints: /admin/subscriptions/*
 * Pattern: Tương tự AdminCategoryController, AdminCouponController
 */
@Controller
@RequestMapping("/admin/subscriptions")
public class AdminSubscriptionController {

    private static final String REDIRECT_SUBSCRIPTIONS = "redirect:/admin/subscriptions";

    private final SubscriptionService subscriptionService;
    private final OrderService orderService;

    @Autowired
    public AdminSubscriptionController(SubscriptionService subscriptionService, OrderService orderService) {
        this.subscriptionService = subscriptionService;
        this.orderService = orderService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Lấy thông tin user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Sinh Subscription ID tự động theo format: "sub_XX"
     */
    private String generateNextSubscriptionId() {
        List<Subscription> allSubscriptions = subscriptionService.getAllSubscriptions();
        int nextNumber = allSubscriptions.size() + 1;
        return String.format("sub_%03d", nextNumber);
    }

    /**
     * Map Subscription entity sang SubscriptionUpdateRequest DTO
     */
    private SubscriptionUpdateRequest mapToUpdateRequest(Subscription subscription) {
        SubscriptionUpdateRequest dto = new SubscriptionUpdateRequest();
        dto.setSubscriptionId(subscription.getSubscriptionId());
        dto.setPackageName(subscription.getPackageName() != null ? subscription.getPackageName().name() : "BASIC");
        dto.setDescription(subscription.getDescription());
        dto.setPrice(subscription.getPrice());
        dto.setDurationDays(subscription.getDurationDays());
        dto.setMaxDevices(subscription.getMaxDevices());
        dto.setIsActive(subscription.getIsActive());
        return dto;
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("packageNames", Subscription.PackageName.values());
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách subscription packages
     * GET /admin/subscriptions
     */
    @GetMapping
    public String subscriptionsList(Authentication authentication, Model model) {
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();

        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("totalSubscriptions", subscriptions.size());

        // Quick statistics
        long activeSubscriptions = subscriptions.stream()
                .filter(Subscription::getIsActive)
                .count();
        long inactiveSubscriptions = subscriptions.stream()
                .filter(s -> !s.getIsActive())
                .count();

        model.addAttribute("activeSubscriptions", activeSubscriptions);
        model.addAttribute("inactiveSubscriptions", inactiveSubscriptions);

        return "admin/subscriptions/list";
    }

    /**
     * Hiển thị form thêm subscription package mới
     * GET /admin/subscriptions/add
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        SubscriptionCreateRequest subscriptionRequest = new SubscriptionCreateRequest();
        subscriptionRequest.setPackageName("BASIC");
        subscriptionRequest.setMaxDevices(1);
        subscriptionRequest.setIsActive(true);

        model.addAttribute("subscriptionRequest", subscriptionRequest);
        addCommonFormAttributes(model, false);

        return "admin/subscriptions/form";
    }

    /**
     * Xử lý thêm subscription package mới
     * POST /admin/subscriptions
     */
    @PostMapping
    public String createSubscription(
            @Valid @ModelAttribute("subscriptionRequest") SubscriptionCreateRequest subscriptionRequest,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            addCommonFormAttributes(model, false);
            return "admin/subscriptions/form";
        }

        try {
            // Create new subscription
            Subscription subscription = new Subscription();
            subscription.setSubscriptionId(generateNextSubscriptionId());
            subscription.setPackageName(Subscription.PackageName.valueOf(subscriptionRequest.getPackageName()));
            subscription.setDescription(subscriptionRequest.getDescription());
            subscription.setPrice(subscriptionRequest.getPrice());
            subscription.setDurationDays(subscriptionRequest.getDurationDays());
            subscription.setMaxDevices(subscriptionRequest.getMaxDevices());
            subscription.setIsActive(subscriptionRequest.getIsActive());
            subscription.setHasAds(false); // Default no ads
            subscription.setDisplayOrder(0);

            subscriptionService.saveSubscription(subscription);

            redirectAttributes.addFlashAttribute("success",
                "Tạo gói subscription '" + subscription.getPackageName() + "' thành công!");
            return REDIRECT_SUBSCRIPTIONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
            return REDIRECT_SUBSCRIPTIONS;
        }
    }

    /**
     * Hiển thị form chỉnh sửa subscription
     * GET /admin/subscriptions/edit/{id}
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(id);

        if (subscriptionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy gói subscription!");
            return REDIRECT_SUBSCRIPTIONS;
        }

        SubscriptionUpdateRequest subscriptionRequest = mapToUpdateRequest(subscriptionOpt.get());
        model.addAttribute("subscriptionRequest", subscriptionRequest);
        addCommonFormAttributes(model, true);

        return "admin/subscriptions/form";
    }

    /**
     * Xử lý cập nhật subscription
     * POST /admin/subscriptions/update/{id}
     */
    @PostMapping("/update/{id}")
    public String updateSubscription(
            @PathVariable String id,
            @Valid @ModelAttribute("subscriptionRequest") SubscriptionUpdateRequest subscriptionRequest,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            addCommonFormAttributes(model, true);
            return "admin/subscriptions/form";
        }

        try {
            Optional<Subscription> existingSubscriptionOpt = subscriptionService.getSubscriptionById(id);
            if (existingSubscriptionOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy gói subscription!");
                return REDIRECT_SUBSCRIPTIONS;
            }

            Subscription subscription = existingSubscriptionOpt.get();

            // Update fields
            subscription.setPackageName(Subscription.PackageName.valueOf(subscriptionRequest.getPackageName()));
            subscription.setDescription(subscriptionRequest.getDescription());
            subscription.setPrice(subscriptionRequest.getPrice());
            subscription.setDurationDays(subscriptionRequest.getDurationDays());
            subscription.setMaxDevices(subscriptionRequest.getMaxDevices());
            subscription.setIsActive(subscriptionRequest.getIsActive());

            subscriptionService.saveSubscription(subscription);

            redirectAttributes.addFlashAttribute("success",
                "Cập nhật gói subscription '" + subscription.getPackageName() + "' thành công!");
            return REDIRECT_SUBSCRIPTIONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
            return REDIRECT_SUBSCRIPTIONS;
        }
    }

    /**
     * Xem chi tiết subscription
     * GET /admin/subscriptions/view/{id}
     */
    @GetMapping("/view/{id}")
    public String viewSubscription(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(id);

        if (subscriptionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy gói subscription!");
            return REDIRECT_SUBSCRIPTIONS;
        }

        Subscription subscription = subscriptionOpt.get();
        model.addAttribute("subscription", subscription);

        // Get orders for this subscription
        List<Order> orders = orderService.getOrdersBySubscription(subscription);
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", orders.size());

        return "admin/subscriptions/view";
    }

    /**
     * Xóa subscription
     * POST /admin/subscriptions/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public String deleteSubscription(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(id);
            if (subscriptionOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy gói subscription!");
                return REDIRECT_SUBSCRIPTIONS;
            }

            // Check if there are orders for this subscription
            List<Order> orders = orderService.getOrdersBySubscription(subscriptionOpt.get());
            if (!orders.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                    "Không thể xóa gói subscription đã có đơn hàng! Hãy ngừng kích hoạt thay vì xóa.");
                return REDIRECT_SUBSCRIPTIONS;
            }

            String packageName = subscriptionOpt.get().getPackageName().name();
            subscriptionService.deleteSubscription(id);

            redirectAttributes.addFlashAttribute("success",
                "Xóa gói subscription '" + packageName + "' thành công!");
            return REDIRECT_SUBSCRIPTIONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Không thể xóa subscription: " + e.getMessage());
            return REDIRECT_SUBSCRIPTIONS;
        }
    }

    /**
     * Toggle trạng thái active của subscription
     * POST /admin/subscriptions/toggle/{id}
     */
    @PostMapping("/toggle/{id}")
    public String toggleSubscriptionStatus(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(id);
            if (subscriptionOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy gói subscription!");
                return REDIRECT_SUBSCRIPTIONS;
            }

            Subscription subscription = subscriptionOpt.get();
            if (subscription.getIsActive()) {
                subscriptionService.deactivateSubscription(id);
                redirectAttributes.addFlashAttribute("success", "Đã ngừng kích hoạt gói subscription!");
            } else {
                subscriptionService.activateSubscription(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt gói subscription!");
            }

            return REDIRECT_SUBSCRIPTIONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
            return REDIRECT_SUBSCRIPTIONS;
        }
    }

    /**
     * Xem thống kê subscription
     * GET /admin/subscriptions/statistics
     */
    @GetMapping("/statistics")
    public String viewStatistics(Model model) {
        List<Subscription> allSubscriptions = subscriptionService.getAllSubscriptions();

        long totalSubscriptions = allSubscriptions.size();
        long activeSubscriptions = allSubscriptions.stream()
                .filter(Subscription::getIsActive)
                .count();
        long inactiveSubscriptions = totalSubscriptions - activeSubscriptions;

        // Order statistics per subscription
        Map<String, Long> orderCountPerSubscription = new HashMap<>();
        Map<String, Double> revenuePerSubscription = new HashMap<>();

        for (Subscription subscription : allSubscriptions) {
            List<Order> orders = orderService.getOrdersBySubscription(subscription);
            long orderCount = orders.size();
            double revenue = orders.stream()
                    .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                    .mapToDouble(o -> o.getTotalAmount().doubleValue())
                    .sum();

            orderCountPerSubscription.put(subscription.getPackageName().name(), orderCount);
            revenuePerSubscription.put(subscription.getPackageName().name(), revenue);
        }

        model.addAttribute("totalSubscriptions", totalSubscriptions);
        model.addAttribute("activeSubscriptions", activeSubscriptions);
        model.addAttribute("inactiveSubscriptions", inactiveSubscriptions);
        model.addAttribute("subscriptions", allSubscriptions);
        model.addAttribute("orderCountPerSubscription", orderCountPerSubscription);
        model.addAttribute("revenuePerSubscription", revenuePerSubscription);

        return "admin/subscriptions/statistics";
    }

    // ============================= AJAX ENDPOINTS =============================

    /**
     * Lấy thông tin subscription (AJAX)
     * GET /admin/subscriptions/api/{id}
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSubscriptionInfo(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();

        Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(id);
        if (subscriptionOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy gói subscription");
            return ResponseEntity.ok(response);
        }

        Subscription subscription = subscriptionOpt.get();
        response.put("success", true);
        response.put("subscriptionId", subscription.getSubscriptionId());
        response.put("packageName", subscription.getPackageName().name());
        response.put("price", subscription.getPrice());
        response.put("durationDays", subscription.getDurationDays());
        response.put("isActive", subscription.getIsActive());

        return ResponseEntity.ok(response);
    }
}

