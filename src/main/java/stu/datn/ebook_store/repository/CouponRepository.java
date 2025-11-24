package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Coupon;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.endDate >= :currentDate")
    Optional<Coupon> findValidCouponByCode(@Param("code") String code, @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT c FROM Coupon c WHERE c.endDate >= :currentDate")
    List<Coupon> findAllActiveCoupons(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT c FROM Coupon c WHERE c.endDate < :currentDate")
    List<Coupon> findAllExpiredCoupons(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT c FROM Coupon c WHERE c.code LIKE %:keyword% OR c.couponId LIKE %:keyword%")
    List<Coupon> searchCoupons(@Param("keyword") String keyword);
}

