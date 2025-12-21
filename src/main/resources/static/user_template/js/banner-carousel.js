/**
 * DYNAMIC BANNER CAROUSEL SCRIPT
 * Author: EBook Store Development Team
 * Date: Dec 20, 2025
 * Description: Enhanced carousel functionality with pause on hover and auto-play
 */

document.addEventListener('DOMContentLoaded', function() {
    const carouselElement = document.getElementById('bannerCarousel');

    if (carouselElement) {
        // Initialize Bootstrap Carousel
        const carousel = new bootstrap.Carousel(carouselElement, {
            interval: 5000,  // Auto-play every 5 seconds (as per banner.md requirement)
            wrap: true,      // Loop infinitely
            pause: 'hover',  // Pause on hover (as per banner.md requirement)
            keyboard: true,  // Enable keyboard navigation
            touch: true      // Enable touch/swipe on mobile
        });

        // Pause on hover (additional manual control for better UX)
        carouselElement.addEventListener('mouseenter', function() {
            carousel.pause();
        });

        // Resume on mouse leave
        carouselElement.addEventListener('mouseleave', function() {
            carousel.cycle();
        });

        // Track click events on banners (for analytics)
        const bannerLinks = carouselElement.querySelectorAll('.carousel-item a');
        bannerLinks.forEach(function(link) {
            link.addEventListener('click', function(e) {
                const bannerTitle = this.querySelector('img').alt || 'Unknown Banner';
                console.log('Banner clicked:', bannerTitle);

                // You can add analytics tracking here
                // Example: gtag('event', 'banner_click', { 'banner_name': bannerTitle });
            });
        });

        // Preload next images for better performance
        const carouselItems = carouselElement.querySelectorAll('.carousel-item');
        carouselElement.addEventListener('slide.bs.carousel', function(e) {
            const nextIndex = e.to;
            if (carouselItems[nextIndex]) {
                const nextImage = carouselItems[nextIndex].querySelector('img');
                if (nextImage && !nextImage.complete) {
                    // Force load next image
                    nextImage.loading = 'eager';
                }
            }
        });

        // Log carousel events (for debugging)
        if (console && console.log) {
            carouselElement.addEventListener('slid.bs.carousel', function(e) {
                console.log('Carousel slid to index:', e.to);
            });
        }

        // Keyboard navigation enhancement
        document.addEventListener('keydown', function(e) {
            if (e.key === 'ArrowLeft') {
                carousel.prev();
            } else if (e.key === 'ArrowRight') {
                carousel.next();
            }
        });

        // Touch/Swipe support for mobile (enhanced)
        let touchStartX = 0;
        let touchEndX = 0;

        carouselElement.addEventListener('touchstart', function(e) {
            touchStartX = e.changedTouches[0].screenX;
        });

        carouselElement.addEventListener('touchend', function(e) {
            touchEndX = e.changedTouches[0].screenX;
            handleSwipe();
        });

        function handleSwipe() {
            const swipeThreshold = 50; // Minimum distance for swipe

            if (touchEndX < touchStartX - swipeThreshold) {
                // Swipe left - next slide
                carousel.next();
            }

            if (touchEndX > touchStartX + swipeThreshold) {
                // Swipe right - previous slide
                carousel.prev();
            }
        }

        console.log('Banner carousel initialized successfully');
    }
});

/**
 * Fallback function to handle banner carousel if Bootstrap is not loaded
 */
function initSimpleCarousel() {
    const carouselElement = document.getElementById('bannerCarousel');

    if (!carouselElement || typeof bootstrap !== 'undefined') {
        return; // Bootstrap is loaded, use it instead
    }

    console.warn('Bootstrap not detected, using simple carousel fallback');

    const items = carouselElement.querySelectorAll('.carousel-item');
    let currentIndex = 0;
    let autoPlayInterval = null;

    function showSlide(index) {
        items.forEach((item, i) => {
            item.classList.remove('active');
            if (i === index) {
                item.classList.add('active');
            }
        });
    }

    function nextSlide() {
        currentIndex = (currentIndex + 1) % items.length;
        showSlide(currentIndex);
    }

    function prevSlide() {
        currentIndex = (currentIndex - 1 + items.length) % items.length;
        showSlide(currentIndex);
    }

    function startAutoPlay() {
        autoPlayInterval = setInterval(nextSlide, 5000);
    }

    function stopAutoPlay() {
        if (autoPlayInterval) {
            clearInterval(autoPlayInterval);
        }
    }

    // Control buttons
    const prevBtn = carouselElement.querySelector('.carousel-control-prev');
    const nextBtn = carouselElement.querySelector('.carousel-control-next');

    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            prevSlide();
            stopAutoPlay();
            startAutoPlay(); // Restart
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            nextSlide();
            stopAutoPlay();
            startAutoPlay(); // Restart
        });
    }

    // Pause on hover
    carouselElement.addEventListener('mouseenter', stopAutoPlay);
    carouselElement.addEventListener('mouseleave', startAutoPlay);

    // Start auto-play
    startAutoPlay();
}

// Try to initialize simple carousel if Bootstrap is not available
if (typeof bootstrap === 'undefined') {
    window.addEventListener('load', initSimpleCarousel);
}

