package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.dto.BookDTO;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;
import stu.datn.ebook_store.repository.AuthorRepository;
import stu.datn.ebook_store.repository.BookCategoryRepository;
import stu.datn.ebook_store.repository.BookRepository;
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

    @Value("${file.upload-dir:uploads/books}")
    private String uploadDir;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository,
                          ReviewRepository reviewRepository,
                          AuthorRepository authorRepository,
                          BookCategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
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
    public void deleteBook(String bookId) {
        bookRepository.deleteById(bookId);
    }

    @Override
    public Book createBook(BookDTO bookDTO, Set<String> authorIds) {
        Book book = new Book();
        book.setBookId(generateBookId());

        // Set basic properties
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setPrice(bookDTO.getPrice());
        book.setCoverImageUrl(bookDTO.getCoverImageUrl());
        book.setPublisher(bookDTO.getPublisher());
        book.setPublicationYear(bookDTO.getPublicationYear());
        book.setLanguage(bookDTO.getLanguage());
        book.setPages(bookDTO.getPages());
        book.setIsbn(bookDTO.getIsbn());
        book.setAccessType(bookDTO.getAccessType() != null ? bookDTO.getAccessType() : Book.AccessType.PURCHASE);
        book.setIsDownloadable(bookDTO.getIsDownloadable() != null ? bookDTO.getIsDownloadable() : false);

        // Set category
        if (bookDTO.getBookCategoryId() != null && !bookDTO.getBookCategoryId().isEmpty()) {
            categoryRepository.findById(bookDTO.getBookCategoryId())
                .ifPresent(book::setBookCategory);
        }

        // Set authors
        if (authorIds != null && !authorIds.isEmpty()) {
            Set<Author> authors = new HashSet<>();
            for (String authorId : authorIds) {
                authorRepository.findById(authorId).ifPresent(authors::add);
            }
            book.setAuthors(authors);
        }

        return bookRepository.save(book);
    }

    @Override
    public Book updateBook(String bookId, BookDTO bookDTO, Set<String> authorIds) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        Book book = bookOpt.get();

        // Update basic properties
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setPrice(bookDTO.getPrice());
        if (bookDTO.getCoverImageUrl() != null && !bookDTO.getCoverImageUrl().isEmpty()) {
            book.setCoverImageUrl(bookDTO.getCoverImageUrl());
        }
        book.setPublisher(bookDTO.getPublisher());
        book.setPublicationYear(bookDTO.getPublicationYear());
        book.setLanguage(bookDTO.getLanguage());
        book.setPages(bookDTO.getPages());
        book.setIsbn(bookDTO.getIsbn());
        book.setAccessType(bookDTO.getAccessType());
        book.setIsDownloadable(bookDTO.getIsDownloadable());

        // Update category
        if (bookDTO.getBookCategoryId() != null && !bookDTO.getBookCategoryId().isEmpty()) {
            categoryRepository.findById(bookDTO.getBookCategoryId())
                .ifPresent(book::setBookCategory);
        }

        // Update authors
        if (authorIds != null) {
            Set<Author> authors = new HashSet<>();
            for (String authorId : authorIds) {
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

            // Return relative URL
            return "/uploads/books/" + filename;
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

    private String generateBookId() {
        long count = bookRepository.count();
        return "book_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

