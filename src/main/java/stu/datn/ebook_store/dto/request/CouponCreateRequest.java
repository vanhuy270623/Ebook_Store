package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for creating a new coupon (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequest {

    @NotBlank(message = "Mã coupon không được để trống")
    @Size(max = 50, message = "Mã coupon không được vượt quá 50 ký tự")
    private String code;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private Coupon.DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0.0", message = "Giá trị giảm tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxDiscountAmount;

    @Min(value = 0, message = "Số lượng sử dụng phải lớn hơn hoặc bằng 0")
    private Integer usageLimit;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime validTo;

    private Boolean isActive = true;
}

