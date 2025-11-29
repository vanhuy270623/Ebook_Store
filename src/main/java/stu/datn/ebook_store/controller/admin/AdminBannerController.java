package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.BannerCreateRequest;
import stu.datn.ebook_store.dto.request.BannerUpdateRequest;
import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.BannerService;
import stu.datn.ebook_store.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý quản lý banner/quảng cáo (CRUD Banner)
 * Pattern: Tương tự AdminAuthorController, AdminCategoryController, AdminCouponController
 */
@Controller
@RequestMapping("/admin/banners")
public class AdminBannerController {

    private static final String REDIRECT_BANNERS = "redirect:/admin/banners";

    private final BannerService bannerService;
    private final FileStorageService fileStorageService;

    @Autowired
    public AdminBannerController(BannerService bannerService, FileStorageService fileStorageService) {
        this.bannerService = bannerService;
        this.fileStorageService = fileStorageService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Lấy thông tin user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Sinh Banner ID tự động theo format: "banner_XX"
     */
    private String generateNextBannerId() {
        List<Banner> allBanners = bannerService.getAllBanners();
        int nextNumber = allBanners.size() + 1;
        return String.format("banner_%02d", nextNumber);
    }

    /**
     * Map Banner entity sang BannerUpdateRequest DTO
     */
    private BannerUpdateRequest mapToUpdateRequest(Banner banner) {
        BannerUpdateRequest dto = new BannerUpdateRequest();
        dto.setBannerId(banner.getBannerId());
        dto.setTitle(banner.getTitle());
        dto.setImageUrl(banner.getImageUrl());
        dto.setTargetUrl(banner.getTargetUrl());
        dto.setPosition(banner.getPosition() != null ? banner.getPosition().name() : "HOME");
        dto.setIsActive(banner.getIsActive());
        return dto;
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("positions", Banner.BannerPosition.values());
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách banner
     */
    @GetMapping
    public String bannersList(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        List<Banner> banners = bannerService.getBannersByUserSortedByDate(currentUser);

        model.addAttribute("banners", banners);
        model.addAttribute("totalBanners", banners.size());

        // Thống kê nhanh
        long activeBanners = banners.stream()
                .filter(Banner::getIsActive)
                .count();
        model.addAttribute("activeBanners", activeBanners);

        return "admin/banners/list";
    }

    /**
     * Xem chi tiết banner
     */
    @GetMapping("/view/{id}")
    public String viewBanner(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Banner banner = bannerService.getBannerById(id).orElse(null);

        if (banner == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner với ID: " + id);
            return REDIRECT_BANNERS;
        }

        model.addAttribute("banner", banner);
        return "admin/banners/view";
    }

    /**
     * Hiển thị form thêm banner mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("bannerRequest", new BannerCreateRequest());
        addCommonFormAttributes(model, false);
        return "admin/banners/form";
    }

    /**
     * Hiển thị form chỉnh sửa banner
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Banner banner = bannerService.getBannerById(id).orElse(null);

        if (banner == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
            return REDIRECT_BANNERS;
        }

        // Chuyển đổi Banner entity sang DTO
        BannerUpdateRequest bannerUpdateRequest = mapToUpdateRequest(banner);

        model.addAttribute("bannerRequest", bannerUpdateRequest);
        model.addAttribute("banner", banner);
        addCommonFormAttributes(model, true);

        return "admin/banners/form";
    }

    /**
     * Tạo banner mới
     */
    @PostMapping("/create")
    public String createBanner(@Valid @ModelAttribute("bannerRequest") BannerCreateRequest request,
                              BindingResult bindingResult,
                              @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            addCommonFormAttributes(model, false);
            return "admin/banners/form";
        }

        try {
            User currentUser = getCurrentUser(authentication);

            // Upload banner image nếu có
            if (bannerImage != null && !bannerImage.isEmpty()) {
                String imageUrl = fileStorageService.storeBannerImage(bannerImage);
                request.setImageUrl(imageUrl);
            }

            // Tạo Banner entity
            String newBannerId = generateNextBannerId();
            Banner newBanner = new Banner();
            newBanner.setBannerId(newBannerId);
            newBanner.setUser(currentUser);
            newBanner.setTitle(request.getTitle());
            newBanner.setImageUrl(request.getImageUrl());
            newBanner.setTargetUrl(request.getTargetUrl());
            newBanner.setPosition(Banner.BannerPosition.valueOf(request.getPosition()));
            newBanner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            newBanner.setCreatedAt(LocalDateTime.now());

            bannerService.saveBanner(newBanner);
            redirectAttributes.addFlashAttribute("success",
                    "Thêm banner thành công! ID: " + newBannerId + ", Tiêu đề: " + request.getTitle());

            return REDIRECT_BANNERS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/banners/add";
        }
    }

    /**
     * Cập nhật banner
     */
    @PostMapping("/update")
    public String updateBanner(@Valid @ModelAttribute("bannerRequest") BannerUpdateRequest request,
                              BindingResult bindingResult,
                              @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);

            Banner banner = bannerService.getBannerById(request.getBannerId()).orElse(null);
            model.addAttribute("banner", banner);
            addCommonFormAttributes(model, true);

            return "admin/banners/form";
        }

        Banner existingBanner = bannerService.getBannerById(request.getBannerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy banner"));

        try {
            // Upload banner image nếu có
            if (bannerImage != null && !bannerImage.isEmpty()) {
                String imageUrl = fileStorageService.storeBannerImage(bannerImage);
                request.setImageUrl(imageUrl);
            }

            // Cập nhật thông tin
            existingBanner.setTitle(request.getTitle());
            if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
                existingBanner.setImageUrl(request.getImageUrl());
            }
            existingBanner.setTargetUrl(request.getTargetUrl());
            existingBanner.setPosition(Banner.BannerPosition.valueOf(request.getPosition()));
            existingBanner.setIsActive(request.getIsActive());

            bannerService.saveBanner(existingBanner);
            redirectAttributes.addFlashAttribute("success", "Cập nhật banner thành công!");

            return REDIRECT_BANNERS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/banners/edit/" + request.getBannerId();
        }
    }

    /**
     * Xóa banner
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBanner(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            bannerService.deleteBanner(id);
            response.put("success", true);
            response.put("message", "Xóa banner thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa banner";
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload banner image
     */
    @PostMapping("/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String imageUrl = fileStorageService.storeBannerImage(file);
            response.put("success", true);
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kích hoạt/vô hiệu hóa banner
     */
    @PostMapping("/toggle-status/{id}")
    public String toggleBannerStatus(@PathVariable String id,
                                     RedirectAttributes redirectAttributes) {
        Banner banner = bannerService.getBannerById(id).orElse(null);
        if (banner == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
            return REDIRECT_BANNERS;
        }

        if (banner.getIsActive()) {
            bannerService.deactivateBanner(id);
        } else {
            bannerService.activateBanner(id);
        }

        redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái banner thành công!");
        return REDIRECT_BANNERS;
    }

    /**
     * Hiển thị thống kê banner
     */
    @GetMapping("/statistics")
    public String bannersStatistics(Model model) {
        List<Banner> allBanners = bannerService.getAllBanners();

        model.addAttribute("totalBanners", allBanners.size());

        long activeBanners = allBanners.stream()
                .filter(Banner::getIsActive)
                .count();
        model.addAttribute("activeBanners", activeBanners);

        // Thống kê theo vị trí
        Map<Banner.BannerPosition, Long> bannersByPosition = allBanners.stream()
                .collect(Collectors.groupingBy(Banner::getPosition, Collectors.counting()));
        model.addAttribute("bannersByPosition", bannersByPosition);

        model.addAttribute("banners", allBanners.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList()));

        return "admin/banners/statistics";
    }
}

