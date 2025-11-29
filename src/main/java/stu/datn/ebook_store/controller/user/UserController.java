package stu.datn.ebook_store.controller.user;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.UserUpdateRequest;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.ReadingProgressService;
import stu.datn.ebook_store.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller xử lý trang user
 * Chức năng: dashboard, profile, order history, reading history
 * Endpoints: /user/*
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final OrderService orderService;
    private final ReadingProgressService readingProgressService;
    private final PasswordEncoder passwordEncoder;
    private final BookService bookService;

    @Autowired
    public UserController(UserService userService, OrderService orderService,
                         ReadingProgressService readingProgressService, PasswordEncoder passwordEncoder,
                         BookService bookService) {
        this.userService = userService;
        this.orderService = orderService;
        this.readingProgressService = readingProgressService;
        this.passwordEncoder = passwordEncoder;
        this.bookService = bookService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
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
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("booksReading", booksReading);
        model.addAttribute("booksCompleted", booksCompleted);
        model.addAttribute("totalBooks", totalBooks);

        return "user/dashboard";
    }

    /**
     * Trang hồ sơ người dùng
     */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        UserUpdateRequest userRequest = new UserUpdateRequest();
        userRequest.setUserId(currentUser.getUserId());
        userRequest.setEmail(currentUser.getEmail());
        userRequest.setFullName(currentUser.getFullName());
        userRequest.setPhone(currentUser.getPhone());
        userRequest.setAvatarUrl(currentUser.getAvatarUrl());

        model.addAttribute("user", currentUser);
        model.addAttribute("userRequest", userRequest);

        return "user/profile";
    }

    /**
     * Cập nhật hồ sơ người dùng
     */
    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute("userRequest") UserUpdateRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            model.addAttribute("user", currentUser);
            return "user/profile";
        }

        try {
            // Kiểm tra email trùng (trừ email hiện tại)
            if (!request.getEmail().equals(currentUser.getEmail()) &&
                userService.checkEmailExists(request.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng");
                return "redirect:/user/profile";
            }

            // Cập nhật thông tin
            currentUser.setEmail(request.getEmail());
            currentUser.setFullName(request.getFullName());
            currentUser.setPhone(request.getPhone());
            currentUser.setAvatarUrl(request.getAvatarUrl());
            currentUser.setUpdatedAt(LocalDateTime.now());

            userService.saveUser(currentUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công");

            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /**
     * Đổi mật khẩu
     */
    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);

        try {
            // Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(currentPassword, currentUser.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng");
                return "redirect:/user/profile";
            }

            // Kiểm tra mật khẩu mới trùng nhau
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không trùng khớp");
                return "redirect:/user/profile";
            }

            // Kiểm tra mật khẩu mới không trống
            if (newPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới không được để trống");
                return "redirect:/user/profile";
            }

            // Cập nhật mật khẩu
            currentUser.setPasswordHash(passwordEncoder.encode(newPassword));
            userService.saveUser(currentUser);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");

            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /**
     * Lịch sử đơn hàng
     */
    @GetMapping("/orders")
    public String orderHistory(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        User currentUser = getCurrentUser(authentication);
        List<Order> orders = orderService.getOrdersByUser(currentUser).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        // Phân trang
        int pageSize = 10;
        int totalOrders = orders.size();
        int totalPages = (int) Math.ceil((double) totalOrders / pageSize);
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = Math.max(0, page * pageSize);
        int endIndex = Math.min(startIndex + pageSize, totalOrders);
        List<Order> pagedOrders = orders.subList(startIndex, endIndex);

        model.addAttribute("orders", pagedOrders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", totalOrders);

        return "user/orders";
    }

    /**
     * Chi tiết đơn hàng
     */
    @GetMapping("/orders/{orderId}")
    public String orderDetail(
            @PathVariable String orderId,
            Model model,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.getOrderById(orderId).orElse(null);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/user/orders";
        }

        model.addAttribute("order", order);

        return "user/order-detail";
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

        return "user/reading-history";
    }

    /**
     * Sách yêu thích (favorites)
     */
    @GetMapping("/favorites")
    public String favorites(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // TODO: Implement favorites logic when Favorite entity is available
        // For now, return empty list

        model.addAttribute("favorites", List.of());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 0);
        model.addAttribute("totalBooks", 0);

        return "user/favorites";
    }
}

