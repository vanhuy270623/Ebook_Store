package stu.datn.ebook_store.controller.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.BookAssetService;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller xử lý chức năng đọc sách
 * Hỗ trợ PDF và EPUB format
 * Tracking reading progress và bookmarks
 */
@Controller
@RequestMapping("/reading")
@RequiredArgsConstructor
@Slf4j
public class ReadingController {

    private final BookService bookService;
    private final BookAssetService bookAssetService;
    private final ReadingProgressService readingProgressService;

    /**
     * Helper method: Lấy User hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * Mở sách để đọc - trang chung cho cả PDF và EPUB
     */
    @GetMapping("/book/{bookId}")
    public String openBook(@PathVariable String bookId,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            log.info("Opening book with ID: {}", bookId);

            // Kiểm tra user đã đăng nhập
            User user = getCurrentUser(authentication);
            if (user == null) {
                log.warn("User not authenticated, redirecting to login");
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }
            log.debug("User found: {} ({})", user.getUsername(), user.getUserId());

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
            log.debug("Book found: {}", book.getTitle());

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(user, book)) {
                log.warn("User {} does not have access to book {}", user.getUserId(), bookId);
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
                        .getReadingProgressByUserAndBook(user, book)
                        .orElseGet(() -> {
                            try {
                                log.info("Creating new reading progress for user {} and book {}", user.getUserId(), book.getBookId());
                                ReadingProgress newProgress = new ReadingProgress();
                                newProgress.setProgressId(UUID.randomUUID().toString());
                                newProgress.setUser(user);
                                newProgress.setBook(book);
                                newProgress.setBookAsset(readableAsset);
                                newProgress.setProgressPercentage(0.0f);
                                newProgress.setIsCompleted(false);
                                newProgress.setIsFavorite(false);
                                newProgress.setAccessType(determineAccessType(book));
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
            model.addAttribute("user", user);

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
     * Test page để kiểm tra PDF loading
     */
    @GetMapping("/test-pdf-load")
    public String testPDFLoad() {
        return "test/test-pdf-load";
    }

    /**
     * Test page để kiểm tra EPUB loading
     */
    @GetMapping("/test-epub-load")
    public String testEPUBLoad() {
        return "test/test-epub-load";
    }

    /**
     * Test endpoint để kiểm tra book assets
     */
    @GetMapping("/test/{bookId}")
    @ResponseBody
    public String testBook(@PathVariable String bookId) {
        try {
            StringBuilder result = new StringBuilder();
            result.append("=== TEST BOOK: ").append(bookId).append(" ===\n\n");

            // Check book exists
            Book book = bookService.getBookById(bookId).orElse(null);
            if (book == null) {
                return result.append("❌ Book not found!").toString();
            }
            result.append("✅ Book found: ").append(book.getTitle()).append("\n");
            result.append("   Access Type: ").append(book.getAccessType()).append("\n\n");

            // Check assets
            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);
            result.append("📁 Total Assets: ").append(assets.size()).append("\n");

            for (BookAsset asset : assets) {
                result.append("\n   Asset ID: ").append(asset.getBookAssetId()).append("\n");
                result.append("   File Type: ").append(asset.getFileType()).append("\n");
                result.append("   File URL: ").append(asset.getFileUrl()).append("\n");
                result.append("   File Size: ").append(asset.getFileSize()).append(" bytes\n");
            }

            // Check readable assets
            BookAsset readable = assets.stream()
                    .filter(asset -> BookAsset.FileType.PDF.equals(asset.getFileType()) ||
                                   BookAsset.FileType.EPUB.equals(asset.getFileType()))
                    .findFirst()
                    .orElse(null);

            if (readable != null) {
                result.append("\n✅ Readable asset found: ").append(readable.getFileType()).append("\n");
                result.append("   URL: ").append(readable.getFileUrl()).append("\n");
                String fullPath = "F:/datn_uploads/book_asset/source/" + readable.getFileUrl();
                result.append("   Full Path: ").append(fullPath).append("\n");

                // Check file exists
                java.io.File file = new java.io.File(fullPath);
                if (file.exists()) {
                    result.append("   ✅ File exists on disk\n");
                    result.append("   File size: ").append(file.length()).append(" bytes\n");
                } else {
                    result.append("   ❌ File NOT found on disk!\n");
                }
            } else {
                result.append("\n❌ No readable asset found!\n");
            }

            return result.toString().replace("\n", "<br>");
        } catch (Exception e) {
            return "❌ ERROR: " + e.getMessage() + "<br><br>Stack trace:<br>" +
                   java.util.Arrays.toString(e.getStackTrace()).replace(",", "<br>");
        }
    }

    /**
     * PDF Viewer - sử dụng PDF.js
     */
    @GetMapping("/pdf/{bookId}")
    public String pdfViewer(@PathVariable String bookId,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "PDF", authentication, model, redirectAttributes, "user/reading/pdf-viewer");
    }

    /**
     * EPUB Reader - sử dụng ePub.js
     */
    @GetMapping("/epub/{bookId}")
    public String epubReader(@PathVariable String bookId,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        return prepareReaderView(bookId, "EPUB", authentication, model, redirectAttributes, "user/reading/epub-viewer");
    }

    /**
     * Trang reader chung với auto-detect format - TỰ ĐỘNG CHUYỂN ĐẾN VIEWER
     */
    @GetMapping("/reader/{bookId}")
    public String reader(@PathVariable String bookId,
                        Authentication authentication,
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
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
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
                        newProgress.setProgressId(UUID.randomUUID().toString());
                        newProgress.setUser(user);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(firstAsset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book));
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
                              @RequestParam int currentPage,
                              @RequestParam int totalPages,
                              @RequestParam(required = false) String bookmarkData,
                              Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            if (user == null) {
                return "{\"status\":\"error\",\"message\":\"User not authenticated\"}";
            }

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            ReadingProgress progress = readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
            if (progress == null) {
                progress = new ReadingProgress();
                progress.setProgressId(UUID.randomUUID().toString());
                progress.setUser(user);
                progress.setBook(book);
                progress.setCreatedAt(LocalDateTime.now());
                progress.setAccessType(determineAccessType(book));
                progress.setIsCompleted(false);
                progress.setIsFavorite(false);
            }

            // Lưu location data (có thể là page number cho PDF hoặc CFI cho EPUB)
            if (bookmarkData != null && !bookmarkData.trim().isEmpty()) {
                progress.setLastReadLocation(bookmarkData);
            } else {
                progress.setLastReadLocation(String.valueOf(currentPage));
            }

            // Tính phần trăm progress
            float percentage = ((float) currentPage / totalPages) * 100;
            progress.setProgressPercentage(percentage);

            // Đánh dấu hoàn thành nếu đọc hết
            if (percentage >= 99.0f) {
                progress.setIsCompleted(true);
            }

            progress.setLastReadAt(LocalDateTime.now());

            readingProgressService.saveReadingProgress(progress);

            return "{\"status\":\"success\",\"message\":\"Progress saved\",\"percentage\":" + percentage + "}";
        } catch (Exception e) {
            log.error("Error saving reading progress: {}", e.getMessage());
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * API: Lấy reading progress
     */
    @GetMapping("/api/progress/{bookId}")
    @ResponseBody
    public ReadingProgress getProgress(@PathVariable String bookId, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            if (user == null) {
                log.error("User not authenticated");
                return null;
            }
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            return readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
        } catch (Exception e) {
            log.error("Error getting reading progress: {}", e.getMessage());
            return null;
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
    private String prepareReaderView(String bookId, String expectedType, Authentication authentication,
                                   Model model, RedirectAttributes redirectAttributes, String viewName) {
        try {
            User user = getCurrentUser(authentication);
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
                        newProgress.setProgressId(UUID.randomUUID().toString());
                        newProgress.setUser(user);
                        newProgress.setBook(book);
                        newProgress.setBookAsset(asset);
                        newProgress.setProgressPercentage(0.0f);
                        newProgress.setIsCompleted(false);
                        newProgress.setIsFavorite(false);
                        newProgress.setAccessType(determineAccessType(book));
                        newProgress.setCreatedAt(LocalDateTime.now());
                        newProgress.setLastReadAt(LocalDateTime.now());
                        return readingProgressService.saveReadingProgress(newProgress);
                    });

            // bookService.incrementViewCount(bookId);

            model.addAttribute("book", book);
            model.addAttribute("asset", asset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", user);

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

        // TODO: Kiểm tra user đã mua sách chưa (qua Orders)
        // TODO: Kiểm tra subscription active

        // Tạm thời cho phép đọc tất cả để test
        return true;
    }

    /**
     * Xác định loại access type dựa trên book
     */
    private ReadingProgress.AccessType determineAccessType(Book book) {
        if (Book.AccessType.FREE.equals(book.getAccessType())) {
            return ReadingProgress.AccessType.FREE;
        }
        // TODO: Check nếu user mua sách thì return PURCHASED
        // TODO: Check nếu user có subscription thì return SUBSCRIPTION
        return ReadingProgress.AccessType.FREE; // Default
    }
}
