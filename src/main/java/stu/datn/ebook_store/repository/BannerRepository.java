package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.Banner.BannerPosition;
import stu.datn.ebook_store.entity.User;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, String> {
    List<Banner> findByIsActiveTrue();

    List<Banner> findByPositionAndIsActiveTrue(BannerPosition position);

    List<Banner> findByUser(User user);

    List<Banner> findByUserOrderByCreatedAtDesc(User user);
}

