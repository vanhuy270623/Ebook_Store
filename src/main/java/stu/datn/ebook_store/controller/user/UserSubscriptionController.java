package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;
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
 * AdminDashboardController hiển thị thông tin gói đăng ký
 * CHỈ XỬ LÝ: Hiển thị plans, my-subscriptions, cancel
 * KHÔNG XỬ LÝ: Thanh toán (đã chuyển sang PaymentController)
 *
 * Subscriptions được lưu trong bảng orders với order_type = 'SUBSCRIPTION'
 */
@Controller
@RequestMapping("/subscription")
public class UserSubscriptionController extends BaseController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private OrderService orderService;


    /**
     * Lấy subscription đang active của user
     */
    private Optional<UserSubscription> getActiveSubscription(String userId) {
        List<Order> orders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);

        return orders.stream()
                .filter(order -> {
                    // Phải đã thanh toán HOẶC đã hủy (CANCELLED vẫn giữ quyền đến hết thời gian đã trả)
                    if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED &&
                        order.getPaymentStatus() != Order.PaymentStatus.PAID &&
                        order.getPaymentStatus() != Order.PaymentStatus.CANCELLED) {
                    }
                    // Phải còn trong thời hạn
                    return order.getEndDate() != null && order.getEndDate().isAfter(LocalDateTime.now());
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
     * Hiển thị trang danh sách các gói đăng ký
     * URL: /subscription/plans
     */
    @GetMapping("/plans")
    public String showSubscriptionPlans(Model model) {
        // Lấy tất cả gói đang active
        List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions();
        model.addAttribute("subscriptions", subscriptions);

        // Nếu user đã đăng nhập, kiểm tra gói hiện tại
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            // QUAN TRỌNG: Thêm user vào model để template có thể hiển thị
            model.addAttribute("user", currentUser);

            Optional<UserSubscription> activeSubscription =
                getActiveSubscription(currentUser.getUserId());

            model.addAttribute("currentSubscription", activeSubscription.orElse(null));
            model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());
        } else {
            model.addAttribute("hasActiveSubscription", false);
        }

        return "user/subscription/plans";
    }

    /**
     * Hiển thị trang quản lý gói đăng ký của user
     * URL: /subscription/my-subscriptions
     */
    @GetMapping("/my-subscriptions")
    public String mySubscriptions(Model model,
                                 RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem gói của bạn");
            return "redirect:/auth/login";
        }

        // QUAN TRỌNG: Thêm user vào model để template có thể hiển thị
        model.addAttribute("user", currentUser);

        // Lấy lịch sử tất cả gói đăng ký
        List<UserSubscription> subscriptions =
            getUserSubscriptionHistory(currentUser.getUserId());
        model.addAttribute("subscriptions", subscriptions);

        // Lấy gói đang active (nếu có)
        Optional<UserSubscription> activeSubscription =
            getActiveSubscription(currentUser.getUserId());
        model.addAttribute("activeSubscription", activeSubscription.orElse(null));
        model.addAttribute("hasActiveSubscription", activeSubscription.isPresent());

        return "user/subscription/my-subscriptions";
    }

    /**
     * Kích hoạt gói FREE trực tiếp không cần thanh toán
     * URL: POST /subscription/activate-free/{subscriptionId}
     */
    @PostMapping("/activate-free/{subscriptionId}")
    public String activateFreeSubscription(@PathVariable String subscriptionId,
                                          RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để kích hoạt gói FREE");
            return "redirect:/auth/login";
        }

        try {
            // Lấy subscription plan
            Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionById(subscriptionId);
            if (subscriptionOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Gói đăng ký không tồn tại");
                return "redirect:/subscription/plans";
            }

            Subscription subscription = subscriptionOpt.get();

            // Kiểm tra xem có phải gói FREE không
            if (subscription.getPrice().compareTo(java.math.BigDecimal.ZERO) != 0) {
                redirectAttributes.addFlashAttribute("error", "Gói này không phải gói miễn phí");
                return "redirect:/subscription/plans";
            }

            // Kiểm tra xem user đã có gói active chưa
            Optional<UserSubscription> activeSubscription = getActiveSubscription(currentUser.getUserId());
            if (activeSubscription.isPresent()) {
                redirectAttributes.addFlashAttribute("error",
                    "Bạn đã có gói đăng ký đang hoạt động. Vui lòng hủy gói hiện tại trước khi đăng ký gói mới.");
                return "redirect:/subscription/plans";
            }

            // Tạo order cho gói FREE
            Order order = new Order();
            order.setOrderId("SUB_FREE_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setUser(currentUser);
            order.setSubscription(subscription);
            order.setOrderType(Order.OrderType.SUBSCRIPTION);
            order.setTotalAmount(subscription.getPrice()); // 0
            order.setPaymentMethod(Order.PaymentMethod.FREE);
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED); // Kích hoạt ngay

            // Set thời gian gói
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(subscription.getDurationDays());
            order.setStartDate(startDate);
            order.setEndDate(endDate);

            // Lưu order
            orderService.saveOrder(order);

            redirectAttributes.addFlashAttribute("success",
                "Đã kích hoạt gói FREE thành công! Bạn có thể bắt đầu đọc sách ngay.");
            return "redirect:/subscription/my-subscriptions";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/subscription/plans";
        }
    }

    /**
     * Hủy gói đăng ký
     * URL: POST /subscription/cancel/{subscriptionId}
     */
    @PostMapping("/cancel/{subscriptionId}")
    public String cancelSubscription(@PathVariable String subscriptionId,
                                    RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
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

