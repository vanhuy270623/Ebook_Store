/**
 * User Payment Utilities JavaScript
 * Common functions for payment pages
 */

// Copy text to clipboard
function copyText(elementId) {
    const element = document.getElementById(elementId);
    const text = element.textContent;

    navigator.clipboard.writeText(text).then(() => {
        const icon = element.nextElementSibling;
        icon.className = 'fas fa-check copy-btn';
        icon.style.color = '#28a745';

        setTimeout(() => {
            icon.className = 'fas fa-copy copy-btn';
            icon.style.color = '';
        }, 2000);
    }).catch(err => {
        console.error('Không thể sao chép:', err);
    });
}

// Select payment method
function selectPayment(method) {
    document.querySelectorAll('.payment-method').forEach(el => {
        el.classList.remove('selected');
    });
    const radio = document.querySelector(`input[value="${method}"]`);
    if (radio) {
        radio.checked = true;
        radio.closest('.payment-method').classList.add('selected');
    }
}

// Initialize payment method selection on page load
document.addEventListener('DOMContentLoaded', function() {
    const checkedRadio = document.querySelector('input[name="paymentMethod"]:checked');
    if (checkedRadio && checkedRadio.closest('.payment-method')) {
        checkedRadio.closest('.payment-method').classList.add('selected');
    }
});

