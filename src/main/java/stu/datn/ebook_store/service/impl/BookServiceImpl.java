package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.dto.request.BookCreateRequest;
import stu.datn.ebook_store.dto.request.BookUpdateRequest;
import stu.datn.ebook_store.dto.response.BookResponse;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;
import stu.datn.ebook_store.repository.AuthorRepository;
import stu.datn.ebook_store.repository.BookCategoryRepository;
import stu.datn.ebook_store.repository.BookRepository;
import stu.datn.ebook_store.repository.CartItemRepository;
import stu.datn.ebook_store.repository.OrderItemRepository;
import stu.datn.ebook_store.repository.ReviewRepository;
import stu.datn.ebook_store.service.BookService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${file.upload-dir:uploads/books}")
    private String uploadDir;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository,
                          ReviewRepository reviewRepository,
                          AuthorRepository authorRepository,
                          BookCategoryRepository categoryRepository,
                          CartItemRepository cartItemRepository,
                          OrderItemRepository orderItemRepository) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Optional<Book> getBookById(String bookId) {
        return bookRepository.findById(bookId);
    }

    @Override
    public Book saveBook(Book book) {
        if (book.getBookId() == null || book.getBookId().isEmpty()) {
            book.setBookId(generateBookId());
        }
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public void deleteBook(String bookId) {
        // Check if book exists before deleting
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + bookId));

        // Proactively check if book is being used before attempting deletion
        List<stu.datn.ebook_store.entity.OrderItem> orderItems = orderItemRepository.findByBook_BookId(bookId);
        if (!orderItems.isEmpty()) {
            throw new RuntimeException("Không thể xóa sách này vì nó đã có trong " + orderItems.size() + " đơn hàng. Sách này cần được giữ lại để bảo toàn lịch sử đơn hàng.");
        }

        try {
            // Clear all relationships before deletion
            // Clear many-to-many relationship with authors
            if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                book.getAuthors().clear();
                bookRepository.saveAndFlush(book);
            }

            // OneToMany relationships with cascade will be handled automatically
            // (bookAssets and reviews)

            // Now delete the book
            bookRepository.delete(book);
            bookRepository.flush();
        } catch (Exception e) {
            // Check if it's a foreign key constraint violation
            String errorMessage = e.getMessage();
            Throwable cause = e.getCause();

            // Check the exception and its causes for constraint violations
            while (cause != null) {
                String causeMessage = cause.getMessage();
                if (causeMessage != null) {
                    if (causeMessage.contains("foreign key") ||
                        causeMessage.contains("constraint") ||
                        causeMessage.contains("ConstraintViolation") ||
                        causeMessage.contains("cannot delete") ||
                        causeMessage.contains("order_items") ||
                        causeMessage.contains("cart_items") ||
                        causeMessage.contains("reading_progress")) {
                        throw new RuntimeException("Không thể xóa sách này vì nó đang được sử dụng trong hệ thống (giỏ hàng, đơn hàng, hoặc lịch sử đọc).");
                    }
                }
                cause = cause.getCause();
            }

            // Check main error message
            if (errorMessage != null && (errorMessage.contains("foreign key") ||
                errorMessage.contains("constraint") ||
                errorMessage.contains("ConstraintViolation") ||
                errorMessage.contains("cannot delete") ||
                errorMessage.contains("order_items") ||
                errorMessage.contains("cart_items") ||
                errorMessage.contains("reading_progress"))) {
                throw new RuntimeException("Không thể xóa sách này vì nó đang được sử dụng trong hệ thống (giỏ hàng, đơn hàng, hoặc lịch sử đọc).");
            }

            throw new RuntimeException("Có lỗi xảy ra khi xóa sách: " + (errorMessage != null ? errorMessage : "Lỗi không xác định"));
        }
    }

    @Override
    public Book createBook(BookCreateRequest request) {
        Book book = new Book();
        book.setBookId(generateBookId());

        // Set basic properties
        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setIsbn(request.getIsbn());
        book.setAccessType(request.getAccessType() != null ? request.getAccessType() : Book.AccessType.PURCHASE);
        book.setIsDownloadable(request.getIsDownloadable() != null ? request.getIsDownloadable() : false);

        // Set category
        if (request.getBookCategoryId() != null && !request.getBookCategoryId().isEmpty()) {
            categoryRepository.findById(request.getBookCategoryId())
                .ifPresent(book::setBookCategory);
        }

        // Set authors
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            Set<Author> authors = new HashSet<>();
            for (String authorId : request.getAuthorIds()) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        }

        return bookRepository.save(book);
    }

    @Override
    public Book updateBook(BookUpdateRequest request) {
        Optional<Book> bookOpt = bookRepository.findById(request.getBookId());
        if (bookOpt.isEmpty()) {
            throw new RuntimeException("Book not found with id: " + request.getBookId());
        }

        Book book = bookOpt.get();

        // Update basic properties
        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isEmpty()) {
            book.setCoverImageUrl(request.getCoverImageUrl());
        }
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setIsbn(request.getIsbn());
        book.setAccessType(request.getAccessType());
        book.setIsDownloadable(request.getIsDownloadable());

        // Update category
        if (request.getBookCategoryId() != null && !request.getBookCategoryId().isEmpty()) {
            categoryRepository.findById(request.getBookCategoryId())
                .ifPresent(book::setBookCategory);
        }

        // Update authors
        if (request.getAuthorIds() != null) {
            Set<Author> authors = new HashSet<>();
            for (String authorId : request.getAuthorIds()) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        }

        return bookRepository.save(book);
    }

    @Override
    public String uploadCoverImage(MultipartFile file) {
        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String filename = UUID.randomUUID() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative URL matching the resource handler mapping
            // File saved to: F:/datn_uploads/book_asset/image/covers/[filename]
            // URL mapping: /book_asset/image/covers/[filename]
            return "/book_asset/image/covers/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Book> getBooksByCategory(BookCategory category) {
        return bookRepository.findByBookCategory(category);
    }

    @Override
    public List<Book> getBooksByAccessType(Book.AccessType accessType) {
        return bookRepository.findByAccessType(accessType);
    }

    @Override
    public List<Book> searchBooksByKeyword(String keyword) {
        return bookRepository.searchByKeyword(keyword);
    }

    @Override
    public List<Book> getTopViewedBooks() {
        return bookRepository.findTop10ByOrderByViewCountDesc();
    }

    @Override
    public List<Book> getNewestBooks() {
        return bookRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Override
    public List<Book> getDownloadableBooks() {
        return bookRepository.findByIsDownloadableTrue();
    }

    @Override
    public List<Book> getBooksByAuthor(String authorId) {
        return bookRepository.findByAuthor(authorId);
    }

    @Override
    public List<Book> getBooksByMinRating(Float minRating) {
        return bookRepository.findByMinRating(minRating);
    }

    @Override
    public long countBooksByAccessType(Book.AccessType accessType) {
        return bookRepository.countByAccessType(accessType);
    }

    @Override
    public void updateBookRating(String bookId) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            // Calculate average rating from reviews
            List<Object[]> ratingStats = reviewRepository.calculateAverageRatingForBook(bookId);
            if (!ratingStats.isEmpty()) {
                Object[] stats = ratingStats.get(0);
                Double avgRating = (Double) stats[0];
                Long totalReviews = (Long) stats[1];
                book.setAverageRating(avgRating != null ? avgRating.floatValue() : 0.0f);
                book.setTotalReviews(totalReviews != null ? totalReviews.intValue() : 0);
                bookRepository.save(book);
            }
        }
    }

    @Override
    public long getTotalBooksCount() {
        return bookRepository.count();
    }

    @Override
    public long getFreeBooks() {
        return bookRepository.countByAccessType(Book.AccessType.FREE);
    }

    @Override
    public long getPaidBooks() {
        return bookRepository.countByAccessType(Book.AccessType.PURCHASE);
    }

    @Override
    public long getSubscriptionBooks() {
        return bookRepository.countByAccessType(Book.AccessType.SUBSCRIPTION) +
               bookRepository.countByAccessType(Book.AccessType.BOTH);
    }

    @Override
    public List<Book> getRecentBooks(int limit) {
        return bookRepository.findTopBooksOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    public List<Book> getTopRatedBooks(int limit) {
        return bookRepository.findTopBooksByRating(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // REST API methods
    @Override
    public BookResponse createBookFromRequest(BookCreateRequest request) {
        Book book = new Book();
        book.setBookId(generateBookId());

        // Map properties from request
        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setIsbn(request.getIsbn());
        book.setAccessType(request.getAccessType());
        book.setIsDownloadable(request.getIsDownloadable() != null ? request.getIsDownloadable() : false);

        // Set category
        if (request.getBookCategoryId() != null) {
            categoryRepository.findById(request.getBookCategoryId())
                .ifPresent(book::setBookCategory);
        }

        // Set authors
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            Set<Author> authors = new HashSet<>();
            for (String authorId : request.getAuthorIds()) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        }

        Book savedBook = bookRepository.save(book);
        return new BookResponse(savedBook);
    }

    @Override
    public BookResponse updateBookFromRequest(String bookId, BookUpdateRequest request) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        Book book = bookOpt.get();

        // Update properties
        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isEmpty()) {
            book.setCoverImageUrl(request.getCoverImageUrl());
        }
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setIsbn(request.getIsbn());
        book.setAccessType(request.getAccessType());
        book.setIsDownloadable(request.getIsDownloadable());

        // Update category
        if (request.getBookCategoryId() != null) {
            categoryRepository.findById(request.getBookCategoryId())
                .ifPresent(book::setBookCategory);
        }

        // Update authors
        if (request.getAuthorIds() != null) {
            Set<Author> authors = new HashSet<>();
            for (String authorId : request.getAuthorIds()) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        }

        Book updatedBook = bookRepository.save(book);
        return new BookResponse(updatedBook);
    }

    @Override
    public BookResponse getBookResponse(String bookId) {
        return bookRepository.findById(bookId)
            .map(BookResponse::new)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));
    }

    private String generateBookId() {
        long count = bookRepository.count();
        return "book_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

