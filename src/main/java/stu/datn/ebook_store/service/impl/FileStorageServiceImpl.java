package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:F:/datn_uploads}")
    private String baseUploadDir;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_BOOK_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String[] ALLOWED_IMAGE_CONTENT_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    // ==================== NEW IMPLEMENTATIONS ====================

    /**
     * Hàm chung lưu ảnh cho Banner, Author, User Avatar
     * Path: book_asset/image/{type}/{entityId}.{ext}
     */
    @Override
    public String storeEntityImage(MultipartFile file, String type, String entityId) throws IOException {
        validateImage(file);

        // Xác định thư mục con (vd: book_asset/image/banners)
        String subDir = "book_asset/image/" + type;

        // Tạo tên file từ ID (vd: banner_01.jpg)
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = entityId + extension;

        return saveFileToSystem(file, subDir, fileName);
    }

    /**
     * Hàm riêng lưu ảnh bìa sách theo danh mục và tên sách
     * Path: book_asset/image/covers/{categorySlug}/{bookSlug}.{ext}
     */
    @Override
    public String storeBookCover(MultipartFile file, String categoryName, String bookTitle) throws IOException {
        validateImage(file);

        // Tạo slug
        String categorySlug = createSlug(categoryName);
        String bookSlug = createSlug(bookTitle);

        // Xác định thư mục con (vd: book_asset/image/covers/tieu-thuyet)
        String subDir = "book_asset/image/covers/" + categorySlug;

        // Tạo tên file (vd: ba-nguoi-linh-ngu-lam.jpg)
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = bookSlug + extension;

        return saveFileToSystem(file, subDir, fileName);
    }

    // ==================== EXISTING / MODIFIED METHODS ====================

    @Override
    public String storeFile(MultipartFile file, String subdirectory) throws IOException {
        // Hàm generic lưu file với tên UUID ngẫu nhiên
        if (file.isEmpty()) throw new IOException("Cannot store empty file");

        String fileName = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
        return saveFileToSystem(file, subdirectory, fileName);
    }

    @Override
    public String storeBookAsset(MultipartFile file) throws IOException {
        validateBookAsset(file);
        return storeFile(file, "books/assets");
    }

    @Override
    public String storeBookSource(MultipartFile file, String categorySlug) throws IOException {
        validateBookAsset(file);

        // Lưu vào thư mục: book_asset/source/{categorySlug}/
        String subDir = "book_asset/source/" + (categorySlug != null ? categorySlug : "uncategorized");

        // Giữ tên file gốc nhưng làm sạch ký tự đặc biệt
        String originalName = file.getOriginalFilename();
        String sanitizedFilename = originalName != null
                ? originalName.replaceAll("[^a-zA-Z0-9.\\-_]", "_")
                : UUID.randomUUID().toString();

        return saveFileToSystem(file, subDir, sanitizedFilename);
    }

    // ==================== HELPER METHODS (CORE LOGIC) ====================

    /**
     * Core method: Tạo thư mục, lưu file và trả về đường dẫn tương đối
     */
    private String saveFileToSystem(MultipartFile file, String subDir, String fileName) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        // Tạo đường dẫn tuyệt đối
        Path uploadPath = Paths.get(baseUploadDir, subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Đường dẫn file đầy đủ
        Path filePath = uploadPath.resolve(fileName);

        // Lưu file (Ghi đè - REPLACE_EXISTING)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn tương đối (dùng '/' chuẩn web)
        return subDir + "/" + fileName;
    }

    /**
     * Tạo slug từ chuỗi (Tiếng Việt có dấu -> không dấu, khoảng trắng -> gạch ngang)
     */
    private String createSlug(String input) {
        if (input == null || input.isEmpty()) return "uncategorized";

        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        return pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-"); // Tránh nhiều dấu gạch ngang liên tiếp
    }

    private String getFileExtension(String filename) {
        return (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf("."))
                : ""; // Hoặc mặc định ".jpg" nếu muốn ép kiểu
    }

    // ==================== VALIDATION & UTILS ====================

    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return false;
        try {
            // Xử lý trường hợp URL bắt đầu bằng "/"
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(baseUploadDir, relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Path loadFile(String fileUrl) {
        return Paths.get(baseUploadDir, fileUrl);
    }

    private void validateImage(MultipartFile file) throws IOException {
        if (!isValidImage(file)) {
            throw new IOException("Invalid image file. Must be JPG, PNG, GIF, or WEBP and under 5MB");
        }
    }

    private void validateBookAsset(MultipartFile file) throws IOException {
        if (!isValidPDF(file) && !isValidEPUB(file)) {
            throw new IOException("Invalid book file. Must be PDF or EPUB and under 100MB");
        }
    }

    @Override
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        if (file.getSize() > MAX_IMAGE_SIZE) return false;

        String contentType = file.getContentType();
        if (contentType == null) return false;

        for (String allowedType : ALLOWED_IMAGE_CONTENT_TYPES) {
            if (contentType.equals(allowedType)) return true;
        }
        return false;
    }

    @Override
    public boolean isValidPDF(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        if (file.getSize() > MAX_BOOK_SIZE) return false;

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        return "application/pdf".equals(contentType) &&
                filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public boolean isValidEPUB(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        if (file.getSize() > MAX_BOOK_SIZE) return false;

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        return (contentType != null &&
                (contentType.equals("application/epub+zip") || contentType.equals("application/epub"))) &&
                filename != null && filename.toLowerCase().endsWith(".epub");
    }

    @Override
    public long getFileSize(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return -1;
        try {
            Path filePath = Paths.get(baseUploadDir, fileUrl);
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (IOException e) {
            return -1;
        }
        return -1;
    }
}