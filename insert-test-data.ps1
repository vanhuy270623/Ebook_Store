# PowerShell script để insert test data vào MySQL
# Chạy: .\insert-test-data.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INSERT TEST DATA CHO DOWNLOAD FLOW" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# MySQL connection info
$mysqlPath = "C:\xampp\mysql\bin\mysql.exe"  # Đường dẫn MySQL (điều chỉnh nếu cần)
$dbName = "ebook_store"
$username = "root"
$password = ""  # Thay đổi nếu có password

# Check MySQL exists
if (-not (Test-Path $mysqlPath)) {
    Write-Host "❌ Không tìm thấy MySQL tại: $mysqlPath" -ForegroundColor Red
    Write-Host "Vui lòng cập nhật đường dẫn MySQL trong script" -ForegroundColor Yellow
    exit
}

# SQL commands
$sqlCommands = @"
-- Insert test orders
INSERT INTO orders (order_id, user_id, subscription_id, order_type, total_amount, payment_status, payment_method, transaction_id, start_date, end_date, created_at) VALUES
('ORDER_TEST_001', 'user_normal_01', NULL, 'BOOK', 120000.00, 'COMPLETED', 'VNPAY', 'TX_TEST_001', NULL, NULL, NOW()),
('ORDER_TEST_002', 'user_normal_01', NULL, 'BOOK', 90000.00, 'COMPLETED', 'VNPAY', 'TX_TEST_002', NULL, NULL, NOW()),
('ORDER_TEST_003', 'user_normal_01', NULL, 'BOOK', 75000.00, 'PAID', 'VNPAY', 'TX_TEST_003', NULL, NULL, NOW());

-- Insert order items
INSERT INTO order_items (order_item_id, order_id, book_id, price_at_purchase) VALUES
('OI_TEST_001', 'ORDER_TEST_001', 'book_02', 120000.00),
('OI_TEST_002', 'ORDER_TEST_002', 'book_03', 90000.00),
('OI_TEST_003', 'ORDER_TEST_003', 'book_07', 75000.00);
"@

Write-Host "Đang kết nối MySQL..." -ForegroundColor Yellow

try {
    # Execute SQL
    $sqlCommands | & $mysqlPath -u $username -p$password $dbName

    Write-Host ""
    Write-Host "✅ INSERT TEST DATA THÀNH CÔNG!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Đã tạo:" -ForegroundColor Cyan
    Write-Host "  - 3 orders (ORDER_TEST_001, 002, 003)" -ForegroundColor White
    Write-Host "  - 3 order_items (book_02, book_03, book_07)" -ForegroundColor White
    Write-Host ""
    Write-Host "👤 User user_normal_01 giờ có thể tải:" -ForegroundColor Cyan
    Write-Host "  ✅ book_02 (Đắc Nhân Tâm)" -ForegroundColor Green
    Write-Host "  ✅ book_03 (Mắt biếc)" -ForegroundColor Green
    Write-Host "  ✅ book_07 (Dế Mèn)" -ForegroundColor Green
    Write-Host ""
    Write-Host "🧪 Test tại: http://localhost:2706/books/view/book_02" -ForegroundColor Yellow
    Write-Host ""

} catch {
    Write-Host ""
    Write-Host "❌ LỖI KHI CHẠY SQL!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Hướng dẫn khắc phục:" -ForegroundColor Yellow
    Write-Host "  1. Kiểm tra MySQL đã chạy chưa (XAMPP/WAMP)" -ForegroundColor White
    Write-Host "  2. Kiểm tra username/password trong script" -ForegroundColor White
    Write-Host "  3. Hoặc chạy SQL thủ công trong phpMyAdmin/Workbench" -ForegroundColor White
    Write-Host ""
}

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

