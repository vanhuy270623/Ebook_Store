/**
 * User Index Page JavaScript
 * Handles search, animations, and cart interactions
 */

// Search functionality
function searchBooks() {
    const query = document.getElementById('searchInput').value;
    if (query.trim()) {
        window.location.href = '/books/search?keyword=' + encodeURIComponent(query);
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Enter key search
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchBooks();
            }
        });
    }

    // Initialize stats observer
    initStatsObserver();

    // Initialize action buttons
    initActionButtons();

    // Cập nhật cart counter ngay khi load trang
    // Đảm bảo hiển thị số lượng chính xác sau khi thêm vào giỏ
    setTimeout(function() {
        if (window.updateCartCount) {
            window.updateCartCount();
        }
    }, 100);

    // Hiển thị thông báo nếu có flash message từ server
    checkFlashMessages();
});

// Kiểm tra và hiển thị flash messages
function checkFlashMessages() {
    // Thymeleaf sẽ render flash attributes vào page
    // Tìm các thông báo success/error/info
    const successMsg = document.querySelector('[data-flash-success]');
    const errorMsg = document.querySelector('[data-flash-error]');
    const infoMsg = document.querySelector('[data-flash-info]');

    if (successMsg) {
        showToast(successMsg.dataset.flashSuccess, 'success');
        // Cập nhật cart counter sau khi thêm thành công
        if (window.updateCartCount) {
            window.updateCartCount();
        }
    }
    if (errorMsg) {
        showToast(errorMsg.dataset.flashError, 'error');
    }
    if (infoMsg) {
        showToast(infoMsg.dataset.flashInfo, 'info');
    }
}

// Hiển thị toast notification
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

// Animate statistics counter
function animateCounter(element) {
    const target = parseInt(element.getAttribute('data-target'));
    const duration = 2000;
    const step = target / (duration / 16);
    let current = 0;

    const timer = setInterval(() => {
        current += step;
        if (current >= target) {
            element.textContent = target;
            clearInterval(timer);
        } else {
            element.textContent = Math.floor(current);
        }
    }, 16);
}

// Intersection Observer for stats animation
function initStatsObserver() {
    const statsObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const counters = entry.target.querySelectorAll('[data-target]');
                counters.forEach(counter => {
                    if (counter.textContent === '0') {
                        animateCounter(counter);
                    }
                });
            }
        });
    }, { threshold: 0.5 });

    const statsSection = document.querySelector('.stats-section');
    if (statsSection) {
        statsObserver.observe(statsSection);
    }
}

// Add to cart functionality
function initActionButtons() {
    // Xử lý nút yêu thích (không phải cart)
    document.querySelectorAll('.action-btn').forEach(btn => {
        const icon = btn.querySelector('i');

        // CHỈ xử lý nút yêu thích, KHÔNG xử lý nút thêm vào giỏ
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
                    // Fallback: chỉ toggle icon nếu không có bookId
                    icon.classList.toggle('far');
                    icon.classList.toggle('fas');
                }
            });
        }

        // CHỈ xử lý nút xem nhanh
        if (icon && icon.classList.contains('fa-eye')) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                // Không preventDefault để link hoạt động bình thường
            });
        }
    });

    // Xử lý form thêm vào giỏ hàng - CHO PHÉP SUBMIT
    document.querySelectorAll('.add-to-cart-form').forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                // Disable nút để tránh submit nhiều lần
                submitBtn.disabled = true;

                // Hiển thị loading
                const originalContent = submitBtn.innerHTML;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

                // Cho phép form submit bình thường (không preventDefault)
                // Form sẽ POST lên server và redirect về trang hiện tại
            }
        });
    });

    // Sau khi trang load (sau redirect), cập nhật cart counter
    if (window.updateCartCount) {
        window.updateCartCount();
    }
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
        console.log('Response data:', data);

        if (data.success) {
            // Cập nhật icon dựa trên trạng thái từ server
            if (data.isFavorite) {
                iconElement.classList.remove('far');
                iconElement.classList.add('fas');
            } else {
                iconElement.classList.remove('fas');
                iconElement.classList.add('far');
            }

            // Hiển thị thông báo
            showToast(data.message, 'success');
        } else {
            // Hiển thị lỗi
            showToast(data.message || 'Có lỗi xảy ra', 'error');
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
        showToast('Không thể kết nối đến server', 'error');
    }
}

