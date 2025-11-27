package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Book;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO for Book form binding in MVC Controllers (Thymeleaf forms)
 * Used by AdminBookController for both create and edit operations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookFormRequest {

    // For edit mode - will be null for create mode
    private String bookId;

    @NotBlank(message = "Tiêu đề sách không được để trống")
    @Size(max = 500, message = "Tiêu đề không được vượt quá 500 ký tự")
    private String title;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    private String coverImageUrl;

    @Size(max = 255, message = "Nhà xuất bản không được vượt quá 255 ký tự")
    private String publisher;

    @Min(value = 1000, message = "Năm xuất bản phải từ 1000 trở lên")
    @Max(value = 9999, message = "Năm xuất bản không hợp lệ")
    private Integer publicationYear;

    @Size(max = 50, message = "Ngôn ngữ không được vượt quá 50 ký tự")
    private String language;

    @Min(value = 1, message = "Số trang phải lớn hơn 0")
    private Integer pages;

    @Size(max = 20, message = "ISBN không được vượt quá 20 ký tự")
    private String isbn;

    @NotNull(message = "Loại truy cập không được để trống")
    private Book.AccessType accessType;

    private Boolean isDownloadable = false;

    @NotBlank(message = "ID danh mục không được để trống")
    private String bookCategoryId;

    // For form display
    private String categoryName;

    // Author IDs will be handled separately in controller
    private Set<String> authorIds;

    /**
     * Constructor from Book entity (for edit form population)
     */
    public BookFormRequest(Book book) {
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

        if (book.getBookCategory() != null) {
            this.bookCategoryId = book.getBookCategory().getBookCategoryId();
            this.categoryName = book.getBookCategory().getCategoryName();
        }
    }
}

