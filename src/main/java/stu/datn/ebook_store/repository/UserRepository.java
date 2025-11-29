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

    // Admin statistics methods
    long countByIsActive(Boolean isActive);

    long countByIsVerified(Boolean isVerified);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findTopUsersOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    // Enhanced search query
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.userId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    // Fetch user with role explicitly
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.userId = :userId")
    Optional<User> findByIdWithRole(@Param("userId") String userId);

    // Count users by role
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName")
    long countByRoleName(@Param("roleName") stu.datn.ebook_store.entity.Role.RoleName roleName);

}
