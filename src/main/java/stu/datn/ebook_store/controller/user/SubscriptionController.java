package stu.datn.ebook_store.controller.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.UserSubscription;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Subscription;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller hiển thị thông tin gói đăng ký
 * CHỈ XỬ LÝ: Hiển thị plans, my-subscriptions, cancel
 * KHÔNG XỬ LÝ: Thanh toán (đã chuyển sang PaymentController)
 *
 * Subscriptions được lưu trong bảng orders với order_type = 'SUBSCRIPTION'
 */
@Controller
@RequestMapping("/subscription")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private OrderService orderService;

    /**
     * Lấy user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * Lấy subscription đang active của user
     */
    private Optional<UserSubscription> getActiveSubscription(String userId) {
        List<Order> orders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);

        return orders.stream()
                .filter(order -> {
                    // Phải đã thanh toán
                    if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED &&
                        order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                        return false;
                    }
                    // Phải còn trong thời hạn
                    return order.getEndDate() != null &&
                           order.getEndDate().isAfter(LocalDateTime.now());
                })
                .map(UserSubscription::new)
                .findFirst();
    }

    /**
     * Lấy lịch sử subscription orders của user
     */
    private List<UserSubscription> getUserSubscriptionHistory(String userId) {
        List<Order> orders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);
        return orders.stream()
                .map(UserSubscription::new)
                .collect(Collectors.toList());
    }

    /**
     * TEST endpoint - Simple template
     * URL: /subscription/plans-test
     */
    @GetMapping("/plans-test")
    public String showSubscriptionPlansTest(Model model) {
        try {
            log.info("=== TEST ENDPOINT ===");
            List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
            log.info("Subscriptions count: {}", subscriptions != null ? subscriptions.size() : "NULL");

            if (subscriptions != null) {
                for (Subscription sub : subscriptions) {
                    log.info("Sub: id={}, name={}, price={}",
                        sub.getSubscriptionId(),
                        sub.getPackageName(),
                        sub.getPrice());
                }
            }

            model.addAttribute("subscriptions", subscriptions);
            return "user/subscription/plans-simple";
        } catch (Exception e) {
            log.error("TEST ERROR: ", e);
            throw e;
        }
    }

    /**
     * Hiển thị trang danh sách các gói đăng ký
     * URL: /subscription/plans
     */
    @GetMapping("/plans")
    public String showSubscriptionPlans(Authentication authentication, Model model) {
        try {
            log.info("=== START showSubscriptionPlans ===");

            // Lấy tất cả gói đang active
            List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
            log.info("Found {} active subscriptions", subscriptions != null ? subscriptions.size() : 0);

            if (subscriptions != null && !subscriptions.isEmpty()) {
                for (Subscription sub : subscriptions) {
                    log.info("Subscription: id={}, packageName={}, price={}, durationDays={}, features={}, maxDevices={}, hasAds={}",
                        sub.getSubscriptionId(),
                        sub.getPackageName(),
                        sub.getPrice(),
                        sub.getDurationDays(),
                        sub.getFeatures(),
                        sub.getMaxDevices(),
                        sub.getHasAds());
                }
            }

            model.addAttribute("subscriptions", subscriptions);

            // Nếu user đã đăng nhập, kiểm tra gói hiện tại
            User currentUser = getCurrentUser(authentication);
            log.info("Current user: {}", currentUser != null ? currentUser.getUserId() : "anonymous");

            if (currentUser != null) {
                // Thêm user vào model để hiển thị trong navbar (giống HomeController)
                model.addAttribute("user", currentUser);

                Optional<UserSubscription> activeSubscription =
                    getActiveSubscription(currentUser.getUserId());

                log.info("Has active subscription: {}", activeSubscription.isPresent());
                if (activeSubscription.isPresent()) {
                    UserSubscription userSub = activeSubscription.get();
                    log.info("Active subscription: userSubscriptionId={}, subscriptionId={}",
                        userSub.getUserSubscriptionId(),
                        userSub.getSubscription() != null ? userSub.getSubscription().getSubscriptionId() : "null");
                }

                model.addAttribute("currentSubscription", activeSubscription.orElse(null));
                model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());
            } else {
                model.addAttribute("hasActiveSubscription", false);
            }

            log.info("=== END showSubscriptionPlans - Returning view ===");
            return "user/subscription/view-plans";

        } catch (Exception e) {
            log.error("ERROR in showSubscriptionPlans: ", e);
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("subscriptions", List.of());
            model.addAttribute("hasActiveSubscription", false);
            return "user/subscription/view-plans";
        }
    }

    /**
     * Hiển thị trang quản lý gói đăng ký của user
     * URL: /subscription/my-subscriptions
     */
    @GetMapping("/my-subscriptions")
    public String mySubscriptions(Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem gói của bạn");
            return "redirect:/auth/login";
        }

        // Thêm user vào model để hiển thị trong navbar (giống HomeController)
        model.addAttribute("user", currentUser);

        // Lấy lịch sử tất cả gói đăng ký
        List<UserSubscription> subscriptions =
            getUserSubscriptionHistory(currentUser.getUserId());
        model.addAttribute("subscriptions", subscriptions);

        // Lấy gói đang active (nếu có)
        Optional<UserSubscription> activeSubscription =
            getActiveSubscription(currentUser.getUserId());
        model.addAttribute("currentSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());

        // Thống kê
        long totalSubs = subscriptions.size();
        long activeSubs = subscriptions.stream().filter(s -> s.isActive()).count();
        long expiredSubs = totalSubs - activeSubs;
        long daysRemaining = activeSubscription.map(s ->
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDateTime.now(),
                s.getEndDate()
            )
        ).orElse(0L);

        model.addAttribute("totalSubscriptions", totalSubs);
        model.addAttribute("activeSubscriptions", activeSubs);
        model.addAttribute("expiredSubscriptions", expiredSubs);
        model.addAttribute("daysRemaining", daysRemaining);

        return "user/subscription/my-subscriptions";
    }

    /**
     * Hủy gói đăng ký
     * URL: POST /subscription/cancel/{subscriptionId}
     */
    @PostMapping("/cancel/{subscriptionId}")
    public String cancelSubscription(@PathVariable String subscriptionId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/auth/login";
        }

        try {
            Optional<Order> orderOpt = orderService.getOrderById(subscriptionId);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();

                // Kiểm tra xem có phải subscription order không
                if (order.getOrderType() != Order.OrderType.SUBSCRIPTION) {
                    redirectAttributes.addFlashAttribute("error", "Đây không phải gói đăng ký");
                    return "redirect:/subscription/my-subscriptions";
                }

                // Kiểm tra quyền sở hữu
                if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
                    redirectAttributes.addFlashAttribute("error", "Bạn không có quyền hủy gói này");
                    return "redirect:/subscription/my-subscriptions";
                }

                // Thực hiện hủy - cập nhật payment status
                order.setPaymentStatus(Order.PaymentStatus.CANCELLED);
                orderService.updateOrder(order);

                redirectAttributes.addFlashAttribute("success", "Đã hủy gói đăng ký thành công");

            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy gói đăng ký");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/subscription/my-subscriptions";
    }
}

