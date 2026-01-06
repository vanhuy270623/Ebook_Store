package stu.datn.ebook_store.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.Banner.BannerPosition;
import stu.datn.ebook_store.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, String> {
    List<Banner> findByIsActiveTrue();

    List<Banner> findByPositionAndIsActiveTrue(BannerPosition position);

    List<Banner> findByUser(User user);

    List<Banner> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Get active banners for home page with business rules:
     * - isActive = true
     * - position = HOME
     * - Optional: Check start_date and end_date
     * - Order by display_order ASC, created_at DESC
     * - Limit to max 7 results
     */
    @Query("SELECT b FROM Banner b WHERE b.isActive = true " +
           "AND b.position = :position " +
           "AND (b.startDate IS NULL OR b.startDate <= :currentDate) " +
           "AND (b.endDate IS NULL OR b.endDate >= :currentDate) " +
           "ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findActiveBannersForDisplay(@Param("position") BannerPosition position,
                                              @Param("currentDate") LocalDateTime currentDate,
                                              Pageable pageable);

    @Query("SELECT b FROM Banner b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.targetUrl) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY b.createdAt DESC")
    List<Banner> searchBanners(@Param("keyword") String keyword);
}

