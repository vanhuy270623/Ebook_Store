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
     * Get the reading URL for this asset in the format:
     * /reading/pdf/{category}/{fileName} or /reading/epub/{category}/{fileName}
     *
     * Example: /reading/pdf/tamly-kynangsong/Cac_The_Gioi_Song_Song_-_Michio_Kaku.pdf
     */
    public String getReadingUrl() {
        if (this.fileUrl == null || this.fileUrl.isEmpty()) {
            return null;
        }

        // fileUrl format: /book_asset/source/category/fileName
        // Extract category and fileName from fileUrl
        String prefix = "/book_asset/source/";
        if (!this.fileUrl.startsWith(prefix)) {
            return null;
        }

        String pathAfterPrefix = this.fileUrl.substring(prefix.length());

        // Determine reader type based on file type
        String readerType = this.fileType == FileType.PDF ? "pdf" : "epub";

        return "/reading/" + readerType + "/" + pathAfterPrefix;
    }

    public enum FileType {
        PDF, EPUB
    }
}

