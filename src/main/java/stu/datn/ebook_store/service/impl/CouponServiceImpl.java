package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Coupon;
import stu.datn.ebook_store.repository.CouponRepository;
import stu.datn.ebook_store.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Autowired
    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public Optional<Coupon> getCouponById(String couponId) {
        return couponRepository.findById(couponId);
    }

    @Override
    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(code);
    }

    @Override
    public Optional<Coupon> getValidCouponByCode(String code) {
        return couponRepository.findValidCouponByCode(code, LocalDateTime.now());
    }

    @Override
    public List<Coupon> getAllActiveCoupons() {
        return couponRepository.findAllActiveCoupons(LocalDateTime.now());
    }

    @Override
    public List<Coupon> getAllExpiredCoupons() {
        return couponRepository.findAllExpiredCoupons(LocalDateTime.now());
    }

    @Override
    public Coupon saveCoupon(Coupon coupon) {
        if (coupon.getCouponId() == null || coupon.getCouponId().isEmpty()) {
            coupon.setCouponId(generateCouponId());
        }
        if (coupon.getMinOrderValue() == null) {
            coupon.setMinOrderValue(BigDecimal.ZERO);
        }
        if (coupon.getUsageLimit() == null) {
            coupon.setUsageLimit(0);
        }
        return couponRepository.save(coupon);
    }

    @Override
    public void deleteCoupon(String couponId) {
        couponRepository.deleteById(couponId);
    }

    @Override
    public List<Coupon> searchCoupons(String keyword) {
        return couponRepository.searchCoupons(keyword);
    }

    @Override
    public boolean isCouponValid(Coupon coupon, BigDecimal orderValue) {
        if (coupon == null) {
            return false;
        }

        // Check if coupon is expired
        if (coupon.getEndDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Check if order value meets minimum requirement
        if (coupon.getMinOrderValue() != null &&
            orderValue.compareTo(coupon.getMinOrderValue()) < 0) {
            return false;
        }

        return true;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderValue) {
        if (!isCouponValid(coupon, orderValue)) {
            return BigDecimal.ZERO;
        }

        switch (coupon.getDiscountType()) {
            case PERCENT:
                // Calculate percentage discount
                return orderValue
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            case FIXED:
                // Return fixed discount amount (but not more than order value)
                return coupon.getDiscountValue().min(orderValue);

            default:
                return BigDecimal.ZERO;
        }
    }

    private String generateCouponId() {
        long count = couponRepository.count();
        return "cpn_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

