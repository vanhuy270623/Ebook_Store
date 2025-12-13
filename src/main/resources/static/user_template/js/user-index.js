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
});

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
    document.querySelectorAll('.action-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            const icon = this.querySelector('i');
            if (icon.classList.contains('fa-cart-plus')) {
                alert('Đã thêm vào giỏ hàng!');
            } else if (icon.classList.contains('fa-heart')) {
                icon.classList.toggle('far');
                icon.classList.toggle('fas');
            }
        });
    });
}

