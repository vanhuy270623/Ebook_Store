package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UserDashboardController - Trang chủ & Tổng quan
 * Chịu trách nhiệm hiển thị trang landing cho user đã login và dashboard thống kê.
 *
 * Endpoints:
 * - GET /user/index      : Trang chủ (landing page sau khi login)
 * - GET /user/dashboard  : Dashboard tổng quan
 */
@Controller
@RequestMapping("/user")
public class UserDashboardController {

    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    private final BookService bookService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ReadingProgressService readingProgressService;

    @Autowired
    public UserDashboardController(BookService bookService,
                                  OrderService orderService,
                                  OrderItemService orderItemService,
                                  ReadingProgressService readingProgressService) {
        this.bookService = bookService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.readingProgressService = readingProgressService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Lấy danh sách ID các sách user đã mua
     */
    private Set<String> getPurchasedBookIds(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return Set.of();
        }
        return new HashSet<>(orderItemService.getPurchasedBookIds(
                currentUser.getUserId(), Order.OrderType.BOOK, PAID_STATUSES, RETAIL_ACCESS_TYPES));
    }

    /**
     * Trang chủ cho user đã đăng nhập
     */
    @GetMapping("/index")
    public String index(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        try {
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
            model.addAttribute("user", currentUser);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("purchasedBookIds", getPurchasedBookIds(authentication));

            // Layout variables
            model.addAttribute("pageTitle", "Trang chủ");
            model.addAttribute("currentPage", "index");

        } catch (Exception e) {
            // Log error but still show the page
            System.err.println("Error loading books: " + e.getMessage());
        }

        return "user/index";
    }

    /**
     * Dashboard người dùng
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        // Lấy đơn hàng gần đây (5 cái mới nhất)
        List<Order> recentOrders = orderService.getOrdersByUser(currentUser).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        // Lấy sách đang đọc
        List<ReadingProgress> readingProgresses = readingProgressService.getReadingProgressByUser(currentUser);
        long booksReading = readingProgresses.stream()
                .filter(rp -> rp.getProgressPercentage() != null && rp.getProgressPercentage() > 0 && rp.getProgressPercentage() < 100)
                .count();

        // Thống kê
        long totalBooks = readingProgressService.getReadingProgressByUser(currentUser).size();
        long booksCompleted = readingProgresses.stream()
                .filter(rp -> rp.getProgressPercentage() != null && rp.getProgressPercentage() == 100)
                .count();

        model.addAttribute("user", currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("booksReading", booksReading);
        model.addAttribute("booksCompleted", booksCompleted);
        model.addAttribute("totalBooks", totalBooks);

        // Layout variables
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");

        return "user/dashboard";
    }
}

