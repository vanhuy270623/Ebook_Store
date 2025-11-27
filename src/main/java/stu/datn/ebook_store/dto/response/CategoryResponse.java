package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.BookCategory;

import java.time.LocalDateTime;

/**
 * DTO for Category response
 * Mapped from table: book_category
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private String categoryId;
    private String categoryName;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Integer totalBooks; // Calculated field - số sách trong danh mục

    // Constructor from Entity
    public CategoryResponse(BookCategory category) {
        this.categoryId = category.getBookCategoryId();
        this.categoryName = category.getCategoryName();
        this.description = category.getDescription();
        this.iconUrl = category.getIconUrl();
        this.displayOrder = category.getDisplayOrder();
        this.isActive = category.getIsActive();
        this.createdAt = category.getCreatedAt();
    }

    // Constructor with book count
    public CategoryResponse(BookCategory category, Integer totalBooks) {
        this(category);
        this.totalBooks = totalBooks;
    }
}

