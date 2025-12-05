package stu.datn.ebook_store.controller.admin;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.request.CouponCreateRequest;
import stu.datn.ebook_store.dto.request.CouponUpdateRequest;
import stu.datn.ebook_store.entity.Coupon;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller quản lý mã giảm giá/coupon (Admin)
 * Endpoints: /admin/coupons/*
 * Pattern: Tương tự AdminBannerController, AdminCategoryController
 */
@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    private static final String REDIRECT_COUPONS = "redirect:/admin/coupons";

    private final CouponService couponService;

    @Autowired
    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Lấy thông tin user hiện tại từ Authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Sinh Coupon ID tự động theo format: "coupon_XX"
     */
    private String generateNextCouponId() {
        List<Coupon> allCoupons = couponService.getAllCoupons();
        int nextNumber = allCoupons.size() + 1;
        return String.format("coupon_%03d", nextNumber);
    }

    /**
     * Map Coupon entity sang CouponUpdateRequest DTO
     */
    private CouponUpdateRequest mapToUpdateRequest(Coupon coupon) {
        CouponUpdateRequest dto = new CouponUpdateRequest();
        dto.setCouponId(coupon.getCouponId());
        dto.setCode(coupon.getCode());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMinOrderValue(coupon.getMinOrderValue());
        dto.setUsageLimit(coupon.getUsageLimit());
        // Note: validFrom/validTo in DTO but entity only has endDate
        // Set validTo to endDate, validFrom to now if endDate exists
        if (coupon.getEndDate() != null) {
            dto.setValidTo(coupon.getEndDate());
            dto.setValidFrom(LocalDateTime.now().minusDays(30)); // Default 30 days before
        }
        return dto;
    }

    /**
     * Thêm thông tin chung vào model cho form
     */
    private void addCommonFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("discountTypes", Coupon.DiscountType.values());
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách coupon
     * GET /admin/coupons
     */
    @GetMapping
    public String couponsList(
            @RequestParam(required = false) String search,
            Authentication authentication,
            Model model) {

        List<Coupon> coupons;

        if (search != null && !search.trim().isEmpty()) {
            coupons = couponService.searchCoupons(search);
            model.addAttribute("search", search);
        } else {
            coupons = couponService.getAllCoupons();
        }

        model.addAttribute("coupons", coupons);
        model.addAttribute("totalCoupons", coupons.size());

        // Thống kê nhanh
        long activeCoupons = coupons.stream()
                .filter(c -> c.getEndDate().isAfter(LocalDateTime.now()) && c.getUsageLimit() > 0)
                .count();
        long expiredCoupons = coupons.stream()
                .filter(c -> c.getEndDate().isBefore(LocalDateTime.now()))
                .count();

        model.addAttribute("activeCoupons", activeCoupons);
        model.addAttribute("expiredCoupons", expiredCoupons);

        return "admin/coupons/list";
    }

    /**
     * Hiển thị form thêm coupon mới
     * GET /admin/coupons/add
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        CouponCreateRequest couponRequest = new CouponCreateRequest();
        couponRequest.setDiscountType(Coupon.DiscountType.PERCENT);
        couponRequest.setUsageLimit(100);
        couponRequest.setValidFrom(LocalDateTime.now());
        couponRequest.setValidTo(LocalDateTime.now().plusMonths(1));

        model.addAttribute("couponRequest", couponRequest);
        addCommonFormAttributes(model, false);

        return "admin/coupons/form";
    }

    /**
     * Xử lý thêm coupon mới
     * POST /admin/coupons
     */
    @PostMapping
    public String createCoupon(
            @Valid @ModelAttribute("couponRequest") CouponCreateRequest couponRequest,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            addCommonFormAttributes(model, false);
            return "admin/coupons/form";
        }

        try {
            // Check code uniqueness
            if (couponService.getCouponByCode(couponRequest.getCode()).isPresent()) {
                result.rejectValue("code", "error.couponRequest", "Mã coupon đã tồn tại");
                addCommonFormAttributes(model, false);
                return "admin/coupons/form";
            }

            // Create new coupon
            Coupon coupon = new Coupon();
            coupon.setCouponId(generateNextCouponId());
            coupon.setCode(couponRequest.getCode().toUpperCase());
            coupon.setDiscountType(couponRequest.getDiscountType());
            coupon.setDiscountValue(couponRequest.getDiscountValue());
            coupon.setMinOrderValue(couponRequest.getMinOrderValue());
            coupon.setEndDate(couponRequest.getValidTo()); // Map validTo to endDate
            coupon.setUsageLimit(couponRequest.getUsageLimit());

            couponService.saveCoupon(coupon);

            redirectAttributes.addFlashAttribute("success",
                "Tạo mã giảm giá '" + coupon.getCode() + "' thành công!");
            return REDIRECT_COUPONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
            return REDIRECT_COUPONS;
        }
    }

    /**
     * Hiển thị form chỉnh sửa coupon
     * GET /admin/coupons/edit/{id}
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Coupon> couponOpt = couponService.getCouponById(id);

        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy coupon!");
            return REDIRECT_COUPONS;
        }

        CouponUpdateRequest couponRequest = mapToUpdateRequest(couponOpt.get());
        model.addAttribute("couponRequest", couponRequest);
        addCommonFormAttributes(model, true);

        return "admin/coupons/form";
    }

    /**
     * Xử lý cập nhật coupon
     * POST /admin/coupons/update/{id}
     */
    @PostMapping("/update/{id}")
    public String updateCoupon(
            @PathVariable String id,
            @Valid @ModelAttribute("couponRequest") CouponUpdateRequest couponRequest,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            addCommonFormAttributes(model, true);
            return "admin/coupons/form";
        }

        try {
            Optional<Coupon> existingCouponOpt = couponService.getCouponById(id);
            if (existingCouponOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy coupon!");
                return REDIRECT_COUPONS;
            }

            Coupon coupon = existingCouponOpt.get();

            // Check code uniqueness (excluding current coupon)
            Optional<Coupon> codeCheck = couponService.getCouponByCode(couponRequest.getCode());
            if (codeCheck.isPresent() && !codeCheck.get().getCouponId().equals(id)) {
                result.rejectValue("code", "error.couponRequest", "Mã coupon đã tồn tại");
                addCommonFormAttributes(model, true);
                return "admin/coupons/form";
            }

            // Update fields
            coupon.setCode(couponRequest.getCode().toUpperCase());
            coupon.setDiscountType(couponRequest.getDiscountType());
            coupon.setDiscountValue(couponRequest.getDiscountValue());
            coupon.setMinOrderValue(couponRequest.getMinOrderValue());
            coupon.setEndDate(couponRequest.getValidTo()); // Map validTo to endDate
            coupon.setUsageLimit(couponRequest.getUsageLimit());

            couponService.saveCoupon(coupon);

            redirectAttributes.addFlashAttribute("success",
                "Cập nhật mã giảm giá '" + coupon.getCode() + "' thành công!");
            return REDIRECT_COUPONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
            return REDIRECT_COUPONS;
        }
    }

    /**
     * Xem chi tiết coupon
     * GET /admin/coupons/view/{id}
     */
    @GetMapping("/view/{id}")
    public String viewCoupon(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Coupon> couponOpt = couponService.getCouponById(id);

        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy coupon!");
            return REDIRECT_COUPONS;
        }

        Coupon coupon = couponOpt.get();
        model.addAttribute("coupon", coupon);

        // Check if coupon is valid
        boolean isExpired = coupon.getEndDate().isBefore(LocalDateTime.now());
        boolean isActive = !isExpired && coupon.getUsageLimit() > 0;

        model.addAttribute("isExpired", isExpired);
        model.addAttribute("isActive", isActive);

        return "admin/coupons/view";
    }

    /**
     * Xóa coupon
     * POST /admin/coupons/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public String deleteCoupon(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Coupon> couponOpt = couponService.getCouponById(id);
            if (couponOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy coupon!");
                return REDIRECT_COUPONS;
            }

            String code = couponOpt.get().getCode();
            couponService.deleteCoupon(id);

            redirectAttributes.addFlashAttribute("success",
                "Xóa mã giảm giá '" + code + "' thành công!");
            return REDIRECT_COUPONS;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Không thể xóa coupon: " + e.getMessage());
            return REDIRECT_COUPONS;
        }
    }

    /**
     * Xem thống kê coupon
     * GET /admin/coupons/statistics
     */
    @GetMapping("/statistics")
    public String viewStatistics(Model model) {
        List<Coupon> allCoupons = couponService.getAllCoupons();

        long totalCoupons = allCoupons.size();
        long activeCoupons = allCoupons.stream()
                .filter(c -> c.getEndDate().isAfter(LocalDateTime.now()) && c.getUsageLimit() > 0)
                .count();
        long expiredCoupons = allCoupons.stream()
                .filter(c -> c.getEndDate().isBefore(LocalDateTime.now()))
                .count();
        long usedUpCoupons = allCoupons.stream()
                .filter(c -> c.getUsageLimit() <= 0)
                .count();

        model.addAttribute("totalCoupons", totalCoupons);
        model.addAttribute("activeCoupons", activeCoupons);
        model.addAttribute("expiredCoupons", expiredCoupons);
        model.addAttribute("usedUpCoupons", usedUpCoupons);
        model.addAttribute("coupons", allCoupons);

        return "admin/coupons/statistics";
    }

    // ============================= AJAX ENDPOINTS =============================

    /**
     * Kiểm tra tính hợp lệ của coupon
     * POST /admin/coupons/validate
     */
    @PostMapping("/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderValue) {

        Map<String, Object> response = new HashMap<>();

        Optional<Coupon> couponOpt = couponService.getCouponByCode(code);
        if (couponOpt.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá không tồn tại");
            return ResponseEntity.ok(response);
        }

        Coupon coupon = couponOpt.get();
        boolean isValid = couponService.isCouponValid(coupon, orderValue);

        if (isValid) {
            BigDecimal discount = couponService.calculateDiscount(coupon, orderValue);
            response.put("valid", true);
            response.put("discount", discount);
            response.put("message", "Mã giảm giá hợp lệ");
        } else {
            response.put("valid", false);
            response.put("message", "Mã giảm giá không hợp lệ hoặc đã hết hạn");
        }

        return ResponseEntity.ok(response);
    }
}

