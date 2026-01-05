package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.BookAssetRepository;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.DownloadAuthorizationService;
import stu.datn.ebook_store.service.FileStorageService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * AdminDashboardController xử lý tải xuống sách
 * Endpoint: /books/download/{bookId}
 *
 * FLOW:
 * 1. Xác thực user (đã đăng nhập)
 * 2. Kiểm tra quyền tải xuống (canDownload)
 * 3. Tìm file BookAsset
 * 4. Stream file về cho user
 */
@Controller
@RequestMapping("/books/download")
public class BookDownloadController extends BaseController {

    private final BookService bookService;
    private final DownloadAuthorizationService downloadAuthService;
    private final BookAssetRepository bookAssetRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public BookDownloadController(
            BookService bookService,
            DownloadAuthorizationService downloadAuthService,
            BookAssetRepository bookAssetRepository,
            FileStorageService fileStorageService) {
        this.bookService = bookService;
        this.downloadAuthService = downloadAuthService;
        this.bookAssetRepository = bookAssetRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{bookId}")
    @ResponseBody
    public ResponseEntity<Resource> downloadBook(
            @PathVariable String bookId,
            @RequestParam(required = false) String fileType) {

        try {
            // 1. Kiểm tra authentication
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                        .body(null);
            }


            // 2. Tìm sách
            Optional<Book> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Book book = bookOpt.get();

            // 3. Kiểm tra quyền tải xuống
            if (!downloadAuthService.canDownload(currentUser, book)) {
                String reason = downloadAuthService.getDownloadDeniedReason(currentUser, book);
                return ResponseEntity.status(403)
                        .header("X-Download-Error", URLEncoder.encode(reason, StandardCharsets.UTF_8))
                        .body(null);
            }

            // 4. Tìm BookAsset theo fileType (nếu được chỉ định)
            Optional<BookAsset> assetOpt = Optional.empty();

            if (fileType != null && !fileType.isEmpty()) {
                // User đã chọn file type cụ thể (PDF hoặc EPUB)
                try {
                    BookAsset.FileType requestedType = BookAsset.FileType.valueOf(fileType.toUpperCase());
                    assetOpt = bookAssetRepository
                            .findByBook_BookIdAndFileType(bookId, requestedType);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(400)
                            .header("X-Download-Error", "Loại file không hợp lệ")
                            .body(null);
                }
            } else {
                // Tự động chọn: ưu tiên EPUB, sau đó PDF
                assetOpt = bookAssetRepository
                        .findByBook_BookIdAndFileType(bookId, BookAsset.FileType.EPUB);

                if (assetOpt.isEmpty()) {
                    assetOpt = bookAssetRepository
                            .findByBook_BookIdAndFileType(bookId, BookAsset.FileType.PDF);
                }
            }

            if (assetOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .header("X-Download-Error", "Không tìm thấy file sách")
                        .body(null);
            }

            BookAsset asset = assetOpt.get();

            // 5. Load file từ storage
            Path filePath = fileStorageService.loadFile(asset.getFileUrl());

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(404)
                        .header("X-Download-Error", "File không tồn tại trên server")
                        .body(null);
            }

            // 6. Get file size
            long fileSize = Files.size(filePath);

            // 7. Create InputStreamResource with BufferedInputStream for better performance
            // This ensures proper file handle closure and prevents file locking issues
            java.io.InputStream inputStream = new java.io.BufferedInputStream(
                    Files.newInputStream(filePath, java.nio.file.StandardOpenOption.READ)
            );
            Resource resource = new org.springframework.core.io.InputStreamResource(inputStream);

            // 8. Xác định Content-Type
            String contentType = determineContentType(asset.getFileType());

            // 9. Tạo tên file download (có dấu tiếng Việt)
            String fileName = sanitizeFileName(book.getTitle()) + getFileExtension(asset.getFileType());
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 10. Trả về file stream
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .header("X-Download-Error", "Lỗi khi tải file: " + e.getMessage())
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .header("X-Download-Error", "Lỗi hệ thống: " + e.getMessage())
                    .body(null);
        }
    }

    /**
     * Xác định Content-Type dựa trên loại file
     */
    private String determineContentType(BookAsset.FileType fileType) {
        return switch (fileType) {
            case PDF -> "application/pdf";
            case EPUB -> "application/epub+zip";
        };
    }

    /**
     * Lấy extension file
     */
    private String getFileExtension(BookAsset.FileType fileType) {
        return switch (fileType) {
            case PDF -> ".pdf";
            case EPUB -> ".epub";
        };
    }

    /**
     * Làm sạch tên file (loại bỏ ký tự đặc biệt)
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "ebook";
        }
        // Giữ lại chữ cái, số, dấu tiếng Việt, khoảng trắng
        return fileName.replaceAll("[^a-zA-ZÀ-ỹ0-9\\s\\-_]", "")
                       .replaceAll("\\s+", "_")
                       .trim();
    }
}

