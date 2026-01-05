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
     * Get the streaming URL for this asset.
     * Returns URL to the secure streaming endpoint: /reading/stream/{category}/{fileName}
     *
     * This endpoint validates user access before serving the file.
     * Used internally by the viewer pages to stream file content.
     * Example: /reading/stream/tamly-kynangsong/Cac_The_Gioi_Song_Song_-_Michio_Kaku.pdf
     */
    public String getReadingUrl() {
        if (this.fileUrl == null || this.fileUrl.isEmpty()) {
            System.err.println("⚠️ getReadingUrl() - fileUrl is null or empty!");
            return null;
        }

        // Normalize fileUrl - ensure it starts with /
        String normalizedFileUrl = this.fileUrl;
        if (!normalizedFileUrl.startsWith("/")) {
            System.out.println("ℹ️ getReadingUrl() - Adding leading slash to fileUrl");
            normalizedFileUrl = "/" + normalizedFileUrl;
        }

        // fileUrl format: /book_asset/source/category/fileName
        // Extract category and fileName from fileUrl
        String prefix = "/book_asset/source/";
        if (!normalizedFileUrl.startsWith(prefix)) {
            System.err.println("⚠️ getReadingUrl() - fileUrl doesn't start with prefix!");
            System.err.println("   normalized fileUrl: " + normalizedFileUrl);
            System.err.println("   expected prefix: " + prefix);
            return null;
        }

        String pathAfterPrefix = normalizedFileUrl.substring(prefix.length());
        String readingUrl = "/reading/stream/" + pathAfterPrefix;

        System.out.println("✅ getReadingUrl() - Generated: " + readingUrl);
        System.out.println("   from fileUrl: " + this.fileUrl);

        return readingUrl;
    }

    public enum FileType {
        PDF, EPUB
    }
}

