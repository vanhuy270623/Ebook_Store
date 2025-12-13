package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Coupon response (Admin view)
 * Mapped from table: coupons
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private String couponId;
    private String code;
    private Coupon.DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private LocalDateTime endDate;
    private Integer usageLimit;

    // Constructor from Entity
    public CouponResponse(Coupon coupon) {
        this.couponId = coupon.getCouponId();
        this.code = coupon.getCode();
        this.discountType = coupon.getDiscountType();
        this.discountValue = coupon.getDiscountValue();
        this.minOrderValue = coupon.getMinOrderValue();
        this.endDate = coupon.getEndDate();
        this.usageLimit = coupon.getUsageLimit();
    }
}

