package stu.datn.ebook_store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.service.BannerService;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/")
    public String home(Model model) {
        // Authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean shouldShowSubscriptionNotification = false;

        if (auth != null && auth.isAuthenticated() &&
            auth.getPrincipal() instanceof stu.datn.ebook_store.entity.User user) {
            model.addAttribute("user", user);

            System.out.println("=== HOME PAGE - SUBSCRIPTION CHECK ===");
            System.out.println("User ID: " + user.getUserId());
            System.out.println("User Name: " + user.getFullName());

            // Kiểm tra xem user có subscription active không
            boolean hasActiveSubscription = checkUserHasActiveSubscription(user.getUserId());

            System.out.println("Has Active Subscription: " + hasActiveSubscription);

            // Nếu user chưa có subscription active, hiển thị thông báo
            if (!hasActiveSubscription) {
                shouldShowSubscriptionNotification = true;
                System.out.println("🔔 SHOULD SHOW NOTIFICATION: TRUE");
            } else {
                System.out.println("✅ User has active subscription - no notification needed");
            }
        } else {
            System.out.println("=== HOME PAGE - USER NOT AUTHENTICATED ===");
        }

        System.out.println("Model attribute 'showSubscriptionNotification': " + shouldShowSubscriptionNotification);
        model.addAttribute("showSubscriptionNotification", shouldShowSubscriptionNotification);

        try {
            // Load active banners for HOME position (with date filtering and ordering)
            List<Banner> homeBanners = bannerService.getActiveBannersForDisplay(Banner.BannerPosition.HOME);
            model.addAttribute("banners", homeBanners);

            // Get free books (ACCESS_TYPE = 'FREE')
            List<Book> freeBooks = bookService.getBooksByAccessType(Book.AccessType.FREE);

            // Get trending books (top viewed)
            List<Book> trendingBooks = bookService.getTopViewedBooks();

            // Get new releases
            List<Book> newBooks = bookService.getNewestBooks();

            // Add to model
            model.addAttribute("freeBooks", freeBooks);
            model.addAttribute("trendingBooks", trendingBooks);
            model.addAttribute("newBooks", newBooks);

        } catch (Exception e) {
            // Log error but still show the page
            System.err.println("Error loading books: " + e.getMessage());
        }

        return "home";
    }

    @GetMapping("/home")
    public String homeAlias() {
        return "redirect:/";
    }

    /**
     * Kiểm tra user có subscription active không
     */
    private boolean checkUserHasActiveSubscription(String userId) {
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);

        return subscriptionOrders.stream()
                .anyMatch(order -> {
                    // Phải đã thanh toán (COMPLETED hoặc PAID)
                    boolean isPaid = order.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                                    order.getPaymentStatus() == Order.PaymentStatus.PAID;

                    // Phải còn trong thời hạn
                    boolean notExpired = order.getEndDate() != null &&
                                        order.getEndDate().isAfter(LocalDateTime.now());

                    return isPaid && notExpired;
                });
    }
}

