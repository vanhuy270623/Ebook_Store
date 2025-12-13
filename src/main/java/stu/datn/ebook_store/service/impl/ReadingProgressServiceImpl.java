package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.repository.ReadingProgressRepository;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReadingProgressServiceImpl implements ReadingProgressService {

    private final ReadingProgressRepository readingProgressRepository;

    @Autowired
    public ReadingProgressServiceImpl(ReadingProgressRepository readingProgressRepository) {
        this.readingProgressRepository = readingProgressRepository;
    }

    @Override
    public List<ReadingProgress> getAllReadingProgress() {
        return readingProgressRepository.findAll();
    }

    @Override
    public Optional<ReadingProgress> getReadingProgressById(String progressId) {
        return readingProgressRepository.findById(progressId);
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
    public void deleteReadingProgress(String progressId) {
        readingProgressRepository.deleteById(progressId);
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
    public List<ReadingProgress> getRecentReadingByUser(User user) {
        return readingProgressRepository.findByUserOrderByLastReadAtDesc(user);
    }

    @Override
    public List<ReadingProgress> getCompletedBooksByUser(User user) {
        return readingProgressRepository.findByUserAndIsCompletedTrue(user);
    }

    @Override
    public List<ReadingProgress> getReadingProgressByUserAndAccessType(User user, ReadingProgress.AccessType accessType) {
        return readingProgressRepository.findByUserAndAccessType(user, accessType);
    }

    @Override
    public List<ReadingProgress> getReadingProgressByBook(Book book) {
        return readingProgressRepository.findByBook(book);
    }

    @Override
    public long countCompletedBooksByUser(User user) {
        return readingProgressRepository.countByUserAndIsCompletedTrue(user);
    }

    @Override
    public List<ReadingProgress> getContinueReadingByUser(User user) {
        return readingProgressRepository.findContinueReading(user);
    }

    @Override
    public void markAsFavorite(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setIsFavorite(true);
            readingProgressRepository.save(progress);
        }
    }

    @Override
    public void unmarkAsFavorite(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setIsFavorite(false);
            readingProgressRepository.save(progress);
        }
    }

    @Override
    public void markAsCompleted(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setIsCompleted(true);
            progress.setProgressPercentage(100.0f);
            progress.setLastReadAt(LocalDateTime.now());
            readingProgressRepository.save(progress);
        }
    }

    @Override
    public void updateProgress(String progressId, Float percentage, String location) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setProgressPercentage(percentage);
            progress.setLastReadLocation(location);
            progress.setLastReadAt(LocalDateTime.now());
            if (percentage >= 100.0f) {
                progress.setIsCompleted(true);
            }
            readingProgressRepository.save(progress);
        }
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
}

