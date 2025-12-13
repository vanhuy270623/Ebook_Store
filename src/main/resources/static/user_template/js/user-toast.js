/**
 * User Toast Notification JavaScript
 * Handles toast notifications for user pages
 */

// Toast notification function - global
function showToast(message, type = 'info') {
    let toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.style.cssText = 'position: fixed; top: 80px; right: 20px; z-index: 9999; width: 350px;';
        document.body.appendChild(toastContainer);
    }

    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white border-0 show mb-2';
    toast.style.cssText = 'box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);';

    const colors = {
        'success': 'bg-success',
        'error': 'bg-danger',
        'info': 'bg-info',
        'warning': 'bg-warning'
    };
    toast.classList.add(colors[type] || 'bg-info');

    const icons = {
        'success': '<i class="fas fa-check-circle me-2"></i>',
        'error': '<i class="fas fa-exclamation-circle me-2"></i>',
        'info': '<i class="fas fa-info-circle me-2"></i>',
        'warning': '<i class="fas fa-exclamation-triangle me-2"></i>'
    };

    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${icons[type] || ''}${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 5000);

    toast.querySelector('.btn-close').addEventListener('click', () => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    });
}

// Initialize toast messages from page data
function initToastMessages() {
    if (window.pageMessages) {
        if (window.pageMessages.success) {
            showToast(window.pageMessages.success, 'success');
        }
        if (window.pageMessages.error) {
            showToast(window.pageMessages.error, 'error');
        }
        if (window.pageMessages.info) {
            showToast(window.pageMessages.info, 'info');
        }
        if (window.pageMessages.warning) {
            showToast(window.pageMessages.warning, 'warning');
        }
    }
}

// Run on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    initToastMessages();

    // Update cart counter if function exists
    if (window.pageMessages && window.pageMessages.success && typeof window.updateCartCount === 'function') {
        setTimeout(() => window.updateCartCount(), 500);
    }
});

