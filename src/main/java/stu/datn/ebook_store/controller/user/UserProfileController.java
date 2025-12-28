package stu.datn.ebook_store.controller.user;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.UserUpdateRequest;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.FileStorageService;
import stu.datn.ebook_store.service.UserService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * UserProfileController - Quản lý tài khoản
 * Chịu trách nhiệm về thông tin cá nhân, cập nhật hồ sơ và bảo mật.
 * * Endpoints:
 * - GET  /user/profile               : Xem hồ sơ cá nhân
 * - POST /user/profile/update        : Cập nhật thông tin hồ sơ
 * - POST /user/profile/change-password : Đổi mật khẩu
 */
@Controller
@RequestMapping("/user")
public class UserProfileController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Autowired
    public UserProfileController(UserService userService,
                                 PasswordEncoder passwordEncoder,
                                 FileStorageService fileStorageService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Trang hồ sơ người dùng
     */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        UserUpdateRequest userRequest = new UserUpdateRequest();
        userRequest.setUserId(currentUser.getUserId());
        userRequest.setEmail(currentUser.getEmail());
        userRequest.setFullName(currentUser.getFullName());
        userRequest.setPhone(currentUser.getPhone());
        userRequest.setAvatarUrl(currentUser.getAvatarUrl());
        userRequest.setRoleId(currentUser.getRole() != null ? currentUser.getRole().getRoleId() : null);

        model.addAttribute("user", currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRequest", userRequest);

        // Layout variables
        model.addAttribute("pageTitle", "Hồ sơ cá nhân");
        model.addAttribute("currentPage", "profile");

        return "user/profile";
    }

    /**
     * Cập nhật hồ sơ người dùng
     */
    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute("userRequest") UserUpdateRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            model.addAttribute("user", currentUser);
            return "user/profile";
        }

        try {
            // Kiểm tra email trùng (trừ email hiện tại)
            if (!request.getEmail().equals(currentUser.getEmail()) &&
                    userService.checkEmailExists(request.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng");
                return "redirect:/user/profile";
            }

            // Xử lý upload avatar nếu có file
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    // Type: "avatars" -> lưu vào book_asset/image/avatars/{userId}.jpg
                    // Hàm này sẽ tự động ghi đè file cũ nếu tồn tại
                    String avatarUrl = fileStorageService.storeEntityImage(avatarFile, "avatars", currentUser.getUserId());

                    // Thêm "/" để thành đường dẫn web
                    currentUser.setAvatarUrl("/" + avatarUrl);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Lỗi khi upload avatar: " + e.getMessage());
                    return "redirect:/user/profile";
                }
            } else if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
                // Nếu không upload file mới nhưng có URL thì dùng URL
                currentUser.setAvatarUrl(request.getAvatarUrl());
            }

            // Cập nhật thông tin
            currentUser.setEmail(request.getEmail());
            currentUser.setFullName(request.getFullName());
            currentUser.setPhone(request.getPhone());
            currentUser.setUpdatedAt(LocalDateTime.now());

            userService.saveUser(currentUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công");

            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /**
     * Đổi mật khẩu
     */
    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser(authentication);

        try {
            // Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(currentPassword, currentUser.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng");
                return "redirect:/user/profile";
            }

            // Kiểm tra mật khẩu mới trùng nhau
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không trùng khớp");
                return "redirect:/user/profile";
            }

            // Kiểm tra mật khẩu mới không trống
            if (newPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới không được để trống");
                return "redirect:/user/profile";
            }

            // Cập nhật mật khẩu
            currentUser.setPasswordHash(passwordEncoder.encode(newPassword));
            userService.saveUser(currentUser);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");

            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }
}