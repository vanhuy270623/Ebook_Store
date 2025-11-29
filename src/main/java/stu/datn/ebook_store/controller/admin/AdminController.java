package stu.datn.ebook_store.controller.admin;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.UserService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.ReviewService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookService bookService;
    private final UserService userService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    @Autowired
    public AdminController(BookService bookService, UserService userService,
                          OrderService orderService, ReviewService reviewService) {
        this.bookService = bookService;
        this.userService = userService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        // Debug: Kiểm tra Spring Security Authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== ADMIN DASHBOARD ACCESS ===");
        System.out.println("Authentication: " + auth);
        if (auth != null) {
            System.out.println("Principal: " + auth.getPrincipal());
            System.out.println("Authorities: " + auth.getAuthorities());
            System.out.println("Is Authenticated: " + auth.isAuthenticated());
        }
        System.out.println("Session username: " + session.getAttribute("username"));
        System.out.println("Session role: " + session.getAttribute("role"));
        System.out.println("==============================");

        // Spring Security đã check authentication và role ADMIN
        // Lấy thông tin thống kê tổng quan
        model.addAttribute("username", session.getAttribute("username"));

        // Thống kê sách
        model.addAttribute("totalBooks", bookService.getTotalBooksCount());
        model.addAttribute("freeBooks", bookService.getFreeBooks());
        model.addAttribute("paidBooks", bookService.getPaidBooks());
        model.addAttribute("subscriptionBooks", bookService.getSubscriptionBooks());
        model.addAttribute("recentBooks", bookService.getRecentBooks(5));
        model.addAttribute("topRatedBooks", bookService.getTopRatedBooks(5));

        // Thống kê người dùng
        model.addAttribute("totalUsers", userService.getTotalUsersCount());
        model.addAttribute("activeUsers", userService.getActiveUsersCount());
        model.addAttribute("verifiedUsers", userService.getVerifiedUsersCount());
        model.addAttribute("recentUsers", userService.getRecentUsers(5));

        return "admin/dashboard";
    }
}

