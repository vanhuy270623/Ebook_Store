package stu.datn.ebook_store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bookassets")
public class BookAsset {
    @Id
    @Column(name = "book_asset_id", length = 50)
    private String bookAssetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Get the viewer page URL for this asset.
     * Returns URL to the viewer page: /reading/pdf/{bookId} or /reading/epub/{bookId}
     *
     * This is the main URL users click to open and read books.
     * Example: /reading/pdf/book_02 or /reading/epub/book_03
     */
    public String getViewerUrl() {
        if (this.book == null || this.book.getBookId() == null) {
            System.err.println("⚠️ getViewerUrl() - book or bookId is null!");
            return null;
        }

        String bookId = this.book.getBookId();

        if (this.fileType == FileType.PDF) {
            return "/reading/pdf/" + bookId;
        } else if (this.fileType == FileType.EPUB) {
            return "/reading/epub/" + bookId;
        }

        return null;
    }

    /**
     * Get the secure reading URL for this asset.
     * Returns URL to the secure streaming endpoint: /reading/stream/{bookId}
     *
     * This endpoint validates user access before serving the file.
     * Used internally by the viewer pages to stream file content.
     * Example: /reading/stream/book_02
     */
    public String getReadingUrl() {
        if (this.book == null || this.book.getBookId() == null) {
            System.err.println("⚠️ getReadingUrl() - book or bookId is null!");
            return null;
        }

        String readingUrl = "/reading/stream/" + this.book.getBookId();

        System.out.println("✅ getReadingUrl() - Generated: " + readingUrl);
        System.out.println("   for bookId: " + this.book.getBookId());

        return readingUrl;
    }

    public enum FileType {
        PDF, EPUB
    }
}
