-- =====================================================
-- DEBUG SCRIPT: Kiểm tra dữ liệu cho chức năng đọc sách
-- =====================================================

USE ebook_store;

-- 1. Kiểm tra bảng reading_progress tồn tại
SHOW TABLES LIKE 'reading_progress';

-- 2. Kiểm tra structure của bảng
DESCRIBE reading_progress;

-- 3. Kiểm tra bảng bookassets
DESCRIBE bookassets;

-- 4. Kiểm tra có sách nào có assets không
SELECT
    b.book_id,
    b.title,
    ba.book_asset_id,
    ba.file_type,
    ba.file_url,
    ba.file_size
FROM books b
LEFT JOIN bookassets ba ON b.book_id = ba.book_id
WHERE ba.book_asset_id IS NOT NULL
LIMIT 10;

-- 5. Kiểm tra số lượng
SELECT
    'Books' as table_name,
    COUNT(*) as count
FROM books
UNION ALL
SELECT
    'BookAssets',
    COUNT(*)
FROM bookassets
UNION ALL
SELECT
    'Reading Progress',
    COUNT(*)
FROM reading_progress;

-- 6. Kiểm tra có book nào có file PDF/EPUB không
SELECT
    b.book_id,
    b.title,
    ba.file_type,
    ba.file_url,
    LENGTH(ba.file_url) as url_length,
    ba.file_size / 1024 / 1024 as size_mb
FROM books b
JOIN bookassets ba ON b.book_id = ba.book_id
WHERE ba.file_type IN ('PDF', 'EPUB')
ORDER BY b.book_id
LIMIT 20;

-- 7. Kiểm tra users
SELECT
    user_id,
    email,
    full_name,
    role_id
FROM users
WHERE is_active = 1
LIMIT 5;

-- 8. Sample query để test openBook logic
SET @test_book_id = (SELECT book_id FROM books LIMIT 1);
SET @test_user_id = (SELECT user_id FROM users WHERE is_active = 1 LIMIT 1);

SELECT
    @test_book_id as test_book_id,
    @test_user_id as test_user_id;

-- Check book exists
SELECT * FROM books WHERE book_id = @test_book_id;

-- Check assets exist
SELECT * FROM bookassets WHERE book_id = @test_book_id;

-- Check reading progress exists
SELECT * FROM reading_progress
WHERE user_id = @test_user_id
AND book_id = @test_book_id;

-- =====================================================
-- Nếu không có dữ liệu, chạy các INSERT sau:
-- =====================================================

-- Tạo sample book asset nếu chưa có
/*
INSERT INTO bookassets (
    book_asset_id,
    book_id,
    file_type,
    file_url,
    file_size,
    created_at
) VALUES (
    CONCAT('asset_test_', FLOOR(RAND() * 1000)),
    (SELECT book_id FROM books LIMIT 1),
    'PDF',
    'kienthuc-hocthuat/sample.pdf',
    5242880,
    NOW()
);
*/

-- Kiểm tra foreign keys
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'ebook_store'
AND TABLE_NAME IN ('reading_progress', 'bookassets')
AND REFERENCED_TABLE_NAME IS NOT NULL;

