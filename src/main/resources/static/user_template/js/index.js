// Subscription Notification Modal - Popup Advertisement Style

// Debug: Check if notification should show
const showNotification = /*[[${showSubscriptionNotification}]]*/ false;
console.log('🔔 Show Subscription Notification:', showNotification);

// Prevent body scroll when modal is open
window.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('subscriptionNotificationModal');
    if (modal && showNotification) {
        console.log('✅ Subscription modal found in DOM');
        console.log('Modal display:', window.getComputedStyle(modal).display);
        console.log('Modal z-index:', window.getComputedStyle(modal).zIndex);

        // Prevent body scroll
        document.body.style.overflow = 'hidden';

        // Add smooth entrance sound effect (optional)
        console.log('🎯 Popup activated - Advertisement style');
    } else {
        console.log('❌ Subscription modal NOT found in DOM');
    }
});

function closeSubscriptionModal() {
    const modal = document.getElementById('subscriptionNotificationModal');
    if (modal) {
        // Get modal content for exit animation
        const modalContent = modal.querySelector('.subscription-modal-content');
        const backdrop = modal.querySelector('[onclick="closeSubscriptionModal()"]');

        // Apply exit animations
        if (modalContent) {
            modalContent.style.animation = 'slideOut 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55)';
        }
        if (backdrop) {
            backdrop.style.animation = 'fadeOut 0.4s ease-out';
        }

        // Remove modal and restore scroll after animation
        setTimeout(() => {
            modal.style.display = 'none';
            document.body.style.overflow = ''; // Restore body scroll
            console.log('✅ Popup closed');
        }, 400);
    }
}

// Prevent closing when clicking inside modal content
document.addEventListener('DOMContentLoaded', function() {
    const modalContent = document.querySelector('.subscription-modal-content');
    if (modalContent) {
        modalContent.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    }
});

// Optional: Auto close after 20 seconds
// setTimeout(closeSubscriptionModal, 20000);

// Close on ESC key
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeSubscriptionModal();
    }
});

// Script điều khiển search box dựa trên banner
document.addEventListener('DOMContentLoaded', function() {
    // Kiểm tra xem có banner hay không
    var hasBanner = /*[[${banners != null and #lists.size(banners) > 0}]]*/ false;

    if (hasBanner) {
        // Nếu có banner: Hiện search box trong navbar, ẩn trong hero
        var navbarSearchForm = document.getElementById('navbarSearchForm');
        if (navbarSearchForm) {
            navbarSearchForm.classList.remove('d-none');
            navbarSearchForm.classList.add('d-flex');
        }
    }
});

// Search function for navbar
function searchBooksFromNavbar() {
    const query = document.getElementById('navbarSearchInput').value;
    if (query.trim()) {
        window.location.href = '/books/search?keyword=' + encodeURIComponent(query);
    }
}

// Enter key search for navbar
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('navbarSearchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                searchBooksFromNavbar();
            }
        });
    }
});

