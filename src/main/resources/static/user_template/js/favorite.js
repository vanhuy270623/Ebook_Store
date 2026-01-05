/**
 * Favorite Book Handler
 * Xử lý chức năng yêu thích sách
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('Favorite handler initialized');

    // Kiểm tra SweetAlert2
    if (typeof Swal === 'undefined') {
        console.warn('SweetAlert2 not loaded');
    }

    const favoriteBtn = document.getElementById('favoriteBtn');

    if (!favoriteBtn) {
        console.warn('Favorite button not found');
        return;
    }

    const bookId = favoriteBtn.getAttribute('data-book-id');

    if (!bookId) {
        console.error('Book ID not found on favorite button');
        return;
    }

    console.log('Favorite button found for book:', bookId);

    // Kiểm tra trạng thái favorite ban đầu
    checkFavoriteStatus(bookId, favoriteBtn);

    // Xử lý click nút favorite
    favoriteBtn.addEventListener('click', function(e) {
        e.preventDefault();
        toggleFavorite(bookId, favoriteBtn);
    });
});

/**
 * Kiểm tra trạng thái yêu thích của sách
 */
function checkFavoriteStatus(bookId, button) {
    fetch(`/api/favorites/check/${bookId}`, {
        method: 'GET',
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        console.log('Favorite status:', data);
        updateFavoriteButton(button, data.isFavorite);
    })
    .catch(error => {
        console.error('Error checking favorite status:', error);
    });
}

/**
 * Toggle trạng thái yêu thích
 */
function toggleFavorite(bookId, button) {
    // Disable button để tránh double click
    button.disabled = true;

    fetch('/api/favorites/toggle', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ bookId: bookId })
    })
    .then(response => response.json())
    .then(data => {
        console.log('Toggle response:', data);

        if (data.success) {
            // Cập nhật UI
            updateFavoriteButton(button, data.isFavorite);

            // Hiển thị thông báo
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'success',
                    title: data.isFavorite ? 'Đã thêm vào yêu thích!' : 'Đã xóa khỏi yêu thích',
                    text: data.message,
                    timer: 2000,
                    showConfirmButton: false,
                    toast: true,
                    position: 'top-end'
                });
            }
        } else {
            // Hiển thị lỗi
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: data.message || 'Không thể cập nhật trạng thái yêu thích',
                    confirmButtonText: 'Đóng'
                });
            } else {
                alert(data.message || 'Có lỗi xảy ra');
            }
        }

        // Enable lại button
        button.disabled = false;
    })
    .catch(error => {
        console.error('Error toggling favorite:', error);

        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'error',
                title: 'Lỗi',
                text: 'Không thể kết nối đến server',
                confirmButtonText: 'Đóng'
            });
        } else {
            alert('Có lỗi xảy ra khi cập nhật yêu thích');
        }

        // Enable lại button
        button.disabled = false;
    });
}

/**
 * Cập nhật giao diện nút favorite
 */
function updateFavoriteButton(button, isFavorite) {
    const icon = button.querySelector('i');
    const text = button.querySelector('i').nextSibling;

    if (isFavorite) {
        // Đã yêu thích
        icon.classList.remove('far');
        icon.classList.add('fas');
        icon.style.color = '#e74c3c'; // Màu đỏ
        button.classList.add('active');
        if (text) {
            text.textContent = 'Đã yêu thích';
        }
    } else {
        // Chưa yêu thích
        icon.classList.remove('fas');
        icon.classList.add('far');
        icon.style.color = '';
        button.classList.remove('active');
        if (text) {
            text.textContent = 'Yêu thích';
        }
    }
}

