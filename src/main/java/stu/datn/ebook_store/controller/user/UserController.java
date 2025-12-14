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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.UserUpdateRequest;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.OrderItem;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.ReadingProgressService;
import stu.datn.ebook_store.service.UserService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller xử lý trang user
 * Chức năng: dashboard, profile, order history, reading history
 * Endpoints: /user/*
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private static final String AVATAR_UPLOAD_DIR = "F:/datn_uploads/book_asset/image/avatars/";
    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    private final UserService userService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ReadingProgressService readingProgressService;
    private final PasswordEncoder passwordEncoder;
    private final BookService bookService;

    @Autowired
    public UserController(UserService userService, OrderService orderService,
                          OrderItemService orderItemService,
                          ReadingProgressService readingProgressService, PasswordEncoder passwordEncoder,
                          BookService bookService) {
        this.userService = userService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
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
            model.addAttribute("purchasedBookIds", getPurchasedBookIds(authentication));

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
        userRequest.setRoleId(currentUser.getRole() != null ? currentUser.getRole().getRoleId() : null);

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
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
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

            // Xử lý upload avatar nếu có file
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String avatarUrl = saveAvatar(avatarFile, currentUser.getUserId());
                    currentUser.setAvatarUrl(avatarUrl);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Lỗi khi upload avatar: " + e.getMessage());
                    return "redirect:/user/profile";
                }
            } else if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
                // Nếu không upload file mới nhưng có URL thì dùng URL
                currentUser.setAvatarUrl(request.getAvatarUrl());
            }

            // Cập nhật thông tin
            currentUser.setEmail(request.getEmail());
            currentUser.setFullName(request.getFullName());
            currentUser.setPhone(request.getPhone());
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
     * Lưu avatar vào thư mục avatars với tên là userId
     */
    private String saveAvatar(MultipartFile file, String userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IOException("File rỗng");
        }

        // Check file size (5MB max)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IOException("Kích thước file không được vượt quá 5MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Chỉ chấp nhận file ảnh (JPG, PNG, GIF)");
        }

        // Tạo thư mục nếu chưa có
        File uploadDir = new File(AVATAR_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("Không thể tạo thư mục upload");
            }
        }

        // Lấy extension của file
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // Validate extension
            if (!fileExtension.matches("\\.(jpg|jpeg|png|gif)$")) {
                throw new IOException("Chỉ chấp nhận định dạng JPG, PNG, GIF");
            }
        } else {
            // Default to jpg if no extension
            fileExtension = ".jpg";
        }

        // Tên file là userId + extension
        String fileName = userId + fileExtension.toLowerCase();
        Path filePath = Paths.get(AVATAR_UPLOAD_DIR + fileName);

        // Lưu file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về URL để truy cập avatar
        return "/book_asset/image/avatars/" + fileName;
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

        // Load order items for each order
        Map<String, List<OrderItem>> orderItemsMap = new HashMap<>();
        for (Order order : pagedOrders) {
            List<OrderItem> items = orderItemService.getOrderItemsByOrderId(order.getOrderId());
            orderItemsMap.put(order.getOrderId(), items);
        }

        model.addAttribute("orders", pagedOrders);
        model.addAttribute("orderItemsMap", orderItemsMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", totalOrders);

        return "user/order/orders";
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

        // Load order items for book orders
        if (order.getOrderType() == Order.OrderType.BOOK) {
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);
            model.addAttribute("orderItems", orderItems);
        }

        model.addAttribute("order", order);

        return "user/order/order-detail";
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

        // Thêm danh sách yêu thích (tạm thời empty list - TODO: implement favorites)
        model.addAttribute("favoriteBooks", new java.util.ArrayList<ReadingProgress>());

        // Thêm danh sách đã hoàn thành
        model.addAttribute("completedBooks", completedBooks);

        // Thêm danh sách sách miễn phí
        model.addAttribute("freeBooks", freeBooks);

        // Thêm các biến thống kê
        model.addAttribute("totalReading", readingProgresses.size());
        model.addAttribute("totalPurchased", purchasedBooks.size());
        model.addAttribute("totalSubscription", subscriptionBooks.size());
        model.addAttribute("totalFavorites", 0); // TODO: Implement favorites logic
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