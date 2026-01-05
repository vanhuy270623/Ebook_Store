package stu.datn.ebook_store.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.dto.DeviceInfoDto;
import stu.datn.ebook_store.entity.*;
import stu.datn.ebook_store.repository.*;
import stu.datn.ebook_store.service.UserService;
import stu.datn.ebook_store.service.UserDeviceService;
import stu.datn.ebook_store.dto.RegisterDto;
import stu.datn.ebook_store.util.DeviceFingerprintUtil;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Device Management - SỬ DỤNG SERVICE LAYER (REFACTORED)
    private final UserDeviceService userDeviceService;
    private final UserDeviceRepository deviceRepository; // Giữ lại cho một số cases cần thiết
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;

    // Configuration
    private static final int DEFAULT_MAX_DEVICES = 1; // FREE users
    private static final int MAX_VIOLATIONS_BEFORE_LOCK = 3;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           UserDeviceService userDeviceService,
                           UserDeviceRepository deviceRepository,
                           SubscriptionRepository subscriptionRepository,
                           OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDeviceService = userDeviceService;
        this.deviceRepository = deviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Lấy giới hạn thiết bị cho user dựa trên subscription
     * ADMIN: Không giới hạn (999 devices)
     * FREE: 1 device, BASIC: 2 devices, PREMIUM/VIP: 3 devices
     */
    private int getMaxDevicesForUser(User user) {
        // ADMIN không giới hạn thiết bị
        if (user.getRole() != null && user.getRole().getRoleName() == Role.RoleName.ADMIN) {
            return 999; // Không giới hạn
        }

        try {
            Optional<Subscription> activeSubscription = subscriptionRepository
                    .findActiveSubscriptionByUserId(user.getUserId(), LocalDateTime.now());

            if (activeSubscription.isPresent()) {
                Integer maxDevices = activeSubscription.get().getMaxDevices();
                return maxDevices != null ? maxDevices : DEFAULT_MAX_DEVICES;
            }

            // Không có subscription active -> FREE (1 device)
            return DEFAULT_MAX_DEVICES;
        } catch (Exception e) {
            // Fallback nếu có lỗi
            return DEFAULT_MAX_DEVICES;
        }
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
        // 1. Kiểm tra username đã tồn tại chưa (kiểm tra CẢ user đã bị xóa mềm)
        Optional<User> existingUserByUsername = userRepository.findByUsernameIncludingDeleted(registerDto.getUsername());
        if (existingUserByUsername.isPresent()) {
            User existingUser = existingUserByUsername.get();
            if (existingUser.isDeleted()) {
                throw new Exception("Tên người dùng '" + registerDto.getUsername() +
                    "' đã từng được sử dụng bởi tài khoản đã bị xóa. Vui lòng chọn tên khác hoặc liên hệ quản trị viên để khôi phục tài khoản.");
            } else {
                throw new Exception("Tên người dùng '" + registerDto.getUsername() +
                    "' đã được sử dụng. Vui lòng chọn tên khác.");
            }
        }

        // 2. Kiểm tra email đã tồn tại chưa (kiểm tra CẢ user đã bị xóa mềm)
        Optional<User> existingUserByEmail = userRepository.findByEmailIncludingDeleted(registerDto.getEmail());
        if (existingUserByEmail.isPresent()) {
            User existingUser = existingUserByEmail.get();
            if (existingUser.isDeleted()) {
                throw new Exception("Email '" + registerDto.getEmail() +
                    "' đã từng được đăng ký cho tài khoản đã bị xóa. Vui lòng sử dụng email khác hoặc liên hệ quản trị viên để khôi phục tài khoản.");
            } else {
                throw new Exception("Email '" + registerDto.getEmail() +
                    "' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
            }
        }

        // 3. Tìm role "USER" (từ CSDL)
        Role userRole = roleRepository.findByRoleName(Role.RoleName.USER)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy Role 'USER'. Vui lòng liên hệ quản trị viên."));

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

        try {
            userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Xử lý lỗi constraint violation từ database (trường hợp hiếm gặp)
            String errorMessage = e.getMessage();

            // Kiểm tra chính xác constraint nào bị vi phạm
            if (errorMessage.contains("key 'users.email'") ||
                (errorMessage.contains("Duplicate entry") && errorMessage.contains("for key 'users.email'"))) {
                throw new Exception("Email '" + registerDto.getEmail() + "' đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
            } else if (errorMessage.contains("key 'users.username'") ||
                       (errorMessage.contains("Duplicate entry") && errorMessage.contains("for key 'users.username'"))) {
                throw new Exception("Tên người dùng '" + registerDto.getUsername() + "' đã được sử dụng. Vui lòng chọn tên khác.");
            } else if (errorMessage.contains("users.email")) {
                throw new Exception("Email '" + registerDto.getEmail() + "' đã được đăng ký. Vui lòng sử dụng email khác.");
            } else if (errorMessage.contains("users.username")) {
                throw new Exception("Tên người dùng '" + registerDto.getUsername() + "' đã tồn tại. Vui lòng chọn tên khác.");
            } else {
                throw new Exception("Không thể tạo tài khoản. Vui lòng kiểm tra lại thông tin và thử lại.");
            }
        } catch (Exception e) {
            // Nếu đã là Exception tùy chỉnh từ catch block trên, throw lại
            if (e.getMessage().contains("đã được") ||
                e.getMessage().contains("đã tồn tại") ||
                e.getMessage().contains("Không thể tạo") ||
                e.getMessage().startsWith("Lỗi hệ thống")) {
                throw e;
            }
            // Ném exception chung cho các lỗi khác
            throw new Exception("Lỗi hệ thống khi đăng ký: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra username đã tồn tại chưa (chỉ kiểm tra user chưa bị xóa)
     */
    @Override
    public boolean checkUsernameExists(String username) {
        return userRepository.findActiveByUsername(username).isPresent();
    }

    /**
     * Xác thực đăng nhập người dùng (chỉ user chưa bị xóa)
     * INTERNAL METHOD - Được gọi từ authenticateWithDeviceCheck
     */
    private User authenticateUser(String username, String password) throws Exception {
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
    public long getAllAdminsCountIncludingDeleted() {
        return userRepository.countAllByRoleName(Role.RoleName.ADMIN);
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
    @Transactional
    public User saveUser(User user) {
        // Logic to differentiate between create and update
        boolean isNew = user.getUserId() == null || user.getUserId().isEmpty();
        boolean isAdmin = user.getRole() != null && user.getRole().getRoleName() == Role.RoleName.ADMIN;

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
        User savedUser = userRepository.save(user);

        // Tự động tạo VIP subscription cho admin mới
        if (isNew && isAdmin) {
            createVipSubscriptionForAdmin(savedUser);
        }

        return savedUser;
    }

    /**
     * Tạo VIP subscription vĩnh viễn cho admin
     */
    private void createVipSubscriptionForAdmin(User admin) {
        try {
            // Tìm gói VIP
            Optional<Subscription> vipSubscription = subscriptionRepository.findByPackageName(Subscription.PackageName.VIP);

            if (vipSubscription.isPresent()) {
                // Tạo order subscription cho admin với thời hạn 100 năm
                Order order = new Order();
                order.setOrderId("SUB_ADMIN_" + admin.getUserId() + "_" + System.currentTimeMillis());
                order.setUser(admin);
                order.setSubscription(vipSubscription.get());
                order.setOrderType(Order.OrderType.SUBSCRIPTION);
                order.setTotalAmount(java.math.BigDecimal.ZERO); // Miễn phí cho admin
                order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
                order.setPaymentMethod(Order.PaymentMethod.BANK_TRANSFER);
                order.setStartDate(LocalDateTime.now());
                order.setEndDate(LocalDateTime.now().plusYears(100)); // 100 năm
                order.setCreatedAt(LocalDateTime.now());

                orderRepository.save(order);
            }
        } catch (Exception e) {
            // Log error nhưng không throw exception để không ảnh hưởng việc tạo user
            System.err.println("Không thể tạo VIP subscription cho admin: " + e.getMessage());
        }
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

    // ============================================================================
    // DEVICE MANAGEMENT IMPLEMENTATION - Tích hợp logic quản lý thiết bị
    // ============================================================================

    /**
     * Xác thực login với kiểm tra device (Logic chính của hệ thống)
     * ADMIN: Không kiểm tra giới hạn thiết bị
     */
    @Override
    @Transactional
    public Map<String, Object> authenticateWithDeviceCheck(
            String username,
            String password,
            DeviceInfoDto deviceInfo,
            HttpServletRequest request) throws Exception {

        Map<String, Object> result = new HashMap<>();

        // 1. Xác thực username/password
        User user = authenticateUser(username, password);

        // 2. Kiểm tra account đã bị lock chưa
        if (user.isLocked()) {
            result.put("status", "ACCOUNT_LOCKED");
            result.put("reason", user.getAccountLockedReason());
            throw new Exception(user.getAccountLockedReason());
        }

        // 3. Lấy thông tin device fingerprint
        String deviceFingerprint = deviceInfo != null && deviceInfo.getDeviceFingerprint() != null
                ? deviceInfo.getDeviceFingerprint()
                : DeviceFingerprintUtil.generateServerSideFingerprint(request);

        String clientIp = DeviceFingerprintUtil.getClientIpAddress(request);

        // 4. Kiểm tra xem device đã tồn tại chưa
        Optional<UserDevice> existingDevice = deviceRepository
                .findActiveDeviceByFingerprint(user.getUserId(), deviceFingerprint);

        if (existingDevice.isPresent()) {
            // Device đã đăng ký -> Cho phép login
            UserDevice device = existingDevice.get();
            updateDeviceOnLogin(device, clientIp, request);

            result.put("status", "SUCCESS");
            result.put("device", device);
            result.put("user", user);
            return result;
        }

        // 5. Device mới -> Kiểm tra giới hạn (ADMIN ĐƯỢC BỎ QUA)
        boolean isAdmin = user.getRole() != null && user.getRole().getRoleName() == Role.RoleName.ADMIN;

        if (!isAdmin) {
            // Chỉ kiểm tra giới hạn cho user thường - SỬ DỤNG SERVICE
            int maxDevicesAllowed = getMaxDevicesForUser(user);
            int activeDeviceCount = userDeviceService.getActiveDeviceCount(user.getUserId());

            if (activeDeviceCount >= maxDevicesAllowed) {
                // VƯỢT QUÁ GIỚI HẠN
                handleDeviceLimitExceeded(user, deviceFingerprint, clientIp, request, maxDevicesAllowed);

                result.put("status", "DEVICE_LIMIT_EXCEEDED");
                result.put("activeDeviceCount", activeDeviceCount);
                result.put("maxDevices", maxDevicesAllowed);
                result.put("violationCount", user.getDeviceViolationCount());

                return result;
            }
        }

        // 6. Đăng ký device mới (ADMIN hoặc user còn slot)
        UserDevice newDevice = registerNewDevice(user, deviceInfo, deviceFingerprint, clientIp, request);

        result.put("status", "SUCCESS");
        result.put("device", newDevice);
        result.put("user", user);
        result.put("isNewDevice", true);

        return result;
    }

    /**
     * Xử lý khi vượt quá giới hạn thiết bị
     */
    private void handleDeviceLimitExceeded(User user, String fingerprint, String ip,
                                          HttpServletRequest request, int maxDevices) throws Exception {
        // Tăng violation count
        user.incrementDeviceViolation();

        // KHÓA TÀI KHOẢN nếu vượt quá 3 lần
        if (user.getDeviceViolationCount() >= MAX_VIOLATIONS_BEFORE_LOCK) {
            user.lockAccount(String.format(
                "Tài khoản đã bị khóa do vượt quá %d lần giới hạn thiết bị. " +
                "Vui lòng liên hệ admin để mở khóa.",
                MAX_VIOLATIONS_BEFORE_LOCK
            ));
        }

        userRepository.save(user);

        // Throw exception
        if (user.isLocked()) {
            throw new Exception(user.getAccountLockedReason());
        } else {
            throw new Exception(String.format(
                "Bạn đã đạt giới hạn %d thiết bị theo gói của bạn. " +
                "Vui lòng xóa thiết bị cũ tại trang quản lý thiết bị hoặc nâng cấp gói. " +
                "Vi phạm: %d/%d lần.",
                maxDevices, user.getDeviceViolationCount(), MAX_VIOLATIONS_BEFORE_LOCK
            ));
        }
    }

    /**
     * Đăng ký device mới
     */
    private UserDevice registerNewDevice(User user, DeviceInfoDto deviceInfo,
                                        String fingerprint, String ip,
                                        HttpServletRequest request) {
        UserDevice device = new UserDevice();
        device.setDeviceId(DeviceFingerprintUtil.generateDeviceId(user.getUserId(), fingerprint));
        device.setUser(user);
        device.setDeviceFingerprint(fingerprint);
        device.setIpAddress(ip);
        device.setLastIp(ip);

        String userAgent = request.getHeader("User-Agent");
        device.setUserAgent(userAgent);
        device.setBrowserName(DeviceFingerprintUtil.parseBrowserName(userAgent));
        device.setOsName(DeviceFingerprintUtil.parseOsName(userAgent));

        if (deviceInfo != null) {
            device.setDeviceName(deviceInfo.getDeviceName() != null
                ? deviceInfo.getDeviceName()
                : device.getBrowserName() + " on " + device.getOsName());

            try {
                device.setDeviceType(UserDevice.DeviceType.valueOf(deviceInfo.getDeviceType()));
            } catch (Exception e) {
                device.setDeviceType(UserDevice.DeviceType.WEB);
            }
        } else {
            device.setDeviceName(device.getBrowserName() + " on " + device.getOsName());
            device.setDeviceType(UserDevice.DeviceType.WEB);
        }

        device.setIsActive(true);
        device.setLoginCount(1);
        device.setLastLogin(LocalDateTime.now());

        // Nếu là device đầu tiên -> Set trusted
        long existingDeviceCount = deviceRepository.countByUser_UserIdAndIsTrustedTrue(user.getUserId());
        if (existingDeviceCount == 0) {
            device.setIsTrusted(true);
        }

        return deviceRepository.save(device);
    }

    /**
     * Cập nhật device khi login thành công
     */
    private void updateDeviceOnLogin(UserDevice device, String currentIp, HttpServletRequest request) {
        device.setLastLogin(LocalDateTime.now());
        device.incrementLoginCount();

        // Kiểm tra IP có đổi không
        if (device.getLastIp() != null && !device.getLastIp().equals(currentIp)) {
            // IP đổi -> Giảm trust score
            if (DeviceFingerprintUtil.isSameSubnet(device.getLastIp(), currentIp)) {
                device.updateTrustScore(-1); // Cùng subnet: -1
            } else {
                device.updateTrustScore(-5); // Khác subnet: -5
                device.incrementSuspiciousLoginCount();
            }
        } else {
            // Cùng IP -> Tăng trust score
            device.updateTrustScore(1);
        }

        device.setLastIp(currentIp);
        deviceRepository.save(device);
    }



    /**
     * Lấy danh sách devices của user - SỬ DỤNG SERVICE
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDevice> getUserDevices(String userId) {
        return userDeviceService.getUserDevices(userId);
    }

    /**
     * Xóa device (KHÔNG cho xóa trusted device và current device)
     */
    @Override
    @Transactional
    public void removeDevice(String userId, String deviceId) throws Exception {
        UserDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new Exception("Không tìm thấy thiết bị"));

        if (!device.getUser().getUserId().equals(userId)) {
            throw new Exception("Bạn không có quyền xóa thiết bị này");
        }

        if (device.getIsTrusted()) {
            throw new Exception("Không thể xóa thiết bị tin cậy (thiết bị đăng ký đầu tiên)");
        }

        // Xóa device (soft delete)
        device.setIsActive(false);
        deviceRepository.save(device);
    }

    /**
     * Xóa device với kiểm tra current device (dành cho AdminDashboardController)
     */
    @Transactional
    public void removeDeviceWithCurrentCheck(String userId, String deviceId, String currentDeviceId) throws Exception {
        // Kiểm tra không cho xóa thiết bị hiện tại
        if (deviceId.equals(currentDeviceId)) {
            throw new Exception("Không thể xóa thiết bị đang sử dụng. Vui lòng đăng xuất trước khi xóa thiết bị này.");
        }

        // Gọi logic xóa thông thường
        removeDevice(userId, deviceId);
    }

    /**
     * Lấy giới hạn thiết bị của user (public method) - SỬ DỤNG SERVICE
     */
    @Override
    @Transactional(readOnly = true)
    public int getUserMaxDevices(String userId) {
        return userDeviceService.getMaxDevicesForUser(userId);
    }

    /**
     * Lấy thông tin subscription hiện tại
     * ADMIN: Hiển thị "VIP (Không giới hạn thiết bị)"
     */
    @Override
    @Transactional(readOnly = true)
    public String getUserSubscriptionInfo(String userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);

            // ADMIN hiển thị thông tin đặc biệt
            if (user != null && user.getRole() != null && user.getRole().getRoleName() == Role.RoleName.ADMIN) {
                return "Gói VIP (Không giới hạn thiết bị)";
            }

            Optional<Subscription> activeSubscription = subscriptionRepository
                    .findActiveSubscriptionByUserId(userId, LocalDateTime.now());

            if (activeSubscription.isPresent()) {
                Subscription sub = activeSubscription.get();
                return String.format("Gói %s (%d thiết bị)",
                    sub.getPackageName().name(),
                    sub.getMaxDevices());
            }

            return "Gói FREE (1 thiết bị)";
        } catch (Exception e) {
            return "Gói FREE (1 thiết bị)";
        }
    }
}
