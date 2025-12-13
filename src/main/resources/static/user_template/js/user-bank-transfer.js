/**
 * User Bank Transfer Page JavaScript
 * Handles bank transfer confirmation and copy functions
 */

// Check QR image on DOM ready
window.addEventListener('DOMContentLoaded', function() {
    var qrImage = document.getElementById('qrCodeImage');
    if (qrImage) {
        console.log('QR Image element found');
        console.log('QR Image src:', qrImage.src);

        // Add load event listener
        qrImage.addEventListener('load', function() {
            console.log('QR Image loaded successfully!');
        });

        // Add error event listener
        qrImage.addEventListener('error', function() {
            console.error('QR Image failed to load!');
            console.error('Failed URL:', qrImage.src);

            // Show debug info
            var debugDiv = document.getElementById('qrDebug');
            if (debugDiv) {
                debugDiv.classList.remove('d-none');
            }
        });
    } else {
        console.error('QR Image element not found!');
    }

    // Debug QR Code URL if available
    if (window.bankTransferData && window.bankTransferData.qrCodeUrl) {
        console.log('QR Code URL:', window.bankTransferData.qrCodeUrl);
    }
});

// Copy to clipboard function
function copyToClipboard(element, text) {
    navigator.clipboard.writeText(text).then(function() {
        // Show success feedback
        const originalIcon = element.className;
        element.className = 'fas fa-check copy-btn text-success';

        setTimeout(function() {
            element.className = originalIcon;
        }, 1500);

        // Show toast notification
        showToast('Đã sao chép vào clipboard!', 'success');
    }).catch(function(err) {
        console.error('Could not copy text: ', err);
        showToast('Không thể sao chép', 'error');
    });
}

// Confirm transfer function
function confirmTransfer() {
    const orderId = window.bankTransferData ? window.bankTransferData.orderId : null;

    console.log('Confirming transfer for orderId:', orderId);

    const confirmBtn = document.getElementById('confirmBtn');

    // Validate orderId
    if (!orderId || orderId === 'default_order_id' || orderId.trim() === '') {
        console.error('Invalid orderId:', orderId);
        showToast('Lỗi: Không tìm thấy mã đơn hàng', 'error');
        return;
    }

    // Disable button and show loading
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang xử lý...';

    // Get CSRF token
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    console.log('CSRF Token:', token ? 'Found' : 'Not found');
    console.log('CSRF Header:', header);

    // Prepare headers
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
    };

    // Add CSRF token if exists
    if (token && header) {
        headers[header] = token;
    }

    // Build URL with proper encoding
    const url = '/payment/bank-transfer/confirm?orderId=' + encodeURIComponent(orderId);
    console.log('Request URL:', url);

    // Send AJAX request
    fetch(url, {
        method: 'POST',
        headers: headers,
        credentials: 'same-origin'
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            throw new Error('HTTP error! status: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        console.log('Response data:', data);
        if (data.success) {
            showToast(data.message, 'success');
            // Redirect after 1 second
            setTimeout(function() {
                window.location.href = data.redirectUrl;
            }, 1000);
        } else {
            showToast(data.message, 'error');
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-check-circle me-2"></i>Tôi đã chuyển khoản';
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('Có lỗi xảy ra. Vui lòng thử lại. Chi tiết: ' + error.message, 'error');
        confirmBtn.disabled = false;
        confirmBtn.innerHTML = '<i class="fas fa-check-circle me-2"></i>Tôi đã chuyển khoản';
    });
}

// Toast notification function
function showToast(message, type) {
    const toastHtml = `
        <div class="toast align-items-center text-white bg-${type === 'success' ? 'success' : 'danger'} border-0"
             role="alert" aria-live="assertive" aria-atomic="true"
             style="position: fixed; top: 20px; right: 20px; z-index: 9999;">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    const toastElement = document.createRange().createContextualFragment(toastHtml).firstElementChild;
    document.body.appendChild(toastElement);

    const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', function () {
        toastElement.remove();
    });
}

