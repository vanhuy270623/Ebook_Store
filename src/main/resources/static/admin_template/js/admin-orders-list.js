/**
 * Admin Orders List - JavaScript
 * Handles order listing with DataTable
 */

$(document).ready(function() {
    console.log('Admin Orders List JS loaded');

    // Initialize DataTables
    if ($.fn.DataTable) {
        $('#ordersTable').DataTable({
            "paging": true,
            "lengthChange": true,
            "searching": false, // ✅ TẮT ô tìm kiếm trong bảng (dùng form tìm kiếm phía trên)
            "ordering": true,
            "info": true,
            "autoWidth": false,
            "pageLength": 10,
            "language": {
                "lengthMenu": "Hiển thị _MENU_ đơn hàng mỗi trang",
                "zeroRecords": "Không tìm thấy đơn hàng nào",
                "info": "Hiển thị trang _PAGE_ / _PAGES_",
                "infoEmpty": "Không có đơn hàng",
                "infoFiltered": "(lọc từ _MAX_ đơn hàng)",
                "paginate": {
                    "first": "Đầu",
                    "last": "Cuối",
                    "next": "Sau",
                    "previous": "Trước"
                }
            },
            "order": [[6, "desc"]], // Sort by created date descending
            "columnDefs": [
                { "orderable": false, "targets": [7] } // Disable sorting for actions column
            ]
        });
    }
});

