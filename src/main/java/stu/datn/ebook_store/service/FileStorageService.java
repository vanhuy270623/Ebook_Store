package stu.datn.ebook_store.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Store a generic file (used for thumbnails, misc assets)
     */
    String storeFile(MultipartFile file, String subdirectory) throws IOException;

    String storeBookAsset(MultipartFile file) throws IOException;

    /**
     * Store a book source file (PDF or EPUB)
     * Path: book_asset/source/{categorySlug}/{filename}
     */
    String storeBookSource(MultipartFile file, String categorySlug) throws IOException;

    /**
     * Hàm chung lưu ảnh cho Banner, Author, User Avatar dựa trên ID
     * Path: book_asset/image/{type}/{entityId}.{ext}
     * @param file File ảnh
     * @param type Loại đối tượng: "banners", "authors", "avatars"
     * @param entityId ID của đối tượng (dùng làm tên file)
     */
    String storeEntityImage(MultipartFile file, String type, String entityId) throws IOException;

    /**
     * Hàm riêng lưu ảnh bìa sách theo danh mục và tên sách
     * Path: book_asset/image/covers/{categorySlug}/{bookTitleSlug}.{ext}
     * @param file File ảnh bìa
     * @param categoryName Tên danh mục (để tạo thư mục con)
     * @param bookTitle Tên sách (để đặt tên file)
     */
    String storeBookCover(MultipartFile file, String categoryName, String bookTitle) throws IOException;

    boolean deleteFile(String fileUrl);
    Path loadFile(String fileUrl);

    // Validation methods
    boolean isValidImage(MultipartFile file);
    boolean isValidPDF(MultipartFile file);
    boolean isValidEPUB(MultipartFile file);
    long getFileSize(String fileUrl);
}