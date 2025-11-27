package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Post;

import java.time.LocalDateTime;

/**
 * DTO for Post/Blog response (Admin view)
 * Mapped from table: post
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private String postId;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String thumbnailUrl;
    private String authorId;
    private String authorName;
    private String categoryId;       // Post category (not book category)
    private String categoryName;     // Category name
    private Boolean isPublished;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor from Entity
    public PostResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.slug = post.getSlug();
        this.excerpt = post.getExcerpt();
        this.content = post.getContent();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.isPublished = post.getIsPublished();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();

        if (post.getUser() != null) {
            this.authorId = post.getUser().getUserId();
            this.authorName = post.getUser().getFullName() != null ?
                post.getUser().getFullName() : post.getUser().getUsername();
        }

        if (post.getCategory() != null) {
            this.categoryId = post.getCategory().getCategoryId();
            this.categoryName = post.getCategory().getCategoryName();
        }
    }
}

