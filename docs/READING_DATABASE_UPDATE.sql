-- =====================================================
-- SQL SCRIPT: UPDATE DATABASE FOR READING FEATURE
-- Ngày tạo: 13/12/2024
-- Mục đích: Đảm bảo database có đầy đủ structure cho chức năng đọc sách
-- =====================================================

-- 1. Kiểm tra và tạo bảng reading_progress (nếu chưa có)
-- =====================================================

CREATE TABLE IF NOT EXISTS reading_progress (
    progress_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    book_asset_id VARCHAR(50),
    last_read_location VARCHAR(500) COMMENT 'Page number (PDF) hoặc CFI location (EPUB)',
    progress_percentage FLOAT DEFAULT 0.0 COMMENT 'Phần trăm tiến độ (0-100)',
    is_completed BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    access_type ENUM('PURCHASED', 'SUBSCRIPTION', 'FREE') DEFAULT 'FREE',
    last_read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_rp_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_asset FOREIGN KEY (book_asset_id) REFERENCES bookassets(book_asset_id) ON DELETE SET NULL,

    -- Unique constraint: mỗi user chỉ có 1 progress cho 1 book
    CONSTRAINT user_book_unique UNIQUE KEY (user_id, book_id),

    -- Indexes
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_last_read_at (last_read_at),
    INDEX idx_is_completed (is_completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Lưu tiến độ đọc sách của user';

-- 2. Kiểm tra và update bảng bookassets
-- =====================================================

-- Check nếu bảng bookassets tồn tại
CREATE TABLE IF NOT EXISTS bookassets (
    book_asset_id VARCHAR(50) PRIMARY KEY,
    book_id VARCHAR(50) NOT NULL,
    file_type ENUM('PDF', 'EPUB') NOT NULL COMMENT 'Loại file',
    file_url VARCHAR(500) NOT NULL COMMENT 'Đường dẫn relative từ thư mục source',
    file_size BIGINT COMMENT 'Kích thước file (bytes)',
    preview_url VARCHAR(500) COMMENT 'URL preview',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_ba_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,

    -- Indexes
    INDEX idx_book_id (book_id),
    INDEX idx_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='File assets của sách (PDF, EPUB)';

-- 3. Sample data cho testing
-- =====================================================

-- Insert sample reading progress (nếu cần test)
-- Uncomment để sử dụng
/*
INSERT INTO reading_progress (
    progress_id,
    user_id,
    book_id,
    last_read_location,
    progress_percentage,
    is_completed,
    access_type,
    last_read_at,
    created_at
) VALUES (
    UUID(),
    'user_id_here',
    'book_id_here',
    '1',
    0.0,
    FALSE,
    'FREE',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    last_read_at = NOW();
*/

-- 4. Queries hữu ích để kiểm tra dữ liệu
-- =====================================================

-- Check reading progress của tất cả users
-- SELECT
--     rp.progress_id,
--     u.email as user_email,
--     b.title as book_title,
--     rp.progress_percentage,
--     rp.is_completed,
--     rp.last_read_at
-- FROM reading_progress rp
-- JOIN users u ON rp.user_id = u.user_id
-- JOIN books b ON rp.book_id = b.book_id
-- ORDER BY rp.last_read_at DESC;

-- Check sách đang đọc (chưa hoàn thành)
-- SELECT
--     b.title,
--     rp.progress_percentage,
--     rp.last_read_at
-- FROM reading_progress rp
-- JOIN books b ON rp.book_id = b.book_id
-- WHERE rp.user_id = 'your_user_id'
-- AND rp.is_completed = FALSE
-- ORDER BY rp.last_read_at DESC;

-- Check sách đã đọc xong
-- SELECT
--     b.title,
--     rp.last_read_at as completed_at
-- FROM reading_progress rp
-- JOIN books b ON rp.book_id = b.book_id
-- WHERE rp.user_id = 'your_user_id'
-- AND rp.is_completed = TRUE
-- ORDER BY rp.last_read_at DESC;

-- Check book assets
-- SELECT
--     ba.book_asset_id,
--     b.title,
--     ba.file_type,
--     ba.file_url,
--     ba.file_size / 1024 / 1024 as size_mb
-- FROM bookassets ba
-- JOIN books b ON ba.book_id = b.book_id
-- ORDER BY ba.created_at DESC;

-- 5. Maintenance queries
-- =====================================================

-- Update progress percentage cho các record cũ
-- UPDATE reading_progress
-- SET progress_percentage = 0.0
-- WHERE progress_percentage IS NULL;

-- Đánh dấu completed cho sách đọc > 99%
-- UPDATE reading_progress
-- SET is_completed = TRUE
-- WHERE progress_percentage >= 99.0;

-- Cleanup old progress (quá 1 năm không đọc)
-- DELETE FROM reading_progress
-- WHERE last_read_at < DATE_SUB(NOW(), INTERVAL 1 YEAR)
-- AND is_completed = FALSE;

-- 6. Statistics queries
-- =====================================================

-- Thống kê reading progress theo user
-- SELECT
--     u.email,
--     COUNT(*) as total_books,
--     SUM(CASE WHEN rp.is_completed THEN 1 ELSE 0 END) as completed_books,
--     AVG(rp.progress_percentage) as avg_progress
-- FROM users u
-- LEFT JOIN reading_progress rp ON u.user_id = rp.user_id
-- GROUP BY u.user_id
-- ORDER BY completed_books DESC;

-- Top sách được đọc nhiều nhất
-- SELECT
--     b.title,
--     COUNT(*) as readers_count,
--     AVG(rp.progress_percentage) as avg_progress
-- FROM books b
-- JOIN reading_progress rp ON b.book_id = rp.book_id
-- GROUP BY b.book_id
-- ORDER BY readers_count DESC
-- LIMIT 10;

-- Reading activity hôm nay
-- SELECT
--     u.email,
--     b.title,
--     rp.progress_percentage,
--     rp.last_read_at
-- FROM reading_progress rp
-- JOIN users u ON rp.user_id = u.user_id
-- JOIN books b ON rp.book_id = b.book_id
-- WHERE DATE(rp.last_read_at) = CURDATE()
-- ORDER BY rp.last_read_at DESC;

-- 7. Index optimization
-- =====================================================

-- Check indexes
-- SHOW INDEX FROM reading_progress;

-- Tạo composite index nếu cần
-- ALTER TABLE reading_progress
-- ADD INDEX idx_user_book (user_id, book_id);

-- ALTER TABLE reading_progress
-- ADD INDEX idx_user_completed (user_id, is_completed);

-- 8. Backup trước khi update
-- =====================================================

-- Backup reading_progress
-- CREATE TABLE reading_progress_backup AS
-- SELECT * FROM reading_progress;

-- Restore from backup
-- INSERT INTO reading_progress
-- SELECT * FROM reading_progress_backup
-- ON DUPLICATE KEY UPDATE
--     last_read_location = VALUES(last_read_location),
--     progress_percentage = VALUES(progress_percentage),
--     last_read_at = VALUES(last_read_at);

-- =====================================================
-- END OF SCRIPT
-- =====================================================

-- Verification queries
SELECT 'Reading Progress Table' as check_name, COUNT(*) as record_count FROM reading_progress
UNION ALL
SELECT 'Book Assets Table', COUNT(*) FROM bookassets
UNION ALL
SELECT 'Books with Assets', COUNT(DISTINCT b.book_id)
FROM books b
JOIN bookassets ba ON b.book_id = ba.book_id;

-- Done!
SELECT '✅ Database update completed!' as status;

