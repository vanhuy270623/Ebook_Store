package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for Review approval/rejection (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewApprovalRequest {

    @NotBlank(message = "ID đánh giá không được để trống")
    private String reviewId;

    private Boolean isApproved;

    private String adminNote; // Ghi chú từ admin (nếu reject)
}

