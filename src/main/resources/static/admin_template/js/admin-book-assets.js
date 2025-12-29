/**
 * Admin Book Assets Management - JavaScript
 * Handles file upload and deletion for book assets
 */

$(document).ready(function() {
    console.log('Book Assets Management JS loaded');

    var bookId = $('#bookId').val();

    // Handle file upload
    $('#uploadAssetForm').on('submit', function(e) {
        e.preventDefault();

        var formData = new FormData(this);
        formData.append('bookId', bookId);

        var $submitBtn = $(this).find('button[type="submit"]');
        var originalText = $submitBtn.html();
        $submitBtn.prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang upload...');

        $.ajax({
            url: '/admin/books/assets/upload',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            headers: {
                'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
            },
            success: function(response) {
                if (response.success) {
                    showAlert('success', response.message);

                    // Reset form
                    $('#uploadAssetForm')[0].reset();
                    $('#filePreview').hide();

                    // Reload page to show new file
                    setTimeout(function() {
                        location.reload();
                    }, 1500);
                } else {
                    showAlert('error', response.message);
                }
            },
            error: function(xhr) {
                var message = 'Lỗi khi upload file';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                showAlert('error', message);
            },
            complete: function() {
                $submitBtn.prop('disabled', false).html(originalText);
            }
        });
    });

    // Preview file before upload
    $('#assetFile').on('change', function() {
        var file = this.files[0];
        if (file) {
            var fileSize = (file.size / 1024 / 1024).toFixed(2); // MB
            var fileName = file.name;
            var fileType = file.type;

            var previewHtml = '<div class="alert alert-info">' +
                '<strong>File đã chọn:</strong> ' + fileName +
                '<br><strong>Kích thước:</strong> ' + fileSize + ' MB' +
                '<br><strong>Loại:</strong> ' + fileType +
                '</div>';

            $('#filePreview').html(previewHtml).show();
        }
    });

    // Handle delete asset
    $('.btn-delete-asset').on('click', function(e) {
        e.preventDefault();

        var assetId = $(this).data('asset-id');
        var fileName = $(this).data('file-name');
        var $row = $(this).closest('tr');

        if (!assetId) {
            alert('Lỗi: Không tìm thấy ID file');
            return;
        }

        if (!confirm('Bạn có chắc chắn muốn xóa file "' + fileName + '"?\n\nThao tác này không thể hoàn tác!')) {
            return;
        }

        $.ajax({
            url: '/admin/books/assets/delete',
            type: 'POST',
            data: {
                assetId: assetId
            },
            headers: {
                'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
            },
            success: function(response) {
                if (response.success) {
                    showAlert('success', response.message);

                    // Remove row
                    $row.fadeOut(300, function() {
                        $(this).remove();

                        // Update total count
                        var currentTotal = parseInt($('.info-box-number').first().text());
                        $('.info-box-number').first().text(currentTotal - 1);

                        // Reload if no files left
                        if ($('.table-assets tbody tr').length === 0) {
                            setTimeout(function() {
                                location.reload();
                            }, 1000);
                        }
                    });
                } else {
                    showAlert('error', response.message);
                }
            },
            error: function(xhr) {
                var message = 'Lỗi khi xóa file';
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

        $('.content').prepend(alertHtml);

        // Auto dismiss after 5 seconds
        setTimeout(function() {
            $('.alert-dismissible').fadeOut(300, function() {
                $(this).remove();
            });
        }, 5000);

        // Scroll to top
        $('html, body').animate({ scrollTop: 0 }, 300);
    }

    // Format file size display
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        var k = 1024;
        var sizes = ['Bytes', 'KB', 'MB', 'GB'];
        var i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
});

