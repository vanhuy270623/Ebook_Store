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
import stu.datn.ebook_store.service.UserService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * UserProfileController - Quản lý tài khoản
 * Chịu trách nhiệm về thông tin cá nhân, cập nhật hồ sơ và bảo mật.
 * 
 * Endpoints:
 * - GET  /user/profile               : Xem hồ sơ cá nhân
 * - POST /user/profile/update        : Cập nhật thông tin hồ sơ
 * - POST /user/profile/change-password : Đổi mật khẩu
 */
@Controller
@RequestMapping("/user")
public class UserProfileController {

    private static final String AVATAR_UPLOAD_DIR = "F:/datn_uploads/book_asset/image/avatars/";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserProfileController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
                    String avatarUrl = saveAvatar(avatarFile, currentUser.getUserId());
                    currentUser.setAvatarUrl(avatarUrl);
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
     * Lưu avatar vào thư mục avatars với tên là userId
     */
    private String saveAvatar(MultipartFile file, String userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IOException("File rỗng");
        }

        // Check file size (5MB max)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IOException("Kích thước file không được vượt quá 5MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Chỉ chấp nhận file ảnh (JPG, PNG, GIF)");
        }

        // Tạo thư mục nếu chưa có
        File uploadDir = new File(AVATAR_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("Không thể tạo thư mục upload");
            }
        }

        // Lấy extension của file
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // Validate extension
            if (!fileExtension.matches("\\.(jpg|jpeg|png|gif)$")) {
                throw new IOException("Chỉ chấp nhận định dạng JPG, PNG, GIF");
            }
        } else {
            // Default to jpg if no extension
            fileExtension = ".jpg";
        }

        // Tên file là userId + extension
        String fileName = userId + fileExtension.toLowerCase();
        Path filePath = Paths.get(AVATAR_UPLOAD_DIR + fileName);

        // Lưu file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về URL để truy cập avatar
        return "/book_asset/image/avatars/" + fileName;
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
