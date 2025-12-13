package stu.datn.ebook_store.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDto {
    @NotEmpty (message = "Username không được để trống")
    private String username;
    @NotEmpty (message = "Email không được để trống")
    private String email;
    @NotEmpty (message = "Mật khẩu không được để trống")
    private String password;
}
