package stu.datn.ebook_store.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO for creating a new subscription package (Admin only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCreateRequest {

    @NotBlank(message = "Tên gói không được để trống")
    @Size(max = 255, message = "Tên gói không được vượt quá 255 ký tự")
    private String packageName;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Số ngày không được để trống")
    @Min(value = 1, message = "Số ngày phải lớn hơn 0")
    private Integer durationDays;

    @Min(value = 1, message = "Số thiết bị tối đa phải lớn hơn 0")
    private Integer maxDevices = 1;

    private Boolean isActive = true;
}

