/**
 * Admin Books List - JavaScript
 * Handles book management actions: delete, manage assets, DataTable
 */

$(document).ready(function() {
    console.log('Admin Books List JS loaded');

    // Initialize DataTables
    if ($.fn.DataTable) {
        $('#booksTable').DataTable({
            "paging": true,
            "lengthChange": true,
            "searching": false, // ✅ TẮT ô tìm kiếm trong bảng (dùng form tìm kiếm phía trên)
            "ordering": true,
            "info": true,
            "autoWidth": false,
            "pageLength": 10,
            "language": {
                "lengthMenu": "Hiển thị _MENU_ sách mỗi trang",
                "zeroRecords": "Không tìm thấy sách nào",
                "info": "Hiển thị trang _PAGE_ / _PAGES_",
                "infoEmpty": "Không có sách",
                "infoFiltered": "(lọc từ _MAX_ sách)",
                "paginate": {
                    "first": "Đầu",
                    "last": "Cuối",
                    "next": "Sau",
                    "previous": "Trước"
                }
            },
            "order": [[7, "desc"]], // Sort by created date descending
            "columnDefs": [
                { "orderable": false, "targets": [0, 8] } // Disable sorting for image and actions
            ]
        });
    }

    // Handle Manage Assets Button
    $('.btn-manage-assets').on('click', function(e) {
        e.preventDefault();
        var bookId = $(this).data('book-id');

        if (bookId) {
            window.location.href = '/admin/books/assets/' + bookId;
        } else {
            alert('Lỗi: Không tìm thấy ID sách');
        }
    });

    // Handle Delete Book Button
    $('.btn-delete-book').on('click', function(e) {
        e.preventDefault();
        var bookId = $(this).data('book-id');
        var $row = $(this).closest('tr');
        var bookTitle = $row.find('strong').first().text();

        if (!bookId) {
            alert('Lỗi: Không tìm thấy ID sách');
            return;
        }

        // Confirm deletion
        if (!confirm('Bạn có chắc chắn muốn xóa sách "' + bookTitle + '"?\n\nThao tác này không thể hoàn tác!')) {
            return;
        }

        // Get CSRF token
        var csrfToken = $('meta[name="_csrf"]').attr('content');
        var csrfHeader = $('meta[name="_csrf_header"]').attr('content');

        // Send delete request
        $.ajax({
            url: '/admin/books/delete/' + bookId,
            type: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            },
            success: function(response) {
                if (response.success) {
                    // Remove row from table
                    var table = $('#booksTable').DataTable();
                    table.row($row).remove().draw();

                    // Update total count
                    var currentTotal = parseInt($('.box-title span').text());
                    $('.box-title span').text(currentTotal - 1);

                    // Show success message
                    showAlert('success', response.message || 'Xóa sách thành công!');
                } else {
                    showAlert('error', response.message || 'Không thể xóa sách');
                }
            },
            error: function(xhr) {
                var message = 'Lỗi khi xóa sách';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                showAlert('error', message);
            }
        });
    });

    /**
     * Show alert message
     */
    function showAlert(type, message) {
        var alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
        var icon = type === 'success' ? 'fa-check' : 'fa-ban';

        var alertHtml = '<div class="alert ' + alertClass + ' alert-dismissible">' +
            '<button type="button" class="close" data-dismiss="alert">×</button>' +
            '<h4><i class="icon fa ' + icon + '"></i> ' +
            (type === 'success' ? 'Thành công!' : 'Lỗi!') + '</h4>' +
            message +
            '</div>';

        // Prepend to content section
        $('.content').prepend(alertHtml);

        // Auto dismiss after 5 seconds
        setTimeout(function() {
            $('.alert').fadeOut(300, function() {
                $(this).remove();
            });
        }, 5000);

        // Scroll to top
        $('html, body').animate({ scrollTop: 0 }, 300);
    }
});

