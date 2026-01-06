package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.controller.BaseController;
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
 * AdminDashboardController xử lý quản lý banner/quảng cáo (CRUD Banner)
 */
@Controller
@RequestMapping("/admin/banners")
public class BannerController extends BaseController {

    private static final String REDIRECT_BANNERS = "redirect:/admin/banners";

    private final BannerService bannerService;
    private final FileStorageService fileStorageService;

    @Autowired
    public BannerController(BannerService bannerService, FileStorageService fileStorageService) {
        this.bannerService = bannerService;
        this.fileStorageService = fileStorageService;
    }

    // ============================= HELPER METHODS =============================

    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    private String generateNextBannerId() {
        List<Banner> allBanners = bannerService.getAllBanners();
        int nextNumber = allBanners.size() + 1;
        return String.format("banner_%02d", nextNumber);
    }

    private BannerUpdateRequest mapToUpdateRequest(Banner banner) {
        BannerUpdateRequest dto = new BannerUpdateRequest();
        dto.setBannerId(banner.getBannerId());
        dto.setTitle(banner.getTitle());
        dto.setImageUrl(banner.getImageUrl());
        dto.setTargetUrl(banner.getTargetUrl());
        dto.setPosition(banner.getPosition() != null ? banner.getPosition().name() : "HOME");
        dto.setDisplayOrder(banner.getDisplayOrder() != null ? banner.getDisplayOrder() : 0);
        dto.setIsActive(banner.getIsActive());
        dto.setStartDate(banner.getStartDate());
        dto.setEndDate(banner.getEndDate());
        return dto;
    }

    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("positions", Banner.BannerPosition.values());
    }

    // ============================= CRUD OPERATIONS =============================

    @GetMapping
    public String bannersList(@RequestParam(value = "search", required = false) String search,
                             Authentication authentication,
                             Model model) {
        User currentUser = getCurrentUser(authentication);
        List<Banner> banners;

        // Tìm kiếm nếu có từ khóa
        if (search != null && !search.trim().isEmpty()) {
            banners = bannerService.searchBanners(search, currentUser);
            model.addAttribute("search", search);
        } else {
            banners = bannerService.getBannersByUserSortedByDate(currentUser);
        }

        model.addAttribute("banners", banners);
        model.addAttribute("totalBanners", banners.size());

        long activeBanners = banners.stream()
                .filter(Banner::getIsActive)
                .count();
        model.addAttribute("activeBanners", activeBanners);

        return "admin/banners/list";
    }

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

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("bannerRequest", new BannerCreateRequest());
        addCommonFormAttributes(model, false);
        return "admin/banners/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Banner banner = bannerService.getBannerById(id).orElse(null);

        if (banner == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
            return REDIRECT_BANNERS;
        }

        BannerUpdateRequest bannerUpdateRequest = mapToUpdateRequest(banner);
        model.addAttribute("bannerRequest", bannerUpdateRequest);
        model.addAttribute("banner", banner);
        addCommonFormAttributes(model, true);

        return "admin/banners/form";
    }

    @PostMapping("/create")
    public String createBanner(@Valid @ModelAttribute("bannerRequest") BannerCreateRequest request,
                               BindingResult bindingResult,
                               @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            addCommonFormAttributes(model, false);
            return "admin/banners/form";
        }

        try {
            User currentUser = getCurrentUser(authentication);

            // 1. Tạo ID trước để dùng làm tên file
            String newBannerId = generateNextBannerId();

            // 2. Upload banner image (Sử dụng hàm storeEntityImage)
            if (bannerImage != null && !bannerImage.isEmpty()) {
                // Lưu vào: book_asset/image/banners/{newBannerId}.jpg
                String relativePath = fileStorageService.storeEntityImage(bannerImage, "banners", newBannerId);
                // Thêm "/" để thành đường dẫn web hợp lệ
                request.setImageUrl("/" + relativePath);
            }

            Banner newBanner = new Banner();
            newBanner.setBannerId(newBannerId);
            newBanner.setUser(currentUser);
            newBanner.setTitle(request.getTitle());
            newBanner.setImageUrl(request.getImageUrl());
            newBanner.setTargetUrl(request.getTargetUrl());
            newBanner.setPosition(Banner.BannerPosition.valueOf(request.getPosition()));
            newBanner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
            newBanner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            newBanner.setStartDate(request.getStartDate());
            newBanner.setEndDate(request.getEndDate());
            newBanner.setCreatedAt(LocalDateTime.now());

            bannerService.saveBanner(newBanner);
            redirectAttributes.addFlashAttribute("success",
                    "Thêm banner thành công! ID: " + newBannerId);

            return REDIRECT_BANNERS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/banners/add";
        }
    }

    @PostMapping("/update")
    public String updateBanner(@Valid @ModelAttribute("bannerRequest") BannerUpdateRequest request,
                               BindingResult bindingResult,
                               @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            model.addAttribute("error", errors);
            Banner banner = bannerService.getBannerById(request.getBannerId()).orElse(null);
            model.addAttribute("banner", banner);
            addCommonFormAttributes(model, true);
            return "admin/banners/form";
        }

        try {
            Banner existingBanner = bannerService.getBannerById(request.getBannerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner"));

            // Upload banner image nếu có (Ghi đè file cũ bằng ID hiện tại)
            if (bannerImage != null && !bannerImage.isEmpty()) {
                String relativePath = fileStorageService.storeEntityImage(bannerImage, "banners", existingBanner.getBannerId());
                existingBanner.setImageUrl("/" + relativePath);
            }

            // Cập nhật thông tin
            existingBanner.setTitle(request.getTitle());
            // Nếu không upload mới nhưng form submit URL (hidden field) thì giữ nguyên, đã xử lý ở trên

            existingBanner.setTargetUrl(request.getTargetUrl());
            existingBanner.setPosition(Banner.BannerPosition.valueOf(request.getPosition()));
            existingBanner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
            existingBanner.setIsActive(request.getIsActive());
            existingBanner.setStartDate(request.getStartDate());
            existingBanner.setEndDate(request.getEndDate());

            bannerService.saveBanner(existingBanner);
            redirectAttributes.addFlashAttribute("success", "Cập nhật banner thành công!");

            return REDIRECT_BANNERS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/banners/edit/" + request.getBannerId();
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBanner(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Có thể thêm logic xóa file vật lý ở đây nếu cần
            // Banner banner = bannerService.getBannerById(id).orElse(null);
            // if (banner != null && banner.getImageUrl() != null) fileStorageService.deleteFile(banner.getImageUrl());

            bannerService.deleteBanner(id);
            response.put("success", true);
            response.put("message", "Xóa banner thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload banner image (Ajax/Quick upload)
     */
    @PostMapping("/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Vì upload nhanh chưa có ID entity, dùng timestamp làm ID tạm
            String tempId = "temp_" + System.currentTimeMillis();
            String relativePath = fileStorageService.storeEntityImage(file, "banners", tempId);

            response.put("success", true);
            response.put("url", "/" + relativePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleBannerStatus(@PathVariable String id, RedirectAttributes redirectAttributes) {
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

    @GetMapping("/statistics")
    public String bannersStatistics(Model model) {
        List<Banner> allBanners = bannerService.getAllBanners();
        model.addAttribute("totalBanners", allBanners.size());

        long activeBanners = allBanners.stream().filter(Banner::getIsActive).count();
        model.addAttribute("activeBanners", activeBanners);

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