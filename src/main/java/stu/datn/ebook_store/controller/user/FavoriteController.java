package stu.datn.ebook_store.controller.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý các request liên quan đến sách yêu thích
 * Endpoint: /api/favorites
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final ReadingProgressService readingProgressService;

    @Autowired
    public FavoriteController(ReadingProgressService readingProgressService) {
        this.readingProgressService = readingProgressService;
    }

    /**
     * Toggle trạng thái yêu thích của một cuốn sách
     * POST /api/favorites/toggle
     * Request Body: { "bookId": "book_xxx" }
     * Response: { "success": true, "isFavorite": true/false, "message": "..." }
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra user đã đăng nhập chưa
            User currentUser = (User) session.getAttribute("loggedInUser");
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để thêm sách yêu thích");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Lấy bookId từ request
            String bookId = request.get("bookId");
            if (bookId == null || bookId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Thiếu thông tin sách");
                return ResponseEntity.badRequest().body(response);
            }

            // Toggle favorite
            boolean isFavorite = readingProgressService.toggleFavorite(currentUser, bookId);

            response.put("success", true);
            response.put("isFavorite", isFavorite);
            response.put("message", isFavorite ?
                "Đã thêm vào sách yêu thích" :
                "Đã xóa khỏi sách yêu thích");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Kiểm tra xem sách có được yêu thích hay không
     * GET /api/favorites/check/{bookId}
     * Response: { "isFavorite": true/false }
     */
    @GetMapping("/check/{bookId}")
    public ResponseEntity<Map<String, Object>> checkFavorite(
            @PathVariable String bookId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = (User) session.getAttribute("loggedInUser");
            if (currentUser == null) {
                response.put("isFavorite", false);
                return ResponseEntity.ok(response);
            }

            // Kiểm tra trong reading progress
            boolean isFavorite = readingProgressService.getReadingProgressByUser(currentUser)
                    .stream()
                    .anyMatch(rp -> rp.getBook().getBookId().equals(bookId) &&
                                   Boolean.TRUE.equals(rp.getIsFavorite()));

            response.put("isFavorite", isFavorite);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("isFavorite", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}

