package stu.datn.ebook_store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private String bookId;
    private String bookCategoryId;
    private String categoryName;
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
    private Set<String> authorIds;
    private String authorNames; // Comma separated author names

    // Constructor from Entity
    public BookDTO(Book book) {
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

        if (book.getBookCategory() != null) {
            this.bookCategoryId = book.getBookCategory().getBookCategoryId();
            this.categoryName = book.getBookCategory().getCategoryName();
        }

        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            this.authorNames = String.join(", ",
                book.getAuthors().stream()
                    .map(Author::getName)
                    .toList());
        }
    }
}

