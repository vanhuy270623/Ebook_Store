package stu.datn.ebook_store.service;

import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.dto.request.BookFormRequest;
import stu.datn.ebook_store.dto.request.BookCreateRequest;
import stu.datn.ebook_store.dto.request.BookUpdateRequest;
import stu.datn.ebook_store.dto.response.BookResponse;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookService {
    // Basic CRUD operations
    List<Book> getAllBooks();
    Optional<Book> getBookById(String bookId);
    Book saveBook(Book book);

    // MVC form operations
    Book createBook(BookFormRequest bookFormRequest, Set<String> authorIds);
    Book updateBook(String bookId, BookFormRequest bookFormRequest, Set<String> authorIds);

    // REST API operations
    BookResponse createBookFromRequest(BookCreateRequest request);
    BookResponse updateBookFromRequest(String bookId, BookUpdateRequest request);
    BookResponse getBookResponse(String bookId);

    void deleteBook(String bookId);

    // File upload
    String uploadCoverImage(MultipartFile file);

    // Query methods
    List<Book> getBooksByCategory(BookCategory category);
    List<Book> getBooksByAccessType(Book.AccessType accessType);
    List<Book> searchBooksByKeyword(String keyword);
    List<Book> getTopViewedBooks();
    List<Book> getNewestBooks();
    List<Book> getDownloadableBooks();
    List<Book> getBooksByAuthor(String authorId);
    List<Book> getBooksByMinRating(Float minRating);
    long countBooksByAccessType(Book.AccessType accessType);
    void updateBookRating(String bookId);

    // Admin statistics methods
    long getTotalBooksCount();
    long getFreeBooks();
    long getPaidBooks();
    long getSubscriptionBooks();
    List<Book> getRecentBooks(int limit);
    List<Book> getTopRatedBooks(int limit);
}

