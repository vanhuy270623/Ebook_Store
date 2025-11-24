package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CouponService {
    List<Coupon> getAllCoupons();
    Optional<Coupon> getCouponById(String couponId);
    Optional<Coupon> getCouponByCode(String code);
    Optional<Coupon> getValidCouponByCode(String code);
    List<Coupon> getAllActiveCoupons();
    List<Coupon> getAllExpiredCoupons();
    Coupon saveCoupon(Coupon coupon);
    void deleteCoupon(String couponId);
    List<Coupon> searchCoupons(String keyword);
    boolean isCouponValid(Coupon coupon, BigDecimal orderValue);
    BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderValue);
}

