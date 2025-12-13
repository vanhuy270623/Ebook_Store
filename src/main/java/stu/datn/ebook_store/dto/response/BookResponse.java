package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for Book response (Admin view with full details)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private String bookId;
    private String title;
    private String description;
    private BigDecimal price;
    private String coverImageUrl;
    private String publisher;
    private Integer publicationYear;
    private String language;
    private Integer pages;
    private String isbn;
    private Book.AccessType accessType;
    private Boolean isDownloadable;
    private Float averageRating;
    private Integer totalReviews;
    private Integer viewCount;
    private String bookCategoryId;
    private String categoryName;
    private List<AuthorInfo> authors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Inner class for author info
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private String authorId;
        private String name;
        private String avatarUrl;
    }

    // Constructor from Entity
    public BookResponse(Book book) {
        this.bookId = book.getBookId();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.price = book.getPrice();
        this.coverImageUrl = book.getCoverImageUrl();
        this.publisher = book.getPublisher();
        this.publicationYear = book.getPublicationYear();
        this.language = book.getLanguage();
        this.pages = book.getPages();
        this.isbn = book.getIsbn();
        this.accessType = book.getAccessType();
        this.isDownloadable = book.getIsDownloadable();
        this.averageRating = book.getAverageRating();
        this.totalReviews = book.getTotalReviews();
        this.viewCount = book.getViewCount();
        this.createdAt = book.getCreatedAt();
        this.updatedAt = book.getUpdatedAt();

        if (book.getBookCategory() != null) {
            this.bookCategoryId = book.getBookCategory().getBookCategoryId();
            this.categoryName = book.getBookCategory().getCategoryName();
        }

        if (book.getAuthors() != null) {
            this.authors = book.getAuthors().stream()
                .map(author -> new AuthorInfo(
                    author.getAuthorId(),
                    author.getName(),
                    author.getAvatarUrl()
                ))
                .collect(Collectors.toList());
        }
    }
}

