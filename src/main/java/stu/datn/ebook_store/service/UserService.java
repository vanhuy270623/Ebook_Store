package stu.datn.ebook_store.service;

import jakarta.servlet.http.HttpServletRequest;
import stu.datn.ebook_store.dto.DeviceInfoDto;
import stu.datn.ebook_store.dto.RegisterDto;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.UserDevice;

import java.util.List;
import java.util.Map;


public interface UserService {
    void registerUser(RegisterDto registerDto) throws Exception;
    boolean checkUsernameExists(String username);
    boolean checkEmailExists(String email);
    User authenticateUser(String username, String password) throws Exception;
    void updateLastLogin(String userId);

    // Admin methods
    long getTotalUsersCount();
    long getActiveUsersCount();
    long getVerifiedUsersCount();
    long getAdminUsersCount();
    long getAllAdminsCountIncludingDeleted(); // Count all admins including deleted ones
    java.util.List<User> getRecentUsers(int limit);
    java.util.List<User> getAllUsers();
    java.util.Optional<User> getUserById(String userId);
    java.util.Optional<User> getUserByIdWithRole(String userId);
    User saveUser(User user);
    void deleteUser(String userId);
    void toggleUserStatus(String userId);
    java.util.List<User> searchUsers(String keyword);

    // Soft deletion methods
    void softDeleteUser(String userId);
    void restoreUser(String userId);
    java.util.List<User> getDeletedUsers();
    java.util.List<User> getAllUsersIncludingDeleted();

    // Device Management methods - Tích hợp vào UserService
    /**
     * Xác thực và xử lý device khi login
     * @return Map với key "status" (SUCCESS/DEVICE_LIMIT_EXCEEDED/ACCOUNT_LOCKED) và "device"
     */
    Map<String, Object> authenticateWithDeviceCheck(String username, String password,
                                                     DeviceInfoDto deviceInfo,
                                                     HttpServletRequest request) throws Exception;

    /**
     * Lấy danh sách devices của user
     */
    List<UserDevice> getUserDevices(String userId);

    /**
     * Xóa device (không cho xóa trusted device)
     */
    void removeDevice(String userId, String deviceId) throws Exception;

    /**
     * Xóa device với kiểm tra current device (không cho xóa thiết bị đang dùng)
     */
    void removeDeviceWithCurrentCheck(String userId, String deviceId, String currentDeviceId) throws Exception;

    /**
     * Đếm số violations chưa xử lý
     */
    long getUnresolvedViolationsCount(String userId);

    /**
     * Lấy giới hạn thiết bị của user theo subscription
     */
    int getUserMaxDevices(String userId);

    /**
     * Lấy thông tin subscription hiện tại của user
     */
    String getUserSubscriptionInfo(String userId);
}
