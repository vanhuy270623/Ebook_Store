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
import java.util.UUID;

@Service
@Transactional
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String baseUploadDir;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_BOOK_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final String[] ALLOWED_IMAGE_CONTENT_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    @Override
    public String storeFile(MultipartFile file, String subdirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(baseUploadDir, subdirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = UUID.randomUUID().toString() + extension;

        // Store file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path
        return subdirectory + "/" + filename;
    }

    @Override
    public String storeCoverImage(MultipartFile file) throws IOException {
        validateImage(file);
        return storeFile(file, "books/covers");
    }

    @Override
    public String storeBookAsset(MultipartFile file) throws IOException {
        validateBookAsset(file);
        return storeFile(file, "books/assets");
    }

    @Override
    public String storeAuthorAvatar(MultipartFile file) throws IOException {
        validateImage(file);
        return storeFile(file, "authors/avatars");
    }

    @Override
    public String storeBannerImage(MultipartFile file) throws IOException {
        validateImage(file);
        return storeFile(file, "banners");
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            Path filePath = Paths.get(baseUploadDir, fileUrl);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Path loadFile(String fileUrl) {
        return Paths.get(baseUploadDir, fileUrl);
    }

    @Override
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Check file size
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return false;
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        for (String allowedType : ALLOWED_IMAGE_CONTENT_TYPES) {
            if (contentType.equals(allowedType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isValidPDF(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Check file size
        if (file.getSize() > MAX_BOOK_SIZE) {
            return false;
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        // Check extension
        String filename = file.getOriginalFilename();
        return contentType.equals("application/pdf") &&
               filename != null &&
               filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public boolean isValidEPUB(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Check file size
        if (file.getSize() > MAX_BOOK_SIZE) {
            return false;
        }

        // Check content type and extension
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        return (contentType != null &&
                (contentType.equals("application/epub+zip") ||
                 contentType.equals("application/epub"))) &&
               filename != null &&
               filename.toLowerCase().endsWith(".epub");
    }

    @Override
    public long getFileSize(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return -1;
        }

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

    // Private helper methods

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
}

