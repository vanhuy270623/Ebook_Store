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
 * DTO for updating an existing book (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdateRequest {

    @NotBlank(message = "ID sách không được để trống")
    private String bookId;

    @NotBlank(message = "Tiêu đề sách không được để trống")
    @Size(max = 500, message = "Tiêu đề không được vượt quá 500 ký tự")
    private String title;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Size(max = 500, message = "URL ảnh bìa không được vượt quá 500 ký tự")
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

    private Boolean isDownloadable;

    @NotBlank(message = "ID danh mục không được để trống")
    private String bookCategoryId;

    @NotEmpty(message = "Phải có ít nhất một tác giả")
    private Set<String> authorIds;
}

