/**
 * Review Submission Handler
 * Handles review form submission with AJAX
 * Includes 20% reading progress validation
 */

$(document).ready(function() {
    // Initialize star rating input
    initStarRating();

    // Handle review form submission
    $('#reviewForm').on('submit', function(e) {
        e.preventDefault();
        submitReview();
    });

    // Check if user can review on page load
    const bookId = $('input[name="bookId"]').val();
    if (bookId && $('#reviewFormContainer').length > 0) {
        checkCanReview(bookId);
    }
});

/**
 * Initialize star rating input
 */
function initStarRating() {
    $('.star-rating-input input[type="radio"]').on('change', function() {
        const rating = $(this).val();
        highlightStars(rating);
    });

    // Hover effect
    $('.star-rating-input label').hover(
        function() {
            const rating = $(this).prev('input').val();
            highlightStars(rating);
        },
        function() {
            const checkedRating = $('.star-rating-input input:checked').val();
            highlightStars(checkedRating || 0);
        }
    );
}

/**
 * Highlight stars up to the given rating
 */
function highlightStars(rating) {
    $('.star-rating-input label i').each(function(index) {
        const starValue = 5 - index; // Stars are in reverse order
        if (starValue <= rating) {
            $(this).removeClass('far').addClass('fas');
        } else {
            $(this).removeClass('fas').addClass('far');
        }
    });
}

/**
 * Check if user can review the book
 */
function checkCanReview(bookId) {
    $.ajax({
        url: '/api/reviews/can-review/' + bookId,
        method: 'GET',
        success: function(response) {
            if (!response.canReview) {
                // Show warning and disable form
                $('#reviewForm button[type="submit"]').prop('disabled', true);
                $('#progressWarning').show();
                $('#progressWarningText').html(response.reason);

                if (response.currentProgress !== undefined) {
                    const progressBar = `
                        <div class="progress mt-2" style="height: 25px;">
                            <div class="progress-bar" role="progressbar" 
                                 style="width: ${response.currentProgress}%"
                                 aria-valuenow="${response.currentProgress}" 
                                 aria-valuemin="0" aria-valuemax="100">
                                ${response.currentProgress.toFixed(1)}%
                            </div>
                        </div>
                        <small class="text-muted">Cần đọc ${response.requiredProgress}% để đánh giá</small>
                    `;
                    $('#progressWarningText').parent().append(progressBar);
                }
            } else {
                // Pre-fill if user is editing
                if (response.hasExistingReview && response.existingReview) {
                    const existingReview = response.existingReview;
                    $('input[name="rating"][value="' + existingReview.rating + '"]').prop('checked', true);
                    highlightStars(existingReview.rating);
                    $('#comment').val(existingReview.comment);
                }
            }
        },
        error: function(xhr) {
            console.error('Error checking review eligibility:', xhr);
        }
    });
}

/**
 * Submit review
 */
function submitReview() {
    const bookId = $('input[name="bookId"]').val();
    const rating = $('input[name="rating"]:checked').val();
    const comment = $('#comment').val().trim();

    // Validation
    if (!rating) {
        Swal.fire({
            icon: 'warning',
            title: 'Thiếu đánh giá',
            text: 'Vui lòng chọn số sao đánh giá.'
        });
        return;
    }

    // Show loading
    Swal.fire({
        title: 'Đang gửi đánh giá...',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    // Get CSRF token
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    // Submit via AJAX
    $.ajax({
        url: '/api/reviews',
        method: 'POST',
        data: {
            bookId: bookId,
            rating: rating,
            comment: comment
        },
        beforeSend: function(xhr) {
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        },
        success: function(response) {
            Swal.fire({
                icon: 'success',
                title: 'Thành công!',
                text: response.message,
                showConfirmButton: true,
                timer: 2000
            }).then(() => {
                // Reload page to show the new review
                location.reload();
            });
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            let errorMessage = 'Có lỗi xảy ra. Vui lòng thử lại.';

            if (response && response.message) {
                errorMessage = response.message;
            }

            // Show progress bar if available
            if (response && response.currentProgress !== undefined) {
                const progressHtml = `
                    <div class="progress mt-3" style="height: 25px;">
                        <div class="progress-bar ${response.currentProgress >= response.requiredProgress ? 'bg-success' : 'bg-warning'}" 
                             role="progressbar" 
                             style="width: ${response.currentProgress}%"
                             aria-valuenow="${response.currentProgress}" 
                             aria-valuemin="0" aria-valuemax="100">
                            ${response.currentProgress.toFixed(1)}%
                        </div>
                    </div>
                    <small class="text-muted d-block mt-2">
                        Cần đọc ${response.requiredProgress}% để đánh giá
                    </small>
                `;

                Swal.fire({
                    icon: 'warning',
                    title: 'Chưa đủ điều kiện',
                    html: '<p>' + errorMessage + '</p>' + progressHtml,
                    confirmButtonText: 'Đọc tiếp'
                });
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: errorMessage
                });
            }
        }
    });
}

/**
 * Edit existing review
 */
function editReview() {
    // Hide the "already reviewed" summary
    $('#userReviewSummary').hide();

    // Show the review form
    const $formContainer = $('#reviewFormContainer');
    $formContainer.show();

    // Scroll to form with safety check
    if ($formContainer.length && $formContainer.offset()) {
        $('html, body').animate({
            scrollTop: $formContainer.offset().top - 100
        }, 500);
    }

    // Optional: Focus on the first input
    setTimeout(function() {
        $('input[name="rating"]:checked').focus();
    }, 600);
}

/**
 * Cancel edit review - go back to summary view
 */
function cancelEditReview() {
    // Hide the form
    $('#reviewFormContainer').hide();

    // Show the summary again
    $('#userReviewSummary').show();

    // Scroll to summary
    const $summary = $('#userReviewSummary');
    if ($summary.length && $summary.offset()) {
        $('html, body').animate({
            scrollTop: $summary.offset().top - 100
        }, 500);
    }
}

/**
 * Delete review
 */
function deleteReview(reviewId) {
    Swal.fire({
        title: 'Xác nhận xóa',
        text: 'Bạn có chắc muốn xóa đánh giá này?',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            // Get CSRF token
            const csrfToken = $('meta[name="_csrf"]').attr('content');
            const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

            $.ajax({
                url: '/api/reviews/' + reviewId,
                method: 'DELETE',
                beforeSend: function(xhr) {
                    if (csrfToken && csrfHeader) {
                        xhr.setRequestHeader(csrfHeader, csrfToken);
                    }
                },
                success: function(response) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Đã xóa!',
                        text: response.message,
                        timer: 2000
                    }).then(() => {
                        location.reload();
                    });
                },
                error: function(xhr) {
                    const response = xhr.responseJSON;
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi',
                        text: response && response.message ? response.message : 'Không thể xóa đánh giá.'
                    });
                }
            });
        }
    });
}

