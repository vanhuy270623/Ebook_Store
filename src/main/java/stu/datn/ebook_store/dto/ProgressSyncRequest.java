package stu.datn.ebook_store.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO cho request đồng bộ tiến độ đọc từ Frontend
 * Hỗ trợ cả PDF (Page Number) và EPUB (CFI)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressSyncRequest {

    /**
     * ID của sách đang đọc
     */
    private String bookId;

    /**
     * ID của book asset (PDF hoặc EPUB)
     */
    private String bookAssetId;

    /**
     * Vị trí đọc thô (format-specific)
     * - PDF: "page-5" hoặc "5"
     * - EPUB: "epubcfi(/6/4[chap01ref]!/4[body01]/10[para05]/2/1:3)"
     */
    private String currentLocationRaw;

    /**
     * Tiến độ đã được chuẩn hóa bởi Frontend (0.0 - 100.0)
     * - PDF: (current_page / total_pages) * 100
     * - EPUB: book.locations.percentageFromCfi(current_cfi) * 100
     */
    private Float progressPercentage;

    /**
     * Số giây tương tác thực tế kể từ lần sync cuối
     * Frontend track thời gian user thực sự đang đọc (tab active, scroll, click...)
     */
    private Integer activeTimeDelta;

    /**
     * Format của sách (PDF hoặc EPUB) - optional, có thể lấy từ bookAssetId
     */
    private String format;
}

