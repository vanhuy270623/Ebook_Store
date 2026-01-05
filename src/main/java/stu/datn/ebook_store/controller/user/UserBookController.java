package stu.datn.ebook_store.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.dto.BookAssetDTO;
import stu.datn.ebook_store.entity.*;
import stu.datn.ebook_store.service.BookAssetService;
import stu.datn.ebook_store.service.BookCategoryService;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderItemService;
import stu.datn.ebook_store.service.ReviewService;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Slf4j
/**
 * AdminDashboardController xử lý trang user - duyệt sách, tìm kiếm, xem chi tiết
 * Endpoints: /books/*
 */
@Controller
@RequestMapping("/books")
public class UserBookController extends BaseController {

    private static final int PAGE_SIZE = 12;
    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    private final BookService bookService;
    private final BookCategoryService bookCategoryService;
    private final OrderItemService orderItemService;
    private final ReviewService reviewService;
    private final BookAssetService bookAssetService;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserBookController(BookService bookService, BookCategoryService bookCategoryService,
                              OrderItemService orderItemService, ReviewService reviewService,
                              BookAssetService bookAssetService, ObjectMapper objectMapper) {
        this.bookService = bookService;
        this.bookCategoryService = bookCategoryService;
        this.orderItemService = orderItemService;
        this.reviewService = reviewService;
        this.bookAssetService = bookAssetService;
        this.objectMapper = objectMapper;
    }

    private Set<String> getPurchasedBookIds() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Set.of();
        }
        return new HashSet<>(orderItemService.getPurchasedBookIds(
                currentUser.getUserId(), Order.OrderType.BOOK, PAID_STATUSES, RETAIL_ACCESS_TYPES));
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

        // REDIRECT sang /categories/{slug} nếu user filter by category
        // URL thân thiện: /categories/van-hoc-trong-va-ngoai-nuoc thay vì /books?category=bcat_5
        if (category != null && !category.isEmpty()) {
            // Tìm category để lấy slug
            BookCategory bookCategory = bookCategoryService.getAllCategories().stream()
                    .filter(cat -> cat.getBookCategoryId().equals(category))
                    .findFirst()
                    .orElse(null);

            if (bookCategory != null && bookCategory.getCategorySlug() != null) {
                // Xây dựng redirect URL với slug
                StringBuilder redirectUrl = new StringBuilder("/categories/");
                redirectUrl.append(bookCategory.getCategorySlug());

                // Giữ nguyên các parameters khác
                boolean hasParams = false;
                if (sort != null && !sort.isEmpty()) {
                    redirectUrl.append("?sort=").append(sort);
                    hasParams = true;
                }
                if (page > 0) {
                    redirectUrl.append(hasParams ? "&" : "?").append("page=").append(page);
                }

                log.info("Redirecting /books?category={} to {}", category, redirectUrl);
                return "redirect:" + redirectUrl.toString();
            }
        }

        // Lấy danh sách sách (không filter by category ở đây nữa)
        List<Book> books = bookService.getAllBooks();

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
        model.addAttribute("categories", bookCategoryService.getAllCategories());
        model.addAttribute("accessTypes", Book.AccessType.values());
        model.addAttribute("sortOptions", new String[]{"newest", "popular", "rating"});
        model.addAttribute("sort", sort);
        model.addAttribute("purchasedBookIds", getPurchasedBookIds());

        return "user/books/list";
    }

    /**
     * Trang chi tiết sách
     */
    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable String id, Model model) {
        return bookService.getBookById(id)
                .map(book -> {
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

                    // Lấy reviews của sách (chỉ lấy reviews đã được duyệt)
                    List<Review> reviews = reviewService.getApprovedReviewsByBook(book);
                    long reviewCount = reviewService.countApprovedReviewsByBook(book);
                    Double avgRating = reviewService.getAverageRatingForBook(book);

                    // Check if current user has reviewed
                    Review userReview = null;
                    if (getCurrentUser() != null) {
                        userReview = reviewService.getReviewByUserAndBook(getCurrentUser(), book)
                                .orElse(null);
                    }

                    // Lấy danh sách assets để hiển thị options download
                    List<BookAsset> bookAssets = bookAssetService.getAssetsByBookId(id);
                    // Lọc chỉ lấy PDF và EPUB
                    List<BookAsset> downloadableAssets = bookAssets.stream()
                            .filter(asset -> asset.getFileType() == BookAsset.FileType.PDF ||
                                           asset.getFileType() == BookAsset.FileType.EPUB)
                            .toList();

                    // Convert to DTO để tránh circular reference và chỉ lấy info cần thiết
                    List<BookAssetDTO> assetDTOs = downloadableAssets.stream()
                            .map(BookAssetDTO::fromEntity)
                            .toList();

                    // Serialize to JSON string for JavaScript
                    String bookAssetsJson = "[]";
                    try {
                        bookAssetsJson = objectMapper.writeValueAsString(assetDTOs);
                        log.info("Serialized {} book assets for bookId {}: {}",
                                assetDTOs.size(), id, bookAssetsJson);
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing bookAssets to JSON", e);
                    }

                    model.addAttribute("book", book);
                    model.addAttribute("relatedBooks", relatedBooks);
                    model.addAttribute("categories", bookCategoryService.getAllCategories());
                    model.addAttribute("purchasedBookIds", getPurchasedBookIds());
                    model.addAttribute("currentUser", getCurrentUser());
                    model.addAttribute("reviews", reviews);
                    model.addAttribute("reviewCount", reviewCount);
                    model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
                    model.addAttribute("userReview", userReview);
                    model.addAttribute("bookAssets", downloadableAssets);
                    model.addAttribute("bookAssetsJson", bookAssetsJson);

                    return "user/books/view";
                })
                .orElse("redirect:/books?error=notfound");
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
        model.addAttribute("purchasedBookIds", getPurchasedBookIds());

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

        // BookCategoryService trả về Category, nhưng BookService cần BookCategory
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
        model.addAttribute("categories", bookCategoryService.getAllCategories());
        model.addAttribute("purchasedBookIds", getPurchasedBookIds());

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
            model.addAttribute("purchasedBookIds", getPurchasedBookIds());

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
        model.addAttribute("categories", bookCategoryService.getAllCategories());
        model.addAttribute("purchasedBookIds", getPurchasedBookIds());

        return "user/books/trending";
    }

    /**
     * Sách mới nhất
     */
    @GetMapping("/newest")
    public String newestBooks(Model model) {
        List<Book> books = bookService.getNewestBooks();
        model.addAttribute("books", books);
        model.addAttribute("categories", bookCategoryService.getAllCategories());
        model.addAttribute("purchasedBookIds", getPurchasedBookIds());

        return "user/books/newest";
    }

    /**
     * Sách được đánh giá cao
     */
    @GetMapping("/top-rated")
    public String topRatedBooks(Model model) {
        List<Book> books = bookService.getTopRatedBooks(20);
        model.addAttribute("books", books);
        model.addAttribute("categories", bookCategoryService.getAllCategories());
        model.addAttribute("purchasedBookIds", getPurchasedBookIds());

        return "user/books/top-rated";
    }
}

