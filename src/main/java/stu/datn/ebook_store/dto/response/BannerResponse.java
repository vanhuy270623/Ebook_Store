package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Banner;

import java.time.LocalDateTime;

/**
 * DTO for Banner response (Admin view)
 * Mapped from entity: Banner
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private String bannerId;
    private String userId;
    private String username;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private String position; // HOME, CATEGORY, DETAIL
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Constructor from Entity
    public BannerResponse(Banner banner) {
        this.bannerId = banner.getBannerId();
        this.title = banner.getTitle();
        this.imageUrl = banner.getImageUrl();
        this.targetUrl = banner.getTargetUrl();
        this.position = banner.getPosition() != null ? banner.getPosition().name() : null;
        this.isActive = banner.getIsActive();
        this.createdAt = banner.getCreatedAt();

        if (banner.getUser() != null) {
            this.userId = banner.getUser().getUserId();
            this.username = banner.getUser().getUsername();
        }
    }
}

