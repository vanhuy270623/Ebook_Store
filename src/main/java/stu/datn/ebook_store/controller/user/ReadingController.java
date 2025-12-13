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
import stu.datn.ebook_store.repository.UserRepository;

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
    private final UserRepository userRepository;

    /**
     * Mở sách để đọc - trang chung cho cả PDF và EPUB
     */
    @GetMapping("/book/{bookId}")
    public String openBook(@PathVariable String bookId,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra user đã đăng nhập
            if (authentication == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }

            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            // Kiểm tra quyền truy cập
            if (!canUserAccessBook(user, book)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }

            // Lấy file asset của sách
            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);
            BookAsset readableAsset = assets.stream()
                    .filter(asset -> BookAsset.FileType.PDF.equals(asset.getFileType()) ||
                                   BookAsset.FileType.EPUB.equals(asset.getFileType()))
                    .findFirst()
                    .orElse(null);

            if (readableAsset == null) {
                redirectAttributes.addFlashAttribute("error", "Sách này chưa có file đọc");
                return "redirect:/books/view/" + bookId;
            }

            // Lấy reading progress hiện tại
            ReadingProgress progress = null;
            try {
                progress = readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
            } catch (Exception e) {
                log.debug("No existing reading progress found");
            }

            // Tăng view count - implement this method later
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
            log.error("Error opening book {}: {}", bookId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi mở sách");
            return "redirect:/books";
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
     * Trang reader chung với auto-detect format
     */
    @GetMapping("/reader/{bookId}")
    public String reader(@PathVariable String bookId,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            if (!canUserAccessBook(user, book)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đọc cuốn sách này");
                return "redirect:/books/view/" + bookId;
            }

            List<BookAsset> assets = bookAssetService.getAssetsByBookId(bookId);
            BookAsset readableAsset = assets.stream()
                    .filter(asset -> BookAsset.FileType.PDF.equals(asset.getFileType()) ||
                                   BookAsset.FileType.EPUB.equals(asset.getFileType()))
                    .findFirst()
                    .orElse(null);

            if (readableAsset == null) {
                redirectAttributes.addFlashAttribute("error", "Sách này chưa có file đọc");
                return "redirect:/books/view/" + bookId;
            }

            ReadingProgress progress = null;
            try {
                progress = readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
            } catch (Exception e) {
                log.debug("No existing reading progress found in reader method");
            }
            // bookService.incrementViewCount(bookId);

            model.addAttribute("book", book);
            model.addAttribute("asset", readableAsset);
            model.addAttribute("progress", progress);
            model.addAttribute("user", user);

            return "user/reading/reader";

        } catch (Exception e) {
            log.error("Error loading reader for book {}: {}", bookId, e.getMessage());
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
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Book book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            ReadingProgress progress = readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
            if (progress == null) {
                progress = new ReadingProgress();
                progress.setProgressId(UUID.randomUUID().toString());
                progress.setUser(user);
                progress.setBook(book);
                progress.setCreatedAt(LocalDateTime.now());
            }

            progress.setLastReadLocation(String.valueOf(currentPage));
            progress.setProgressPercentage((float) currentPage / totalPages * 100);
            progress.setLastReadAt(LocalDateTime.now());
            // progress.setUpdatedAt(LocalDateTime.now()); // This field may not exist

            if (bookmarkData != null && !bookmarkData.trim().isEmpty()) {
                // Store bookmark data in lastReadLocation or create a separate field
                progress.setLastReadLocation(bookmarkData);
            }

            readingProgressService.saveReadingProgress(progress);

            return "{\"status\":\"success\",\"message\":\"Progress saved\"}";
        } catch (Exception e) {
            log.error("Error saving reading progress: {}", e.getMessage());
            return "{\"status\":\"error\",\"message\":\"Failed to save progress\"}";
        }
    }

    /**
     * API: Lấy reading progress
     */
    @GetMapping("/api/progress/{bookId}")
    @ResponseBody
    public ReadingProgress getProgress(@PathVariable String bookId, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
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
            if (authentication == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đọc sách");
                return "redirect:/auth/login";
            }

            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
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

            ReadingProgress progress = null;
            try {
                progress = readingProgressService.getReadingProgressByUserAndBook(user, book).orElse(null);
            } catch (Exception e) {
                log.debug("No existing reading progress found in prepareReaderView");
            }
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
}
