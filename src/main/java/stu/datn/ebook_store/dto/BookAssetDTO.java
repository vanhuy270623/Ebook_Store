package stu.datn.ebook_store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stu.datn.ebook_store.entity.BookAsset;

/**
 * DTO for BookAsset to be serialized to JavaScript
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookAssetDTO {
    private String assetId;
    private String fileType;
    private String fileUrl;
    private String readingUrl;
    private Long fileSize;

    /**
     * Create DTO from BookAsset entity
     */
    public static BookAssetDTO fromEntity(BookAsset asset) {
        if (asset == null) {
            return null;
        }

        BookAssetDTO dto = new BookAssetDTO();
        dto.setAssetId(asset.getBookAssetId());
        dto.setFileType(asset.getFileType() != null ? asset.getFileType().name() : null);
        dto.setFileUrl(asset.getFileUrl());
        dto.setReadingUrl(asset.getReadingUrl());
        dto.setFileSize(asset.getFileSize());

        return dto;
    }
}

