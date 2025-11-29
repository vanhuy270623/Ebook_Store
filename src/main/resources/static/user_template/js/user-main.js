/**
 * ============================================
 * EBOOKSTORE - USER TEMPLATE JAVASCRIPT
 * Version: 1.0.0
 * Modern User Interface Interactions
 * ============================================
 */

(function() {
    'use strict';

    // ============================================
    // 1. INITIALIZE APP
    // ============================================
    const EbookStore = {
        init: function() {
            this.setupNavigation();
            this.setupScrollEffects();
            this.setupBookCards();
            this.setupSearch();
            this.setupNotifications();
            this.setupLazyLoading();
            console.log('EbookStore User Interface initialized');
        },

        // ============================================
        // 2. NAVIGATION ENHANCEMENTS
        // ============================================
        setupNavigation: function() {
            // Active nav link highlighting
            const currentPath = window.location.pathname;
            document.querySelectorAll('.custom-navbar-nav .nav-link').forEach(link => {
                if (link.getAttribute('href') === currentPath) {
                    link.classList.add('active');
                }
            });

            // Mobile menu enhancement
            const navbarToggler = document.querySelector('.navbar-toggler');
            if (navbarToggler) {
                navbarToggler.addEventListener('click', function() {
                    this.classList.toggle('active');
                });
            }

            // Smooth scroll for anchor links
            document.querySelectorAll('a[href^="#"]').forEach(anchor => {
                anchor.addEventListener('click', function(e) {
                    const href = this.getAttribute('href');
                    if (href !== '#' && document.querySelector(href)) {
                        e.preventDefault();
                        document.querySelector(href).scrollIntoView({
                            behavior: 'smooth',
                            block: 'start'
                        });
                    }
                });
            });
        },

        // ============================================
        // 3. SCROLL EFFECTS
        // ============================================
        setupScrollEffects: function() {
            // Navbar shadow on scroll
            const navbar = document.querySelector('.custom-navbar');
            if (navbar) {
                let lastScroll = 0;
                window.addEventListener('scroll', () => {
                    const currentScroll = window.pageYOffset;

                    if (currentScroll > 100) {
                        navbar.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)';
                    } else {
                        navbar.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.1)';
                    }

                    lastScroll = currentScroll;
                });
            }

            // Fade in elements on scroll
            const observerOptions = {
                threshold: 0.1,
                rootMargin: '0px 0px -50px 0px'
            };

            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('fade-in');
                        observer.unobserve(entry.target);
                    }
                });
            }, observerOptions);

            document.querySelectorAll('.product-item, .feature').forEach(el => {
                observer.observe(el);
            });
        },

        // ============================================
        // 4. BOOK CARD INTERACTIONS
        // ============================================
        setupBookCards: function() {
            // Book card hover effects
            document.querySelectorAll('.product-item').forEach(card => {
                card.addEventListener('mouseenter', function() {
                    this.style.transform = 'translateY(-8px)';
                });

                card.addEventListener('mouseleave', function() {
                    this.style.transform = 'translateY(0)';
                });

                // Quick view on icon click
                const iconCross = card.querySelector('.icon-cross');
                if (iconCross) {
                    iconCross.addEventListener('click', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        const bookId = card.getAttribute('href').split('/').pop();
                        EbookStore.showQuickView(bookId);
                    });
                }
            });

            // Add to cart with animation
            document.querySelectorAll('.btn-add-to-cart').forEach(btn => {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    const bookId = this.dataset.bookId;
                    EbookStore.addToCart(bookId, this);
                });
            });
        },

        // ============================================
        // 5. SEARCH FUNCTIONALITY
        // ============================================
        setupSearch: function() {
            const searchInput = document.querySelector('.search-input');
            if (searchInput) {
                let searchTimeout;
                searchInput.addEventListener('input', function() {
                    clearTimeout(searchTimeout);
                    searchTimeout = setTimeout(() => {
                        const query = this.value.trim();
                        if (query.length >= 2) {
                            EbookStore.performSearch(query);
                        }
                    }, 300);
                });
            }
        },

        // ============================================
        // 6. NOTIFICATION SYSTEM
        // ============================================
        setupNotifications: function() {
            // Create notification container if not exists
            if (!document.querySelector('.notification-container')) {
                const container = document.createElement('div');
                container.className = 'notification-container';
                document.body.appendChild(container);
            }
        },

        showNotification: function(message, type = 'success', duration = 3000) {
            const container = document.querySelector('.notification-container');
            const notification = document.createElement('div');
            notification.className = `notification notification-${type}`;

            const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
            notification.innerHTML = `
                <span class="notification-icon">${icon}</span>
                <span class="notification-message">${message}</span>
            `;

            container.appendChild(notification);

            // Animate in
            setTimeout(() => notification.classList.add('show'), 10);

            // Remove after duration
            setTimeout(() => {
                notification.classList.remove('show');
                setTimeout(() => notification.remove(), 300);
            }, duration);
        },

        // ============================================
        // 7. ADD TO CART FUNCTIONALITY
        // ============================================
        addToCart: function(bookId, button) {
            const originalText = button.innerHTML;
            button.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang thêm...';
            button.disabled = true;

            fetch('/api/cart/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
                },
                body: JSON.stringify({ bookId: bookId, quantity: 1 })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.showNotification('Đã thêm vào giỏ hàng!', 'success');
                    this.updateCartCount();

                    // Animate button
                    button.innerHTML = '<i class="fa fa-check"></i> Đã thêm';
                    button.classList.add('btn-success');

                    setTimeout(() => {
                        button.innerHTML = originalText;
                        button.classList.remove('btn-success');
                        button.disabled = false;
                    }, 2000);
                } else {
                    throw new Error(data.message || 'Có lỗi xảy ra');
                }
            })
            .catch(error => {
                this.showNotification(error.message, 'error');
                button.innerHTML = originalText;
                button.disabled = false;
            });
        },

        // ============================================
        // 8. CART COUNT UPDATE
        // ============================================
        updateCartCount: function() {
            fetch('/api/cart/count')
                .then(response => response.json())
                .then(data => {
                    const cartBadge = document.querySelector('.cart-count-badge');
                    if (cartBadge) {
                        cartBadge.textContent = data.count;
                        if (data.count > 0) {
                            cartBadge.style.display = 'inline-block';
                        }
                    }
                })
                .catch(error => console.error('Error updating cart count:', error));
        },

        // ============================================
        // 9. LAZY LOADING IMAGES
        // ============================================
        setupLazyLoading: function() {
            if ('IntersectionObserver' in window) {
                const imageObserver = new IntersectionObserver((entries, observer) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            const img = entry.target;
                            if (img.dataset.src) {
                                img.src = img.dataset.src;
                                img.removeAttribute('data-src');
                                img.classList.add('loaded');
                            }
                            observer.unobserve(img);
                        }
                    });
                });

                document.querySelectorAll('img[data-src]').forEach(img => {
                    imageObserver.observe(img);
                });
            }
        },

        // ============================================
        // 10. SEARCH FUNCTIONALITY
        // ============================================
        performSearch: function(query) {
            const searchResults = document.querySelector('.search-results');
            if (!searchResults) return;

            searchResults.innerHTML = '<div class="loading">Đang tìm kiếm...</div>';
            searchResults.style.display = 'block';

            fetch(`/api/books/search?q=${encodeURIComponent(query)}`)
                .then(response => response.json())
                .then(data => {
                    if (data.books && data.books.length > 0) {
                        let html = '<div class="search-results-list">';
                        data.books.forEach(book => {
                            html += `
                                <a href="/books/${book.bookId}" class="search-result-item">
                                    <img src="${book.coverImageUrl}" alt="${book.title}">
                                    <div class="search-result-info">
                                        <h4>${book.title}</h4>
                                        <p>${book.author?.name || 'Unknown'}</p>
                                        <span class="price">${book.isFree ? 'Miễn phí' : book.price.toLocaleString('vi-VN') + ' ₫'}</span>
                                    </div>
                                </a>
                            `;
                        });
                        html += '</div>';
                        searchResults.innerHTML = html;
                    } else {
                        searchResults.innerHTML = '<div class="no-results">Không tìm thấy kết quả</div>';
                    }
                })
                .catch(error => {
                    searchResults.innerHTML = '<div class="error">Có lỗi xảy ra khi tìm kiếm</div>';
                    console.error('Search error:', error);
                });
        },

        // ============================================
        // 11. QUICK VIEW MODAL
        // ============================================
        showQuickView: function(bookId) {
            // TODO: Implement quick view modal
            console.log('Quick view for book:', bookId);
        },

        // ============================================
        // 12. UTILITIES
        // ============================================
        formatPrice: function(price) {
            return new Intl.NumberFormat('vi-VN', {
                style: 'currency',
                currency: 'VND'
            }).format(price);
        },

        debounce: function(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        }
    };

    // ============================================
    // INITIALIZE ON DOM READY
    // ============================================
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => EbookStore.init());
    } else {
        EbookStore.init();
    }

    // Expose to window for external access
    window.EbookStore = EbookStore;

})();

// ============================================
// NOTIFICATION STYLES (Injected via JS)
// ============================================
const notificationStyles = `
    .notification-container {
        position: fixed;
        top: 80px;
        right: 20px;
        z-index: 9999;
        pointer-events: none;
    }

    .notification {
        background: white;
        padding: 16px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        margin-bottom: 12px;
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 300px;
        opacity: 0;
        transform: translateX(400px);
        transition: all 0.3s ease;
        pointer-events: auto;
    }

    .notification.show {
        opacity: 1;
        transform: translateX(0);
    }

    .notification-icon {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: bold;
        font-size: 14px;
    }

    .notification-success {
        border-left: 4px solid #00c853;
    }

    .notification-success .notification-icon {
        background: #00c853;
        color: white;
    }

    .notification-error {
        border-left: 4px solid #e53935;
    }

    .notification-error .notification-icon {
        background: #e53935;
        color: white;
    }

    .notification-info {
        border-left: 4px solid #29b6f6;
    }

    .notification-info .notification-icon {
        background: #29b6f6;
        color: white;
    }

    .notification-message {
        flex: 1;
        font-size: 14px;
        color: #1a1a1a;
    }
`;

// Inject notification styles
if (!document.querySelector('#notification-styles')) {
    const styleEl = document.createElement('style');
    styleEl.id = 'notification-styles';
    styleEl.textContent = notificationStyles;
    document.head.appendChild(styleEl);
}

