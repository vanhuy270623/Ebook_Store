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
import stu.datn.ebook_store.repository.BookCategoryRepository;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.AuthorService;

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
    private final stu.datn.ebook_store.service.FileStorageService fileStorageService;
    private final stu.datn.ebook_store.repository.BookAssetRepository bookAssetRepository;

    @Autowired
    public BookController(BookService bookService,
                          BookCategoryRepository bookCategoryRepository,
                          AuthorService authorService,
                          stu.datn.ebook_store.service.FileStorageService fileStorageService,
                          stu.datn.ebook_store.repository.BookAssetRepository bookAssetRepository) {
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
            // Upload cover image if provided
            if (coverImage != null && !coverImage.isEmpty()) {
                String imageUrl = bookService.uploadCoverImage(coverImage);
                request.setCoverImageUrl(imageUrl);
            }

            // Create book
            Book book = bookService.createBook(request);

            // Get category slug for file storage
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

    /**
     * Helper method to create slug from category name
     */
    private String createSlug(String text) {
        if (text == null || text.isEmpty()) {
            return "uncategorized";
        }

        // Convert Vietnamese characters to ASCII
        String slug = text.toLowerCase()
            .replaceAll("à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ", "a")
            .replaceAll("è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ", "e")
            .replaceAll("ì|í|ị|ỉ|ĩ", "i")
            .replaceAll("ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ", "o")
            .replaceAll("ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ", "u")
            .replaceAll("ỳ|ý|ỵ|ỷ|ỹ", "y")
            .replaceAll("đ", "d")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        return slug.isEmpty() ? "uncategorized" : slug;
    }

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

    /**
     * Map Book entity to BookUpdateRequest DTO
     */
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
        // Kiểm tra validation errors
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
            // Upload cover image if provided
            if (coverImage != null && !coverImage.isEmpty()) {
                String imageUrl = bookService.uploadCoverImage(coverImage);
                request.setCoverImageUrl(imageUrl);
            }

            // Update book
            Book book = bookService.updateBook(request);

            // Get category slug for file storage
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

    @PostMapping("/upload-cover")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadCoverImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String imageUrl = bookService.uploadCoverImage(file);
            response.put("success", true);
            response.put("url", imageUrl);
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
