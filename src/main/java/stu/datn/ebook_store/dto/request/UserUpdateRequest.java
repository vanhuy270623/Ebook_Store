package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.User;

/**
 * DTO for updating an existing user (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "User ID không được để trống")
    private String userId;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải là 10-11 chữ số")
    private String phone;

    @Size(max = 500, message = "URL avatar không được vượt quá 500 ký tự")
    private String avatarUrl;

    @NotBlank(message = "Role không được để trống")
    private String roleId;

    private Boolean isActive;

    private Boolean isVerified;

    private User.ReadingMode preferredReadingMode;

    // Admin có thể reset password
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6-100 ký tự")
    private String newPassword;
}

