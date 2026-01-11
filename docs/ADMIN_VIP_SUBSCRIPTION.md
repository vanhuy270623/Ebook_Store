# Admin VIP Subscription Management

## Tổng quan
Hệ thống đã được cấu hình để đảm bảo tất cả admin đều có gói VIP subscription với thời hạn không giới hạn (100 năm).

## Tính năng được thêm

### 1. Tự động tạo VIP subscription cho admin mới
- Khi tạo admin mới thông qua Admin Panel, hệ thống sẽ tự động tạo VIP subscription
- VIP subscription có thời hạn 100 năm (được coi là vĩnh viễn)
- Admin có thể truy cập tất cả sách và tính năng premium

### 2. Cập nhật VIP subscription cho admin hiện tại
- **Tự động**: Khi khởi động ứng dụng, hệ thống sẽ tự động kiểm tra và tạo VIP subscription cho admin chưa có
- **Thủ công**: Admin có thể click nút "Cập nhật VIP cho tất cả Admin" trong Dashboard

### 3. Giao diện quản lý
- Thêm section "Quản lý Admin VIP" trong Admin Dashboard
- Nút để cập nhật VIP subscription cho tất cả admin
- Hiển thị thông báo thành công/lỗi

## Files được thêm/sửa đổi

### 1. Service Layer
- `UserService.java`: Thêm methods `getAllAdmins()`, `ensureAllAdminsHaveVipSubscription()`
- `UserServiceImpl.java`: 
  - Implement logic tạo VIP subscription cho admin
  - Method `createVipSubscriptionForAdmin()` - tạo VIP subscription với thời hạn 100 năm
  - Method `saveUser()` - tự động tạo VIP khi tạo admin mới

### 2. Repository Layer
- `UserRepository.java`: Thêm method `findActiveByRoleName()`
- `OrderRepository.java`: Thêm method `findActiveSubscriptionByUserIdAndPackageName()`

### 3. Controller Layer
- `AdminDashboardController.java`: Thêm endpoint `POST /admin/update-admin-vip`

### 4. Configuration
- `AdminVipInitializer.java`: ApplicationRunner tự động chạy khi khởi động app

### 5. Database
- `update_admin_vip.sql`: Script SQL để cập nhật admin hiện tại

### 6. Template
- `admin/dashboard.html`: Thêm section quản lý Admin VIP

## Cách sử dụng

### Cho admin hiện tại
1. **Cách 1 (Tự động)**: Restart ứng dụng, hệ thống sẽ tự động cập nhật
2. **Cách 2 (Thủ công)**: Vào Admin Dashboard → Click "Cập nhật VIP cho tất cả Admin"
3. **Cách 3 (Database)**: Chạy script `update_admin_vip.sql`

### Cho admin mới
- Admin mới sẽ tự động có VIP subscription khi được tạo

## Kiểm tra VIP subscription
```sql
-- Kiểm tra admin nào có VIP subscription
SELECT 
    u.user_id,
    u.username,
    r.role_name,
    s.package_name,
    o.start_date,
    o.end_date,
    CASE WHEN o.end_date > NOW() THEN 'Active' ELSE 'Expired' END as status
FROM users u
JOIN roles r ON u.role_id = r.role_id
LEFT JOIN orders o ON u.user_id = o.user_id AND o.order_type = 'SUBSCRIPTION' AND o.payment_status = 'COMPLETED'
LEFT JOIN subscriptions s ON o.subscription_id = s.subscription_id
WHERE r.role_name = 'ADMIN' AND u.deleted_at IS NULL
ORDER BY u.created_at;
```

## Ghi chú kỹ thuật
- VIP subscription cho admin có giá 0đ (miễn phí)
- Thời hạn 100 năm để đảm bảo "vĩnh viễn"
- Payment status: COMPLETED
- Payment method: BANK_TRANSFER
- Order ID format: `SUB_ADMIN_{user_id}_{timestamp}`
