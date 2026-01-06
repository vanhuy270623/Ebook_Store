/**
 * Admin Review List - JavaScript
 * Handles review moderation actions
 */

$(document).ready(function() {
    // Initialize DataTable
    $('#reviewsTable').DataTable({
        "paging": true,
        "lengthChange": true,
        "searching": false, // Disabled - using custom search form instead
        "ordering": true,
        "info": true,
        "autoWidth": false,
        "pageLength": 25,
        "order": [[5, "desc"]], // Sort by date column (index 5) descending
        "language": {
            "lengthMenu": "Hiển thị _MENU_ dòng",
            "zeroRecords": "Không tìm thấy dữ liệu",
            "info": "Trang _PAGE_ / _PAGES_",
            "infoEmpty": "Không có dữ liệu",
            "infoFiltered": "(lọc từ _MAX_ dòng)",
            "search": "Tìm kiếm:",
            "paginate": {
                "first": "Đầu",
                "last": "Cuối",
                "next": "Sau",
                "previous": "Trước"
            }
        }
    });
});

/**
 * Delete review
 */
function deleteReview(reviewId) {
    if (confirm('Bạn có chắc chắn muốn xóa đánh giá này?')) {
        $.ajax({
            url: '/admin/reviews/delete/' + reviewId,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
            },
            success: function(response) {
                if (response.success) {
                    alert(response.message || 'Xóa đánh giá thành công!');
                    location.reload();
                } else {
                    alert(response.message || 'Có lỗi xảy ra khi xóa đánh giá.');
                }
            },
            error: function(xhr) {
                alert('Có lỗi xảy ra. Vui lòng thử lại.');
                console.error('Error deleting review:', xhr);
            }
        });
    }
}

/**
 * Bulk approve reviews
 */
function bulkApproveReviews() {
    const checkedBoxes = $('input[name="reviewIds"]:checked');
    if (checkedBoxes.length === 0) {
        alert('Vui lòng chọn ít nhất một đánh giá để duyệt.');
        return;
    }

    const reviewIds = [];
    checkedBoxes.each(function() {
        reviewIds.push($(this).val());
    });

    if (confirm(`Bạn có chắc muốn duyệt ${reviewIds.length} đánh giá đã chọn?`)) {
        $.ajax({
            url: '/admin/reviews/bulk-approve',
            method: 'POST',
            data: { reviewIds: reviewIds },
            headers: {
                'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
            },
            success: function(response) {
                if (response.success) {
                    alert(response.message || 'Duyệt đánh giá thành công!');
                    location.reload();
                } else {
                    alert(response.message || 'Có lỗi xảy ra.');
                }
            },
            error: function(xhr) {
                alert('Có lỗi xảy ra. Vui lòng thử lại.');
                console.error('Error bulk approving reviews:', xhr);
            }
        });
    }
}

/**
 * Bulk reject reviews
 */
function bulkRejectReviews() {
    const checkedBoxes = $('input[name="reviewIds"]:checked');
    if (checkedBoxes.length === 0) {
        alert('Vui lòng chọn ít nhất một đánh giá để từ chối.');
        return;
    }

    const reviewIds = [];
    checkedBoxes.each(function() {
        reviewIds.push($(this).val());
    });

    if (confirm(`Bạn có chắc muốn từ chối ${reviewIds.length} đánh giá đã chọn?`)) {
        $.ajax({
            url: '/admin/reviews/bulk-reject',
            method: 'POST',
            data: { reviewIds: reviewIds },
            headers: {
                'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
            },
            success: function(response) {
                if (response.success) {
                    alert(response.message || 'Từ chối đánh giá thành công!');
                    location.reload();
                } else {
                    alert(response.message || 'Có lỗi xảy ra.');
                }
            },
            error: function(xhr) {
                alert('Có lỗi xảy ra. Vui lòng thử lại.');
                console.error('Error bulk rejecting reviews:', xhr);
            }
        });
    }
}

