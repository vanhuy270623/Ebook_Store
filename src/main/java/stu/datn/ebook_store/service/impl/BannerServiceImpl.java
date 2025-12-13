package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.Banner.BannerPosition;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.repository.BannerRepository;
import stu.datn.ebook_store.service.BannerService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Autowired
    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    @Override
    public Optional<Banner> getBannerById(String bannerId) {
        return bannerRepository.findById(bannerId);
    }

    @Override
    public Banner saveBanner(Banner banner) {
        if (banner.getBannerId() == null || banner.getBannerId().isEmpty()) {
            banner.setBannerId(generateBannerId());
        }
        return bannerRepository.save(banner);
    }

    @Override
    public void deleteBanner(String bannerId) {
        bannerRepository.deleteById(bannerId);
    }

    @Override
    public List<Banner> getActiveBanners() {
        return bannerRepository.findByIsActiveTrue();
    }

    @Override
    public List<Banner> getActiveBannersByPosition(BannerPosition position) {
        return bannerRepository.findByPositionAndIsActiveTrue(position);
    }

    @Override
    public List<Banner> getBannersByUser(User user) {
        return bannerRepository.findByUser(user);
    }

    @Override
    public List<Banner> getBannersByUserSortedByDate(User user) {
        return bannerRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public void activateBanner(String bannerId) {
        Optional<Banner> bannerOpt = bannerRepository.findById(bannerId);
        if (bannerOpt.isPresent()) {
            Banner banner = bannerOpt.get();
            banner.setIsActive(true);
            bannerRepository.save(banner);
        }
    }

    @Override
    public void deactivateBanner(String bannerId) {
        Optional<Banner> bannerOpt = bannerRepository.findById(bannerId);
        if (bannerOpt.isPresent()) {
            Banner banner = bannerOpt.get();
            banner.setIsActive(false);
            bannerRepository.save(banner);
        }
    }

    private String generateBannerId() {
        long count = bannerRepository.count();
        return "banner_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

