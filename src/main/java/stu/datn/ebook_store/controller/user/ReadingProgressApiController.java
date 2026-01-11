package stu.datn.ebook_store.controller.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.controller.BaseController;
import stu.datn.ebook_store.dto.ProgressSyncRequest;
import stu.datn.ebook_store.dto.ProgressSyncResponse;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller cho Reading Progress với Anti-Skimming Validation
 *
 * Endpoints:
 * - POST /api/reading/sync - Đồng bộ tiến độ đọc từ Frontend
 * - GET /api/reading/can-review/{bookId} - Kiểm tra quyền đánh giá
 */
@RestController
@RequestMapping("/api/reading")
@Slf4j
public class ReadingProgressApiController extends BaseController {

    private final ReadingProgressService readingProgressService;

    @Autowired
    public ReadingProgressApiController(ReadingProgressService readingProgressService) {
        this.readingProgressService = readingProgressService;
    }

    /**
     * Đồng bộ tiến độ đọc từ Frontend với Anti-Skimming Validation
     *
     * POST /api/reading/sync
     *
     * Request Body:
     * {
     *   "bookId": "book_001",
     *   "bookAssetId": "asset_pdf_001",
     *   "currentLocationRaw": "page-15",
     *   "progressPercentage": 25.5,
     *   "activeTimeDelta": 30
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "isSkimming": false,
     *   "currentProgress": 25.5,
     *   "totalActiveTime": 450,
     *   "canReview": true,
     *   "isCompleted": false,
     *   "message": "Tiến độ đã được đồng bộ thành công!",
     *   "readingVelocity": 0.035,
     *   "progressId": "prog_01"
     * }
     */
    @PostMapping("/sync")
    public ResponseEntity<ProgressSyncResponse> syncProgress(@RequestBody ProgressSyncRequest request) {
        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                log.warn("User not authenticated when trying to sync progress");
                ProgressSyncResponse errorResponse = ProgressSyncResponse.builder()
                        .success(false)
                        .message("Vui lòng đăng nhập để đồng bộ tiến độ đọc")
                        .build();
                return ResponseEntity.status(401).body(errorResponse);
            }

            // Validate request
            if (request.getBookId() == null || request.getBookId().isEmpty()) {
                log.warn("Missing bookId in sync request");
                ProgressSyncResponse errorResponse = ProgressSyncResponse.builder()
                        .success(false)
                        .message("Thiếu thông tin sách")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getProgressPercentage() == null ||
                request.getProgressPercentage() < 0 ||
                request.getProgressPercentage() > 100) {
                log.warn("Invalid progress percentage: {}", request.getProgressPercentage());
                ProgressSyncResponse errorResponse = ProgressSyncResponse.builder()
                        .success(false)
                        .message("Tiến độ không hợp lệ (phải từ 0-100)")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Syncing progress for user {} - book {} - progress {}%",
                    currentUser.getUserId(),
                    request.getBookId(),
                    request.getProgressPercentage());

            // Gọi service để xử lý
            ProgressSyncResponse response = readingProgressService.syncProgress(currentUser, request);

            if (response.isSkimming()) {
                log.warn("Skimming detected for user {} - velocity: {} %/s",
                        currentUser.getUserId(),
                        response.getReadingVelocity());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing progress: {}", e.getMessage(), e);
            ProgressSyncResponse errorResponse = ProgressSyncResponse.builder()
                    .success(false)
                    .message("Có lỗi xảy ra khi đồng bộ tiến độ: " + e.getMessage())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Kiểm tra người dùng có thể viết review không
     *
     * GET /api/reading/can-review/{bookId}
     *
     * Response:
     * {
     *   "canReview": true,
     *   "currentProgress": 25.5,
     *   "requiredProgress": 20.0,
     *   "message": "Bạn có thể đánh giá sách này"
     * }
     */
    @GetMapping("/can-review/{bookId}")
    public ResponseEntity<Map<String, Object>> checkReviewEligibility(@PathVariable String bookId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                response.put("canReview", false);
                response.put("message", "Vui lòng đăng nhập");
                return ResponseEntity.status(401).body(response);
            }

            boolean canReview = readingProgressService.canUserReview(currentUser, bookId);

            response.put("canReview", canReview);
            response.put("requiredProgress", 20.0);

            if (canReview) {
                response.put("message", "Bạn có thể đánh giá sách này");
            } else {
                response.put("message", "Bạn cần đọc ít nhất 20% nội dung để đánh giá");
            }

            log.info("Review eligibility check - user: {}, book: {}, canReview: {}",
                    currentUser.getUserId(), bookId, canReview);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking review eligibility: {}", e.getMessage(), e);
            response.put("canReview", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Lấy thông tin tiến độ đọc chi tiết
     *
     * GET /api/reading/progress/{bookId}
     */
    @GetMapping("/progress/{bookId}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable String bookId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                response.put("error", "Vui lòng đăng nhập");
                return ResponseEntity.status(401).body(response);
            }

            // Sử dụng BookRepository để lấy book và sau đó query progress
            // (Implementation tùy thuộc vào nhu cầu cụ thể)

            response.put("success", true);
            response.put("message", "Endpoint đang được phát triển");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting progress: {}", e.getMessage(), e);
            response.put("error", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

