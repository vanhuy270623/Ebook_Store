package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.User;

import java.time.LocalDateTime;

/**
 * DTO for User response (Admin view)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String roleName;
    private String roleId;
    private Boolean isActive;
    private Boolean isVerified;
    private User.ReadingMode preferredReadingMode;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor from Entity
    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.avatarUrl = user.getAvatarUrl();
        this.isActive = user.getIsActive();
        this.isVerified = user.getIsVerified();
        this.preferredReadingMode = user.getPreferredReadingMode();
        this.lastLogin = user.getLastLogin();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();

        if (user.getRole() != null) {
            this.roleId = user.getRole().getRoleId();
            this.roleName = user.getRole().getRoleName().name(); // Convert enum to String
        }
    }
}

