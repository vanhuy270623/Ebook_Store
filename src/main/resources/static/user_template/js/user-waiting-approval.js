/**
 * User Waiting Approval Page JavaScript
 * Handles auto-refresh and status checking
 */

// Auto refresh page every 30 seconds to check for status updates
let refreshInterval = setInterval(function() {
    location.reload();
}, 30000); // 30 seconds

// Stop auto refresh when user leaves the page
window.addEventListener('beforeunload', function() {
    clearInterval(refreshInterval);
});

// Check order status via AJAX
function checkOrderStatus() {
    const orderId = window.waitingData ? window.waitingData.orderId : null;
    if (!orderId) return;

    fetch('/order/api/status?orderId=' + orderId)
        .then(response => response.json())
        .then(data => {
            if (data.paymentStatus === 'PAID' || data.paymentStatus === 'COMPLETED') {
                window.location.href = '/payment/success?orderId=' + orderId;
            }
        })
        .catch(error => {
            console.error('Error checking order status:', error);
        });
}

// Check status every 10 seconds
setInterval(checkOrderStatus, 10000);

