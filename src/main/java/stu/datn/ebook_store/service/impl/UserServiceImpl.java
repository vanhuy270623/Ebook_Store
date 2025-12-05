package stu.datn.ebook_store.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.service.UserService;
import stu.datn.ebook_store.repository.UserRepository;
import stu.datn.ebook_store.repository.RoleRepository;
import stu.datn.ebook_store.dto.RegisterDto;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Role;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Xác thực đăng nhập người dùng (chỉ user chưa bị xóa)
     */
    @Override
    public User authenticateUser(String username, String password) throws Exception {
        // Tìm user theo username (chỉ tìm user chưa bị xóa)
        User user = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new Exception("Tên đăng nhập hoặc mật khẩu không đúng"));

        // Kiểm tra tài khoản có bị khóa không
        if (!user.getIsActive()) {
            throw new Exception("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        // Kiểm tra password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new Exception("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        return user;
    }

    /**
     * Cập nhật thời gian đăng nhập cuối cùng (chỉ cho user chưa bị xóa)
     */
    @Override
    public void updateLastLogin(String userId) {
        userRepository.findActiveById(userId).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Đăng ký người dùng mới
     */
    @Override
    public void registerUser(RegisterDto registerDto) throws Exception {
        // 1. Kiểm tra username đã tồn tại chưa (chỉ kiểm tra user chưa bị xóa)
        if (userRepository.findActiveByUsername(registerDto.getUsername()).isPresent()) {
            throw new Exception("Username đã tồn tại");
        }

        // 2. Kiểm tra email đã tồn tại chưa (chỉ kiểm tra user chưa bị xóa)
        if (userRepository.findActiveByEmail(registerDto.getEmail()).isPresent()) {
            throw new Exception("Email đã được sử dụng");
        }

        // 3. Tìm role "USER" (từ CSDL)
        Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
                .orElseThrow(() -> new Exception("Không tìm thấy Role 'USER'. Vui lòng thêm role này vào CSDL."));

        // 4. Tự động sinh User ID theo format "user_normal_số thứ tự"
        String newUserId = generateNextUserId();

        // 5. Tạo đối tượng User mới
        User user = new User();
        user.setUserId(newUserId);
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        user.setRole(userRole);
        user.setIsActive(true);
        user.setIsVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * Kiểm tra username đã tồn tại chưa (chỉ kiểm tra user chưa bị xóa)
     */
    @Override
    public boolean checkUsernameExists(String username) {
        return userRepository.findActiveByUsername(username).isPresent();
    }

    /**
     * Kiểm tra email đã tồn tại chưa (chỉ kiểm tra user chưa bị xóa)
     */
    @Override
    public boolean checkEmailExists(String email) {
        return userRepository.findActiveByEmail(email).isPresent();
    }

    /**
     * Sinh User ID tự động theo format "user_normal_XX"
     */
    private String generateNextUserId() {
        long userCount = userRepository.countActive();
        int nextNumber = (int) userCount + 1;
        return String.format("user_normal_%02d", nextNumber);
    }

    @Override
    public long getTotalUsersCount() {
        return userRepository.countActive();
    }

    @Override
    public long getActiveUsersCount() {
        return userRepository.countActiveByIsActive(true);
    }

    @Override
    @Transactional(readOnly = true)
    public long getVerifiedUsersCount() {
        return userRepository.countActiveByIsVerified(true);
    }

    @Override
    @Transactional(readOnly = true)
    public long getAdminUsersCount() {
        return userRepository.countActiveByRoleName(Role.RoleName.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getRecentUsers(int limit) {
        return userRepository.findTopActiveUsersOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllActive();
    }

    @Override
    public Optional<User> getUserById(String userId) {
        return userRepository.findActiveById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByIdWithRole(String userId) {
        return userRepository.findActiveByIdWithRole(userId);
    }

    @Override
    @Transactional
    public User saveUser(User user) {
        // Logic to differentiate between create and update
        boolean isNew = user.getUserId() == null || user.getUserId().isEmpty();

        if (isNew) {
            // For new users created from admin panel
            user.setCreatedAt(LocalDateTime.now());

            // Set default values if not provided
            if (user.getIsActive() == null) {
                user.setIsActive(true);
            }
            if (user.getIsVerified() == null) {
                user.setIsVerified(false);
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        // This method is kept for backward compatibility but now does soft delete
        softDeleteUser(userId);
    }

    @Override
    @Transactional
    public void softDeleteUser(String userId) {
        userRepository.findByIdIncludingDeleted(userId).ifPresent(user -> {
            if (!user.isDeleted()) {
                user.markAsDeleted();
                userRepository.save(user);
            }
        });
    }

    @Override
    @Transactional
    public void restoreUser(String userId) {
        userRepository.findByIdIncludingDeleted(userId).ifPresent(user -> {
            if (user.isDeleted()) {
                user.restore();
                userRepository.save(user);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getDeletedUsers() {
        return userRepository.findDeletedUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsersIncludingDeleted() {
        return userRepository.findAllIncludingDeleted();
    }

    @Override
    @Transactional
    public void toggleUserStatus(String userId) {
        userRepository.findActiveById(userId).ifPresent(user -> {
            user.setIsActive(!user.getIsActive());
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        // If keyword is null or empty, return all active users
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAllActive();
        }

        // Use the custom repository search method for active users only
        return userRepository.searchActiveByKeyword(keyword.trim());
    }
}
