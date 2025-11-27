package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating a new post/blog (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 500, message = "Tiêu đề không được vượt quá 500 ký tự")
    private String title;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 500, message = "Slug không được vượt quá 500 ký tự")
    private String slug;

    @Size(max = 1000, message = "Tóm tắt không được vượt quá 1000 ký tự")
    private String excerpt;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @Size(max = 500, message = "URL thumbnail không được vượt quá 500 ký tự")
    private String thumbnailUrl;

    @NotBlank(message = "ID tác giả không được để trống")
    private String authorId;

    private Boolean isPublished = false;
}

