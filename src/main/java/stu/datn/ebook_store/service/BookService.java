package stu.datn.ebook_store.service;

import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.dto.request.BookCreateRequest;
import stu.datn.ebook_store.dto.request.BookUpdateRequest;
import stu.datn.ebook_store.dto.response.BookResponse;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookCategory;

import java.util.List;
import java.util.Optional;

/**
 * Book Service Interface
 * REFACTORED: Removed 7 unused methods (REST API methods, uploadCoverImage, getDownloadableBooks, getBooksByMinRating, countBooksByAccessType)
 */
public interface BookService {
    // Basic CRUD operations
    List<Book> getAllBooks();
    Optional<Book> getBookById(String bookId);
    Book saveBook(Book book);

    // MVC form operations (Admin) - ĐANG DÙNG
    Book createBook(BookCreateRequest request);
    Book updateBook(BookUpdateRequest request);

    void deleteBook(String bookId);

    // Query methods
    List<Book> getBooksByCategory(BookCategory category);
    List<Book> getBooksByAccessType(Book.AccessType accessType);
    List<Book> searchBooksByKeyword(String keyword);
    List<Book> getTopViewedBooks();
    List<Book> getNewestBooks();
    List<Book> getBooksByAuthor(String authorId); // GIỮ LẠI - author page future
    void updateBookRating(String bookId);

    // Admin statistics methods
    long getTotalBooksCount();
    long getFreeBooks();
    long getPaidBooks();
    long getSubscriptionBooks();
    List<Book> getRecentBooks(int limit);
    List<Book> getTopRatedBooks(int limit);
}

