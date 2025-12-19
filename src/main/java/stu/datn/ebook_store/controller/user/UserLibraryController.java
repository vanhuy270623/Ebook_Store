package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserLibraryController - Thư viện & Đọc sách
 * Chịu trách nhiệm về tủ sách cá nhân và tiến độ đọc.
 * 
 * Endpoints:
 * - GET /user/library          : Thư viện cá nhân
 * - GET /user/reading-history  : Lịch sử đọc sách
 */
@Controller
@RequestMapping("/user")
public class UserLibraryController {

    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);

    private final BookService bookService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ReadingProgressService readingProgressService;

    @Autowired
    public UserLibraryController(BookService bookService, 
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
     * Thư viện cá nhân - Hiển thị cả sách đang đọc và sách đã mua
     */
    @GetMapping("/library")
    public String library(
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        User currentUser = getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        model.addAttribute("currentUser", currentUser);

        // Layout variables
        model.addAttribute("pageTitle", "Thư viện của tôi");
        model.addAttribute("currentPage", "library");

        // Lấy danh sách sách đang đọc (Reading History)
        // Filter để loại bỏ các record có book null (dữ liệu không nhất quán)
        List<ReadingProgress> readingProgresses = readingProgressService.getReadingProgressByUserWithBookDetails(currentUser).stream()
                .filter(progress -> progress.getBook() != null) // Bỏ qua các record có book null
                .sorted((a, b) -> b.getLastReadAt() != null ? b.getLastReadAt().compareTo(a.getLastReadAt()) : 0)
                .toList();

        // Lấy danh sách sách đã mua (Purchased Books)
        List<Order> completedOrders = orderService.getOrdersByUser(currentUser).stream()
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                .filter(order -> order.getOrderType() == Order.OrderType.BOOK)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        // Lấy tất cả sách đã mua từ các đơn hàng
        List<Book> purchasedBooks = completedOrders.stream()
                .flatMap(order -> orderItemService.getOrderItemsByOrderId(order.getOrderId()).stream())
                .map(OrderItem::getBook)
                .distinct()
                .toList();

        // Lọc sách đã hoàn thành
        List<ReadingProgress> completedBooks = readingProgresses.stream()
                .filter(rp -> rp.getProgressPercentage() != null && rp.getProgressPercentage() >= 100)
                .collect(Collectors.toList());

        long totalCompleted = completedBooks.size();

        // Lấy sách miễn phí
        List<Book> freeBooks = bookService.getBooksByAccessType(Book.AccessType.FREE);

        // Kiểm tra subscription đang active của user
        List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(currentUser.getUserId(), Order.OrderType.SUBSCRIPTION);

        boolean hasActiveSubscription = false;
        String subscriptionPackageName = "";
        LocalDateTime subscriptionEndDate = null;
        List<Book> subscriptionBooks = new java.util.ArrayList<>();

        // Tìm subscription đang active
        for (Order order : subscriptionOrders) {
            if ((order.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                 order.getPaymentStatus() == Order.PaymentStatus.PAID) &&
                order.getEndDate() != null &&
                order.getEndDate().isAfter(LocalDateTime.now())) {

                hasActiveSubscription = true;
                subscriptionPackageName = order.getSubscription() != null && order.getSubscription().getPackageName() != null ?
                    order.getSubscription().getPackageName().toString() : "VIP";
                subscriptionEndDate = order.getEndDate();

                // Lấy sách từ gói subscription (SUBSCRIPTION hoặc BOTH)
                subscriptionBooks = bookService.getBooksByAccessType(Book.AccessType.SUBSCRIPTION);
                List<Book> bothBooks = bookService.getBooksByAccessType(Book.AccessType.BOTH);

                // Merge 2 lists và loại bỏ trùng lặp
                subscriptionBooks = new java.util.ArrayList<>(subscriptionBooks);
                subscriptionBooks.addAll(bothBooks);
                subscriptionBooks = subscriptionBooks.stream().distinct().collect(Collectors.toList());

                break; // Chỉ lấy subscription active đầu tiên
            }
        }

        // Thêm biến readingBooks cho template (tương đương với readingProgresses)
        model.addAttribute("readingBooks", readingProgresses);

        // Thêm danh sách sách từ subscription
        model.addAttribute("subscriptionBooks", subscriptionBooks);

        // Lấy danh sách sách yêu thích từ reading progress
        List<ReadingProgress> favoriteBooks = readingProgressService.getFavoriteBooksByUser(currentUser);
        model.addAttribute("favoriteBooks", favoriteBooks);

        // Thêm danh sách đã hoàn thành
        model.addAttribute("completedBooks", completedBooks);

        // Thêm danh sách sách miễn phí
        model.addAttribute("freeBooks", freeBooks);

        // Thêm các biến thống kê
        model.addAttribute("totalReading", readingProgresses.size());
        model.addAttribute("totalPurchased", purchasedBooks.size());
        model.addAttribute("totalSubscription", subscriptionBooks.size());
        model.addAttribute("totalFavorites", favoriteBooks.size());
        model.addAttribute("totalCompleted", totalCompleted);

        // Thông tin subscription
        model.addAttribute("hasActiveSubscription", hasActiveSubscription);
        model.addAttribute("subscriptionPackageName", subscriptionPackageName);
        model.addAttribute("subscriptionEndDate", subscriptionEndDate);

        // Lấy tất cả sách miễn phí (cho tab "all")
        List<Book> allFreeBooks = bookService.getBooksByAccessType(Book.AccessType.FREE);
        if (allFreeBooks == null) {
            allFreeBooks = new ArrayList<>();
        }

        // Đảm bảo tất cả danh sách luôn có trong model (để tránh lỗi isEmpty() trong template)
        model.addAttribute("purchasedBooks", purchasedBooks);
        model.addAttribute("subscriptionBooks", subscriptionBooks != null ? subscriptionBooks : new ArrayList<>());
        model.addAttribute("freeBooks", allFreeBooks);

        // Thêm tổng số sách miễn phí
        model.addAttribute("totalFreeBooks", allFreeBooks.size());

        // Load tất cả dữ liệu cho các tab (không phân trang vì sẽ dùng JavaScript để chuyển tab)
        // Điều này giúp chuyển tab mượt mà không cần reload trang
        model.addAttribute("readingProgresses", readingProgresses);

        // Thêm thống kê
        model.addAttribute("totalReadingBooks", readingProgresses.size());
        model.addAttribute("totalPurchasedBooks", purchasedBooks.size());
        model.addAttribute("activeTab", tab != null && !tab.isEmpty() ? tab : "all");

        // Tính tổng số sách trong thư viện (đã mua + subscription + free)
        int totalLibraryBooks = purchasedBooks.size() + subscriptionBooks.size() + allFreeBooks.size();
        model.addAttribute("totalBooks", totalLibraryBooks);

        return "user/library";
    }

    /**
     * Lịch sử đọc sách
     */
    @GetMapping("/reading-history")
    public String readingHistory(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        User currentUser = getCurrentUser(authentication);
        List<ReadingProgress> readingProgresses = readingProgressService.getReadingProgressByUserWithBookDetails(currentUser).stream()
                .sorted((a, b) -> b.getLastReadAt() != null ? b.getLastReadAt().compareTo(a.getLastReadAt()) : 0)
                .toList();

        // Phân trang
        int pageSize = 12;
        int totalBooks = readingProgresses.size();
        int totalPages = (int) Math.ceil((double) totalBooks / pageSize);
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = Math.max(0, page * pageSize);
        int endIndex = Math.min(startIndex + pageSize, totalBooks);
        List<ReadingProgress> pagedProgress = readingProgresses.subList(startIndex, endIndex);

        model.addAttribute("readingProgresses", pagedProgress);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBooks", totalBooks);

        return "user/reading/reading-history";
    }
}
