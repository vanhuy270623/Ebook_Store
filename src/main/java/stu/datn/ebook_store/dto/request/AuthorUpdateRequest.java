package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for updating an existing author (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorUpdateRequest {

    @NotBlank(message = "ID tác giả không được để trống")
    private String authorId;

    @NotBlank(message = "Tên tác giả không được để trống")
    @Size(max = 255, message = "Tên tác giả không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 5000, message = "Tiểu sử không được vượt quá 5000 ký tự")
    private String biography;

    @Size(max = 500, message = "URL avatar không được vượt quá 500 ký tự")
    private String avatarUrl;
}

