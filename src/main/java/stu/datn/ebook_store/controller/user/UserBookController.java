package stu.datn.ebook_store.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.CategoryService;

import java.util.List;

/**
 * Controller xử lý trang user - duyệt sách, tìm kiếm, xem chi tiết
 * Endpoints: /books/*
 */
@Controller
@RequestMapping("/books")
public class UserBookController {

    private static final int PAGE_SIZE = 12;

    private final BookService bookService;
    private final CategoryService categoryService;

    @Autowired
    public UserBookController(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }

    /**
     * Trang danh sách sách với phân trang và lọc
     */
    @GetMapping
    public String booksList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String access,
            Model model) {

        // Lấy danh sách sách
        List<Book> books = bookService.getAllBooks();

        // Lọc theo category
        if (category != null && !category.isEmpty()) {
            books = books.stream()
                    .filter(b -> b.getBookCategory() != null &&
                            b.getBookCategory().getBookCategoryId().equals(category))
                    .toList();
            model.addAttribute("selectedCategory", category);
        }

        // Lọc theo access type
        if (access != null && !access.isEmpty()) {
            try {
                Book.AccessType accessType = Book.AccessType.valueOf(access.toUpperCase());
                books = books.stream()
                        .filter(b -> b.getAccessType() == accessType)
                        .toList();
            } catch (IllegalArgumentException e) {
                // Ignore invalid access type
            }
            model.addAttribute("selectedAccess", access);
        }

        // Sắp xếp
        if ("newest".equals(sort)) {
            books = books.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        } else if ("popular".equals(sort)) {
            books = books.stream()
                    .sorted((a, b) -> Integer.compare(b.getViewCount() != null ? b.getViewCount() : 0,
                                                      a.getViewCount() != null ? a.getViewCount() : 0))
                    .toList();
        } else if ("rating".equals(sort)) {
            books = books.stream()
                    .sorted((a, b) -> Float.compare(b.getAverageRating() != null ? b.getAverageRating() : 0,
                                                    a.getAverageRating() != null ? a.getAverageRating() : 0))
                    .toList();
        }

        // Phân trang
        int totalBooks = books.size();
        int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = Math.max(0, page * PAGE_SIZE);
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalBooks);
        List<Book> pagedBooks = books.subList(startIndex, endIndex);

        model.addAttribute("books", pagedBooks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("accessTypes", Book.AccessType.values());
        model.addAttribute("sortOptions", new String[]{"newest", "popular", "rating"});
        model.addAttribute("sort", sort);

        return "user/books/list";
    }

    /**
     * Trang chi tiết sách
     */
    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable String id, Model model) {
        Book book = bookService.getBookById(id).orElse(null);

        if (book == null) {
            return "redirect:/books?error=notfound";
        }

        // Tăng view count
        book.setViewCount(book.getViewCount() != null ? book.getViewCount() + 1 : 1);
        bookService.saveBook(book);

        // Lấy sách liên quan (cùng category)
        List<Book> relatedBooks = new java.util.ArrayList<>();
        if (book.getBookCategory() != null) {
            relatedBooks = bookService.getBooksByCategory(book.getBookCategory()).stream()
                    .filter(b -> !b.getBookId().equals(id))
                    .limit(4)
                    .toList();
        }

        model.addAttribute("book", book);
        model.addAttribute("relatedBooks", relatedBooks);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "user/books/view";
    }

    /**
     * Tìm kiếm sách
     */
    @GetMapping("/search")
    public String searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<Book> books = new java.util.ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            books = bookService.searchBooksByKeyword(keyword);
            model.addAttribute("keyword", keyword);
        }

        // Phân trang
        int totalBooks = books.size();
        int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = Math.max(0, page * PAGE_SIZE);
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalBooks);
        List<Book> pagedBooks = books.subList(startIndex, endIndex);

        model.addAttribute("books", pagedBooks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBooks", totalBooks);

        return "user/books/search";
    }

    /**
     * Xem sách theo danh mục
     */
    @GetMapping("/category/{categoryId}")
    public String booksByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // CategoryService trả về Category, nhưng BookService cần BookCategory
        // Sử dụng getBooksByCategory với lọc theo category ID
        List<Book> books = bookService.getAllBooks().stream()
                .filter(b -> b.getBookCategory() != null &&
                        b.getBookCategory().getBookCategoryId().equals(categoryId))
                .toList();

        // Phân trang
        int totalBooks = books.size();
        int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        int startIndex = Math.max(0, page * PAGE_SIZE);
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalBooks);
        List<Book> pagedBooks = books.subList(startIndex, endIndex);

        model.addAttribute("books", pagedBooks);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "user/books/category";
    }

    /**
     * Lấy sách theo access type (FREE, PURCHASE, SUBSCRIPTION)
     */
    @GetMapping("/access/{accessType}")
    public String booksByAccessType(
            @PathVariable String accessType,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        try {
            Book.AccessType type = Book.AccessType.valueOf(accessType.toUpperCase());
            List<Book> books = bookService.getBooksByAccessType(type);

            // Phân trang
            int totalBooks = books.size();
            int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);
            if (page >= totalPages && totalPages > 0) {
                page = totalPages - 1;
            }

            int startIndex = Math.max(0, page * PAGE_SIZE);
            int endIndex = Math.min(startIndex + PAGE_SIZE, totalBooks);
            List<Book> pagedBooks = books.subList(startIndex, endIndex);

            model.addAttribute("books", pagedBooks);
            model.addAttribute("accessType", type);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalBooks", totalBooks);

            return "user/books/by-access-type";
        } catch (IllegalArgumentException e) {
            return "redirect:/books?error=invalid_access_type";
        }
    }

    /**
     * Sách hot/trending
     */
    @GetMapping("/trending")
    public String trendingBooks(Model model) {
        List<Book> books = bookService.getTopViewedBooks();
        model.addAttribute("books", books);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "user/books/trending";
    }

    /**
     * Sách mới nhất
     */
    @GetMapping("/newest")
    public String newestBooks(Model model) {
        List<Book> books = bookService.getNewestBooks();
        model.addAttribute("books", books);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "user/books/newest";
    }

    /**
     * Sách được đánh giá cao
     */
    @GetMapping("/top-rated")
    public String topRatedBooks(Model model) {
        List<Book> books = bookService.getTopRatedBooks(20);
        model.addAttribute("books", books);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "user/books/top-rated";
    }
}

