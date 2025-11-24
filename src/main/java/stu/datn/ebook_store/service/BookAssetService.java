package stu.datn.ebook_store.service;

import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.entity.BookAsset;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing book assets (PDF, EPUB files)
 */
public interface BookAssetService {

    /**
     * Get all book assets for a specific book
     * @param bookId The book ID
     * @return List of book assets
     */
    List<BookAsset> getAssetsByBookId(String bookId);

    /**
     * Get a book asset by ID
     * @param assetId The asset ID
     * @return Optional containing the book asset if found
     */
    Optional<BookAsset> getAssetById(String assetId);

    /**
     * Create a new book asset
     * @param bookId The book ID
     * @param file The file to upload
     * @param fileType The file type (PDF or EPUB)
     * @return The created book asset
     * @throws IOException if file upload fails
     */
    BookAsset createAsset(String bookId, MultipartFile file, BookAsset.FileType fileType) throws IOException;

    /**
     * Update an existing book asset
     * @param assetId The asset ID
     * @param file The new file (optional, can be null to keep existing file)
     * @param previewUrl The preview URL (optional)
     * @return The updated book asset
     * @throws IOException if file upload fails
     */
    BookAsset updateAsset(String assetId, MultipartFile file, String previewUrl) throws IOException;

    /**
     * Delete a book asset
     * @param assetId The asset ID
     * @return true if deleted successfully
     */
    boolean deleteAsset(String assetId);

    /**
     * Get book asset by book ID and file type
     * @param bookId The book ID
     * @param fileType The file type
     * @return Optional containing the book asset if found
     */
    Optional<BookAsset> getAssetByBookIdAndFileType(String bookId, BookAsset.FileType fileType);

    /**
     * Check if a book has assets
     * @param bookId The book ID
     * @return true if the book has at least one asset
     */
    boolean hasAssets(String bookId);

    /**
     * Get total file size of all assets for a book
     * @param bookId The book ID
     * @return Total file size in bytes
     */
    long getTotalAssetSize(String bookId);
}

