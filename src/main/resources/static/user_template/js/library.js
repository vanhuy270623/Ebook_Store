/**
 * Library Page JavaScript
 * Trang thư viện người dùng
 */

document.addEventListener('DOMContentLoaded', function() {
    // Tab URL sync - Đồng bộ tab với URL
    initTabUrlSync();

    // Initialize tooltips if Bootstrap is available
    initTooltips();

    // Image fallback
    initImageFallback();

    // Initialize favorite buttons
    initFavoriteButtons();
});

/**
 * Đồng bộ tab với URL parameter
 */
function initTabUrlSync() {
    const tabLinks = document.querySelectorAll('#libraryTabs a[data-bs-toggle="tab"]');

    // Xóa tất cả event listener cũ và thêm mới
    tabLinks.forEach(link => {
        // Clone node để xóa tất cả event listeners cũ
        const newLink = link.cloneNode(true);
        link.parentNode.replaceChild(newLink, link);
    });

    // Lấy lại danh sách links sau khi clone
    const newTabLinks = document.querySelectorAll('#libraryTabs a[data-bs-toggle="tab"]');

    newTabLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            const tabId = this.getAttribute('data-bs-target').replace('#', '');

            // Xóa class active khỏi tất cả tabs
            newTabLinks.forEach(l => l.classList.remove('active'));

            // Thêm class active vào tab được click
            this.classList.add('active');

            // Ẩn tất cả tab panes
            document.querySelectorAll('.tab-pane').forEach(pane => {
                pane.classList.remove('show', 'active');
            });

            // Hiển thị tab pane tương ứng
            const targetPane = document.querySelector(this.getAttribute('data-bs-target'));
            if (targetPane) {
                targetPane.classList.add('show', 'active');
            }

            // Cập nhật URL mà không reload trang
            const url = new URL(window.location);
            url.searchParams.set('tab', tabId);
            window.history.replaceState({}, '', url);

            console.log('Tab switched to:', tabId);
        });
    });

    // Activate tab from URL on page load
    const urlParams = new URLSearchParams(window.location.search);
    const activeTab = urlParams.get('tab');

    if (activeTab) {
        const tabLink = document.querySelector(`#libraryTabs a[data-bs-target="#${activeTab}"]`);
        if (tabLink) {
            // Remove active from all
            newTabLinks.forEach(l => l.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(pane => {
                pane.classList.remove('show', 'active');
            });

            // Activate the target tab
            tabLink.classList.add('active');
            const targetPane = document.querySelector(`#${activeTab}`);
            if (targetPane) {
                targetPane.classList.add('show', 'active');
            }
        }
    } else {
        // Nếu không có tab trong URL, kích hoạt tab "all"
        const defaultTab = document.querySelector('#libraryTabs a[data-bs-target="#all"]');
        const allPane = document.querySelector('#all');

        if (defaultTab && allPane) {
            newTabLinks.forEach(l => l.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(pane => {
                pane.classList.remove('show', 'active');
            });

            defaultTab.classList.add('active');
            allPane.classList.add('show', 'active');
        }
    }
}

/**
 * Initialize Bootstrap tooltips
 */
function initTooltips() {
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        tooltipTriggerList.forEach(function(tooltipTriggerEl) {
            new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
}

/**
 * Image fallback handler
 */
function initImageFallback() {
    const images = document.querySelectorAll('.book-cover img');

    images.forEach(img => {
        img.addEventListener('error', function() {
            this.src = '/shared/images/book.png';
        });
    });
}

/**
 * Initialize favorite buttons
 */
function initFavoriteButtons() {
    const favoriteButtons = document.querySelectorAll('.action-btn.favorite');

    favoriteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            // Lấy bookId từ card cha
            const bookCard = this.closest('a.book-card');
            if (bookCard) {
                const bookId = extractBookIdFromUrl(bookCard.href);
                if (bookId) {
                    toggleFavorite(bookId, this);
                }
            }
        });
    });
}

/**
 * Extract bookId từ URL
 */
function extractBookIdFromUrl(url) {
    // URL format: /books/view/{bookId} hoặc /reading/reader/{bookId}
    const match = url.match(/\/(books\/view|reading\/reader)\/([^/?]+)/);
    return match ? match[2] : null;
}

/**
 * Toggle favorite status
 * @param {string} bookId - Book ID
 * @param {HTMLElement} button - Button element
 */
function toggleFavorite(bookId, button) {
    fetch('/api/favorites/toggle', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ bookId: bookId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            const icon = button.querySelector('i');
            if (data.isFavorite) {
                button.classList.add('active');
                icon.classList.remove('far');
                icon.classList.add('fas');
                icon.style.color = '#dc3545';
            } else {
                button.classList.remove('active');
                icon.classList.remove('fas');
                icon.classList.add('far');
                icon.style.color = '';

                // Nếu đang ở tab favorites, reload lại trang để cập nhật
                const urlParams = new URLSearchParams(window.location.search);
                if (urlParams.get('tab') === 'favorites') {
                    setTimeout(() => {
                        window.location.reload();
                    }, 500);
                }
            }
            showToast(data.message, 'success');
        } else {
            showToast(data.message || 'Có lỗi xảy ra', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Không thể kết nối đến server', 'error');
    });
}

/**
 * Show toast notification
 * @param {string} message - Message to display
 * @param {string} type - Toast type: success, error, info, warning
 */
function showToast(message, type = 'info') {
    // Check if toast container exists
    let toastContainer = document.getElementById('toastContainer');

    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        toastContainer.style.zIndex = '1100';
        document.body.appendChild(toastContainer);
    }

    // Create toast element
    const toastId = 'toast-' + Date.now();
    const bgClass = {
        'success': 'bg-success',
        'error': 'bg-danger',
        'warning': 'bg-warning',
        'info': 'bg-info'
    }[type] || 'bg-info';

    const toastHTML = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHTML);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
    toast.show();

    // Remove toast element after hidden
    toastElement.addEventListener('hidden.bs.toast', function() {
        this.remove();
    });
}

/**
 * Continue reading - redirect to reader
 * @param {string} bookId - Book ID
 */
function continueReading(bookId) {
    window.location.href = `/read/${bookId}`;
}

/**
 * View book detail
 * @param {string} bookId - Book ID
 */
function viewBookDetail(bookId) {
    window.location.href = `/books/view/${bookId}`;
}

/**
 * Filter books by search term (client-side)
 * @param {string} searchTerm - Search term
 */
function filterBooks(searchTerm) {
    const term = searchTerm.toLowerCase().trim();
    const bookCards = document.querySelectorAll('.tab-pane.active .book-card');

    bookCards.forEach(card => {
        const title = card.querySelector('.book-title')?.textContent.toLowerCase() || '';
        const author = card.querySelector('.book-author')?.textContent.toLowerCase() || '';
        const parent = card.closest('.col-lg-3, .col-md-4, .col-sm-6');

        if (title.includes(term) || author.includes(term) || term === '') {
            parent.style.display = '';
        } else {
            parent.style.display = 'none';
        }
    });
}

