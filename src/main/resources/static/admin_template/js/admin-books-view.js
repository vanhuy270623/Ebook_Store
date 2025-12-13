/**
 * Admin Books View Page JavaScript
 * Handles book deletion from view page
 */
$(document).ready(function() {
    $('.btn-delete-book').on('click', function() {
        console.log('Delete button clicked in view page');
        var bookId = $(this).data('book-id');
        console.log('Book ID:', bookId);
        deleteBook(bookId);
    });
});

function deleteBook(bookId) {
    console.log('deleteBook function called with ID:', bookId);
    if(confirm('Bạn có chắc chắn muốn xóa sách này?\nHành động này không thể hoàn tác!')) {
        console.log('User confirmed delete. Sending AJAX request...');

        // Get CSRF token from cookie
        var token = getCookie('XSRF-TOKEN');
        console.log('CSRF Token:', token);

        $.ajax({
            url: '/admin/books/delete/' + bookId,
            type: 'POST',
            dataType: 'json',
            xhrFields: {
                withCredentials: true
            },
            headers: {
                'X-XSRF-TOKEN': token,
                'X-Requested-With': 'XMLHttpRequest'
            },
            beforeSend: function() {
                console.log('Sending DELETE request to:', '/admin/books/delete/' + bookId);
            },
            success: function(response) {
                console.log('Success response:', response);

                // Check if response is HTML (login page redirect)
                if(typeof response === 'string' && response.includes('<!DOCTYPE html>')) {
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
                    window.location.href = '/auth/login';
                    return;
                }

                if(response.success) {
                    alert(response.message);
                    window.location.href = '/admin/books';
                } else {
                    alert('Lỗi: ' + (response.message || 'Có lỗi xảy ra khi xóa sách'));
                }
            },
            error: function(xhr, status, error) {
                console.error('Error occurred:', {xhr: xhr, status: status, error: error});
                console.error('Response Text:', xhr.responseText);
                console.error('Response JSON:', xhr.responseJSON);
                console.error('Status Code:', xhr.status);

                // Check for 401 Unauthorized (session expired)
                if(xhr.status === 401) {
                    var message = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!';
                    if(xhr.responseJSON && xhr.responseJSON.message) {
                        message = xhr.responseJSON.message;
                    }
                    alert(message);
                    window.location.href = '/auth/login';
                    return;
                }

                // Check if redirected to login page (HTML response)
                if(xhr.responseText && xhr.responseText.includes('<!DOCTYPE html>')) {
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
                    window.location.href = '/auth/login';
                    return;
                }

                var errorMessage = 'Có lỗi xảy ra khi xóa sách!';
                if(xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                } else if(xhr.status === 403) {
                    errorMessage = 'Lỗi bảo mật: Không có quyền thực hiện thao tác này!';
                }
                alert(errorMessage);
            }
        });
    } else {
        console.log('User cancelled delete');
    }
}

// Helper function to get cookie value
function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length == 2) return parts.pop().split(";").shift();
    return null;
}

