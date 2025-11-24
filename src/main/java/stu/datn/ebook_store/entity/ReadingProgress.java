package stu.datn.ebook_store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reading_progress", uniqueConstraints = {
    @UniqueConstraint(name = "user_book_unique", columnNames = {"user_id", "book_id"})
})
public class ReadingProgress {
    @Id
    @Column(name = "progress_id", length = 50)
    private String progressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_asset_id")
    private BookAsset bookAsset;

    @Column(name = "last_read_location", length = 500)
    private String lastReadLocation;

    @Column(name = "progress_percentage")
    private Float progressPercentage = 0.0f;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private AccessType accessType;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastReadAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastReadAt = LocalDateTime.now();
    }

    public enum AccessType {
        PURCHASED, SUBSCRIPTION, FREE
    }
}

