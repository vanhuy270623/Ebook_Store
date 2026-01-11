package stu.datn.ebook_store.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller xử lý báo cáo thống kê
 * Endpoints: /admin/reports/*
 */
@Controller
@RequestMapping("/admin/reports")
public class ReportController extends BaseController {

    private final OrderService orderService;
    private final BookService bookService;
    private final UserService userService;

    @Autowired
    public ReportController(OrderService orderService,
                           BookService bookService,
                           UserService userService) {
        this.orderService = orderService;
        this.bookService = bookService;
        this.userService = userService;
    }

    /**
     * Báo cáo doanh thu
     * GET /admin/reports/sales
     */
    @GetMapping("/sales")
    public String salesReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            // Default: 30 ngày gần nhất
            LocalDateTime start = startDate != null ?
                LocalDateTime.parse(startDate + "T00:00:00") :
                LocalDateTime.now().minusDays(30);

            LocalDateTime end = endDate != null ?
                LocalDateTime.parse(endDate + "T23:59:59") :
                LocalDateTime.now();

            // Lấy tất cả orders đã hoàn thành trong khoảng thời gian
            List<Order> completedOrders = orderService.getAllOrders().stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                            o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().isAfter(start) &&
                            o.getCreatedAt().isBefore(end))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(Collectors.toList());

            // Tính tổng doanh thu
            BigDecimal totalRevenue = completedOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Doanh thu theo ngày
            Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            completedOrders.forEach(order -> {
                String date = order.getCreatedAt().format(formatter);
                BigDecimal amount = order.getTotalAmount() != null ?
                    order.getTotalAmount() : BigDecimal.ZERO;
                dailyRevenue.merge(date, amount, BigDecimal::add);
            });

            // Doanh thu theo phương thức thanh toán
            Map<String, BigDecimal> revenueByMethod = completedOrders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getPaymentMethod() != null ?
                         o.getPaymentMethod().toString() : "UNKNOWN",
                    Collectors.reducing(BigDecimal.ZERO,
                        o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                        BigDecimal::add)
                ));

            // Doanh thu theo loại đơn hàng (BOOK vs SUBSCRIPTION) - Item f)
            Map<String, BigDecimal> revenueByType = completedOrders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getOrderType() != null ?
                         o.getOrderType().toString() : "UNKNOWN",
                    Collectors.reducing(BigDecimal.ZERO,
                        o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                        BigDecimal::add)
                ));

            // Tính doanh thu hôm nay
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

            BigDecimal todayRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().isAfter(todayStart) &&
                            o.getCreatedAt().isBefore(todayEnd))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính doanh thu tháng này
            LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            BigDecimal thisMonthRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().isAfter(monthStart))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính doanh thu năm này
            LocalDateTime yearStart = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            BigDecimal thisYearRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().isAfter(yearStart))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính số lượng đơn hàng hôm nay
            long todayOrdersCount = completedOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().isAfter(todayStart) &&
                            o.getCreatedAt().isBefore(todayEnd))
                .count();

            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalOrders", completedOrders.size());
            model.addAttribute("averageOrderValue",
                completedOrders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(completedOrders.size()), 2, BigDecimal.ROUND_HALF_UP));
            model.addAttribute("dailyRevenue", dailyRevenue);
            model.addAttribute("revenueByMethod", revenueByMethod);
            model.addAttribute("revenueByType", revenueByType);
            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("thisMonthRevenue", thisMonthRevenue);
            model.addAttribute("thisYearRevenue", thisYearRevenue);
            model.addAttribute("todayOrdersCount", todayOrdersCount);
            model.addAttribute("orders", completedOrders);
            model.addAttribute("startDate", start.toLocalDate());
            model.addAttribute("endDate", end.toLocalDate());

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải dữ liệu: " + e.getMessage());
        }

        return "admin/reports/sales";
    }

    /**
     * Báo cáo sách bán chạy
     * GET /admin/reports/books
     */
    @GetMapping("/books")
    public String booksReport(
            @RequestParam(required = false, defaultValue = "30") int days,
            Model model) {

        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);

            // Lấy orders đã hoàn thành
            List<Order> completedOrders = orderService.getAllOrders().stream()
                .filter(o -> (o.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                             o.getPaymentStatus() == Order.PaymentStatus.PAID) &&
                            o.getCreatedAt() != null &&
                            o.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

            // Thống kê sách bán chạy (từ OrderItems)
            Map<Book, Long> bookSalesCount = new HashMap<>();
            Map<Book, BigDecimal> bookRevenue = new HashMap<>();

            completedOrders.forEach(order ->
                order.getOrderItems().forEach(item -> {
                    Book book = item.getBook();
                    bookSalesCount.merge(book, 1L, Long::sum);
                    BigDecimal price = item.getPriceAtPurchase() != null ? item.getPriceAtPurchase() : BigDecimal.ZERO;
                    bookRevenue.merge(book, price, BigDecimal::add);
                })
            );

            // Sort theo số lượng bán
            List<Map.Entry<Book, Long>> topBooks = bookSalesCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(20)
                .collect(Collectors.toList());

            // Tạo Map authorNames để tránh lazy loading issue trong Thymeleaf
            Map<Book, String> authorNames = new HashMap<>();
            topBooks.forEach(entry -> {
                Book book = entry.getKey();
                if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                    // Lấy tác giả đầu tiên
                    String authorName = book.getAuthors().stream()
                        .findFirst()
                        .map(author -> author.getName())
                        .orElse("Chưa có tác giả");
                    authorNames.put(book, authorName);
                } else {
                    authorNames.put(book, "Chưa có tác giả");
                }
            });

            model.addAttribute("topBooks", topBooks);
            model.addAttribute("bookRevenue", bookRevenue);
            model.addAttribute("authorNames", authorNames);
            model.addAttribute("days", days);
            model.addAttribute("totalBooksSold",
                bookSalesCount.values().stream().mapToLong(Long::longValue).sum());

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải dữ liệu: " + e.getMessage());
        }

        return "admin/reports/books";
    }

    /**
     * Báo cáo người dùng
     * GET /admin/reports/users
     */
    @GetMapping("/users")
    public String usersReport(Model model) {

        try {
            // Thống kê users
            long totalUsers = userService.getTotalUsersCount();
            long activeUsers = userService.getActiveUsersCount();
            long verifiedUsers = userService.getVerifiedUsersCount();

            // Users mới theo tháng
            List<Map<String, Object>> newUsersByMonth = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 5; i >= 0; i--) {
                LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

                long count = userService.getAllUsers().stream()
                    .filter(u -> u.getCreatedAt() != null &&
                                u.getCreatedAt().isAfter(monthStart) &&
                                u.getCreatedAt().isBefore(monthEnd))
                    .count();

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", monthStart.format(DateTimeFormatter.ofPattern("MM/yyyy")));
                monthData.put("count", count);
                newUsersByMonth.add(monthData);
            }

            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("activeUsers", activeUsers);
            model.addAttribute("verifiedUsers", verifiedUsers);
            model.addAttribute("inactiveUsers", totalUsers - activeUsers);
            model.addAttribute("newUsersByMonth", newUsersByMonth);
            model.addAttribute("recentUsers", userService.getRecentUsers(10));

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải dữ liệu: " + e.getMessage());
        }

        return "admin/reports/users";
    }
}

