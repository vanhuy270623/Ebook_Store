package stu.datn.ebook_store.controller.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.BookAssetService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AdminDashboardController xử lý chức năng đọc sách
 * Hỗ trợ PDF và EPUB format
 * Tracking reading progress và bookmarks
 */
@Controller
@RequestMapping("/reading")
@Slf4j
public class ReadingController extends BaseController {

    private final BookService bookService;
    private final BookAssetService bookAssetService;
    private final ReadingProgressService readingProgressService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @Autowired
    public ReadingController(BookService bookService,
                             BookAssetService bookAssetService,
                             ReadingProgressService readingProgressService,
                             OrderService orderService,
                             OrderItemService orderItemService) {
        this.bookService = bookService;
        this.bookAssetService = bookAssetService;
        this.readingProgressService = readingProgressService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
    }

    /**
     * Mở sách để đọc - trang chung cho cả PDF và EPUB
     */
    @GetMapping("/book/{bookId}")
    public String openBook(@PathVariable String bookId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            log.info("Opening book with ID: {}", bookId);
            User currentUser = getCurrentUser();

            // Kiểm tra user đã đăng nhập
            if (currentUser == null) {
                log.warn("User not authenticated, redirecting to login");
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }
            log.debug("User found: {} ({})", currentUser.getUsername(), currentUser.getUserId());

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
            log.debug("Book found: {}", book.getTitle());

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(currentUser, book)) {
                log.warn("User {} does not have access to book {}", currentUser.getUserId(), bookId);
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }

            // Lấy file asset của sách
            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);
            log.debug("Found {} assets for book {}", assets.size(), bookId);

            BookAsset readableAsset = assets.stream()
                    .filter(asset -> BookAsset.FileType.PDF.equals(asset.getFileType()) ||
                            BookAsset.FileType.EPUB.equals(asset.getFileType()))
                    .findFirst()
                    .orElse(null);

            if (readableAsset == null) {
                log.warn("No readable asset found for book {}", bookId);
                redirectAttributes.addFlashAttribute("error", "Sách này chưa có file đọc");
                return "redirect:/books/view/" + bookId;
            }

            log.info("Readable asset found: {} - {}", readableAsset.getFileType(), readableAsset.getFileUrl());

            // Kiểm tra file tồn tại trên disk
            // fileUrl có dạng: /book_asset/source/tamly-kynangsong/Dac nhan tam.pdf
            // Cần lấy phần sau /book_asset/source/ để ghép với base path
            String fileUrl = readableAsset.getFileUrl();
            String relativePath = fileUrl.replace("/book_asset/source/", "");
            String fullPath = "F:/datn_uploads/book_asset/source/" + relativePath;

            java.io.File file = new java.io.File(fullPath);
            if (!file.exists()) {
                log.error("File not found on disk: {}", fullPath);
                log.error("FileUrl from DB: {}", fileUrl);
                log.error("Relative path: {}", relativePath);
                redirectAttributes.addFlashAttribute("error", "File sách không tồn tại trên hệ thống");
                return "redirect:/books/view/" + bookId;
            }
            log.info("File exists on disk: {} (size: {} bytes)", fullPath, file.length());

            // Lấy hoặc tạo mới reading progress
            ReadingProgress progress = null;
            try {
                progress = readingProgressService
                        .getReadingProgressByUserAndBook(currentUser, book)
                        .orElseGet(() -> {
                            try {
                                log.info("Creating new reading progress for user {} and book {}", currentUser.getUserId(), book.getBookId());
                                ReadingProgress newProgress = new ReadingProgress();
                                // Không set progressId - để service tự generate với format prog_XX
                                newProgress.setUser(currentUser);
                                newProgress.setBook(book);
                                newProgress.setBookAsset(readableAsset);
                                newProgress.setProgressPercentage(0.0f);
                                newProgress.setIsCompleted(false);
                                newProgress.setIsFavorite(false);
                                newProgress.setAccessType(determineAccessType(book, currentUser));
                                newProgress.setCreatedAt(LocalDateTime.now());
                                newProgress.setLastReadAt(LocalDateTime.now());
                                ReadingProgress saved = readingProgressService.saveReadingProgress(newProgress);
                                log.info("Reading progress created successfully: {}", saved.getProgressId());
                                return saved;
                            } catch (Exception e) {
                                log.error("Error creating reading progress: {}", e.getMessage(), e);
                                throw new RuntimeException("Cannot create reading progress: " + e.getMessage(), e);
                            }
                        });
            } catch (Exception e) {
                log.error("Error with reading progress: {}", e.getMessage(), e);
                // Nếu lỗi tạo progress, vẫn cho phép đọc nhưng không track progress
                log.warn("Continuing without progress tracking");
                progress = null;
            }

            // Tăng view count nếu cần
            // bookService.incrementViewCount(bookId);

            model.addAttribute("book", book);
            model.addAttribute("asset", readableAsset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", currentUser);

            // Chuyển hướng đến reader phù hợp
            if (BookAsset.FileType.PDF.equals(readableAsset.getFileType())) {
                return "user/reading/pdf-viewer";
            } else {
                return "user/reading/epub-viewer";
            }

        } catch (Exception e) {
            log.error("Error opening book {}: {}", bookId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi mở sách: " + e.getMessage());
            return "redirect:/books";
        }
    }


    /**
     * PDF Viewer - sử dụng PDF.js
     */
    @GetMapping("/pdf/{bookId}")
    public String pdfViewer(@PathVariable String bookId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "PDF", model, redirectAttributes, "user/reading/pdf-viewer");
    }

    /**
     * EPUB Reader - sử dụng ePub.js
     */
    @GetMapping("/epub/{bookId}")
    public String epubReader(@PathVariable String bookId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "EPUB", model, redirectAttributes, "user/reading/epub-viewer");
    }

    /**
     * Trang reader chung với auto-detect format - TỰ ĐỘNG CHUYỂN ĐẾN VIEWER
     */
    @GetMapping("/reader/{bookId}")
    public String reader(@PathVariable String bookId,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        // Redirect đến /choose-format để kiểm tra số lượng file và tự động chọn
        return "redirect:/reading/choose-format/" + bookId;
    }

    /**
     * Trang chọn format đọc sách (PDF/EPUB)
     * - Nếu chỉ có 1 file → TỰ ĐỘNG load luôn
     * - Nếu có cả 2 file → Hiển thị trang chọn
     */
    @GetMapping("/choose-format/{bookId}")
    public String chooseFormat(@PathVariable String bookId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            if (!canUserAccessBook(user, book)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }

            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);

            // Lọc các file đọc được
            List<BookAsset> readableAssets = assets.stream()
                    .filter(asset -> BookAsset.FileType.PDF.equals(asset.getFileType()) ||
                            BookAsset.FileType.EPUB.equals(asset.getFileType()))
                    .toList();

            if (readableAssets.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Sách này chưa có file đọc");
                return "redirect:/books/view/" + bookId;
            }

            // Kiểm tra có bao nhiêu loại file
            boolean hasPDF = readableAssets.stream().anyMatch(a -> BookAsset.FileType.PDF.equals(a.getFileType()));
            boolean hasEPUB = readableAssets.stream().anyMatch(a -> BookAsset.FileType.EPUB.equals(a.getFileType()));

            // Nếu CHỈ có 1 loại file → TỰ ĐỘNG redirect
            if (hasPDF && !hasEPUB) {
                log.info("Book {} only has PDF, auto-redirecting to PDF viewer", bookId);
                return "redirect:/reading/pdf/" + bookId;
            }

            if (hasEPUB && !hasPDF) {
                log.info("Book {} only has EPUB, auto-redirecting to EPUB viewer", bookId);
                return "redirect:/reading/epub/" + bookId;
            }

            // Nếu có CẢ 2 loại → Hiển thị trang chọn format
            log.info("Book {} has both PDF and EPUB, showing format chooser", bookId);

            BookAsset firstAsset = readableAssets.get(0);

            // Lấy hoặc tạo mới reading progress
            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(user, book)
                    .orElseGet(() -> {
                        ReadingProgress newProgress = new ReadingProgress();
                        // Không set progressId - để service tự generate với format prog_XX
                        newProgress.setUser(user);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(firstAsset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book, user));
                        newProgress.setCreatedAt(LocalDateTime.now());
                        newProgress.setLastReadAt(LocalDateTime.now());
                        return readingProgressService.saveReadingProgress(newProgress);
                    });

            model.addAttribute("book", book);
            model.addAttribute("asset", firstAsset);
            model.addAttribute("assets", readableAssets); // Truyền tất cả assets
            model.addAttribute("hasPDF", hasPDF);
            model.addAttribute("hasEPUB", hasEPUB);
            model.addAttribute("progress", progress);
            model.addAttribute("user", user);

            // Hiển thị trang chọn format
            return "user/reading/reader";

        } catch (Exception e) {
            log.error("Error loading format chooser for book {}: {}", bookId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi mở sách");
            return "redirect:/books";
        }
    }


    /**
     * API: Lưu reading progress
     */
    @PostMapping("/api/progress/{bookId}")
    @ResponseBody
    public String saveProgress(@PathVariable String bookId,
                               @RequestParam(required = false) Integer currentPage,
                               @RequestParam(required = false) Integer totalPages,
                               @RequestParam(required = false) String location,
                               @RequestParam(required = false) Float percentage) {
        try {
            log.info("=== SAVE PROGRESS API CALLED ===");
            log.info("bookId: {}, currentPage: {}, totalPages: {}, location: {}, percentage: {}",
                    bookId, currentPage, totalPages, location, percentage);

            User user = getCurrentUser();
            if (user == null) {
                log.error("User not authenticated for progress save");
                return "{\"status\":\"error\",\"message\":\"User not authenticated\"}";
            }
            log.info("User authenticated: {}", user.getUserId());

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            log.info("Book found: {} ({})", book.getTitle(), book.getBookId());

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(user, book)) {
                log.warn("User {} does not have access to book {}", user.getUserId(), bookId);
                return "{\"status\":\"error\",\"message\":\"Bạn không có quyền đọc cuốn sách này\"}";
            }

            ReadingProgress progress = readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);

            if (progress == null) {
                log.info("Creating NEW ReadingProgress for user {} and book {}", user.getUserId(), bookId);
                progress = new ReadingProgress();
                progress.setUser(user);
                progress.setBook(book);
                progress.setCreatedAt(LocalDateTime.now());
                progress.setAccessType(determineAccessType(book, user));
                progress.setIsCompleted(false);
                progress.setIsFavorite(false);
            } else {
                log.info("Found existing progress: {}", progress.getProgressId());
            }

            // FIXED: Lưu location cho cả PDF và EPUB
            // - EPUB: location là CFI string (epubcfi(...))
            // - PDF: location là "page-X" hoặc null (dùng currentPage)
            if (location != null && !location.trim().isEmpty()) {
                progress.setLastReadLocation(location);
                log.info("Saved location: {}", location);
            } else if (currentPage != null) {
                // PDF fallback
                progress.setLastReadLocation("page-" + currentPage);
                log.info("Saved location (PDF): page-{}", currentPage);
            }

            // Tính phần trăm progress
            float calculatedPercentage;
            if (percentage != null) {
                // EPUB gửi percentage trực tiếp
                calculatedPercentage = percentage;
            } else if (totalPages != null && totalPages > 0 && currentPage != null) {
                // PDF tính từ currentPage/totalPages
                calculatedPercentage = ((float) currentPage / totalPages) * 100;
            } else {
                calculatedPercentage = 0;
            }

            // Đảm bảo không vượt 100%
            calculatedPercentage = Math.min(Math.round(calculatedPercentage * 100.0f) / 100.0f, 100.0f);
            progress.setProgressPercentage(calculatedPercentage);

            // Đánh dấu hoàn thành nếu đọc hết
            // >= 95% để tránh lỗi làm tròn hoặc đã đến trang cuối
            if (calculatedPercentage >= 95.0f || (currentPage != null && totalPages != null && currentPage >= totalPages)) {
                progress.setIsCompleted(true);
                progress.setProgressPercentage(100.0f);
            } else {
                progress.setIsCompleted(false);
            }

            progress.setLastReadAt(LocalDateTime.now());

            log.info("Saving progress - location: {}, percentage: {}%, isCompleted: {}",
                    progress.getLastReadLocation(), calculatedPercentage, progress.getIsCompleted());

            ReadingProgress savedProgress = readingProgressService.saveReadingProgress(progress);

            log.info("=== PROGRESS SAVED SUCCESSFULLY ===");
            log.info("Progress ID: {}, Location: {}, Percentage: {}%",
                    savedProgress.getProgressId(), savedProgress.getLastReadLocation(), savedProgress.getProgressPercentage());

            return "{\"status\":\"success\",\"message\":\"Progress saved\",\"percentage\":" + calculatedPercentage + "}";
        } catch (Exception e) {
            log.error("=== ERROR SAVING PROGRESS ===");
            log.error("Error saving reading progress: {}", e.getMessage(), e);
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * API: Lấy reading progress
     */
    @GetMapping("/api/progress/{bookId}")
    @ResponseBody
    public ReadingProgress getProgress(@PathVariable String bookId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                log.error("User not authenticated");
                return null;
            }
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(user, book)) {
                log.warn("User {} does not have access to book {}", user.getUserId(), bookId);
                return null;
            }

            return readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
        } catch (Exception e) {
            log.error("Error getting reading progress: {}", e.getMessage());
            return null;
        }
    }

    /**
     * API: Thêm bookmark mới
     */
    @PostMapping("/api/bookmarks/{bookId}")
    @ResponseBody
    public String addBookmark(@PathVariable String bookId,
                              @RequestParam String location,
                              @RequestParam(required = false) Integer pageNumber,
                              @RequestParam(required = false) Float percentage,
                              @RequestParam(required = false) String note) {
        try {
            log.info("=== ADD BOOKMARK REQUEST ===");
            log.info("bookId: {}, location: {}, pageNumber: {}, percentage: {}, note: {}",
                    bookId, location, pageNumber, percentage, note);

            User user = getCurrentUser();
            if (user == null) {
                log.error("User not authenticated");
                return "{\"status\":\"error\",\"message\":\"User not authenticated\"}";
            }
            log.info("User: {}", user.getUserId());

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
            log.info("Book found: {}", book.getTitle());

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(user, book)) {
                log.warn("User {} does not have access to book {}", user.getUserId(), bookId);
                return "{\"status\":\"error\",\"message\":\"Bạn không có quyền đọc cuốn sách này\"}";
            }

            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(user, book)
                    .orElse(null);

            // Tự động tạo progress nếu chưa có
            if (progress == null) {
                log.info("Creating new ReadingProgress for user {} and book {}", user.getUserId(), bookId);
                progress = new ReadingProgress();
                progress.setUser(user);
                progress.setBook(book);
                progress.setAccessType(determineAccessType(book, user));
                progress = readingProgressService.saveReadingProgress(progress);
                log.info("Created progress with ID: {}", progress.getProgressId());
            }

            log.info("Progress ID: {}", progress.getProgressId());

            readingProgressService.addBookmark(
                    progress.getProgressId(),
                    location,
                    pageNumber,
                    percentage,
                    note
            );

            log.info("=== BOOKMARK ADDED SUCCESSFULLY ===");
            return "{\"status\":\"success\",\"message\":\"Bookmark added\"}";
        } catch (Exception e) {
            log.error("Error adding bookmark: {}", e.getMessage(), e);
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * API: Xóa bookmark
     */
    @DeleteMapping("/api/bookmarks/{bookId}/{bookmarkId}")
    @ResponseBody
    public String removeBookmark(@PathVariable String bookId,
                                 @PathVariable String bookmarkId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "{\"status\":\"error\",\"message\":\"User not authenticated\"}";
            }

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(user, book)
                    .orElse(null);

            if (progress == null) {
                return "{\"status\":\"error\",\"message\":\"Reading progress not found\"}";
            }

            readingProgressService.removeBookmark(progress.getProgressId(), bookmarkId);

            return "{\"status\":\"success\",\"message\":\"Bookmark removed\"}";
        } catch (Exception e) {
            log.error("Error removing bookmark: {}", e.getMessage());
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * API: Lấy danh sách bookmarks
     */
    @GetMapping("/api/bookmarks/{bookId}")
    @ResponseBody
    public List<ReadingProgress.BookmarkData> getBookmarks(@PathVariable String bookId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return new java.util.ArrayList<>();
            }

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(user, book)) {
                log.warn("User {} does not have access to book {}", user.getUserId(), bookId);
                return new java.util.ArrayList<>();
            }

            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(user, book)
                    .orElse(null);

            if (progress == null) {
                return new java.util.ArrayList<>();
            }

            return readingProgressService.getBookmarks(progress.getProgressId());
        } catch (Exception e) {
            log.error("Error getting bookmarks: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * API: Toggle reading mode (dark/light)
     */
    @PostMapping("/api/toggle-mode")
    @ResponseBody
    public String toggleReadingMode(@RequestParam String mode) {
        try {
            // Lưu preference vào session hoặc user preferences
            // Tạm thời return success
            return "{\"status\":\"success\",\"mode\":\"" + mode + "\"}";
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"Failed to toggle mode\"}";
        }
    }

    /**
     * Helper method to prepare reader view
     */
    private String prepareReaderView(String bookId, String expectedType,
                                     Model model, RedirectAttributes redirectAttributes, String viewName) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            if (!canUserAccessBook(user, book)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }

            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);
            BookAsset asset = assets.stream()
                    .filter(a -> BookAsset.FileType.valueOf(expectedType).equals(a.getFileType()))
                    .findFirst()
                    .orElse(null);

            if (asset == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy file " + expectedType + " cho cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }

            // Lấy hoặc tạo mới reading progress
            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(user, book)
                    .orElseGet(() -> {
                        ReadingProgress newProgress = new ReadingProgress();
                        // Không set progressId - để service tự generate với format prog_XX
                        newProgress.setUser(user);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(asset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book, user));
                        newProgress.setCreatedAt(LocalDateTime.now());
                        newProgress.setLastReadAt(LocalDateTime.now());
                        return readingProgressService.saveReadingProgress(newProgress);
                    });

            // bookService.incrementViewCount(bookId);

            model.addAttribute("book", book);
            model.addAttribute("asset", asset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", user);

            // Encode lastReadLocation để tránh lỗi HTML attribute với ký tự đặc biệt
            if (progress != null && progress.getLastReadLocation() != null) {
                try {
                    String encodedLocation = java.util.Base64.getEncoder()
                            .encodeToString(progress.getLastReadLocation().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    model.addAttribute("encodedLocation", encodedLocation);
                } catch (Exception e) {
                    log.warn("Could not encode location: {}", e.getMessage());
                    model.addAttribute("encodedLocation", null);
                }
            } else {
                model.addAttribute("encodedLocation", null);
            }

            return viewName;

        } catch (Exception e) {
            log.error("Error preparing {} reader for book {}: {}", expectedType, bookId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi mở sách");
            return "redirect:/books";
        }
    }

    /**
     * Kiểm tra user có quyền đọc sách không
     */
    private boolean canUserAccessBook(User user, Book book) {
        // Admin có thể đọc mọi sách
        if (user.getRole() != null && user.getRole().getRoleName() != null &&
                user.getRole().getRoleName().name().equals("ADMIN")) {
            return true;
        }

        // Sách miễn phí thì ai cũng đọc được
        if (Book.AccessType.FREE.equals(book.getAccessType())) {
            return true;
        }

        Book.AccessType accessType = book.getAccessType();

        // Kiểm tra sách PURCHASE hoặc BOTH - user đã mua sách chưa
        if (accessType == Book.AccessType.PURCHASE || accessType == Book.AccessType.BOTH) {
            boolean hasPurchased = orderItemService.hasUserPurchasedBook(user.getUserId(), book.getBookId());
            if (hasPurchased) {
                log.debug("User {} has purchased book {}", user.getUserId(), book.getBookId());
                return true;
            }
        }

        // Kiểm tra sách SUBSCRIPTION hoặc BOTH - user có subscription active không
        if (accessType == Book.AccessType.SUBSCRIPTION || accessType == Book.AccessType.BOTH) {
            boolean hasActiveSubscription = hasActiveSubscription(user.getUserId());
            if (hasActiveSubscription) {
                log.debug("User {} has active subscription for book {}", user.getUserId(), book.getBookId());
                return true;
            }
        }

        // Không có quyền truy cập
        log.warn("User {} does not have access to book {} (accessType: {})",
                 user.getUserId(), book.getBookId(), accessType);
        return false;
    }

    /**
     * Kiểm tra user có subscription active không (không tính gói FREE)
     * Chỉ gói BASIC, PREMIUM, VIP mới được coi là có subscription
     *
     * QUAN TRỌNG: User hủy gói (CANCELLED) vẫn được duy trì quyền đến hết end_date
     * Ví dụ: Đăng ký 01/12 → 30/12, hủy 07/12 → Vẫn đọc đến 30/12
     */
    private boolean hasActiveSubscription(String userId) {
        try {
            List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);

            LocalDateTime now = LocalDateTime.now();
            return subscriptionOrders.stream()
                    .anyMatch(order -> {
                        // Kiểm tra payment status:
                        // - COMPLETED, PAID: Đang hoạt động bình thường
                        // - CANCELLED: Đã hủy NHƯNG vẫn duy trì quyền đến hết thời gian đã thanh toán
                        boolean isValidPaymentStatus = (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                                                        order.getPaymentStatus() == Order.PaymentStatus.PAID ||
                                                        order.getPaymentStatus() == Order.PaymentStatus.CANCELLED);

                        // Kiểm tra end_date còn hạn
                        boolean isNotExpired = order.getEndDate() != null &&
                                               order.getEndDate().isAfter(now);

                        if (!isValidPaymentStatus || !isNotExpired) {
                            return false;
                        }

                        // Kiểm tra package_name - LOẠI TRỪ GÓI FREE
                        if (order.getSubscription() != null &&
                            order.getSubscription().getPackageName() != null) {
                            String packageName = order.getSubscription().getPackageName().name();
                            boolean isValidPackage = !packageName.equals("FREE");

                            log.debug("User {} subscription check - Package: {}, PaymentStatus: {}, EndDate: {}, Valid: {}",
                                     userId, packageName, order.getPaymentStatus(), order.getEndDate(), isValidPackage);

                            return isValidPackage;
                        }

                        return false;
                    });
        } catch (Exception e) {
            log.error("Error checking subscription status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Xác định loại access type dựa trên book và user
     */
    private ReadingProgress.AccessType determineAccessType(Book book, User user) {
        if (Book.AccessType.FREE.equals(book.getAccessType())) {
            return ReadingProgress.AccessType.FREE;
        }

        // Kiểm tra user đã mua sách chưa
        boolean hasPurchased = orderItemService.hasUserPurchasedBook(user.getUserId(), book.getBookId());
        if (hasPurchased) {
            return ReadingProgress.AccessType.PURCHASED;
        }

        // Kiểm tra user có subscription active không
        boolean hasActiveSubscription = hasActiveSubscription(user.getUserId());
        if (hasActiveSubscription) {
            return ReadingProgress.AccessType.SUBSCRIPTION;
        }

        // Mặc định là FREE (không nên đến đây nếu logic canUserAccessBook đúng)
        return ReadingProgress.AccessType.FREE;
    }
}



