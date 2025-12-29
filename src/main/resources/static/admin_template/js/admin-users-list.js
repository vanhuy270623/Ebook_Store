/**
 * Admin Users List - JavaScript
 * Handles user listing with DataTable
 */

$(document).ready(function() {
    console.log('Admin Users List JS loaded');

    // Check if showing deleted users
    var showDeleted = $('input[name="showDeleted"]').is(':checked');
    var colCount = showDeleted ? 7 : 6;

    // Initialize DataTables
    if ($.fn.DataTable) {
        $('#usersTable').DataTable({
            "paging": true,
            "lengthChange": true,
            "searching": false, // ✅ TẮT ô tìm kiếm trong bảng (dùng form tìm kiếm phía trên)
            "ordering": true,
            "info": true,
            "autoWidth": false,
            "pageLength": 10,
            "language": {
                "lengthMenu": "Hiển thị _MENU_ người dùng mỗi trang",
                "zeroRecords": "Không tìm thấy người dùng nào",
                "info": "Hiển thị trang _PAGE_ / _PAGES_",
                "infoEmpty": "Không có người dùng",
                "infoFiltered": "(lọc từ _MAX_ người dùng)",
                "paginate": {
                    "first": "Đầu",
                    "last": "Cuối",
                    "next": "Sau",
                    "previous": "Trước"
                }
            },
            "order": [[4, "desc"]], // Sort by created date descending
            "columnDefs": [
                { "orderable": false, "targets": [0, colCount - 1] } // Disable sorting for avatar and actions
            ]
        });
    }
});

