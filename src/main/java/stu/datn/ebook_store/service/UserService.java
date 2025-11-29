package stu.datn.ebook_store.service;

import stu.datn.ebook_store.dto.RegisterDto;
import stu.datn.ebook_store.entity.User;


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
    java.util.List<User> getRecentUsers(int limit);
    java.util.List<User> getAllUsers();
    java.util.Optional<User> getUserById(String userId);
    java.util.Optional<User> getUserByIdWithRole(String userId);
    User saveUser(User user);
    void deleteUser(String userId);
    void toggleUserStatus(String userId);
    java.util.List<User> searchUsers(String keyword);
}
