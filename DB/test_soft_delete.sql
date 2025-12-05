-- SQL Script để test tính năng Soft Delete
-- Date: December 4, 2025

USE ebook_store;

-- ============================================
-- 1. KIỂM TRA CẤU TRÚC BẢNG SAU KHI MIGRATION
-- ============================================
DESCRIBE users;
-- Kết quả mong đợi: Phải có cột 'deleted_at' với kiểu DATETIME, DEFAULT NULL

-- ============================================
-- 2. KIỂM TRA INDEXES
-- ============================================
SHOW INDEX FROM users WHERE Key_name IN ('idx_users_deleted_at', 'idx_users_active');
-- Kết quả mong đợi: 2 indexes đã được tạo

-- ============================================
-- 3. TẠO USER MẪU ĐỂ TEST (nếu chưa có)
-- ============================================
INSERT INTO users (user_id, role_id, username, email, password_hash, full_name, phone, is_active, is_verified)
VALUES
('user_test_01', 'role_user', 'testuser1', 'test1@test.com', '$2a$10$testpasswordhash1', 'Test User 1', '0900000001', 1, 1),
('user_test_02', 'role_user', 'testuser2', 'test2@test.com', '$2a$10$testpasswordhash2', 'Test User 2', '0900000002', 1, 1),
('user_test_03', 'role_user', 'testuser3', 'test3@test.com', '$2a$10$testpasswordhash3', 'Test User 3', '0900000003', 1, 0)
ON DUPLICATE KEY UPDATE username=username;

-- ============================================
-- 4. TEST SOFT DELETE
-- ============================================

-- 4.1. Xem user trước khi xóa
SELECT user_id, username, email, is_active, deleted_at
FROM users
WHERE user_id IN ('user_test_01', 'user_test_02', 'user_test_03');

-- 4.2. Thực hiện soft delete user_test_01
UPDATE users
SET deleted_at = NOW(), updated_at = NOW()
WHERE user_id = 'user_test_01';

-- 4.3. Kiểm tra kết quả soft delete
SELECT user_id, username, email, deleted_at
FROM users
WHERE user_id = 'user_test_01';
-- Kết quả mong đợi: deleted_at có giá trị timestamp

-- 4.4. Query chỉ lấy active users (deleted_at IS NULL)
SELECT user_id, username, email, deleted_at
FROM users
WHERE deleted_at IS NULL
  AND user_id LIKE 'user_test%';
-- Kết quả mong đợi: Chỉ có user_test_02 và user_test_03

-- 4.5. Query lấy tất cả users kể cả đã xóa
SELECT user_id, username, email, deleted_at,
       CASE WHEN deleted_at IS NULL THEN 'Active' ELSE 'Deleted' END AS status
FROM users
WHERE user_id LIKE 'user_test%'
ORDER BY deleted_at DESC;

-- ============================================
-- 5. TEST RESTORE
-- ============================================

-- 5.1. Khôi phục user_test_01
UPDATE users
SET deleted_at = NULL, updated_at = NOW()
WHERE user_id = 'user_test_01';

-- 5.2. Kiểm tra kết quả restore
SELECT user_id, username, email, deleted_at
FROM users
WHERE user_id = 'user_test_01';
-- Kết quả mong đợi: deleted_at = NULL

-- ============================================
-- 6. TEST AUTHENTICATION QUERIES
-- ============================================

-- 6.1. Soft delete user_test_02 để test
UPDATE users
SET deleted_at = NOW()
WHERE user_id = 'user_test_02';

-- 6.2. Query như trong authentication (chỉ lấy active users)
SELECT user_id, username, email, password_hash
FROM users
WHERE username = 'testuser2'
  AND deleted_at IS NULL;
-- Kết quả mong đợi: Không có kết quả (user đã bị xóa)

-- 6.3. Restore lại để tiếp tục test
UPDATE users
SET deleted_at = NULL
WHERE user_id = 'user_test_02';

-- ============================================
-- 7. TEST THỐNG KÊ
-- ============================================

-- 7.1. Đếm tổng số active users
SELECT COUNT(*) AS total_active_users
FROM users
WHERE deleted_at IS NULL;

-- 7.2. Đếm tổng số deleted users
SELECT COUNT(*) AS total_deleted_users
FROM users
WHERE deleted_at IS NOT NULL;

-- 7.3. Thống kê chi tiết
SELECT
    COUNT(*) AS total_users,
    SUM(CASE WHEN deleted_at IS NULL THEN 1 ELSE 0 END) AS active_users,
    SUM(CASE WHEN deleted_at IS NOT NULL THEN 1 ELSE 0 END) AS deleted_users,
    SUM(CASE WHEN deleted_at IS NULL AND is_active = 1 THEN 1 ELSE 0 END) AS active_and_enabled,
    SUM(CASE WHEN deleted_at IS NULL AND is_active = 0 THEN 1 ELSE 0 END) AS active_but_locked
FROM users;

-- ============================================
-- 8. TEST PERFORMANCE VỚI INDEX
-- ============================================

-- 8.1. Explain query để kiểm tra index được sử dụng
EXPLAIN SELECT * FROM users WHERE deleted_at IS NULL;
-- Kết quả mong đợi: Key = idx_users_deleted_at hoặc idx_users_active

EXPLAIN SELECT * FROM users WHERE deleted_at IS NULL AND is_active = 1;
-- Kết quả mong đợi: Key = idx_users_active

-- ============================================
-- 9. TEST FOREIGN KEY RELATIONSHIPS
-- ============================================

-- 9.1. Kiểm tra orders của user đã xóa
SELECT o.order_id, o.user_id, u.username, u.deleted_at
FROM orders o
JOIN users u ON o.user_id = u.user_id
WHERE u.deleted_at IS NOT NULL
LIMIT 5;

-- 9.2. Kiểm tra reviews của user đã xóa
SELECT r.review_id, r.user_id, u.username, u.deleted_at
FROM reviews r
JOIN users u ON r.user_id = u.user_id
WHERE u.deleted_at IS NOT NULL
LIMIT 5;

-- ============================================
-- 10. TEST UNIQUE CONSTRAINTS
-- ============================================

-- 10.1. Soft delete một user
UPDATE users SET deleted_at = NOW() WHERE user_id = 'user_test_03';

-- 10.2. Thử tạo user mới với email/username đã xóa (phải LỖI)
-- Uncomment để test (sẽ báo lỗi duplicate entry)
/*
INSERT INTO users (user_id, role_id, username, email, password_hash, full_name)
VALUES ('user_test_04', 'role_user', 'testuser3', 'test3@test.com', '$2a$10$hash', 'Test User 4');
*/
-- Kết quả mong đợi: Error 1062 - Duplicate entry

-- 10.3. Restore user
UPDATE users SET deleted_at = NULL WHERE user_id = 'user_test_03';

-- ============================================
-- 11. DANH SÁCH USERS ĐÃ XÓA (FOR ADMIN VIEW)
-- ============================================

SELECT
    user_id,
    username,
    email,
    full_name,
    CONCAT(r.role_name) AS role,
    deleted_at,
    TIMESTAMPDIFF(DAY, deleted_at, NOW()) AS days_since_deleted
FROM users u
LEFT JOIN roles r ON u.role_id = r.role_id
WHERE u.deleted_at IS NOT NULL
ORDER BY u.deleted_at DESC;

-- ============================================
-- 12. CLEANUP (XÓA DỮ LIỆU TEST)
-- ============================================

-- Uncomment để xóa dữ liệu test
/*
DELETE FROM users WHERE user_id IN ('user_test_01', 'user_test_02', 'user_test_03');
*/

-- ============================================
-- 13. KẾT LUẬN VÀ KIỂM TRA CUỐI CÙNG
-- ============================================

-- Kiểm tra tất cả users hiện tại
SELECT
    CASE
        WHEN deleted_at IS NULL THEN 'Active'
        ELSE 'Deleted'
    END AS status,
    COUNT(*) AS count
FROM users
GROUP BY status;

-- Hiển thị 5 users gần đây nhất (bao gồm cả deleted)
SELECT
    user_id,
    username,
    email,
    is_active,
    deleted_at,
    created_at,
    CASE
        WHEN deleted_at IS NOT NULL THEN 'DELETED'
        WHEN is_active = 0 THEN 'LOCKED'
        ELSE 'ACTIVE'
    END AS current_status
FROM users
ORDER BY created_at DESC
LIMIT 5;

-- ============================================
-- NOTES FOR TESTING
-- ============================================
/*
✅ Test Cases cần verify:
1. Soft delete user → deleted_at được set
2. Restore user → deleted_at = NULL
3. Active users query không bao gồm deleted
4. Authentication không cho phép login với deleted user
5. Không thể đăng ký lại với email/username đã xóa
6. Foreign keys không bị ảnh hưởng
7. Statistics queries đúng
8. Indexes được sử dụng đúng

⚠️ Important:
- Không bao giờ hard delete user trong production
- Luôn kiểm tra foreign keys trước khi hard delete
- Backup database trước khi chạy migration
- Test trên development environment trước
*/
