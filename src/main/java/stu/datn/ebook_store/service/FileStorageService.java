package stu.datn.ebook_store.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Service interface for file storage operations
 * Handles upload, download, and deletion of files (Images, PDFs, EPUBs)
 */
public interface FileStorageService {

    /**
     * Store a file in the file system
     * @param file The file to store
     * @param subdirectory The subdirectory to store the file (e.g., "books", "covers", "avatars")
     * @return The relative path to the stored file
     * @throws IOException if file storage fails
     */
    String storeFile(MultipartFile file, String subdirectory) throws IOException;

    /**
     * Store a book cover image
     * @param file The image file
     * @return The relative path to the stored image
     * @throws IOException if file storage fails
     */
    String storeCoverImage(MultipartFile file) throws IOException;

    /**
     * Store a book asset (PDF or EPUB)
     * @param file The book file
     * @return The relative path to the stored file
     * @throws IOException if file storage fails
     */
    String storeBookAsset(MultipartFile file) throws IOException;

    /**
     * Store an author avatar image
     * @param file The avatar image
     * @return The relative path to the stored image
     * @throws IOException if file storage fails
     */
    String storeAuthorAvatar(MultipartFile file) throws IOException;

    /**
     * Store a banner image
     * @param file The banner image
     * @return The relative path to the stored image
     * @throws IOException if file storage fails
     */
    String storeBannerImage(MultipartFile file) throws IOException;

    /**
     * Delete a file from the file system
     * @param fileUrl The relative path to the file
     * @return true if file was deleted, false otherwise
     */
    boolean deleteFile(String fileUrl);

    /**
     * Load a file as a Path
     * @param fileUrl The relative path to the file
     * @return The Path to the file
     */
    Path loadFile(String fileUrl);

    /**
     * Validate if the file is an image
     * @param file The file to validate
     * @return true if the file is a valid image
     */
    boolean isValidImage(MultipartFile file);

    /**
     * Validate if the file is a PDF
     * @param file The file to validate
     * @return true if the file is a valid PDF
     */
    boolean isValidPDF(MultipartFile file);

    /**
     * Validate if the file is an EPUB
     * @param file The file to validate
     * @return true if the file is a valid EPUB
     */
    boolean isValidEPUB(MultipartFile file);

    /**
     * Get file size in bytes
     * @param fileUrl The relative path to the file
     * @return The file size in bytes, or -1 if file not found
     */
    long getFileSize(String fileUrl);
}

