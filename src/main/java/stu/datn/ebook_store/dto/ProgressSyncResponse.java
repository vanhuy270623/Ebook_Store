package stu.datn.ebook_store.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO cho response sau khi đồng bộ tiến độ đọc
 * Chứa thông tin về trạng thái sync và khả năng đánh giá
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressSyncResponse {

    /**
     * Trạng thái đồng bộ thành công hay không
     */
    private boolean success;

    /**
     * Có phát hiện hành vi đọc lướt (skimming) không?
     */
    @JsonProperty("isSkimming")
    private boolean isSkimming;

    /**
     * Tiến độ hiện tại sau khi sync (0.0 - 100.0)
     */
    private Float currentProgress;

    /**
     * Tổng thời gian đọc hợp lệ (giây)
     */
    private Long totalActiveTime;

    /**
     * Người dùng có thể viết review không? (progress >= 20%)
     */
    private boolean canReview;

    /**
     * Sách đã hoàn thành chưa? (progress >= 100%)
     */
    @JsonProperty("isCompleted")
    private boolean isCompleted;

    /**
     * Thông báo cho người dùng (nếu có)
     */
    private String message;

    /**
     * Tốc độ đọc tính toán (% per second) - for debugging
     */
    private Float readingVelocity;

    /**
     * ID của reading progress
     */
    private String progressId;
}

