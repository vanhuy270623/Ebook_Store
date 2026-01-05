package stu.datn.ebook_store.controller.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.dto.BookAssetDTO;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.BookRepository;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.BookAssetService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.OrderService;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private final BookRepository bookRepository;
    private final stu.datn.ebook_store.service.FileStorageService fileStorageService;

    @Autowired
    public ReadingController(BookService bookService,
                             BookAssetService bookAssetService,
                             ReadingProgressService readingProgressService,
                             OrderService orderService,
                             OrderItemService orderItemService,
                             BookRepository bookRepository,
                             stu.datn.ebook_store.service.FileStorageService fileStorageService) {
        this.bookService = bookService;
        this.bookAssetService = bookAssetService;
        this.readingProgressService = readingProgressService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.bookRepository = bookRepository;
        this.fileStorageService = fileStorageService;
    }


    /**
     * PDF Viewer - sử dụng PDF.js (route theo bookId)
     */
    @GetMapping("/pdf/{bookId}")
    public String pdfViewer(@PathVariable String bookId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "PDF", model, redirectAttributes, "user/reading/pdf-viewer");
    }

    /**
     * PDF Viewer - sử dụng PDF.js (route theo category/fileName)
     * URL format: /reading/pdf/tamly-kynangsong/Cac_The_Gioi_Song_Song_-_Michio_Kaku.pdf
     */
    @GetMapping("/pdf/{category}/{fileName:.+}")
    public String pdfViewerByPath(@PathVariable String category,
                                   @PathVariable String fileName,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            log.info("Opening PDF by path - category: {}, fileName: {}", category, fileName);
            User currentUser = getCurrentUser();

            // Kiểm tra user đã đăng nhập
            if (currentUser == null) {
                log.warn("User not authenticated, redirecting to login");
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }

            // Tìm book theo file path
            String fileUrl = "/book_asset/source/" + category + "/" + fileName;
            BookAsset asset = bookAssetService.findByFileUrl(fileUrl);

            if (asset == null) {
                log.error("Asset not found for fileUrl: {}", fileUrl);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy file sách");
                return "redirect:/books";
            }

            Book book = asset.getBook();
            if (book == null) {
                log.error("Book not found for asset: {}", asset.getBookAssetId());
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin sách");
                return "redirect:/books";
            }

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(currentUser, book)) {
                log.warn("User {} does not have access to book {}", currentUser.getUserId(), book.getBookId());
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + book.getBookId();
            }

            // Lấy hoặc tạo mới reading progress
            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(currentUser, book)
                    .orElseGet(() -> {
                        ReadingProgress newProgress = new ReadingProgress();
                        newProgress.setUser(currentUser);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(asset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book, currentUser));
                        newProgress.setCreatedAt(LocalDateTime.now());
                        newProgress.setLastReadAt(LocalDateTime.now());
                        return readingProgressService.saveReadingProgress(newProgress);
                    });

            // Debug logging for asset readingUrl
            log.info("📘 PDF Viewer rendering - Book: {}", book.getTitle());
            log.info("📁 Asset fileUrl: {}", asset.getFileUrl());
            log.info("🔗 Asset readingUrl: {}", asset.getReadingUrl());
            log.info("📄 Asset fileType: {}", asset.getFileType());

            model.addAttribute("book", book);
            model.addAttribute("asset", asset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", currentUser);

            // Encode lastReadLocation
            if (progress != null && progress.getLastReadLocation() != null) {
                try {
                    String encodedLocation = java.util.Base64.getEncoder()
                            .encodeToString(progress.getLastReadLocation().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    model.addAttribute("encodedLocation", encodedLocation);
                    log.debug("Encoded location: {}", encodedLocation);
                } catch (Exception e) {
                    log.warn("Could not encode location: {}", e.getMessage());
                    model.addAttribute("encodedLocation", null);
                }
            } else {
                model.addAttribute("encodedLocation", null);
            }

            log.info("✅ Rendering PDF viewer for book: {}", book.getBookId());
            return "user/reading/pdf-viewer";

        } catch (Exception e) {
            log.error("Error opening PDF by path: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi mở sách");
            return "redirect:/books";
        }
    }

    /**
     * EPUB Reader - sử dụng ePub.js (route theo bookId)
     */
    @GetMapping("/epub/{bookId}")
    public String epubReader(@PathVariable String bookId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "EPUB", model, redirectAttributes, "user/reading/epub-viewer");
    }

    /**
     * EPUB Reader - sử dụng ePub.js (route theo category/fileName)
     * URL format: /reading/epub/tamly-kynangsong/Cac_The_Gioi_Song_Song_-_Michio_Kaku.epub
     */
    @GetMapping("/epub/{category}/{fileName:.+}")
    public String epubReaderByPath(@PathVariable String category,
                                    @PathVariable String fileName,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            log.info("Opening EPUB by path - category: {}, fileName: {}", category, fileName);
            User currentUser = getCurrentUser();

            // Kiểm tra user đã đăng nhập
            if (currentUser == null) {
                log.warn("User not authenticated, redirecting to login");
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }

            // Tìm book theo file path
            String fileUrl = "/book_asset/source/" + category + "/" + fileName;
            BookAsset asset = bookAssetService.findByFileUrl(fileUrl);

            if (asset == null) {
                log.error("Asset not found for fileUrl: {}", fileUrl);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy file sách");
                return "redirect:/books";
            }

            Book book = asset.getBook();
            if (book == null) {
                log.error("Book not found for asset: {}", asset.getBookAssetId());
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin sách");
                return "redirect:/books";
            }

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(currentUser, book)) {
                log.warn("User {} does not have access to book {}", currentUser.getUserId(), book.getBookId());
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + book.getBookId();
            }

            // Lấy hoặc tạo mới reading progress
            ReadingProgress progress = readingProgressService
                    .getReadingProgressByUserAndBook(currentUser, book)
                    .orElseGet(() -> {
                        ReadingProgress newProgress = new ReadingProgress();
                        newProgress.setUser(currentUser);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(asset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book, currentUser));
                        newProgress.setCreatedAt(LocalDateTime.now());
                        newProgress.setLastReadAt(LocalDateTime.now());
                        return readingProgressService.saveReadingProgress(newProgress);
                    });

            // Debug logging for asset readingUrl
            log.info("📗 EPUB Viewer rendering - Book: {}", book.getTitle());
            log.info("📁 Asset fileUrl: {}", asset.getFileUrl());
            log.info("🔗 Asset readingUrl: {}", asset.getReadingUrl());
            log.info("📄 Asset fileType: {}", asset.getFileType());

            model.addAttribute("book", book);
            model.addAttribute("asset", asset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", currentUser);

            // Encode lastReadLocation
            if (progress != null && progress.getLastReadLocation() != null) {
                try {
                    String encodedLocation = java.util.Base64.getEncoder()
                            .encodeToString(progress.getLastReadLocation().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    model.addAttribute("encodedLocation", encodedLocation);
                    log.debug("Encoded location: {}", encodedLocation);
                } catch (Exception e) {
                    log.warn("Could not encode location: {}", e.getMessage());
                    model.addAttribute("encodedLocation", null);
                }
            } else {
                model.addAttribute("encodedLocation", null);
            }

            log.info("✅ Rendering EPUB viewer for book: {}", book.getBookId());
            return "user/reading/epub-viewer";

        } catch (Exception e) {
            log.error("Error opening EPUB by path: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi mở sách");
            return "redirect:/books";
        }
    }

    /**
     * Trang reader chung với auto-detect format - TỰ ĐỘNG CHUYỂN ĐẾN VIEWER
     */
    @GetMapping("/reader/{bookId}")
    public String reader(@PathVariable String bookId,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        // Redirect đến /book để kiểm tra số lượng file và tự động chọn
        return "redirect:/reading/book/" + bookId;
    }

    /**
     * Trang chọn format đọc sách (PDF/EPUB)
     * - Nếu chỉ có 1 file → TỰ ĐỘNG load luôn
     * - Nếu có cả 2 file → Hiển thị trang chọn
     *
     * URL: /reading/book/{bookId}
     */
    @GetMapping("/book/{bookId}")
    public String chooseFormat(@PathVariable String bookId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }

            Book book = bookRepository.findByIdWithAuthors(bookId)
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
            // Convert assets to DTO for JavaScript serialization
            List<BookAssetDTO> assetDTOs = readableAssets.stream()
                    .map(BookAssetDTO::fromEntity)
                    .collect(Collectors.toList());
            model.addAttribute("assets", assetDTOs);
            model.addAttribute("hasPDF", hasPDF);
            model.addAttribute("hasEPUB", hasEPUB);
            model.addAttribute("progress", progress);
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Đọc sách: " + book.getTitle());

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
    /**
     * Kiểm tra user có quyền đọc sách không
     *
     * QUAN TRỌNG: Người dùng BẮT BUỘC phải có subscription active (bao gồm cả gói FREE)
     * để đọc bất kỳ sách nào, kể cả sách có access_type = FREE
     */
    private boolean canUserAccessBook(User user, Book book) {
        // Admin có thể đọc mọi sách
        if (user.getRole() != null && user.getRole().getRoleName() != null &&
                user.getRole().getRoleName().name().equals("ADMIN")) {
            return true;
        }

        // KIỂM TRA BẮT BUỘC: User phải có subscription active (bao gồm cả FREE)
        boolean hasAnyActiveSubscription = hasAnyActiveSubscription(user.getUserId());
        if (!hasAnyActiveSubscription) {
            log.warn("User {} does not have any active subscription (including FREE)", user.getUserId());
            return false;
        }

        Book.AccessType accessType = book.getAccessType();

        // Sách miễn phí - chỉ cần có subscription (kể cả FREE)
        if (Book.AccessType.FREE.equals(accessType)) {
            return true;
        }

        // Kiểm tra sách PURCHASE hoặc BOTH - user đã mua sách chưa
        if (accessType == Book.AccessType.PURCHASE || accessType == Book.AccessType.BOTH) {
            boolean hasPurchased = orderItemService.hasUserPurchasedBook(user.getUserId(), book.getBookId());
            if (hasPurchased) {
                log.debug("User {} has purchased book {}", user.getUserId(), book.getBookId());
                return true;
            }
        }

        // Kiểm tra sách SUBSCRIPTION hoặc BOTH - user có subscription PREMIUM không (không tính FREE)
        if (accessType == Book.AccessType.SUBSCRIPTION || accessType == Book.AccessType.BOTH) {
            boolean hasPremiumSubscription = hasActiveSubscription(user.getUserId());
            if (hasPremiumSubscription) {
                log.debug("User {} has premium subscription for book {}", user.getUserId(), book.getBookId());
                return true;
            }
        }

        // Không có quyền truy cập
        log.warn("User {} does not have access to book {} (accessType: {})",
                 user.getUserId(), book.getBookId(), accessType);
        return false;
    }

    /**
     * Kiểm tra user có BẤT KỲ subscription active nào không (bao gồm cả FREE)
     * Dùng để kiểm tra điều kiện bắt buộc phải có gói đăng ký
     */
    private boolean hasAnyActiveSubscription(String userId) {
        try {
            List<Order> subscriptionOrders = orderService.getOrdersByUserIdAndType(userId, Order.OrderType.SUBSCRIPTION);

            LocalDateTime now = LocalDateTime.now();
            return subscriptionOrders.stream()
                    .anyMatch(order -> {
                        // Kiểm tra payment status
                        boolean isValidPaymentStatus = (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED ||
                                                        order.getPaymentStatus() == Order.PaymentStatus.PAID);

                        // Kiểm tra end_date còn hạn
                        boolean isNotExpired = order.getEndDate() != null &&
                                               order.getEndDate().isAfter(now);

                        return isValidPaymentStatus && isNotExpired;
                    });
        } catch (Exception e) {
            log.error("Error checking any subscription status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra user có subscription PREMIUM active không (không tính gói FREE)
     * Chỉ gói BASIC, PREMIUM, VIP mới được coi là có subscription premium
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

    /**
     * Xử lý khi user không có quyền đọc sách
     * Redirect về subscription plans nếu chưa có subscription
     * Redirect về book detail nếu đã có subscription nhưng không đủ quyền
     */
    private String handleAccessDenied(User user, String bookId, RedirectAttributes redirectAttributes) {
        // Kiểm tra xem user có subscription active không
        if (!hasAnyActiveSubscription(user.getUserId())) {
            redirectAttributes.addFlashAttribute("error",
                "Bạn cần kích hoạt gói đăng ký để đọc sách. Vui lòng kích hoạt gói FREE hoặc nâng cấp lên gói Premium.");
            return "redirect:/subscription/plans";
        }

        // User có subscription nhưng không đủ quyền đọc sách này (ví dụ: sách PREMIUM mà user dùng FREE)
        redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này. Vui lòng mua sách hoặc nâng cấp gói đăng ký.");
        return "redirect:/books/view/" + bookId;
    }

    /**
     * SECURE STREAMING ENDPOINT
     * Stream file content sau khi kiểm tra quyền truy cập
     * URL: /reading/stream/{bookId}
     *
     * Endpoint này thay thế việc truy cập trực tiếp vào /book_asset/source/**
     */
    @GetMapping("/stream/{bookId}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> streamFile(
            @PathVariable String bookId) {
        try {
            log.info("Streaming file request - bookId: {}", bookId);

            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("Unauthorized streaming attempt for book: {}", bookId);
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .build();
            }

            // Tìm asset theo bookId và fileType PDF (ưu tiên PDF trước)
            log.debug("Searching for asset with bookId: {}", bookId);

            BookAsset asset = bookAssetService.getAssetByBookIdAndFileType(bookId, BookAsset.FileType.PDF).orElse(null);

            // Fallback: Thử tìm EPUB nếu không có PDF
            if (asset == null) {
                log.warn("PDF asset not found for book {}, trying EPUB", bookId);
                asset = bookAssetService.getAssetByBookIdAndFileType(bookId, BookAsset.FileType.EPUB).orElse(null);
            }

            if (asset == null) {
                log.error("No asset found for book: {}", bookId);
                log.error("Please check database: SELECT * FROM bookassets WHERE book_id = '{}'", bookId);
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.NOT_FOUND)
                        .build();
            }

            log.info("✅ Found asset: {} (fileUrl: {})", asset.getBookAssetId(), asset.getFileUrl());

            Book book = asset.getBook();
            if (book == null) {
                log.error("Book not found for asset: {}", asset.getBookAssetId());
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.NOT_FOUND)
                        .build();
            }

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(currentUser, book)) {
                log.warn("User {} forbidden from streaming book {}", currentUser.getUserId(), book.getBookId());
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .build();
            }

            // Load file từ storage service (giống download controller)
            String fileUrl = asset.getFileUrl();
            java.nio.file.Path filePath = fileStorageService.loadFile(fileUrl);

            if (!java.nio.file.Files.exists(filePath)) {
                log.error("Physical file not found: {}", filePath);
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.NOT_FOUND)
                        .build();
            }

            // Xác định content type
            String contentType;
            if (asset.getFileType() == BookAsset.FileType.PDF) {
                contentType = "application/pdf";
            } else if (asset.getFileType() == BookAsset.FileType.EPUB) {
                contentType = "application/epub+zip";
            } else {
                contentType = "application/octet-stream";
            }

            // Extract filename from fileUrl for logging and header
            String fileName = java.nio.file.Paths.get(fileUrl).getFileName().toString();
            log.info("✅ Streaming file {} ({}) to user {}", fileName, contentType, currentUser.getUserId());

            // Get file size for content-length header
            long fileSize = java.nio.file.Files.size(filePath);

            // Use InputStreamResource with BufferedInputStream for better performance
            // This ensures proper file handle closure and prevents file locking issues
            java.io.InputStream inputStream = new java.io.BufferedInputStream(
                    java.nio.file.Files.newInputStream(filePath, java.nio.file.StandardOpenOption.READ)
            );

            org.springframework.core.io.InputStreamResource resource =
                    new org.springframework.core.io.InputStreamResource(inputStream);

            // Trả về file với headers phù hợp cho streaming
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20") + "\"")
                    .header(org.springframework.http.HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                    .header(org.springframework.http.HttpHeaders.EXPIRES, "0")
                    .contentLength(fileSize)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error streaming file for bookId {}: {}", bookId, e.getMessage(), e);
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * DEBUG ENDPOINT - List all book assets for a book
     * URL: /reading/debug/assets/{bookId}
     * Remove this in production!
     */
    @GetMapping("/debug/assets/{bookId}")
    @ResponseBody
    public String debugAssets(@PathVariable String bookId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "ERROR: Not authenticated";
            }

            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);

            if (assets.isEmpty()) {
                return "No assets found for book: " + bookId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== ASSETS FOR BOOK: ").append(bookId).append(" ===\n\n");

            for (BookAsset asset : assets) {
                sb.append("Asset ID: ").append(asset.getBookAssetId()).append("\n");
                sb.append("File Type: ").append(asset.getFileType()).append("\n");
                sb.append("File URL: ").append(asset.getFileUrl()).append("\n");
                sb.append("Reading URL: ").append(asset.getReadingUrl()).append("\n");
                sb.append("File Size: ").append(asset.getFileSize()).append(" bytes\n");
                sb.append("---\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}






