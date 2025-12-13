/**
 * Admin Users List Page JavaScript
 * Handles DataTable initialization with sorting for date column
 */
$(document).ready(function() {
    // Lấy giá trị showDeleted từ URL params hoặc từ checkbox
    var urlParams = new URLSearchParams(window.location.search);
    var showDeleted = urlParams.get('showDeleted') === 'true';

    // Xác định các cột không được sắp xếp (tất cả trừ cột "Ngày tạo")
    var nonSortableColumns = [0, 1, 2, 3]; // Avatar, Thông tin, Email & Trạng thái, Vai trò
    if (showDeleted) {
        nonSortableColumns.push(5); // Trạng thái xóa
        nonSortableColumns.push(6); // Thao tác
    } else {
        nonSortableColumns.push(5); // Thao tác
    }

    $('#usersTable').DataTable({
        "paging": true,
        "lengthChange": true,
        "searching": false,
        "ordering": true,
        "info": true,
        "autoWidth": false,
        "pageLength": 25,
        "order": [[4, "desc"]], // Sắp xếp mặc định theo cột "Ngày tạo" (index 4) giảm dần
        "columnDefs": [
            {
                "orderable": false,
                "targets": nonSortableColumns // Tắt sắp xếp tất cả các cột trừ "Ngày tạo"
            },
            {
                "orderable": true,
                "targets": 4 // Chỉ bật sắp xếp cho cột "Ngày tạo" (index 4)
            }
        ],
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.25/i18n/Vietnamese.json"
        }
    });

    // Debug: Kiểm tra xem các class đã được apply chưa
    console.log('DataTable initialized - sorting enabled for column 4 (Ngày tạo)');
    $('#usersTable thead th').each(function(index) {
        console.log('Column ' + index + ' classes:', $(this).attr('class'));
    });
});

