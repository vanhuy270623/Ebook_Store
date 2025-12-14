/**
 * Books Common JavaScript
 * Xử lý chức năng chung cho các trang books (list, category, search, etc.)
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize action buttons
    initBookActionButtons();

    // Cập nhật cart counter
    if (window.updateCartCount) {
        window.updateCartCount();
    }

    // Load favorite status for all books
    loadFavoriteStatuses();
});

// Initialize action buttons cho các trang books
function initBookActionButtons() {
    // Xử lý nút yêu thích
    document.querySelectorAll('.action-btn').forEach(btn => {
        const icon = btn.querySelector('i');

        if (icon && icon.classList.contains('fa-heart')) {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();

                // Lấy bookId từ card cha
                const bookCard = this.closest('a.book-card');
                const bookId = bookCard ? extractBookIdFromUrl(bookCard.href) : null;

                if (bookId) {
                    toggleFavorite(bookId, icon);
                } else {
                    console.warn('Cannot find bookId for favorite button');
                }
            });
        }
    });

    // Xử lý form add to cart
    document.querySelectorAll('.add-to-cart-form').forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                const originalContent = submitBtn.innerHTML;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            }
        });
    });
}

// Extract bookId từ URL
function extractBookIdFromUrl(url) {
    // URL format: /books/view/{bookId}
    const match = url.match(/\/books\/view\/([^/?]+)/);
    return match ? match[1] : null;
}

// Toggle favorite book
async function toggleFavorite(bookId, iconElement) {
    try {
        const response = await fetch('/api/favorites/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ bookId: bookId })
        });

        const data = await response.json();

        if (data.success) {
            // Cập nhật icon dựa trên trạng thái từ server
            if (data.isFavorite) {
                iconElement.classList.remove('far');
                iconElement.classList.add('fas');
                iconElement.style.color = '#dc3545'; // Màu đỏ
            } else {
                iconElement.classList.remove('fas');
                iconElement.classList.add('far');
                iconElement.style.color = '';
            }

            // Hiển thị thông báo
            if (window.showToast) {
                window.showToast(data.message, 'success');
            } else {
                showToast(data.message, 'success');
            }
        } else {
            // Hiển thị lỗi
            if (window.showToast) {
                window.showToast(data.message || 'Có lỗi xảy ra', 'error');
            } else {
                showToast(data.message || 'Có lỗi xảy ra', 'error');
            }
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
        const message = 'Không thể kết nối đến server';
        if (window.showToast) {
            window.showToast(message, 'error');
        } else {
            showToast(message, 'error');
        }
    }
}

// Load trạng thái favorite cho tất cả các sách trên trang
async function loadFavoriteStatuses() {
    // Lấy tất cả book cards
    const bookCards = document.querySelectorAll('a.book-card');

    for (const card of bookCards) {
        const bookId = extractBookIdFromUrl(card.href);
        if (bookId) {
            try {
                const response = await fetch(`/api/favorites/check/${bookId}`);
                const data = await response.json();

                if (data.isFavorite) {
                    // Tìm icon heart trong card này và cập nhật
                    const heartIcon = card.querySelector('.fa-heart');
                    if (heartIcon) {
                        heartIcon.classList.remove('far');
                        heartIcon.classList.add('fas');
                        heartIcon.style.color = '#dc3545';
                    }
                }
            } catch (error) {
                console.error('Error loading favorite status for book:', bookId, error);
            }
        }
    }
}

// Hiển thị toast notification (fallback nếu user-main.js chưa load)
function showToast(message, type = 'success') {
    // Tạo toast element
    const toast = document.createElement('div');
    toast.className = `toast-notification toast-${type}`;
    toast.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        background: ${type === 'success' ? '#28a745' : type === 'error' ? '#dc3545' : '#17a2b8'};
        color: white;
        padding: 15px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 9999;
        animation: slideInRight 0.3s ease-out;
        max-width: 400px;
    `;

    const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
    toast.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
            <span style="font-size: 20px;">${icon}</span>
            <span>${message}</span>
        </div>
    `;

    document.body.appendChild(toast);

    // Tự động ẩn sau 3 giây
    setTimeout(() => {
        toast.style.animation = 'slideOutRight 0.3s ease-in';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// CSS animations (thêm vào head nếu chưa có)
if (!document.getElementById('favorite-animations')) {
    const style = document.createElement('style');
    style.id = 'favorite-animations';
    style.textContent = `
        @keyframes slideInRight {
            from {
                transform: translateX(400px);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes slideOutRight {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(400px);
                opacity: 0;
            }
        }
        
        .fa-heart.fas {
            animation: heartBeat 0.3s ease-in-out;
        }
        
        @keyframes heartBeat {
            0%, 100% {
                transform: scale(1);
            }
            25% {
                transform: scale(1.3);
            }
            50% {
                transform: scale(1.1);
            }
            75% {
                transform: scale(1.2);
            }
        }
    `;
    document.head.appendChild(style);
}

