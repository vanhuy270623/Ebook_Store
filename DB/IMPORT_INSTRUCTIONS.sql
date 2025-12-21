-- =============================================
-- QUICK FIX: Import Database với Banner Carousel
-- File: ebook_store (8).sql đã được cập nhật
-- =============================================

-- BƯỚC 1: Drop database cũ và tạo mới
DROP DATABASE IF EXISTS ebook_store;
CREATE DATABASE ebook_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ebook_store;

-- BƯỚC 2: Source file SQL đã sửa
-- Trong MySQL Workbench: File → Run SQL Script → Chọn ebook_store (8).sql

-- HOẶC trong command line:
-- mysql -u root -p < "C:\Projects\Ebook_Store\DB\ebook_store (8).sql"

-- BƯỚC 3: Verify banners
SELECT
    banner_id,
    title,
    position,
    display_order,
    is_active,
    start_date,
    end_date,
    created_at,
    updated_at,
    CASE
        WHEN is_active = 1
        AND (start_date IS NULL OR start_date <= NOW())
        AND (end_date IS NULL OR end_date >= NOW())
        THEN '✅ VISIBLE'
        ELSE '❌ HIDDEN'
    END as status
FROM banner
WHERE position = 'HOME'
ORDER BY display_order ASC;

-- Kết quả mong đợi: 3 banners với status '✅ VISIBLE'

-- BƯỚC 4: Test query giống BannerRepository
SELECT
    banner_id,
    title,
    image_url,
    target_url,
    display_order,
    '✅ Will be displayed on homepage' as note
FROM banner
WHERE is_active = 1
  AND position = 'HOME'
  AND (start_date IS NULL OR start_date <= NOW())
  AND (end_date IS NULL OR end_date >= NOW())
ORDER BY display_order ASC, created_at DESC
LIMIT 7;

-- BƯỚC 5: Summary
SELECT
    '✅ Database ready!' as status,
    COUNT(*) as total_banners,
    SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as active_banners,
    SUM(CASE WHEN position = 'HOME' THEN 1 ELSE 0 END) as home_banners
FROM banner;

-- =============================================
-- HƯỚNG DẪN IMPORT:
-- =============================================

-- Option 1: MySQL Workbench (Khuyến nghị)
-- 1. Mở MySQL Workbench
-- 2. Kết nối vào MySQL Server
-- 3. Menu: Server → Data Import
-- 4. Chọn "Import from Self-Contained File"
-- 5. Browse to: C:\Projects\Ebook_Store\DB\ebook_store (8).sql
-- 6. Default Target Schema: ebook_store
-- 7. Click "Start Import"
-- 8. Đợi hoàn tất

-- Option 2: Command Line
-- mysql -u root -p
-- DROP DATABASE IF EXISTS ebook_store;
-- CREATE DATABASE ebook_store;
-- USE ebook_store;
-- SOURCE C:/Projects/Ebook_Store/DB/ebook_store (8).sql;

-- Option 3: phpMyAdmin
-- 1. Truy cập http://localhost/phpmyadmin
-- 2. Chọn database "ebook_store" (hoặc tạo mới)
-- 3. Tab "Import"
-- 4. Choose File: Chọn ebook_store (8).sql
-- 5. Click "Go"

-- =============================================
-- SAU KHI IMPORT:
-- =============================================
-- 1. Chạy các query verify ở trên
-- 2. Restart Spring Boot application
-- 3. Test: http://localhost:8080/
-- 4. Banner carousel phải hiển thị!

SELECT '🎉 Ready to import! Follow the instructions above.' as message;

