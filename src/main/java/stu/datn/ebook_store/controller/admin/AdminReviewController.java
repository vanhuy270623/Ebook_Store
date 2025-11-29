package stu.datn.ebook_store.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.entity.Review;
import stu.datn.ebook_store.service.ReviewService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý quản lý đánh giá (Review Management)
 * Chức năng: Duyệt, từ chối đánh giá từ người dùng
 * Pattern: Tương tự AdminPostController, AdminBannerController
 */
@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private static final String REDIRECT_REVIEWS = "redirect:/admin/reviews";

    private final ReviewService reviewService;

    @Autowired
    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ============================= HELPER METHODS =============================

    /**
     * Thêm thông tin chung vào model cho filter
     */
    private void addCommonAttributes(Model model) {
        model.addAttribute("ratingValues", new Integer[]{1, 2, 3, 4, 5});
    }

    // ============================= CRUD OPERATIONS =============================

    /**
     * Hiển thị danh sách đánh giá
     */
    @GetMapping
    public String reviewsList(@RequestParam(required = false, defaultValue = "unapproved") String filter,
                             Model model) {
        List<Review> reviews;

        switch (filter.toLowerCase()) {
            case "approved":
                // Lấy những reviews đã được duyệt
                reviews = reviewService.getAllReviews().stream()
                        .filter(Review::getIsApproved)
                        .collect(Collectors.toList());
                break;
            case "rejected":
                // Lấy những reviews bị từ chối
                reviews = reviewService.getAllReviews().stream()
                        .filter(r -> !r.getIsApproved())
                        .collect(Collectors.toList());
                break;
            case "verified":
                // Lấy những reviews từ mua hàng đã xác thực
                reviews = reviewService.getVerifiedPurchaseReviews();
                break;
            case "unapproved":
            default:
                reviews = reviewService.getUnapprovedReviews();
                break;
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("totalReviews", reviews.size());
        model.addAttribute("currentFilter", filter);

        // Thống kê nhanh
        long totalUnapproved = reviewService.getUnapprovedReviews().size();
        long totalVerified = reviewService.getVerifiedPurchaseReviews().size();
        model.addAttribute("totalUnapproved", totalUnapproved);
        model.addAttribute("totalVerified", totalVerified);

        addCommonAttributes(model);
        return "admin/reviews/list";
    }

    /**
     * Xem chi tiết đánh giá
     */
    @GetMapping("/view/{id}")
    public String viewReview(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Review review = reviewService.getReviewById(id).orElse(null);

        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đánh giá với ID: " + id);
            return REDIRECT_REVIEWS;
        }

        model.addAttribute("review", review);
        addCommonAttributes(model);
        return "admin/reviews/view";
    }

    /**
     * Duyệt/từ chối đánh giá
     */
    @PostMapping("/approve/{id}")
    public String approveReview(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Review review = reviewService.getReviewById(id).orElse(null);

        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đánh giá!");
            return REDIRECT_REVIEWS;
        }

        try {
            reviewService.approveReview(id);
            redirectAttributes.addFlashAttribute("success", "Duyệt đánh giá thành công!");
            return REDIRECT_REVIEWS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/reviews/view/" + id;
        }
    }

    /**
     * Từ chối đánh giá
     */
    @PostMapping("/reject/{id}")
    public String rejectReview(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Review review = reviewService.getReviewById(id).orElse(null);

        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đánh giá!");
            return REDIRECT_REVIEWS;
        }

        try {
            reviewService.rejectReview(id);
            redirectAttributes.addFlashAttribute("success", "Từ chối đánh giá thành công!");
            return REDIRECT_REVIEWS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/reviews/view/" + id;
        }
    }

    /**
     * Xóa đánh giá
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            reviewService.deleteReview(id);
            response.put("success", true);
            response.put("message", "Xóa đánh giá thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa đánh giá";
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Bulk action - Duyệt nhiều đánh giá
     */
    @PostMapping("/bulk-approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkApprove(@RequestParam List<String> reviewIds) {
        Map<String, Object> response = new HashMap<>();
        try {
            int count = 0;
            for (String reviewId : reviewIds) {
                reviewService.approveReview(reviewId);
                count++;
            }
            response.put("success", true);
            response.put("message", "Duyệt " + count + " đánh giá thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Bulk action - Từ chối nhiều đánh giá
     */
    @PostMapping("/bulk-reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkReject(@RequestParam List<String> reviewIds) {
        Map<String, Object> response = new HashMap<>();
        try {
            int count = 0;
            for (String reviewId : reviewIds) {
                reviewService.rejectReview(reviewId);
                count++;
            }
            response.put("success", true);
            response.put("message", "Từ chối " + count + " đánh giá thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị thống kê đánh giá
     */
    @GetMapping("/statistics")
    public String reviewsStatistics(Model model) {
        List<Review> allReviews = reviewService.getAllReviews();

        model.addAttribute("totalReviews", allReviews.size());

        long approvedReviews = allReviews.stream()
                .filter(Review::getIsApproved)
                .count();
        model.addAttribute("approvedReviews", approvedReviews);

        long rejectedReviews = allReviews.stream()
                .filter(r -> !r.getIsApproved())
                .count();
        model.addAttribute("rejectedReviews", rejectedReviews);

        long verifiedPurchaseReviews = allReviews.stream()
                .filter(Review::getIsVerifiedPurchase)
                .count();
        model.addAttribute("verifiedPurchaseReviews", verifiedPurchaseReviews);

        // Phân bố đánh giá theo rating
        Map<Integer, Long> ratingDistribution = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));
        model.addAttribute("ratingDistribution", ratingDistribution);

        // Top rated reviews
        List<Review> topReviews = allReviews.stream()
                .filter(Review::getIsApproved)
                .sorted((a, b) -> b.getRating().compareTo(a.getRating()))
                .limit(10)
                .collect(Collectors.toList());
        model.addAttribute("topReviews", topReviews);

        return "admin/reviews/statistics";
    }
}

