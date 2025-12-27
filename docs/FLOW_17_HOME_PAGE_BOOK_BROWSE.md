# 🏠 FLOW 17: HOME PAGE & PUBLIC BOOK BROWSE (Trang Chủ & Duyệt Sách Công Khai)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow 17.1: Load Home Page](#flow-171-load-home-page)
3. [Flow 17.2: Browse All Books](#flow-172-browse-all-books)
4. [Flow 17.3: Search Books](#flow-173-search-books)
5. [Flow 17.4: Filter by Category](#flow-174-filter-by-category)
6. [Flow 17.5: Filter by Access Type](#flow-175-filter-by-access-type)
7. [Flow 17.6: View Book Details](#flow-176-view-book-details)
8. [Flow 17.7: Sort Books](#flow-177-sort-books)
9. [Performance Optimization](#performance-optimization)

---

## Tổng Quan

### Components
- **Controllers**: 
  - `HomeController.java` - Home page
  - `UserBookController.java` - Book browsing (under user/ package)
- **Services**: 
  - `BookService.java` - Book queries
  - `BannerService.java` - Display banners
  - `CategoryService.java` - Category info
  - `AuthorService.java` - Author info
- **Repositories**: 
  - `BookRepository.java`
  - `BannerRepository.java`
  - `CategoryRepository.java`
- **Entities**: `Book.java`, `Banner.java`, `Category.java`, `Author.java`

### URLs
- `GET /` or `GET /home` - Home page
- `GET /user/books` - Browse all books
- `GET /user/books/search?q={query}` - Search books
- `GET /user/books/category/{slug}` - Filter by category
- `GET /user/books/details/{id}` - Book details page

### Access Types
```java
public enum AccessType {
    FREE,         // Free books (no purchase needed)
    PAID,         // One-time purchase
    SUBSCRIPTION  // Requires active subscription
}
```

---

## Flow 17.1: Load Home Page

### Sequence Diagram
```
User → Browser → HomeController → BookService → BannerService → Database
  │       │            │              │              │             │
  │  GET /                                                         │
  │──────────────────────►│                                        │
  │       │                │ getTopViewedBooks()                   │
  │       │                ├─────────────►│                        │
  │       │                │               │ findTop10ByViews()    │
  │       │                │               ├───────────────────►│  │
  │       │                │               │◄───────────────────┤  │
  │       │                │◄─────────────┤                        │
  │       │                │ getNewestBooks()                      │
  │       │                ├─────────────►│                        │
  │       │                │◄─────────────┤                        │
  │       │                │ getFreeBooks()                        │
  │       │                ├─────────────►│                        │
  │       │                │◄─────────────┤                        │
  │       │                │ getHomeBanners()                      │
  │       │                ├────────────────────────►│             │
  │       │                │◄────────────────────────┤             │
  │◄──────────────────────┤ (return home.html)                    │
```

### Implementation Details

**Controller**:
```java
@Controller
public class HomeController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private BannerService bannerService;
    
    @GetMapping("/")
    public String home(Model model) {
        // Authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() &&
            auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
        }
        
        try {
            // Get free books (ACCESS_TYPE = 'FREE')
            List<Book> freeBooks = bookService.getBooksByAccessType(Book.AccessType.FREE);
            
            // Get trending books (top viewed)
            List<Book> trendingBooks = bookService.getTopViewedBooks();
            
            // Get new releases
            List<Book> newBooks = bookService.getNewestBooks();
            
            // Get home banners
            List<Banner> homeBanners = bannerService.getActiveBannersByPosition(
                Banner.BannerPosition.HOME);
            
            // Add to model
            model.addAttribute("freeBooks", freeBooks);
            model.addAttribute("trendingBooks", trendingBooks);
            model.addAttribute("newBooks", newBooks);
            model.addAttribute("homeBanners", homeBanners);
            
        } catch (Exception e) {
            // Log error but still show the page
            System.err.println("Error loading books: " + e.getMessage());
        }
        
        return "home";
    }
    
    @GetMapping("/home")
    public String homeAlias() {
        return "redirect:/";
    }
}
```

**Service Methods**:
```java
@Override
public List<Book> getBooksByAccessType(Book.AccessType accessType) {
    return bookRepository.findByAccessTypeAndIsAvailableTrue(accessType);
}

@Override
public List<Book> getTopViewedBooks() {
    return bookRepository.findTop10ByIsAvailableTrueOrderByViewsDesc();
}

@Override
public List<Book> getNewestBooks() {
    return bookRepository.findTop10ByIsAvailableTrueOrderByCreatedAtDesc();
}
```

**SQL Queries**:
```sql
-- Get free books
SELECT b.*, ba.cover_image_url
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
WHERE b.access_type = 'FREE' AND b.is_available = true
ORDER BY b.created_at DESC
LIMIT 12;

-- Get top viewed books
SELECT b.*, ba.cover_image_url
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
WHERE b.is_available = true
ORDER BY b.views DESC
LIMIT 10;

-- Get newest books
SELECT b.*, ba.cover_image_url
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
WHERE b.is_available = true
ORDER BY b.created_at DESC
LIMIT 10;

-- Get home banners
SELECT * FROM banners
WHERE position = 'HOME' AND is_active = true
ORDER BY created_at DESC
LIMIT 5;
```

**Home Page Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│                        HEADER & NAV                          │
├─────────────────────────────────────────────────────────────┤
│                    BANNER CAROUSEL                           │
│  [Banner 1] [Banner 2] [Banner 3]                           │
├─────────────────────────────────────────────────────────────┤
│  📚 MIỄN PHÍ (Free Books)                                   │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐              │
│  │ Book 1 │ │ Book 2 │ │ Book 3 │ │ Book 4 │              │
│  └────────┘ └────────┘ └────────┘ └────────┘              │
├─────────────────────────────────────────────────────────────┤
│  🔥 TRENDING (Top Viewed)                                   │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐              │
│  │ Book A │ │ Book B │ │ Book C │ │ Book D │              │
│  └────────┘ └────────┘ └────────┘ └────────┘              │
├─────────────────────────────────────────────────────────────┤
│  🆕 MỚI NHẤT (New Releases)                                 │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐              │
│  │ Book X │ │ Book Y │ │ Book Z │ │ Book W │              │
│  └────────┘ └────────┘ └────────┘ └────────┘              │
├─────────────────────────────────────────────────────────────┤
│                        FOOTER                                │
└─────────────────────────────────────────────────────────────┘
```

---

## Flow 17.2: Browse All Books

### Sequence Diagram
```
User → Browser → UserBookController → BookService → BookRepository → Database
  │       │              │                 │              │             │
  │  GET /user/books?page=1&size=12&sort=newest                        │
  │────────────────────────►│                                           │
  │       │                 │ getAllAvailableBooks(pageable)            │
  │       │                 ├────────────────►│                         │
  │       │                 │                 │ findAll(pageable)       │
  │       │                 │                 ├────────────►│           │
  │       │                 │                 │             │ SELECT    │
  │       │                 │                 │             ├──────────►│
  │       │                 │                 │◄────────────┤           │
  │       │                 │◄────────────────┤                         │
  │◄────────────────────────┤ (return user/books/list.html)            │
```

### Implementation Details

**Controller**:
```java
@Controller
@RequestMapping("/user/books")
public class UserBookController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private CategoryService bookCategoryService;
    
    @GetMapping
    public String browseBooks(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "12") int size,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) String accessType,
                             Model model) {
        // Create pageable with sort
        Sort sortObj = getSortObject(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        // Get books
        Page<Book> bookPage;
        if (accessType != null && !accessType.isEmpty()) {
            Book.AccessType type = Book.AccessType.valueOf(accessType);
            bookPage = bookService.getBooksByAccessType(type, pageable);
        } else {
            bookPage = bookService.getAllAvailableBooks(pageable);
        }
        
        // Get all categories for filter
        List<Category> categories = bookCategoryService.getActiveCategories();
        
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());
        model.addAttribute("sort", sort);
        model.addAttribute("accessType", accessType);
        
        return "user/books/list";
    }
    
    private Sort getSortObject(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "popular" -> Sort.by(Sort.Direction.DESC, "views");
            case "title-asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "title-desc" -> Sort.by(Sort.Direction.DESC, "title");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
```

**Service**:
```java
@Override
public Page<Book> getAllAvailableBooks(Pageable pageable) {
    return bookRepository.findByIsAvailableTrue(pageable);
}

@Override
public Page<Book> getBooksByAccessType(Book.AccessType accessType, Pageable pageable) {
    return bookRepository.findByAccessTypeAndIsAvailableTrue(accessType, pageable);
}
```

**SQL Query**:
```sql
-- Get all available books with pagination
SELECT b.*, ba.cover_image_url, c.category_name,
       GROUP_CONCAT(a.name) as authors
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
LEFT JOIN categories c ON b.category_id = c.category_id
LEFT JOIN book_authors bauth ON b.book_id = bauth.book_id
LEFT JOIN authors a ON bauth.author_id = a.author_id
WHERE b.is_available = true
GROUP BY b.book_id
ORDER BY b.created_at DESC
LIMIT ? OFFSET ?;

-- Count total books
SELECT COUNT(*) FROM books WHERE is_available = true;
```

---

## Flow 17.3: Search Books

### Sequence Diagram
```
User → Browser → UserBookController → BookService → BookRepository → Database
  │       │              │                 │              │             │
  │  GET /user/books/search?q=dale carnegie                            │
  │────────────────────────►│                                           │
  │       │                 │ searchBooks(query, pageable)              │
  │       │                 ├────────────────►│                         │
  │       │                 │                 │ findByTitleOrAuthor()   │
  │       │                 │                 ├────────────►│           │
  │       │                 │                 │             │ FULLTEXT  │
  │       │                 │                 │             ├──────────►│
  │       │                 │                 │◄────────────┤           │
  │       │                 │◄────────────────┤                         │
  │◄────────────────────────┤ (return user/books/search.html)          │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/search")
public String searchBooks(@RequestParam String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "12") int size,
                         Model model) {
    if (q == null || q.trim().isEmpty()) {
        return "redirect:/user/books";
    }
    
    Pageable pageable = PageRequest.of(page, size);
    Page<Book> bookPage = bookService.searchBooks(q, pageable);
    
    model.addAttribute("bookPage", bookPage);
    model.addAttribute("books", bookPage.getContent());
    model.addAttribute("query", q);
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", bookPage.getTotalPages());
    model.addAttribute("totalItems", bookPage.getTotalElements());
    
    return "user/books/search";
}
```

**Service**:
```java
@Override
public Page<Book> searchBooks(String query, Pageable pageable) {
    return bookRepository.searchByTitleOrAuthorOrIsbn(query, pageable);
}
```

**Repository**:
```java
@Query("SELECT DISTINCT b FROM Book b " +
       "LEFT JOIN b.bookAuthors ba " +
       "LEFT JOIN ba.author a " +
       "WHERE b.isAvailable = true " +
       "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
       "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
       "OR b.isbn LIKE CONCAT('%', :query, '%'))")
Page<Book> searchByTitleOrAuthorOrIsbn(@Param("query") String query, Pageable pageable);
```

**SQL Query**:
```sql
SELECT DISTINCT b.*, ba.cover_image_url, c.category_name,
       GROUP_CONCAT(a.name) as authors
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
LEFT JOIN categories c ON b.category_id = c.category_id
LEFT JOIN book_authors bauth ON b.book_id = bauth.book_id
LEFT JOIN authors a ON bauth.author_id = a.author_id
WHERE b.is_available = true
  AND (LOWER(b.title) LIKE LOWER(CONCAT('%', ?, '%'))
       OR LOWER(a.name) LIKE LOWER(CONCAT('%', ?, '%'))
       OR b.isbn LIKE CONCAT('%', ?, '%'))
GROUP BY b.book_id
LIMIT ? OFFSET ?;
```

---

## Flow 17.4: Filter by Category

### Implementation Details

**Controller**:
```java
@GetMapping("/category/{slug}")
public String booksByCategory(@PathVariable String slug,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "12") int size,
                             Model model) {
    // Find category by slug
    Category category = bookCategoryService.getCategoryBySlug(slug)
        .orElseThrow(() -> new RuntimeException("Category not found"));
    
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Book> bookPage = bookService.getBooksByCategory(category.getCategoryId(), pageable);
    
    model.addAttribute("bookPage", bookPage);
    model.addAttribute("books", bookPage.getContent());
    model.addAttribute("category", category);
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", bookPage.getTotalPages());
    
    return "user/books/category";
}
```

**Service**:
```java
@Override
public Page<Book> getBooksByCategory(String categoryId, Pageable pageable) {
    return bookRepository.findByCategoryIdAndIsAvailableTrue(categoryId, pageable);
}
```

**SQL Query**:
```sql
SELECT b.*, ba.cover_image_url, c.category_name
FROM books b
INNER JOIN categories c ON b.category_id = c.category_id
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
WHERE b.category_id = ? AND b.is_available = true
ORDER BY b.created_at DESC
LIMIT ? OFFSET ?;
```

---

## Flow 17.5: Filter by Access Type

### Implementation Details

**URL Examples**:
- `/user/books?accessType=FREE` - Free books only
- `/user/books?accessType=PAID` - Paid books only
- `/user/books?accessType=SUBSCRIPTION` - Subscription books only

**Service**:
```java
@Override
public Page<Book> getBooksByAccessType(Book.AccessType accessType, Pageable pageable) {
    return bookRepository.findByAccessTypeAndIsAvailableTrue(accessType, pageable);
}
```

---

## Flow 17.6: View Book Details

### Sequence Diagram
```
User → Browser → UserBookController → BookService → ReviewService → Database
  │       │              │                 │              │             │
  │  GET /user/books/details/{id}                                      │
  │────────────────────────►│                                           │
  │       │                 │ getBookById(id)                           │
  │       │                 ├────────────────►│                         │
  │       │                 │◄────────────────┤                         │
  │       │                 │ incrementViews(id)                        │
  │       │                 ├────────────────►│                         │
  │       │                 │ getReviewsByBook(id)                      │
  │       │                 ├─────────────────────────────►│            │
  │       │                 │◄─────────────────────────────┤            │
  │       │                 │ getRelatedBooks(category)                 │
  │       │                 ├────────────────►│                         │
  │       │                 │◄────────────────┤                         │
  │◄────────────────────────┤ (return user/books/details.html)         │
```

### Implementation Details

**Controller**:
```java
@GetMapping("/details/{id}")
public String bookDetails(@PathVariable String id, Model model) {
    // Get book details
    Book book = bookService.getBookById(id)
        .orElseThrow(() -> new RuntimeException("Book not found"));
    
    if (!book.getIsAvailable()) {
        throw new RuntimeException("Book not available");
    }
    
    // Increment view count
    bookService.incrementBookViews(id);
    
    // Get reviews
    List<Review> reviews = reviewService.getApprovedReviewsByBook(id);
    
    // Get related books (same category)
    List<Book> relatedBooks = bookService.getRelatedBooks(book.getCategoryId(), id, 6);
    
    // Check if user owns this book
    boolean userOwnsBook = false;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
        userOwnsBook = orderService.hasUserPurchasedBook(user.getUserId(), id);
        model.addAttribute("user", user);
    }
    
    model.addAttribute("book", book);
    model.addAttribute("reviews", reviews);
    model.addAttribute("relatedBooks", relatedBooks);
    model.addAttribute("userOwnsBook", userOwnsBook);
    
    return "user/books/details";
}
```

**Service Methods**:
```java
@Override
@Transactional
public void incrementBookViews(String bookId) {
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new RuntimeException("Book not found"));
    book.setViews(book.getViews() + 1);
    bookRepository.save(book);
}

@Override
public List<Book> getRelatedBooks(String categoryId, String excludeBookId, int limit) {
    return bookRepository.findTop6ByCategoryIdAndBookIdNotAndIsAvailableTrue(
        categoryId, excludeBookId);
}
```

**SQL Queries**:
```sql
-- Get book details with full info
SELECT b.*, ba.cover_image_url, ba.source_file_url, ba.preview_file_url,
       c.category_name, c.category_slug,
       GROUP_CONCAT(a.name) as authors,
       AVG(r.rating) as average_rating,
       COUNT(r.review_id) as review_count
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
LEFT JOIN categories c ON b.category_id = c.category_id
LEFT JOIN book_authors bauth ON b.book_id = bauth.book_id
LEFT JOIN authors a ON bauth.author_id = a.author_id
LEFT JOIN reviews r ON b.book_id = r.book_id AND r.status = 'APPROVED'
WHERE b.book_id = ?
GROUP BY b.book_id;

-- Increment views
UPDATE books SET views = views + 1 WHERE book_id = ?;

-- Get related books
SELECT b.*, ba.cover_image_url
FROM books b
LEFT JOIN book_assets ba ON b.book_id = ba.book_id
WHERE b.category_id = ? 
  AND b.book_id != ? 
  AND b.is_available = true
ORDER BY RAND()
LIMIT 6;
```

---

## Flow 17.7: Sort Books

### Sort Options

**Supported Sort Values**:
- `newest` - Mới nhất (default)
- `oldest` - Cũ nhất
- `price-asc` - Giá tăng dần
- `price-desc` - Giá giảm dần
- `popular` - Phổ biến nhất (most views)
- `title-asc` - Tên A-Z
- `title-desc` - Tên Z-A

**Implementation**:
```java
private Sort getSortObject(String sort) {
    if (sort == null) {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    
    return switch (sort) {
        case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
        case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
        case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
        case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
        case "popular" -> Sort.by(Sort.Direction.DESC, "views");
        case "title-asc" -> Sort.by(Sort.Direction.ASC, "title");
        case "title-desc" -> Sort.by(Sort.Direction.DESC, "title");
        default -> Sort.by(Sort.Direction.DESC, "createdAt");
    };
}
```

---

## Performance Optimization

### 1. Database Indexing
```sql
-- Create indexes for better performance
CREATE INDEX idx_books_available ON books(is_available);
CREATE INDEX idx_books_created_at ON books(created_at);
CREATE INDEX idx_books_views ON books(views);
CREATE INDEX idx_books_category ON books(category_id);
CREATE INDEX idx_books_access_type ON books(access_type);
CREATE INDEX idx_books_title ON books(title);

-- Full-text search index
CREATE FULLTEXT INDEX idx_books_title_fulltext ON books(title);
```

### 2. Caching
```java
@Cacheable(value = "books", key = "'home-free'")
public List<Book> getBooksByAccessType(Book.AccessType accessType) {
    return bookRepository.findByAccessTypeAndIsAvailableTrue(accessType);
}

@Cacheable(value = "books", key = "'home-trending'")
public List<Book> getTopViewedBooks() {
    return bookRepository.findTop10ByIsAvailableTrueOrderByViewsDesc();
}

@CacheEvict(value = "books", allEntries = true)
public void clearBooksCache() {
    // Clear cache when books are updated
}
```

### 3. Lazy Loading
```html
<!-- Lazy load book cover images -->
<img th:src="${book.coverImageUrl}" 
     th:alt="${book.title}" 
     loading="lazy" />
```

### 4. Pagination
- Use `Page<Book>` instead of `List<Book>`
- Default page size: 12 items
- Load only necessary fields in list views
- Fetch full details only on detail page

### 5. Query Optimization
```java
// Use JOIN FETCH to avoid N+1 problem
@Query("SELECT b FROM Book b " +
       "LEFT JOIN FETCH b.bookAssets " +
       "LEFT JOIN FETCH b.category " +
       "WHERE b.isAvailable = true")
List<Book> findAllWithAssets();
```

---

## Best Practices

### 1. Book Display
- Show clear pricing (Free, Price, or Subscription)
- Display book cover prominently
- Show author names
- Display average rating and review count
- Indicate if user already owns the book

### 2. Navigation
- Breadcrumbs for category navigation
- Clear filter and sort options
- Pagination controls
- "Back to top" button

### 3. Search Experience
- Auto-suggest as user types
- Search in title, author, ISBN
- Highlight search terms in results
- Show "No results" message with suggestions

### 4. Mobile Optimization
- Responsive grid layout
- Touch-friendly buttons
- Optimized images for mobile
- Fast loading on slow connections

### 5. SEO Optimization
```html
<!-- Meta tags for book details page -->
<meta th:name="description" th:content="${book.description}" />
<meta th:property="og:title" th:content="${book.title}" />
<meta th:property="og:image" th:content="${book.coverImageUrl}" />
<meta th:property="og:description" th:content="${book.description}" />
```

---

## Security Considerations

### 1. Input Validation
- Sanitize search queries
- Validate page numbers
- Check category slugs
- Prevent SQL injection

### 2. Access Control
- Check book availability before showing
- Verify ownership for reading
- Handle authentication gracefully
- Redirect to login when needed

### 3. Rate Limiting
- Limit search requests per user
- Prevent view count manipulation
- Monitor suspicious patterns

---

## Related Flows
- 🔐 **FLOW 01**: Authentication - User login to purchase
- 🛒 **FLOW 03**: Shopping Cart & Checkout - Add to cart from details page
- 📖 **FLOW 07**: Reading Interface - Read book from details page
- ⭐ **FLOW 11**: Review & Rating - View and submit reviews
- 📂 **FLOW 12**: Admin Category Management - Categories used for filtering
- ✍️ **FLOW 13**: Admin Author Management - Authors displayed in book info

---

**Last Updated**: December 7, 2025  
**Version**: 1.0  
**Author**: Ebook Store Development Team

