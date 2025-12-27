package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.dto.request.BookCreateRequest;
import stu.datn.ebook_store.dto.request.BookUpdateRequest;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;
import stu.datn.ebook_store.repository.BookAssetRepository;
import stu.datn.ebook_store.repository.BookCategoryRepository;
import stu.datn.ebook_store.service.AuthorService;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.FileStorageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/books")
public class BookController extends BaseController {

    private final BookService bookService;
    private final BookCategoryRepository bookCategoryRepository;
    private final AuthorService authorService;
    private final FileStorageService fileStorageService;
    private final BookAssetRepository bookAssetRepository;

    @Autowired
    public BookController(BookService bookService,
                          BookCategoryRepository bookCategoryRepository,
                          AuthorService authorService,
                          FileStorageService fileStorageService,
                          BookAssetRepository bookAssetRepository) {
        this.bookService = bookService;
        this.bookCategoryRepository = bookCategoryRepository;
        this.authorService = authorService;
        this.fileStorageService = fileStorageService;
        this.bookAssetRepository = bookAssetRepository;
    }

    @GetMapping
    public String booksList(Model model) {
        List<Book> books = bookService.getAllBooks();
        model.addAttribute("books", books);
        model.addAttribute("totalBooks", books.size());
        return "admin/books/list";
    }

    @GetMapping("/add")
    public String addBookForm(Model model) {
        model.addAttribute("bookRequest", new BookCreateRequest());
        model.addAttribute("categories", bookCategoryRepository.findAll());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("accessTypes", Book.AccessType.values());
        model.addAttribute("isEdit", false);
        return "admin/books/form";
    }

    @PostMapping("/create")
    public String addBook(@Valid @ModelAttribute("bookRequest") BookCreateRequest request,
                          BindingResult bindingResult,
                          @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                          @RequestParam(value = "sourceFilePdf", required = false) MultipartFile sourceFilePdf,
                          @RequestParam(value = "sourceFileEpub", required = false) MultipartFile sourceFileEpub,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            model.addAttribute("categories", bookCategoryRepository.findAll());
            model.addAttribute("authors", authorService.getAllAuthors());
            model.addAttribute("accessTypes", Book.AccessType.values());
            model.addAttribute("isEdit", false);
            return "admin/books/form";
        }

        try {
            // --- CẬP NHẬT LOGIC LƯU ẢNH BÌA ---
            if (coverImage != null && !coverImage.isEmpty()) {
                // Lấy tên danh mục để tạo đường dẫn thư mục
                String categoryName = "uncategorized";
                if (request.getBookCategoryId() != null) {
                    categoryName = bookCategoryRepository.findById(request.getBookCategoryId())
                            .map(BookCategory::getCategoryName)
                            .orElse("uncategorized");
                }

                // Gọi hàm lưu bìa sách mới: storeBookCover(file, categoryName, bookTitle)
                // Path: book_asset/image/covers/{categorySlug}/{bookSlug}.jpg
                String imageUrl = fileStorageService.storeBookCover(coverImage, categoryName, request.getTitle());
                request.setCoverImageUrl("/" + imageUrl);
            }

            // Create book
            Book book = bookService.createBook(request);

            // Get category slug for file storage (PDF/EPUB)
            String categorySlug = book.getBookCategory() != null ?
                    book.getBookCategory().getCategorySlug() : "uncategorized";

            // Upload PDF file if provided
            if (sourceFilePdf != null && !sourceFilePdf.isEmpty()) {
                String pdfUrl = fileStorageService.storeBookSource(sourceFilePdf, categorySlug);
                createBookAsset(book, pdfUrl, stu.datn.ebook_store.entity.BookAsset.FileType.PDF, sourceFilePdf.getSize());
            }

            // Upload EPUB file if provided
            if (sourceFileEpub != null && !sourceFileEpub.isEmpty()) {
                String epubUrl = fileStorageService.storeBookSource(sourceFileEpub, categorySlug);
                createBookAsset(book, epubUrl, stu.datn.ebook_store.entity.BookAsset.FileType.EPUB, sourceFileEpub.getSize());
            }

            redirectAttributes.addFlashAttribute("success", "Thêm sách mới thành công!");
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/books/add";
        }
    }

    /**
     * Helper method to create BookAsset entity
     */
    private void createBookAsset(Book book, String fileUrl, stu.datn.ebook_store.entity.BookAsset.FileType fileType, long fileSize) {
        stu.datn.ebook_store.entity.BookAsset asset = new stu.datn.ebook_store.entity.BookAsset();
        asset.setBookAssetId(java.util.UUID.randomUUID().toString());
        asset.setBook(book);
        asset.setFileUrl(fileUrl);
        asset.setFileType(fileType);
        asset.setFileSize(fileSize);
        bookAssetRepository.save(asset);
    }

    // Hàm createSlug cũ không còn cần thiết ở Controller vì logic đã chuyển sang Service
    // Nhưng nếu bạn dùng nó cho mục đích khác thì giữ lại, ở đây tôi comment out để code gọn
    /*
    private String createSlug(String text) { ... }
    */

    @GetMapping("/edit/{id}")
    public String editBookForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Book book = bookService.getBookById(id).orElse(null);
        if (book == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sách với ID: " + id);
            return "redirect:/admin/books";
        }

        // Map Book entity to BookUpdateRequest DTO
        BookUpdateRequest bookUpdateRequest = mapToUpdateRequest(book);

        model.addAttribute("bookRequest", bookUpdateRequest);
        model.addAttribute("bookEntity", book);
        model.addAttribute("categories", bookCategoryRepository.findAll());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("accessTypes", Book.AccessType.values());
        model.addAttribute("isEdit", true);

        // Get current author IDs
        Set<String> currentAuthorIds = book.getAuthors().stream()
                .map(Author::getAuthorId)
                .collect(Collectors.toSet());
        model.addAttribute("currentAuthorIds", currentAuthorIds);

        return "admin/books/form";
    }

    private BookUpdateRequest mapToUpdateRequest(Book book) {
        BookUpdateRequest dto = new BookUpdateRequest();
        dto.setBookId(book.getBookId());
        dto.setTitle(book.getTitle());
        dto.setDescription(book.getDescription());
        dto.setPrice(book.getPrice());
        dto.setCoverImageUrl(book.getCoverImageUrl());
        dto.setPublisher(book.getPublisher());
        dto.setPublicationYear(book.getPublicationYear());
        dto.setLanguage(book.getLanguage());
        dto.setPages(book.getPages());
        dto.setIsbn(book.getIsbn());
        dto.setAccessType(book.getAccessType());
        dto.setIsDownloadable(book.getIsDownloadable());

        if (book.getBookCategory() != null) {
            dto.setBookCategoryId(book.getBookCategory().getBookCategoryId());
        }

        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            Set<String> authorIds = book.getAuthors().stream()
                    .map(Author::getAuthorId)
                    .collect(Collectors.toSet());
            dto.setAuthorIds(authorIds);
        }

        return dto;
    }

    @PostMapping("/update")
    public String editBook(@Valid @ModelAttribute("bookRequest") BookUpdateRequest request,
                           BindingResult bindingResult,
                           @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                           @RequestParam(value = "sourceFilePdf", required = false) MultipartFile sourceFilePdf,
                           @RequestParam(value = "sourceFileEpub", required = false) MultipartFile sourceFileEpub,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);

            Book book = bookService.getBookById(request.getBookId()).orElse(null);
            model.addAttribute("bookEntity", book);
            model.addAttribute("categories", bookCategoryRepository.findAll());
            model.addAttribute("authors", authorService.getAllAuthors());
            model.addAttribute("accessTypes", Book.AccessType.values());
            model.addAttribute("isEdit", true);

            return "admin/books/form";
        }

        try {
            // --- CẬP NHẬT LOGIC LƯU ẢNH BÌA KHI EDIT ---
            if (coverImage != null && !coverImage.isEmpty()) {
                String categoryName = "uncategorized";
                if (request.getBookCategoryId() != null) {
                    categoryName = bookCategoryRepository.findById(request.getBookCategoryId())
                            .map(BookCategory::getCategoryName)
                            .orElse("uncategorized");
                }

                String imageUrl = fileStorageService.storeBookCover(coverImage, categoryName, request.getTitle());
                request.setCoverImageUrl("/" + imageUrl);
            }

            // Update book
            Book book = bookService.updateBook(request);
            String categorySlug = book.getBookCategory() != null && book.getBookCategory().getCategorySlug() != null ?
                    book.getBookCategory().getCategorySlug() : "uncategorized";

            // Upload PDF file if provided
            if (sourceFilePdf != null && !sourceFilePdf.isEmpty()) {
                // Delete old PDF asset if exists
                book.getBookAssets().stream()
                        .filter(asset -> asset.getFileType() == stu.datn.ebook_store.entity.BookAsset.FileType.PDF)
                        .findFirst()
                        .ifPresent(asset -> {
                            fileStorageService.deleteFile(asset.getFileUrl());
                            bookAssetRepository.delete(asset);
                        });

                // Upload new PDF
                String pdfUrl = fileStorageService.storeBookSource(sourceFilePdf, categorySlug);
                createBookAsset(book, pdfUrl, stu.datn.ebook_store.entity.BookAsset.FileType.PDF, sourceFilePdf.getSize());
            }

            // Upload EPUB file if provided
            if (sourceFileEpub != null && !sourceFileEpub.isEmpty()) {
                // Delete old EPUB asset if exists
                book.getBookAssets().stream()
                        .filter(asset -> asset.getFileType() == stu.datn.ebook_store.entity.BookAsset.FileType.EPUB)
                        .findFirst()
                        .ifPresent(asset -> {
                            fileStorageService.deleteFile(asset.getFileUrl());
                            bookAssetRepository.delete(asset);
                        });

                // Upload new EPUB
                String epubUrl = fileStorageService.storeBookSource(sourceFileEpub, categorySlug);
                createBookAsset(book, epubUrl, stu.datn.ebook_store.entity.BookAsset.FileType.EPUB, sourceFileEpub.getSize());
            }

            redirectAttributes.addFlashAttribute("success", "Cập nhật sách thành công!");
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/books/edit/" + request.getBookId();
        }
    }

    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable String id, Model model) {
        return bookService.getBookById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    return "admin/books/view";
                })
                .orElse("redirect:/admin/books?error=notfound");
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBook(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            bookService.deleteBook(id);
            response.put("success", true);
            response.put("message", "Xóa sách thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa sách";
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API Upload nhanh (cho Ajax)
     * Vì API này thường gửi lên trước khi có thông tin sách đầy đủ,
     * nên ta lưu tạm vào thư mục covers chung hoặc dùng tên ngẫu nhiên.
     */
    @PostMapping("/upload-cover")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadCoverImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Lưu vào thư mục covers với tên ngẫu nhiên (hoặc temp)
            // book_asset/image/covers/temp_uuid.jpg
            String imageUrl = fileStorageService.storeFile(file, "book_asset/image/covers");

            response.put("success", true);
            response.put("url", "/" + imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/statistics")
    public String booksStatistics(Model model) {
        model.addAttribute("totalBooks", bookService.getTotalBooksCount());
        model.addAttribute("freeBooks", bookService.getFreeBooks());
        model.addAttribute("paidBooks", bookService.getPaidBooks());
        model.addAttribute("subscriptionBooks", bookService.getSubscriptionBooks());
        model.addAttribute("recentBooks", bookService.getRecentBooks(10));
        model.addAttribute("topRatedBooks", bookService.getTopRatedBooks(10));
        return "admin/books/statistics";
    }
}