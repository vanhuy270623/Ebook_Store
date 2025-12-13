/**
 * Admin Books List Page JavaScript
 * Handles DataTable initialization and book management operations
 */
$(document).ready(function() {
    // Khởi tạo DataTable
    var table = $('#booksTable').DataTable({
        "paging": true,
        "lengthChange": true,
        "searching": false,
        "ordering": true,
        "info": true,
        "autoWidth": false,
        "pageLength": 25,
        "order": [[7, 'desc']], // Sắp xếp mặc định theo ngày tạo giảm dần
        "columnDefs": [
            {
                "orderable": false,
                "targets": [0, 1, 2, 3, 4, 6, 8] // Tắt sắp xếp các cột này
            },
            {
                "orderable": true,
                "targets": [5, 7] // Chỉ bật sắp xếp cho Đánh giá và Ngày tạo
            }
        ],
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.25/i18n/Vietnamese.json"
        }
    });

    // Debug: Kiểm tra xem các class đã được apply chưa
    console.log('DataTable initialized');
    $('#booksTable thead th').each(function(index) {
        console.log('Column ' + index + ' classes:', $(this).attr('class'));
    });

    // 1. Bắt sự kiện cho nút XÓA (.btn-delete-book)
    $(document).on('click', '.btn-delete-book', function() {
        var bookId = $(this).data('book-id');
        deleteBook(bookId);
    });

    // 2. Bắt sự kiện cho nút QUẢN LÝ FILE (.btn-manage-assets)
    $(document).on('click', '.btn-manage-assets', function() {
        var bookId = $(this).data('book-id');
        manageBookAssets(bookId);
    });
});

// Hàm xử lý quản lý file (Placeholder)
function manageBookAssets(bookId) {
    alert('Chức năng quản lý file sách sẽ được phát triển sau.\nBook ID: ' + bookId);
}

// Hàm xử lý xóa sách
function deleteBook(bookId) {
    if(confirm('Bạn có chắc chắn muốn xóa sách này?\nHành động này không thể hoàn tác!')) {
        // Lấy CSRF token (nếu dùng Spring Security mặc định)
        var token = getCookie('XSRF-TOKEN');

        $.ajax({
            url: '/admin/books/delete/' + bookId,
            type: 'POST',
            xhrFields: {
                withCredentials: true
            },
            headers: {
                'X-XSRF-TOKEN': token,
                'X-Requested-With': 'XMLHttpRequest'
            },
            success: function(response) {
                // Xử lý khi server trả về trang Login HTML (hết session)
                if(typeof response === 'string' && response.includes('<!DOCTYPE html>')) {
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
                    window.location.href = '/auth/login';
                    return;
                }

                // Parse JSON nếu cần
                if(typeof response === 'string') {
                    try {
                        response = JSON.parse(response);
                    } catch(e) {
                        console.error("Lỗi parse JSON", e);
                    }
                }

                if(response && response.success) {
                    alert(response.message);
                    location.reload();
                } else {
                    alert('Lỗi: ' + (response && response.message ? response.message : 'Có lỗi xảy ra'));
                }
            },
            error: function(xhr, status, error) {
                if(xhr.status === 401) {
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
                    window.location.href = '/auth/login';
                    return;
                }

                var errorMessage = 'Có lỗi xảy ra khi xóa sách!';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                } else if (xhr.responseText) {
                    try {
                        var json = JSON.parse(xhr.responseText);
                        if(json.message) errorMessage = json.message;
                    } catch(e) {}
                }

                alert(errorMessage);
            }
        });
    }
}

// Helper function to get cookie value
function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length == 2) return parts.pop().split(";").shift();
    return null;
}

