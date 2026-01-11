package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.dto.ProgressSyncRequest;
import stu.datn.ebook_store.dto.ProgressSyncResponse;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.repository.ReadingProgressRepository;
import stu.datn.ebook_store.repository.BookRepository;
import stu.datn.ebook_store.repository.BookAssetRepository;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReadingProgressServiceImpl implements ReadingProgressService {

    // ========== CHIẾN THUẬT TIME-CAPPING: TÁCH BIỆT VỊ TRÍ VÀ TIẾN ĐỘ ==========

    // Tốc độ tiến độ tối đa cho phép: 0.5% mỗi giây
    // = Tương đương ~3.3 phút (200 giây) để đọc hết 100% sách
    // = Ngăn chặn việc skip/jump trong cả EPUB và PDF
    // Ví dụ: Đọc 10 giây → Tối đa tăng 5% tiến độ
    private static final float MAX_PROGRESS_GAIN_PER_SECOND = 0.5f;

    // Ngưỡng tiến độ tối thiểu để có thể đánh giá: 20%
    private static final float REVIEW_ELIGIBILITY_THRESHOLD = 20.0f;

    private final ReadingProgressRepository readingProgressRepository;
    private final BookRepository bookRepository;
    private final BookAssetRepository bookAssetRepository;

    @Autowired
    public ReadingProgressServiceImpl(ReadingProgressRepository readingProgressRepository,
                                     BookRepository bookRepository,
                                     BookAssetRepository bookAssetRepository) {
        this.readingProgressRepository = readingProgressRepository;
        this.bookRepository = bookRepository;
        this.bookAssetRepository = bookAssetRepository;
    }


    @Override
    public Optional<ReadingProgress> getReadingProgressByUserAndBook(User user, Book book) {
        return readingProgressRepository.findByUserAndBook(user, book);
    }

    @Override
    @Transactional
    public ReadingProgress saveReadingProgress(ReadingProgress readingProgress) {
        if (readingProgress.getProgressId() == null || readingProgress.getProgressId().isEmpty()) {
            readingProgress.setProgressId(generateProgressId());
        }
        System.out.println("=== SAVING READING PROGRESS ===");
        System.out.println("Progress ID: " + readingProgress.getProgressId());
        System.out.println("User: " + (readingProgress.getUser() != null ? readingProgress.getUser().getUserId() : "null"));
        System.out.println("Book: " + (readingProgress.getBook() != null ? readingProgress.getBook().getBookId() : "null"));
        System.out.println("Location: " + readingProgress.getLastReadLocation());
        System.out.println("Percentage: " + readingProgress.getProgressPercentage());

        ReadingProgress saved = readingProgressRepository.saveAndFlush(readingProgress);

        System.out.println("=== PROGRESS SAVED TO DB ===");
        System.out.println("Saved ID: " + saved.getProgressId());
        return saved;
    }


    @Override
    public List<ReadingProgress> getReadingProgressByUser(User user) {
        return readingProgressRepository.findByUser(user);
    }

    @Override
    public List<ReadingProgress> getReadingProgressByUserWithBookDetails(User user) {
        return readingProgressRepository.findByUserWithBookDetails(user);
    }

    @Override
    public List<ReadingProgress> getFavoriteBooksByUser(User user) {
        return readingProgressRepository.findByUserAndIsFavoriteTrue(user);
    }


    @Override
    @Transactional
    public boolean toggleFavorite(User user, String bookId) {
        // Tìm book từ database
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));

        // Tìm hoặc tạo reading progress cho user và book
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findByUserAndBook(user, book);
        ReadingProgress progress;

        if (progressOpt.isPresent()) {
            // Nếu đã có progress, toggle trạng thái favorite
            progress = progressOpt.get();
            progress.setIsFavorite(!progress.getIsFavorite());
        } else {
            // Nếu chưa có progress, tạo mới với favorite = true
            progress = new ReadingProgress();
            progress.setProgressId(generateProgressId());
            progress.setUser(user);
            progress.setBook(book);
            progress.setIsFavorite(true);
            progress.setProgressPercentage(0.0f);
            progress.setIsCompleted(false);
            progress.setCreatedAt(LocalDateTime.now());
            progress.setLastReadAt(LocalDateTime.now());
        }

        readingProgressRepository.save(progress);
        return progress.getIsFavorite();
    }


    private String generateProgressId() {
        // Format: prog_XX (ví dụ: prog_01, prog_02, prog_100)
        // Tìm số lớn nhất hiện có và +1 để tránh trùng
        long maxId = 0;
        try {
            List<ReadingProgress> allProgress = readingProgressRepository.findAll();
            for (ReadingProgress p : allProgress) {
                String id = p.getProgressId();
                if (id != null && id.startsWith("prog_")) {
                    try {
                        long num = Long.parseLong(id.substring(5));
                        if (num > maxId) {
                            maxId = num;
                        }
                    } catch (NumberFormatException ignored) {
                        // Bỏ qua các ID không đúng format
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to count
            maxId = readingProgressRepository.count();
        }
        return String.format("prog_%02d", maxId + 1);
    }

    @Override
    @Transactional
    public void addBookmark(String progressId, String location, Integer pageNumber, Float percentage, String note) {
        System.out.println("=== ADD BOOKMARK START ===");
        System.out.println("progressId: " + progressId);
        System.out.println("location: " + location);
        System.out.println("pageNumber: " + pageNumber);
        System.out.println("percentage: " + percentage);
        System.out.println("note: " + note);

        ReadingProgress progress = readingProgressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Reading progress not found: " + progressId));

        System.out.println("Found progress for book: " + progress.getBook().getTitle());
        System.out.println("Current bookmarks_data: " + progress.getBookmarksData());

        // Parse existing bookmarks from current progress (avoid extra query)
        List<ReadingProgress.BookmarkData> bookmarks = new java.util.ArrayList<>();
        String existingJson = progress.getBookmarksData();

        if (existingJson != null && !existingJson.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(existingJson);
                com.fasterxml.jackson.databind.JsonNode bookmarksNode = root.get("bookmarks");

                if (bookmarksNode != null && bookmarksNode.isArray()) {
                    bookmarks = mapper.convertValue(
                        bookmarksNode,
                        mapper.getTypeFactory().constructCollectionType(
                            java.util.List.class,
                            ReadingProgress.BookmarkData.class
                        )
                    );
                }
                System.out.println("Parsed " + bookmarks.size() + " existing bookmarks");
            } catch (Exception e) {
                // Log error but continue with empty list
                System.err.println("Error parsing existing bookmarks: " + e.getMessage());
                e.printStackTrace();
                bookmarks = new java.util.ArrayList<>();
            }
        }

        // Add new bookmark
        ReadingProgress.BookmarkData newBookmark = new ReadingProgress.BookmarkData(
            location, pageNumber, percentage, note
        );
        bookmarks.add(newBookmark);
        System.out.println("Added new bookmark with ID: " + newBookmark.getId());

        // Convert to JSON and save
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("bookmarks", bookmarks);
            String jsonData = mapper.writeValueAsString(data);
            progress.setBookmarksData(jsonData);

            // Use saveAndFlush to immediately persist to database
            readingProgressRepository.saveAndFlush(progress);
            System.out.println("=== BOOKMARK SAVED SUCCESSFULLY ===");
            System.out.println("New bookmarks_data: " + jsonData);
        } catch (Exception e) {
            System.err.println("=== ERROR SAVING BOOKMARK ===");
            e.printStackTrace();
            throw new RuntimeException("Error saving bookmark: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeBookmark(String progressId, String bookmarkId) {
        System.out.println("=== REMOVE BOOKMARK START ===");
        System.out.println("progressId: " + progressId + ", bookmarkId: " + bookmarkId);

        ReadingProgress progress = readingProgressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Reading progress not found: " + progressId));

        // Parse existing bookmarks from current progress
        List<ReadingProgress.BookmarkData> bookmarks = new java.util.ArrayList<>();
        String existingJson = progress.getBookmarksData();

        if (existingJson != null && !existingJson.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(existingJson);
                com.fasterxml.jackson.databind.JsonNode bookmarksNode = root.get("bookmarks");

                if (bookmarksNode != null && bookmarksNode.isArray()) {
                    bookmarks = mapper.convertValue(
                        bookmarksNode,
                        mapper.getTypeFactory().constructCollectionType(
                            java.util.List.class,
                            ReadingProgress.BookmarkData.class
                        )
                    );
                }
            } catch (Exception e) {
                System.err.println("Error parsing existing bookmarks: " + e.getMessage());
                e.printStackTrace();
                bookmarks = new java.util.ArrayList<>();
            }
        }

        // Remove the bookmark with matching ID
        int sizeBefore = bookmarks.size();
        bookmarks.removeIf(bm -> bm.getId().equals(bookmarkId));
        int sizeAfter = bookmarks.size();
        System.out.println("Removing bookmark " + bookmarkId + ": " + sizeBefore + " -> " + sizeAfter);

        // Convert to JSON and save
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("bookmarks", bookmarks);
            String jsonData = mapper.writeValueAsString(data);
            progress.setBookmarksData(jsonData);

            // Use saveAndFlush to immediately persist to database
            readingProgressRepository.saveAndFlush(progress);
            System.out.println("=== BOOKMARK REMOVED SUCCESSFULLY ===");
            System.out.println("New bookmarks_data: " + jsonData);
        } catch (Exception e) {
            System.err.println("=== ERROR REMOVING BOOKMARK ===");
            e.printStackTrace();
            throw new RuntimeException("Error removing bookmark: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ReadingProgress.BookmarkData> getBookmarks(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            String bookmarksJson = progress.getBookmarksData();

            if (bookmarksJson == null || bookmarksJson.trim().isEmpty()) {
                return new java.util.ArrayList<>();
            }

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(bookmarksJson);
                com.fasterxml.jackson.databind.JsonNode bookmarksNode = root.get("bookmarks");

                if (bookmarksNode != null && bookmarksNode.isArray()) {
                    return mapper.convertValue(
                        bookmarksNode,
                        mapper.getTypeFactory().constructCollectionType(
                            java.util.List.class,
                            ReadingProgress.BookmarkData.class
                        )
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Error parsing bookmarks: " + e.getMessage(), e);
            }
        }
        return new java.util.ArrayList<>();
    }

    /**
     * Đồng bộ tiến độ đọc từ Frontend với Anti-Skimming Validation
     *
     * @param user Người dùng hiện tại
     * @param request Dữ liệu đồng bộ từ Frontend
     * @return Response chứa thông tin sync và trạng thái skimming
     */
    @Override
    @Transactional
    public ProgressSyncResponse syncProgress(User user, ProgressSyncRequest request) {
        System.out.println("=== SYNC PROGRESS WITH TIME-CAPPING (LOCATION vs PROGRESS) ===");
        System.out.println("User: " + user.getUserId());
        System.out.println("BookId: " + request.getBookId());
        System.out.println("Client Progress: " + request.getProgressPercentage() + "%");
        System.out.println("Active Time Delta: " + request.getActiveTimeDelta() + "s");
        System.out.println("Location: " + request.getCurrentLocationRaw());

        // 1. Lấy Book entity
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found: " + request.getBookId()));

        // 2. Lấy hoặc tạo mới ReadingProgress
        ReadingProgress progress = readingProgressRepository.findByUserAndBook(user, book)
                .orElseGet(() -> createNewProgress(user, book, request));

        // 3. Lấy dữ liệu cũ
        float oldProgress = progress.getProgressPercentage() != null ? progress.getProgressPercentage() : 0.0f;
        float clientProgress = request.getProgressPercentage() != null ? request.getProgressPercentage() : 0.0f;
        int deltaTime = request.getActiveTimeDelta() != null ? request.getActiveTimeDelta() : 0;

        // ========== CHIẾN THUẬT TÁCH BIỆT VỊ TRÍ VÀ TIẾN ĐỘ ==========

        // BƯỚC 1: LUÔN LƯU VỊ TRÍ (Đảm bảo UX - người dùng mở lại đúng chỗ)
        progress.setLastReadLocation(request.getCurrentLocationRaw());
        System.out.println("✓ Location saved: " + request.getCurrentLocationRaw());

        // BƯỚC 2: TÍNH TOÁN TIẾN ĐỘ DựA trên TIME-CAPPING
        float clientGain = clientProgress - oldProgress;
        float maxPossibleGain = deltaTime * MAX_PROGRESS_GAIN_PER_SECOND;

        // Chốt tiến độ mới = min(client báo, tối đa cho phép)
        float actualGain = Math.min(clientGain, maxPossibleGain);
        float finalProgress = oldProgress + actualGain;

        // Đảm bảo không vượt quá 100%
        if (finalProgress > 100.0f) {
            finalProgress = 100.0f;
        }

        // Tính velocity để log
        float velocity = (deltaTime > 0) ? (clientGain / deltaTime) : 0.0f;
        boolean wasCapped = clientGain > maxPossibleGain;

        System.out.println("--- TIME-CAPPING CALCULATION ---");
        System.out.println("Old Progress: " + oldProgress + "%");
        System.out.println("Client Progress: " + clientProgress + "%");
        System.out.println("Client Gain: " + clientGain + "%");
        System.out.println("Delta Time: " + deltaTime + "s");
        System.out.println("Max Possible Gain: " + maxPossibleGain + "% (" + deltaTime + "s × " + MAX_PROGRESS_GAIN_PER_SECOND + "%/s)");
        System.out.println("Actual Gain: " + actualGain + "%");
        System.out.println("Final Progress: " + finalProgress + "%");
        System.out.println("Was Capped: " + wasCapped);
        System.out.println("Velocity: " + velocity + " %/s");

        // BƯỚC 3: Lưu tiến độ đã được kiểm soát
        progress.setProgressPercentage(finalProgress);

        // 4. Cập nhật BookAsset nếu có
        if (request.getBookAssetId() != null && !request.getBookAssetId().isEmpty()) {
            bookAssetRepository.findById(request.getBookAssetId())
                    .ifPresent(progress::setBookAsset);
        }

        // 5. Tích lũy thời gian (chỉ khi có deltaTime > 0)
        if (deltaTime > 0) {
            long currentActiveTime = progress.getTotalActiveSeconds() != null ? progress.getTotalActiveSeconds() : 0L;
            progress.setTotalActiveSeconds(currentActiveTime + deltaTime);
            System.out.println("✓ Time accumulated: " + deltaTime + "s (Total: " + progress.getTotalActiveSeconds() + "s)");
        }

        // 6. Kiểm tra completion
        if (finalProgress >= 100.0f) {
            progress.setIsCompleted(true);
        }

        // 7. Cập nhật timestamp
        progress.setLastReadAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        // 8. Lưu vào database
        ReadingProgress saved = readingProgressRepository.saveAndFlush(progress);

        // 9. Tạo response message
        String message;
        if (wasCapped) {
            message = String.format(
                "⚠️ Tiến độ đã được điều chỉnh từ %.1f%% → %.1f%% (Thời gian đọc: %ds). Hãy dành thời gian đọc kỹ!",
                clientProgress, finalProgress, deltaTime
            );
        } else {
            message = "✅ Tiến độ đã được đồng bộ thành công!";
        }

        // 10. Tạo response
        ProgressSyncResponse response = ProgressSyncResponse.builder()
                .success(true)
                .isSkimming(wasCapped) // Đánh dấu nếu bị capping
                .currentProgress(saved.getProgressPercentage())
                .totalActiveTime(saved.getTotalActiveSeconds())
                .canReview(saved.getProgressPercentage() >= REVIEW_ELIGIBILITY_THRESHOLD)
                .isCompleted(saved.getIsCompleted())
                .readingVelocity(velocity)
                .progressId(saved.getProgressId())
                .message(message)
                .build();

        System.out.println("=== SYNC COMPLETED ===");
        System.out.println("Progress Saved: " + saved.getProgressPercentage() + "%");
        System.out.println("Can Review: " + response.isCanReview());
        System.out.println("Total Active Time: " + response.getTotalActiveTime() + "s");
        System.out.println("Message: " + message);

        return response;
    }

    /**
     * Kiểm tra người dùng có thể viết review không (progress >= 20%)
     *
     * @param user Người dùng
     * @param bookId ID sách
     * @return true nếu progress >= 20%, false nếu ngược lại
     */
    @Override
    public boolean canUserReview(User user, String bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return false;
        }

        return readingProgressRepository.findByUserAndBook(user, book)
                .map(progress -> {
                    float progressPercent = progress.getProgressPercentage() != null
                            ? progress.getProgressPercentage()
                            : 0.0f;
                    boolean canReview = progressPercent >= REVIEW_ELIGIBILITY_THRESHOLD;

                    System.out.println("=== CAN USER REVIEW CHECK ===");
                    System.out.println("User: " + user.getUserId());
                    System.out.println("Book: " + bookId);
                    System.out.println("Progress: " + progressPercent + "%");
                    System.out.println("Can Review: " + canReview);

                    return canReview;
                })
                .orElse(false);
    }

    /**
     * Tạo mới ReadingProgress khi chưa có
     */
    private ReadingProgress createNewProgress(User user, Book book, ProgressSyncRequest request) {
        ReadingProgress progress = new ReadingProgress();
        progress.setProgressId(generateProgressId());
        progress.setUser(user);
        progress.setBook(book);
        progress.setProgressPercentage(0.0f);
        progress.setTotalActiveSeconds(0L);
        progress.setIsCompleted(false);
        progress.setIsFavorite(false);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setLastReadAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        // Set BookAsset if provided
        if (request.getBookAssetId() != null && !request.getBookAssetId().isEmpty()) {
            bookAssetRepository.findById(request.getBookAssetId())
                    .ifPresent(progress::setBookAsset);
        }

        return progress;
    }
}


