/**
 * Admin Orders List Page JavaScript
 * Handles DataTable initialization with sorting
 */
$(function () {
    $('#ordersTable').DataTable({
        'paging': true,
        'lengthChange': true,
        'searching': true,
        'ordering': true,
        'info': true,
        'autoWidth': false,
        'pageLength': 25,
        'order': [[6, 'desc']], // Sắp xếp mặc định theo ngày tạo giảm dần
        'columnDefs': [
            {
                'orderable': false,
                'targets': [0, 1, 2, 4, 5, 7] // Tắt sắp xếp: Mã đơn, Khách hàng, Loại, Phương thức, Trạng thái, Thao tác
            },
            {
                'orderable': true,
                'targets': [3, 6] // Chỉ bật sắp xếp cho cột "Tổng tiền" (3) và "Ngày tạo" (6)
            }
        ],
        'language': {
            'url': '//cdn.datatables.net/plug-ins/1.10.24/i18n/Vietnamese.json'
        }
    });

    // Debug: Kiểm tra xem các class đã được apply chưa
    console.log('DataTable initialized - sorting enabled for columns 3 (Tổng tiền) and 6 (Ngày tạo)');
    $('#ordersTable thead th').each(function(index) {
        console.log('Column ' + index + ' classes:', $(this).attr('class'));
    });
});

