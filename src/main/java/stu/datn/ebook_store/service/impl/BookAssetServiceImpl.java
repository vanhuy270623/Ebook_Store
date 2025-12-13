package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;
import stu.datn.ebook_store.repository.BookAssetRepository;
import stu.datn.ebook_store.repository.BookRepository;
import stu.datn.ebook_store.service.BookAssetService;
import stu.datn.ebook_store.service.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BookAssetServiceImpl implements BookAssetService {

    private final BookAssetRepository bookAssetRepository;
    private final BookRepository bookRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public BookAssetServiceImpl(BookAssetRepository bookAssetRepository,
                                BookRepository bookRepository,
                                FileStorageService fileStorageService) {
        this.bookAssetRepository = bookAssetRepository;
        this.bookRepository = bookRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookAsset> getAssetsByBookId(String bookId) {
        return bookAssetRepository.findByBook_BookId(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookAsset> getAssetById(String assetId) {
        return bookAssetRepository.findById(assetId);
    }

    @Override
    public BookAsset createAsset(String bookId, MultipartFile file, BookAsset.FileType fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Validate file type
        validateFileType(file, fileType);

        // Get book
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        // Upload file
        String fileUrl = fileStorageService.storeBookAsset(file);
        long fileSize = file.getSize();

        // Create book asset
        BookAsset bookAsset = new BookAsset();
        bookAsset.setBookAssetId(generateAssetId());
        bookAsset.setBook(book);
        bookAsset.setFileType(fileType);
        bookAsset.setFileUrl(fileUrl);
        bookAsset.setFileSize(fileSize);

        return bookAssetRepository.save(bookAsset);
    }

    @Override
    public BookAsset updateAsset(String assetId, MultipartFile file, String previewUrl) throws IOException {
        BookAsset bookAsset = bookAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Book asset not found with id: " + assetId));

        // Update file if provided
        if (file != null && !file.isEmpty()) {
            // Validate file type
            validateFileType(file, bookAsset.getFileType());

            // Delete old file
            if (bookAsset.getFileUrl() != null) {
                fileStorageService.deleteFile(bookAsset.getFileUrl());
            }

            // Upload new file
            String fileUrl = fileStorageService.storeBookAsset(file);
            long fileSize = file.getSize();

            bookAsset.setFileUrl(fileUrl);
            bookAsset.setFileSize(fileSize);
        }

        // Update preview URL if provided
        if (previewUrl != null) {
            bookAsset.setPreviewUrl(previewUrl);
        }

        return bookAssetRepository.save(bookAsset);
    }

    @Override
    public boolean deleteAsset(String assetId) {
        Optional<BookAsset> bookAssetOpt = bookAssetRepository.findById(assetId);
        if (bookAssetOpt.isEmpty()) {
            return false;
        }

        BookAsset bookAsset = bookAssetOpt.get();

        // Delete file from storage
        if (bookAsset.getFileUrl() != null) {
            fileStorageService.deleteFile(bookAsset.getFileUrl());
        }

        // Delete from database
        bookAssetRepository.delete(bookAsset);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookAsset> getAssetByBookIdAndFileType(String bookId, BookAsset.FileType fileType) {
        return bookAssetRepository.findByBook_BookIdAndFileType(bookId, fileType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAssets(String bookId) {
        return bookAssetRepository.existsByBook_BookId(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalAssetSize(String bookId) {
        List<BookAsset> assets = bookAssetRepository.findByBook_BookId(bookId);
        return assets.stream()
                .mapToLong(asset -> asset.getFileSize() != null ? asset.getFileSize() : 0L)
                .sum();
    }

    // Private helper methods

    private String generateAssetId() {
        return "BA" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
    }

    private void validateFileType(MultipartFile file, BookAsset.FileType fileType) throws IOException {
        switch (fileType) {
            case PDF:
                if (!fileStorageService.isValidPDF(file)) {
                    throw new IOException("Invalid PDF file");
                }
                break;
            case EPUB:
                if (!fileStorageService.isValidEPUB(file)) {
                    throw new IOException("Invalid EPUB file");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
}

