package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.Banner; // Import Banner
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Post;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BannerService; // Import Service
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.PostService;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserDashboardController {

    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    // 1. Khai báo BannerService
    private final BookService bookService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ReadingProgressService readingProgressService;
    private final BannerService bannerService;
    private final PostService postService;

    @Autowired
    public UserDashboardController(BookService bookService,
                                   OrderService orderService,
                                   OrderItemService orderItemService,
                                   ReadingProgressService readingProgressService,
                                   BannerService bannerService,
                                   PostService postService) { // 2. Inject vào Constructor
        this.bookService = bookService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.readingProgressService = readingProgressService;
        this.bannerService = bannerService;
        this.postService = postService;
    }

    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    private Set<String> getPurchasedBookIds(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return Set.of();
        }
        return new HashSet<>(orderItemService.getPurchasedBookIds(
                currentUser.getUserId(), Order.OrderType.BOOK, PAID_STATUSES, RETAIL_ACCESS_TYPES));
    }

    @GetMapping("/index")
    public String index(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        try {
            // 3. LẤY DANH SÁCH BANNER VÀ ĐƯA VÀO MODEL
            // Lưu ý: Đảm bảo BannerPosition khớp với DB (HOME hoặc HOME_MAIN)
            List<Banner> banners = bannerService.getActiveBannersForDisplay(Banner.BannerPosition.HOME);
            model.addAttribute("banners", banners);

            // Get free books
            List<Book> freeBooks = bookService.getBooksByAccessType(Book.AccessType.FREE);
            // Get trending books
            List<Book> trendingBooks = bookService.getTopViewedBooks();
            // Get new releases
            List<Book> newBooks = bookService.getNewestBooks();

            // Get latest posts (limit 6)
            List<Post> latestPosts = postService.getLatestPosts()
                    .stream()
                    .limit(6)
                    .toList();

            model.addAttribute("freeBooks", freeBooks);
            model.addAttribute("trendingBooks", trendingBooks);
            model.addAttribute("newBooks", newBooks);
            model.addAttribute("latestPosts", latestPosts);
            model.addAttribute("user", currentUser);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("purchasedBookIds", getPurchasedBookIds(authentication));

            model.addAttribute("pageTitle", "Trang chủ");
            model.addAttribute("currentPage", "index");

        } catch (Exception e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }

        return "user/index";
    }

    // ... (Phần code dashboard giữ nguyên) ...
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
    @GetMapping("/favorites")
    public String favorites(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        // 1. Lấy thông tin User để hiển thị layout
        model.addAttribute("user", currentUser);
        model.addAttribute("currentUser", currentUser);

        // 2. Lấy danh sách sách yêu thích từ Service
        // Lưu ý: Hàm này trả về List<ReadingProgress> chứ không phải List<Book>
        List<ReadingProgress> favoriteProgresses = readingProgressService.getFavoriteBooksByUser(currentUser);

        // Lọc những bản ghi có Book != null để tránh lỗi NullPointerException
        List<ReadingProgress> safeFavorites = favoriteProgresses.stream()
                .filter(rp -> rp.getBook() != null)
                .collect(Collectors.toList());

        model.addAttribute("favoriteBooks", safeFavorites);
        model.addAttribute("totalFavorites", safeFavorites.size());

        // 3. Thiết lập biến cho Layout (Active menu, Title)
        model.addAttribute("pageTitle", "Sách yêu thích");
        model.addAttribute("currentPage", "favorites"); // Dùng để highlight menu bên trái nếu có

        return "user/favorites"; // Trả về file view: templates/user/favorites.html
    }
}