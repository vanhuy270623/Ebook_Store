package stu.datn.ebook_store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stu.datn.ebook_store.entity.BookAsset;

/**
 * DTO đơn giản để truyền thông tin BookAsset sang JavaScript
 * Tránh circular reference và chỉ chứa thông tin cần thiết
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookAssetDTO {
    private String fileType;  // "PDF" hoặc "EPUB"
    private Long fileSize;    // Size in bytes
    private String fileUrl;   // URL của file (optional, for debugging)

    /**
     * Convert từ BookAsset entity sang DTO
     */
    public static BookAssetDTO fromEntity(BookAsset asset) {
        if (asset == null) {
            return null;
        }
        return new BookAssetDTO(
                asset.getFileType() != null ? asset.getFileType().name() : null,
                asset.getFileSize(),
                asset.getFileUrl()
        );
    }
}

