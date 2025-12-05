package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String userName);

    Optional<User> findByEmail(String email);

    // Find active (non-deleted) users only
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.userId = :userId")
    Optional<User> findActiveById(@Param("userId") String userId);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.username = :username")
    Optional<User> findActiveByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.email = :email")
    Optional<User> findActiveByEmail(@Param("email") String email);

    // Admin statistics methods - only count active users
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.isActive = :isActive")
    long countActiveByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.isVerified = :isVerified")
    long countActiveByIsVerified(@Param("isVerified") Boolean isVerified);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    long countActive();

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    List<User> findTopActiveUsersOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    // Enhanced search query - only search active users
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (" +
           "LOWER(u.userId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchActiveByKeyword(@Param("keyword") String keyword);

    // Fetch active user with role explicitly
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.deletedAt IS NULL AND u.userId = :userId")
    Optional<User> findActiveByIdWithRole(@Param("userId") String userId);

    // Count active users by role
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.role.roleName = :roleName")
    long countActiveByRoleName(@Param("roleName") stu.datn.ebook_store.entity.Role.RoleName roleName);

    // Admin methods to include deleted users for management
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllIncludingDeleted();

    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByIdIncludingDeleted(@Param("userId") String userId);

    // Method to find only deleted users
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL ORDER BY u.deletedAt DESC")
    List<User> findDeletedUsers();

}
