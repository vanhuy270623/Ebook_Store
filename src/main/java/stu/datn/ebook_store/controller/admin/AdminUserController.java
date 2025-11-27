package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.UserCreateRequest;
import stu.datn.ebook_store.dto.request.UserUpdateRequest;
import stu.datn.ebook_store.entity.Role;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.RoleRepository;
import stu.datn.ebook_store.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller xử lý quản lý người dùng (CRUD User)
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final String ROOT_ADMIN_ID = "user_admin_01";
    private static final String REDIRECT_USERS = "redirect:/admin/users";

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminUserController(UserService userService, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Lấy thông tin user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Kiểm tra xem user có phải là admin gốc không
     */
    private boolean isRootAdmin(User user) {
        return ROOT_ADMIN_ID.equals(user.getUserId());
    }

    /**
     * Kiểm tra xem user có phải là admin không
     */
    private boolean isAdmin(User user) {
        return user.getRole() != null && user.getRole().getRoleName() == Role.RoleName.ADMIN;
    }

    /**
     * Kiểm tra quyền quản lý admin
     * Chỉ admin gốc mới có quyền tạo/sửa/xóa admin khác
     */
    private boolean canManageAdmin(User currentUser, User targetUser, boolean isEditingSelf) {
        if (isRootAdmin(currentUser)) {
            return true;
        }
        if (isAdmin(targetUser) && !isEditingSelf) {
            return false;
        }
        return true;
    }

    /**
     * Sinh User ID tự động theo format:
     * - "user_admin_XX" cho admin
     * - "user_normal_XX" cho user thường
     */
    private String generateNextUserId(Role.RoleName roleName) {
        if (roleName == Role.RoleName.ADMIN) {
            long adminCount = userService.getAdminUsersCount();
            int nextNumber = (int) adminCount + 1;
            return String.format("user_admin_%02d", nextNumber);
        } else {
            long userCount = userService.getTotalUsersCount();
            int nextNumber = (int) userCount + 1;
            return String.format("user_normal_%02d", nextNumber);
        }
    }

    /**
     * Validate email uniqueness (trừ email hiện tại của user đang sửa)
     */
    private boolean isEmailDuplicate(String email, String currentEmail) {
        if (email.equals(currentEmail)) {
            return false;
        }
        return userService.checkEmailExists(email);
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, User currentUser, User user, boolean isEdit) {
        model.addAttribute("adminUser", currentUser);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("canManageAdmin", isRootAdmin(currentUser));
        if (isEdit && user != null) {
            model.addAttribute("user", user);
            model.addAttribute("isEditingSelf", currentUser.getUserId().equals(user.getUserId()));
        }
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách người dùng
     */
    @GetMapping
    public String usersList(@RequestParam(required = false) String search,
                           Authentication authentication,
                           Model model) {
        User currentUser = getCurrentUser(authentication);
        List<User> users = userService.searchUsers(search);

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("search", search);
        model.addAttribute("currentAdminId", currentUser.getUserId());
        model.addAttribute("isRootAdmin", isRootAdmin(currentUser));

        return "admin/users/list";
    }

    /**
     * Xem chi tiết người dùng
     */
    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByIdWithRole(id).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng với ID: " + id);
            return REDIRECT_USERS;
        }

        String roleName = (user.getRole() != null && user.getRole().getRoleName() != null)
            ? user.getRole().getRoleName().name() : null;

        model.addAttribute("user", user);
        model.addAttribute("roleName", roleName);
        return "admin/users/view";
    }

    /**
     * Hiển thị form thêm người dùng mới
     */
    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);

        addCommonFormAttributes(model, currentUser, null, false);
        model.addAttribute("userRequest", new UserCreateRequest());

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
        User currentUser = getCurrentUser(authentication);
        User user = userService.getUserById(id).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return REDIRECT_USERS;
        }

        // Kiểm tra quyền sửa
        boolean isEditingSelf = currentUser.getUserId().equals(id);
        if (!canManageAdmin(currentUser, user, isEditingSelf)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc mới có quyền chỉnh sửa admin khác!");
            return REDIRECT_USERS;
        }

        // Chuyển đổi User entity sang DTO
        UserUpdateRequest userUpdateRequest = mapToUpdateRequest(user);

        addCommonFormAttributes(model, currentUser, user, true);
        model.addAttribute("userRequest", userUpdateRequest);

        return "admin/users/form";
    }

    /**
     * Map User entity sang UserUpdateRequest DTO
     */
    private UserUpdateRequest mapToUpdateRequest(User user) {
        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRoleId(user.getRole() != null ? user.getRole().getRoleId() : null);
        dto.setIsActive(user.getIsActive());
        dto.setIsVerified(user.getIsVerified());
        dto.setPreferredReadingMode(user.getPreferredReadingMode());
        return dto;
    }

    /**
     * Tạo người dùng mới
     */
    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("userRequest") UserCreateRequest request,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            addCommonFormAttributes(model, currentUser, null, false);
            return "admin/users/form";
        }

        // Kiểm tra quyền tạo admin
        Role targetRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        if (targetRole.getRoleName() == Role.RoleName.ADMIN && !isRootAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc mới có quyền tạo tài khoản admin!");
            return "redirect:/admin/users/add";
        }

        // Validate uniqueness
        if (userService.checkUsernameExists(request.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
            return "redirect:/admin/users/add";
        }

        if (userService.checkEmailExists(request.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng!");
            return "redirect:/admin/users/add";
        }

        // Tạo User entity
        String newUserId = generateNextUserId(targetRole.getRoleName());
        User newUser = createUserFromRequest(request, targetRole, newUserId);

        userService.saveUser(newUser);
        redirectAttributes.addFlashAttribute("success",
            "Thêm người dùng thành công! ID: " + newUserId + ", Username: " + request.getUsername());

        return REDIRECT_USERS;
    }

    /**
     * Tạo User entity từ CreateRequest DTO
     */
    private User createUserFromRequest(UserCreateRequest request, Role role, String userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        user.setIsVerified(request.getIsVerified() != null ? request.getIsVerified() : false);
        user.setPreferredReadingMode(request.getPreferredReadingMode());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Cập nhật người dùng
     */
    @PostMapping("/update")
    public String updateUser(@Valid @ModelAttribute("userRequest") UserUpdateRequest request,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));

            model.addAttribute("error", errors);
            User user = userService.getUserById(request.getUserId()).orElse(null);
            addCommonFormAttributes(model, currentUser, user, true);
            return "admin/users/form";
        }

        User existingUser = userService.getUserById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra email trùng
        if (isEmailDuplicate(request.getEmail(), existingUser.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi người dùng khác!");
            return "redirect:/admin/users/edit/" + request.getUserId();
        }

        // Kiểm tra quyền sửa admin
        boolean isEditingSelf = currentUser.getUserId().equals(existingUser.getUserId());
        if (!canManageAdmin(currentUser, existingUser, isEditingSelf)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc mới có quyền sửa admin khác!");
            return REDIRECT_USERS;
        }

        // Cập nhật thông tin cơ bản
        updateBasicInfo(existingUser, request);

        // Cập nhật role và status (chỉ khi không sửa chính mình)
        if (!isEditingSelf) {
            updateRoleAndStatus(existingUser, request, currentUser, redirectAttributes);
        }

        // Cập nhật password nếu có
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            existingUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        existingUser.setUpdatedAt(LocalDateTime.now());
        userService.saveUser(existingUser);

        redirectAttributes.addFlashAttribute("success", "Cập nhật người dùng thành công!");
        return REDIRECT_USERS;
    }

    /**
     * Cập nhật thông tin cơ bản của user
     */
    private void updateBasicInfo(User user, UserUpdateRequest request) {
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPreferredReadingMode(request.getPreferredReadingMode());
    }

    /**
     * Cập nhật role và status của user (chỉ khi admin sửa user khác)
     */
    private void updateRoleAndStatus(User user, UserUpdateRequest request,
                                     User currentUser, RedirectAttributes redirectAttributes) {
        user.setIsActive(request.getIsActive());
        user.setIsVerified(request.getIsVerified());

        Role targetRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        if (targetRole.getRoleName() == Role.RoleName.ADMIN && !isRootAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("warning", "Chỉ admin gốc mới có quyền cấp quyền admin!");
            return;
        }

        user.setRole(targetRole);
    }

    /**
     * Xóa người dùng
     */
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        User targetUser = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra quyền xóa admin
        if (isAdmin(targetUser) && !isRootAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin gốc mới có quyền xóa admin!");
            return REDIRECT_USERS;
        }

        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");

        return REDIRECT_USERS;
    }

    /**
     * Kích hoạt/vô hiệu hóa người dùng
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable String id,
                                   RedirectAttributes redirectAttributes) {
        userService.toggleUserStatus(id);
        redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái người dùng thành công!");
        return REDIRECT_USERS;
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
