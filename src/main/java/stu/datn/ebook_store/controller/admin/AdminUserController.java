package stu.datn.ebook_store.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.Role;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.RoleRepository;
import stu.datn.ebook_store.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller xử lý quản lý người dùng (CRUD User)
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminUserController(UserService userService, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hiển thị danh sách người dùng
     */
    @GetMapping
    public String usersList(@RequestParam(required = false) String search, Model model) {
        List<User> users = userService.searchUsers(search);
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("search", search);
        return "admin/users/list";
    }

    /**
     * Hiển thị chi tiết người dùng
     */
    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        return "admin/users/view";
    }

    /**
     * Hiển thị form thêm người dùng mới
     */
    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        User adminUser = (User) authentication.getPrincipal();
        List<Role> roles = roleRepository.findAll();

        // Chỉ admin_1 có quyền tạo admin
        boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());

        model.addAttribute("adminUser", adminUser);
        model.addAttribute("roles", roles);
        model.addAttribute("user", new User());
        model.addAttribute("isEdit", false);
        model.addAttribute("canManageAdmin", canManageAdmin);

        return "admin/users/form";
    }

    /**
     * Hiển thị form chỉnh sửa người dùng
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User adminUser = (User) authentication.getPrincipal();
        User user = userService.getUserById(id).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/admin/users";
        }

        // Kiểm tra quyền sửa admin: chỉ admin_1 có thể sửa admin khác
        boolean isTargetUserAdmin = user.getRole() != null && user.getRole().getRoleName() == Role.RoleName.ADMIN;
        boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());

        if (isTargetUserAdmin && !canManageAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc mới có quyền chỉnh sửa admin khác!");
            return "redirect:/admin/users";
        }

        List<Role> roles = roleRepository.findAll();

        model.addAttribute("adminUser", adminUser);
        model.addAttribute("roles", roles);
        model.addAttribute("user", user);
        model.addAttribute("isEdit", true);
        model.addAttribute("canManageAdmin", canManageAdmin);

        return "admin/users/form";
    }

    /**
     * Lưu người dùng (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveUser(@ModelAttribute User user,
                          @RequestParam(required = false) String roleId,
                          @RequestParam(required = false) String password,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            User adminUser = (User) authentication.getPrincipal();
            boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());
            boolean isNew = user.getUserId() == null || user.getUserId().isEmpty();

            // Kiểm tra quyền tạo/sửa admin
            Role targetRole = null;
            if (roleId != null && !roleId.isEmpty()) {
                targetRole = roleRepository.findById(roleId).orElse(null);
                if (targetRole != null && targetRole.getRoleName() == Role.RoleName.ADMIN && !canManageAdmin) {
                    redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc (admin_1) mới có quyền tạo/quản lý admin khác!");
                    return "redirect:/admin/users";
                }
            }

            if (isNew) {
                // Create new user - Tự động sinh ID
                String newUserId = generateNextUserId();
                user.setUserId(newUserId);
                user.setCreatedAt(LocalDateTime.now());

                // Set default password if not provided
                if (password == null || password.isEmpty()) {
                    password = "123456"; // Default password
                }
                user.setPasswordHash(passwordEncoder.encode(password));

                // Set role
                if (targetRole != null) {
                    user.setRole(targetRole);
                } else {
                    // Set default USER role
                    Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
                            .orElseThrow(() -> new RuntimeException("Default USER role not found"));
                    user.setRole(userRole);
                }

                // Set default active status if not set
                if (user.getIsActive() == null) {
                    user.setIsActive(true);
                }

                redirectAttributes.addFlashAttribute("success", "Thêm người dùng thành công! ID: " + newUserId);
            } else {
                // Update existing user
                User existingUser = userService.getUserById(user.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // Kiểm tra quyền sửa admin
                boolean isTargetUserAdmin = existingUser.getRole() != null &&
                                           existingUser.getRole().getRoleName() == Role.RoleName.ADMIN;
                if (isTargetUserAdmin && !canManageAdmin) {
                    redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc (admin_1) mới có quyền sửa admin khác!");
                    return "redirect:/admin/users";
                }

                existingUser.setFullName(user.getFullName());
                existingUser.setEmail(user.getEmail());
                existingUser.setPhone(user.getPhone());
                existingUser.setAvatarUrl(user.getAvatarUrl());
                existingUser.setIsActive(user.getIsActive());
                existingUser.setUpdatedAt(LocalDateTime.now());

                // Update password if provided
                if (password != null && !password.isEmpty()) {
                    existingUser.setPasswordHash(passwordEncoder.encode(password));
                }

                // Update role if provided
                if (targetRole != null) {
                    existingUser.setRole(targetRole);
                }

                user = existingUser;
                redirectAttributes.addFlashAttribute("success", "Cập nhật người dùng thành công!");
            }

            userService.saveUser(user);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Sinh User ID tự động theo format "user_normal_XX"
     */
    private String generateNextUserId() {
        // Đếm số lượng user hiện có
        long userCount = userService.getTotalUsersCount();

        // Tạo ID mới với format user_normal_XX
        int nextNumber = (int) userCount + 1;
        return String.format("user_normal_%02d", nextNumber);
    }

    /**
     * Xóa người dùng
     */
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User adminUser = (User) authentication.getPrincipal();
            boolean canManageAdmin = "admin_1".equals(adminUser.getUserId());

            // Prevent deleting admin_1
            if ("admin_1".equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản admin gốc (admin_1)!");
                return "redirect:/admin/users";
            }

            // Prevent admin from deleting themselves
            if (adminUser.getUserId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không thể xóa tài khoản của chính mình!");
                return "redirect:/admin/users";
            }

            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Kiểm tra quyền xóa admin
            boolean isTargetUserAdmin = user.getRole() != null &&
                                       user.getRole().getRoleName() == Role.RoleName.ADMIN;
            if (isTargetUserAdmin && !canManageAdmin) {
                redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc (admin_1) mới có quyền xóa admin khác!");
                return "redirect:/admin/users";
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Kích hoạt/vô hiệu hóa người dùng
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable String id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            User adminUser = (User) authentication.getPrincipal();

            // Prevent admin from disabling themselves
            if (adminUser.getUserId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không thể thay đổi trạng thái tài khoản của chính mình!");
                return "redirect:/admin/users";
            }

            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái người dùng thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Hiển thị thống kê người dùng
     */
    @GetMapping("/statistics")
    public String usersStatistics(Model model) {
        model.addAttribute("totalUsers", userService.getTotalUsersCount());
        model.addAttribute("activeUsers", userService.getActiveUsersCount());
        model.addAttribute("verifiedUsers", userService.getVerifiedUsersCount());
        model.addAttribute("recentUsers", userService.getRecentUsers(10));
        return "admin/users/statistics";
    }
}
