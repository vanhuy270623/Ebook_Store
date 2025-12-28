package stu.datn.ebook_store.controller.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;
import stu.datn.ebook_store.entity.Order;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BookCategoryService;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.OrderItemService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller xử lý trang danh mục sách cho user
 */
@Controller
@Slf4j
public class CategoryController extends BaseController {

    private static final int BOOKS_PER_CATEGORY = 6; // Số sách hiển thị mỗi category
    private static final Set<Order.PaymentStatus> PAID_STATUSES =
            EnumSet.of(Order.PaymentStatus.COMPLETED, Order.PaymentStatus.PAID);
    private static final Set<Book.AccessType> RETAIL_ACCESS_TYPES =
            EnumSet.of(Book.AccessType.PURCHASE, Book.AccessType.BOTH);

    private final BookCategoryService bookCategoryService;
    private final BookService bookService;
    private final OrderItemService orderItemService;

    @Autowired
    public CategoryController(BookCategoryService bookCategoryService,
                              BookService bookService,
                              OrderItemService orderItemService) {
        this.bookCategoryService = bookCategoryService;
        this.bookService = bookService;
        this.orderItemService = orderItemService;
    }

    /**
     * Trang danh sách tất cả categories với preview sách
     */
    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        try {
            log.info("Loading categories page");

            // Lấy tất cả categories active
            List<BookCategory> categories = bookCategoryService.getAllCategories().stream()
                    .filter(cat -> cat.getIsActive() != null && cat.getIsActive())
                    .sorted(Comparator.comparing(cat ->
                            cat.getDisplayOrder() != null ? cat.getDisplayOrder() : Integer.MAX_VALUE))
                    .collect(Collectors.toList());

            // Lấy tất cả sách
            List<Book> allBooks = bookService.getAllBooks();

            // Tạo map: category -> danh sách sách
            Map<BookCategory, List<Book>> categoryBooksMap = new LinkedHashMap<>();

            for (BookCategory category : categories) {
                List<Book> booksInCategory = allBooks.stream()
                        .filter(book -> book.getBookCategory() != null &&
                                      book.getBookCategory().getBookCategoryId().equals(category.getBookCategoryId()))
                        .sorted((a, b) -> {
                            // Sắp xếp theo viewCount giảm dần
                            int viewA = a.getViewCount() != null ? a.getViewCount() : 0;
                            int viewB = b.getViewCount() != null ? b.getViewCount() : 0;
                            return Integer.compare(viewB, viewA);
                        })
                        .limit(BOOKS_PER_CATEGORY)
                        .collect(Collectors.toList());

                if (!booksInCategory.isEmpty()) {
                    categoryBooksMap.put(category, booksInCategory);
                }
            }

            // Lấy danh sách sách đã mua của user (nếu đã đăng nhập)
            Set<String> purchasedBookIds = getPurchasedBookIds();

            model.addAttribute("categoryBooksMap", categoryBooksMap);
            model.addAttribute("purchasedBookIds", purchasedBookIds);
            model.addAttribute("currentPage", "categories");

            log.info("Loaded {} categories with books", categoryBooksMap.size());
            return "user/category/categories";

        } catch (Exception e) {
            log.error("Error loading categories page: {}", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi tải danh mục");
            return "redirect:/";
        }
    }

    /**
     * Category detail endpoint với URL thân thiện
     * Render list.html với category filter đã apply
     */
    @GetMapping("/categories/{categorySlug}")
    public String categoryDetail(@PathVariable String categorySlug,
                                  @RequestParam(required = false) String sort,
                                  @RequestParam(required = false) String access,
                                  @RequestParam(defaultValue = "0") int page,
                                  Model model) {
        try {
            log.info("Loading category detail for slug: {}", categorySlug);

            // Tìm category theo slug
            BookCategory category = bookCategoryService.getAllCategories().stream()
                    .filter(cat -> cat.getCategorySlug() != null &&
                                 cat.getCategorySlug().equals(categorySlug))
                    .findFirst()
                    .orElse(null);

            if (category == null) {
                log.warn("Category not found for slug: {}", categorySlug);
                model.addAttribute("error", "Không tìm thấy danh mục");
                return "redirect:/categories";
            }

            // Lấy tất cả sách
            List<Book> books = bookService.getAllBooks();

            // Filter by category
            books = books.stream()
                    .filter(b -> b.getBookCategory() != null &&
                            b.getBookCategory().getBookCategoryId().equals(category.getBookCategoryId()))
                    .collect(Collectors.toList());

            // Filter by access type (nếu có)
            if (access != null && !access.isEmpty()) {
                try {
                    Book.AccessType accessType = Book.AccessType.valueOf(access.toUpperCase());
                    books = books.stream()
                            .filter(b -> b.getAccessType() == accessType)
                            .collect(Collectors.toList());
                    model.addAttribute("selectedAccess", access);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid access type
                }
            }

            // Sort
            if ("newest".equals(sort)) {
                books = books.stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .collect(Collectors.toList());
            } else if ("popular".equals(sort)) {
                books = books.stream()
                        .sorted((a, b) -> {
                            int viewA = a.getViewCount() != null ? a.getViewCount() : 0;
                            int viewB = b.getViewCount() != null ? b.getViewCount() : 0;
                            return Integer.compare(viewB, viewA);
                        })
                        .collect(Collectors.toList());
            } else if ("rating".equals(sort)) {
                books = books.stream()
                        .sorted((a, b) -> {
                            float ratingA = a.getAverageRating() != null ? a.getAverageRating() : 0;
                            float ratingB = b.getAverageRating() != null ? b.getAverageRating() : 0;
                            return Float.compare(ratingB, ratingA);
                        })
                        .collect(Collectors.toList());
            }

            // Pagination
            int pageSize = 12;
            int totalBooks = books.size();
            int totalPages = (int) Math.ceil((double) totalBooks / pageSize);
            if (page >= totalPages && totalPages > 0) {
                page = totalPages - 1;
            }

            int startIndex = Math.max(0, page * pageSize);
            int endIndex = Math.min(startIndex + pageSize, totalBooks);
            List<Book> pagedBooks = books.subList(startIndex, endIndex);

            // Load all categories for sidebar
            List<BookCategory> allCategories = bookCategoryService.getAllCategories();

            // Load purchased books
            Set<String> purchasedBookIds = getPurchasedBookIds();

            // Model attributes
            model.addAttribute("books", pagedBooks);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalBooks", totalBooks);
            model.addAttribute("categories", allCategories);
            model.addAttribute("accessTypes", Book.AccessType.values());
            model.addAttribute("sortOptions", new String[]{"newest", "popular", "rating"});
            model.addAttribute("sort", sort);
            model.addAttribute("purchasedBookIds", purchasedBookIds);
            model.addAttribute("selectedCategory", category.getBookCategoryId());
            model.addAttribute("categoryInfo", category); // Để hiển thị banner

            log.info("Loaded {} books for category {}", pagedBooks.size(), category.getCategoryName());
            return "user/books/list"; // Sử dụng chung template list.html

        } catch (Exception e) {
            log.error("Error loading category detail: {}", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra");
            return "redirect:/categories";
        }
    }

    /**
     * Helper: Lấy danh sách ID sách đã mua của user
     */
    private Set<String> getPurchasedBookIds() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(orderItemService.getPurchasedBookIds(
                currentUser.getUserId(), Order.OrderType.BOOK, PAID_STATUSES, RETAIL_ACCESS_TYPES));
    }
}

