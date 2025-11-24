package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.Banner.BannerPosition;
import stu.datn.ebook_store.entity.User;

import java.util.List;
import java.util.Optional;

public interface BannerService {
    List<Banner> getAllBanners();
    Optional<Banner> getBannerById(String bannerId);
    Banner saveBanner(Banner banner);
    void deleteBanner(String bannerId);
    List<Banner> getActiveBanners();
    List<Banner> getActiveBannersByPosition(BannerPosition position);
    List<Banner> getBannersByUser(User user);
    List<Banner> getBannersByUserSortedByDate(User user);
    void activateBanner(String bannerId);
    void deactivateBanner(String bannerId);
}

