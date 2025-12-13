package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating a new banner (Admin only)
 * Mapped to table: banner
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerCreateRequest {

    @NotBlank(message = "Tiêu đề banner không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Size(max = 500, message = "URL hình ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    @Size(max = 500, message = "URL đích không được vượt quá 500 ký tự")
    private String targetUrl;

    @NotBlank(message = "Vị trí banner không được để trống")
    private String position; // HOME, CATEGORY, DETAIL

    private Boolean isActive = true;
}

