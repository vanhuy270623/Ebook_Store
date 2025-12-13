package stu.datn.ebook_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
public class Book {

    public enum AccessType {
        FREE, PURCHASE, SUBSCRIPTION, BOTH
    }

    @Id
    @Column(name = "book_id", length = 50)
    private String bookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_category_id")
    private BookCategory bookCategory;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "publisher", length = 255)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "language", length = 10)
    private String language = "vi";

    @Column(name = "pages")
    private Integer pages;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private AccessType accessType = AccessType.PURCHASE;

    @Column(name = "is_downloadable")
    private Boolean isDownloadable = false;

    @Column(name = "average_rating")
    private Float averageRating = 0.0f;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookAsset> bookAssets = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public String getFirstAuthorName() {
        if (authors != null && !authors.isEmpty()) {
            return authors.iterator().next().getName();
        }
        return "Unknown Author";
    }

    public String getFormattedPrice() {
        if (accessType == AccessType.FREE) {
            return "Miễn phí";
        }
        return String.format("%,.0fđ", price);
    }

    public boolean isFree() {
        return accessType == AccessType.FREE;
    }

    public boolean isPurchasable() {
        return accessType == AccessType.PURCHASE || accessType == AccessType.BOTH;
    }

    public boolean requiresSubscription() {
        return accessType == AccessType.SUBSCRIPTION || accessType == AccessType.BOTH;
    }
}

