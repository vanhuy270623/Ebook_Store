/**
 * EPUB Reader Script
 * Handles EPUB book reading with ePub.js library
 * Fixed: Lưu CFI vào last_read_location thay vì bookmarkData
 */

// ePub.js variables
let book = null;
let rendition = null;
let currentLocation = null;
let isLoading = true;

// Zoom variables
let currentZoom = 100;
const baseFontSize = 16;

// Track loading attempts
let loadAttempts = 0;
const maxAttempts = 50;

// Touch variables
let touchStartX = 0;
let touchEndX = 0;

// Global bookId and bookAssetId
let bookId = null;
let bookAssetId = null;

// Anti-Skimming Progress Tracker
let progressTracker = null;

/**
 * Initialize EPUB Viewer
 */
function initEPUBViewer() {
    loadAttempts++;

    if (typeof ePub === 'undefined') {
        if (loadAttempts >= maxAttempts) {
            console.error('ePub.js failed to load after ' + (maxAttempts * 100) + 'ms');
            showLoading(false);
            alert('Lỗi: Không thể tải thư viện ePub.js từ CDN.\n\n' +
                'Nguyên nhân có thể:\n' +
                '1. Trình duyệt chặn CDN (Tracking Prevention)\n' +
                '2. Không có kết nối internet\n' +
                '3. CDN jsdelivr.net bị chặn\n\n' +
                'Giải pháp:\n' +
                '- Tắt Tracking Prevention trong browser\n' +
                '- Kiểm tra kết nối internet\n' +
                '- Thử browser khác (Chrome, Firefox)');
            return;
        }
        console.log('Waiting for ePub.js to load... (attempt ' + loadAttempts + ')');
        setTimeout(initEPUBViewer, 100);
        return;
    }

    console.log('✅ ePub.js loaded successfully');

    try {
        loadEPUB();

        // Initialize Anti-Skimming Progress Tracker (giống PDF reader)
        if (typeof ReadingProgressTracker !== 'undefined') {
            // Note: progressTracker sẽ được khởi tạo sau khi book loaded
            console.log('✅ ReadingProgressTracker available');
        } else {
            console.warn('⚠️ ReadingProgressTracker not found - using fallback');
            // Fallback: Auto-save every 30s nếu tracker không có
            setInterval(saveProgress, 30000);
        }
    } catch (error) {
        console.error('Error initializing EPUB viewer:', error);
        showLoading(false);
        alert('Lỗi khởi tạo EPUB viewer: ' + error.message);
    }
}

/**
 * Load EPUB file
 */
async function loadEPUB() {
    try {
        showLoading(true);

        // Get data from HTML data attributes
        const dataContainer = document.getElementById('epub-data');
        if (!dataContainer) {
            throw new Error('EPUB data container not found');
        }

        bookId = dataContainer.dataset.bookId;
        bookAssetId = dataContainer.dataset.bookAssetId;
        const assetPath = dataContainer.dataset.assetPath;
        const assetFileUrl = dataContainer.dataset.assetFileUrl; // Fallback
        const encodedLocation = dataContainer.dataset.encodedLocation;

        console.log('=== EPUB LOADING DEBUG ===');
        console.log('bookId:', bookId);
        console.log('bookAssetId:', bookAssetId);
        console.log('assetPath (readingUrl):', assetPath);
        console.log('assetFileUrl (fallback):', assetFileUrl);
        console.log('encodedLocation:', encodedLocation);

        // Debug: Log asset type
        console.log('AssetPath type:', typeof assetPath);
        console.log('AssetFileUrl type:', typeof assetFileUrl);
        console.log('AssetPath empty?', !assetPath || assetPath.trim() === '');
        console.log('AssetFileUrl empty?', !assetFileUrl || assetFileUrl.trim() === '');

        // Decode saved location (CFI string)
        let savedCFI = null;
        if (encodedLocation && encodedLocation !== 'null' && encodedLocation.trim() !== '') {
            try {
                savedCFI = atob(encodedLocation);
                console.log('Decoded saved CFI:', savedCFI);
            } catch (e) {
                console.warn('Could not decode location:', e);
            }
        }

        // Validate path - use readingUrl if available, otherwise fileUrl
        let finalPath = assetPath;
        if (!assetPath || assetPath.trim() === '' || assetPath === 'null' || assetPath === 'undefined') {
            console.warn('⚠️ readingUrl is null/empty, trying fileUrl as fallback...');
            finalPath = assetFileUrl;

            // **FIX:** Nếu fileUrl là direct path, convert sang streaming endpoint
            if (finalPath && (finalPath.startsWith('/book_asset/source/') || finalPath.startsWith('/uploads/source/'))) {
                console.warn('⚠️ Direct path detected, converting to streaming endpoint for security...');
                const streamingPath = `/reading/stream/${bookId}?assetId=${bookAssetId}&format=EPUB`;
                console.log(`Converting: ${finalPath} → ${streamingPath}`);
                finalPath = streamingPath;
            }
        }

        // **FIX:** Thêm assetId và format vào URL nếu chưa có
        if (finalPath && finalPath.startsWith('/reading/stream/') && !finalPath.includes('?')) {
            finalPath = `${finalPath}?assetId=${bookAssetId}&format=EPUB`;
            console.log('✅ Added assetId and format to streaming URL:', finalPath);
        }

        if (!finalPath || finalPath.trim() === '' || finalPath === 'null' || finalPath === 'undefined') {
            console.error('❌ Both readingUrl and fileUrl are invalid!');
            console.error('assetPath:', assetPath);
            console.error('assetFileUrl:', assetFileUrl);

            alert('❌ Không thể tải sách!\n\n' +
                  'Nguyên nhân: Đường dẫn file không hợp lệ.\n\n' +
                  'Vui lòng:\n' +
                  '1. Liên hệ admin để kiểm tra file\n' +
                  '2. Thử tải lại trang\n' +
                  '3. Chọn sách khác');
            throw new Error('Đường dẫn file EPUB không hợp lệ. Vui lòng thử lại hoặc liên hệ admin.');
        }

        console.log('✅ Using finalPath:', finalPath);

        // Test file accessibility với error handling chi tiết
        console.log('Testing file accessibility...');
        try {
            const testResponse = await fetch(finalPath, { method: 'HEAD' });
            console.log('HEAD response status:', testResponse.status);
            console.log('HEAD response headers:', Object.fromEntries(testResponse.headers.entries()));

            if (!testResponse.ok) {
                throw new Error(`File không tồn tại. HTTP ${testResponse.status} - ${testResponse.statusText}`);
            }
            console.log('✅ File accessible');
        } catch (headError) {
            console.error('❌ HEAD request failed:', headError);

            // Fallback: Try direct GET if HEAD fails
            console.warn('⚠️ HEAD failed, trying direct GET...');
            try {
                const getResponse = await fetch(finalPath, { method: 'GET', headers: { 'Range': 'bytes=0-1' } });
                console.log('GET (range) response status:', getResponse.status);

                if (!getResponse.ok) {
                    throw new Error(`File không accessible. HTTP ${getResponse.status}`);
                }
                console.log('✅ File accessible via GET');
            } catch (getError) {
                console.error('❌ Both HEAD and GET failed:', getError);

                alert(`❌ Không thể truy cập file EPUB!\n\n` +
                      `URL: ${finalPath}\n` +
                      `Lỗi: ${headError.message}\n\n` +
                      `Vui lòng:\n` +
                      `1. Kiểm tra file có tồn tại trên server\n` +
                      `2. Kiểm tra quyền truy cập\n` +
                      `3. Liên hệ admin nếu vẫn lỗi`);
                throw headError;
            }
        }

        // **FIX:** Load EPUB as ArrayBuffer với Progress Indicator
        updateLoadingProgress(0, 'Đang tải EPUB...');
        console.log('📥 Downloading EPUB file as ArrayBuffer...');

        const epubResponse = await fetch(finalPath);
        if (!epubResponse.ok) {
            throw new Error(`Failed to download EPUB: ${epubResponse.status}`);
        }

        // Get total file size
        const contentLength = epubResponse.headers.get('Content-Length');
        const total = parseInt(contentLength, 10);

        // Stream download với progress
        const reader = epubResponse.body.getReader();
        let receivedLength = 0;
        let chunks = [];

        while(true) {
            const {done, value} = await reader.read();

            if (done) break;

            chunks.push(value);
            receivedLength += value.length;

            // Update progress bar
            if (total) {
                const percent = Math.round((receivedLength / total) * 100);
                const mbDownloaded = (receivedLength / 1024 / 1024).toFixed(2);
                const mbTotal = (total / 1024 / 1024).toFixed(2);
                updateLoadingProgress(percent, `Đang tải EPUB... ${mbDownloaded}MB / ${mbTotal}MB`);
                console.log(`Download progress: ${percent}%`);
            }
        }

        // Merge chunks thành ArrayBuffer
        const chunksAll = new Uint8Array(receivedLength);
        let position = 0;
        for(let chunk of chunks) {
            chunksAll.set(chunk, position);
            position += chunk.length;
        }

        const epubArrayBuffer = chunksAll.buffer;
        console.log('✅ EPUB downloaded:', (epubArrayBuffer.byteLength / 1024 / 1024).toFixed(2), 'MB');

        // Initialize book với ArrayBuffer
        updateLoadingProgress(100, 'Đang khởi tạo sách...');
        book = ePub(epubArrayBuffer);
        console.log('Book instance created from ArrayBuffer');

        // Open book
        try {
            await book.opened;
            console.log('✅ Book opened from ArrayBuffer');
        } catch (openErr) {
            console.warn('⚠️ book.opened failed:', openErr);
        }

        // Verify book data
        const spine = await book.loaded.spine;
        console.log('✅ Spine loaded:', spine.length, 'items');

        // Create rendition
        const viewerElement = document.getElementById("epub-viewer");
        if (!viewerElement) {
            throw new Error('Element #epub-viewer không tồn tại');
        }

        rendition = book.renderTo("epub-viewer", {
            width: "100%",
            height: "100%",
            spread: "none",
            minSpreadWidth: 0,
            allowScriptedContent: true
        });
        console.log('✅ Rendition created');

        // Apply default CSS
        rendition.themes.default({
            'body': {
                'overflow-x': 'hidden !important',
                'max-width': '100% !important',
                'box-sizing': 'border-box !important'
            },
            'img': {
                'max-width': '100% !important',
                'height': 'auto !important'
            },
            'svg': {
                'max-width': '100% !important',
                'height': 'auto !important'
            },
            'table': {
                'max-width': '100% !important',
                'overflow-x': 'auto !important'
            },
            'pre': {
                'white-space': 'pre-wrap !important',
                'overflow-x': 'auto !important',
                'max-width': '100% !important'
            },
            '*': {
                'box-sizing': 'border-box !important'
            }
        });

        // Display book - restore saved location or start from beginning
        console.log('Displaying book...');
        try {
            if (savedCFI && savedCFI.startsWith('epubcfi')) {
                console.log('Restoring to saved CFI:', savedCFI);
                await rendition.display(savedCFI);
                console.log('✅ Restored to saved location');
            } else {
                console.log('Starting from beginning');
                if (book.spine && book.spine.items && book.spine.items.length > 0) {
                    await rendition.display(book.spine.items[0].href);
                } else {
                    await rendition.display(0);
                }
                console.log('✅ Started from beginning');
            }
        } catch (displayError) {
            console.error('Display failed:', displayError);
            await rendition.display(0); // Fallback
        }

        // Setup navigation
        setupNavigation();

        // Load table of contents
        await loadTableOfContents();

        // Generate locations for percentage calculation (important!)
        console.log('Generating locations for percentage calculation...');
        try {
            await book.locations.generate(1024); // Generate with 1024 chars per "page"
            console.log('✅ Locations generated:', book.locations.total, 'locations');
        } catch (locError) {
            console.warn('⚠️ Could not generate locations:', locError);
            console.warn('Percentage tracking may not work correctly');
        }

        // Apply default zoom
        applyZoom();

        showLoading(false);
        isLoading = false;
        console.log('=== EPUB LOADED SUCCESSFULLY ===');

        // Initialize Anti-Skimming Progress Tracker (sau khi book load xong)
        if (typeof ReadingProgressTracker !== 'undefined') {
            progressTracker = new ReadingProgressTracker({
                bookId: bookId,
                bookAssetId: bookAssetId,
                format: 'EPUB',
                syncInterval: 30000, // Sync every 30 seconds

                onSyncSuccess: (data) => {
                    console.log('✅ EPUB Progress synced with anti-skimming:', data);
                    // Check both 'isSkimming' and 'skimming' (Jackson may strip 'is' prefix)
                    const isSkimmingDetected = data.isSkimming || data.skimming;
                    if (isSkimmingDetected) {
                        console.warn('⚠️ Skimming detected - time not accumulated');
                        showSkimmingWarning(data.message || 'Bạn đang đọc quá nhanh! Vui lòng đọc chậm lại.');
                    }
                },

                onSkimmingDetected: (data) => {
                    console.warn('🚨 EPUB Skimming behavior detected!');
                    console.log('Reading velocity:', data.readingVelocity, '%/s');
                    console.log('Max allowed: 0.60 %/s');
                    showSkimmingWarning('🚫 Đang đọc quá nhanh! Tiến độ KHÔNG được lưu. Hãy đọc chậm lại.');
                }
            });

            progressTracker.start();
            console.log('✅ Anti-Skimming Tracker initialized for EPUB');

            // **IMPORTANT:** Update initial progress sau khi khởi tạo
            if (currentLocation && book.locations && book.locations.total > 0) {
                const cfi = currentLocation.start.cfi;
                const percentage = book.locations.percentageFromCfi(cfi) * 100;
                progressTracker.updateProgress(cfi, percentage);
                console.log('✅ Initial EPUB progress set:', percentage.toFixed(2) + '%');
            }
        }

    } catch (error) {
        console.error('❌ Error loading EPUB:', error);
        showLoading(false);
        alert('Lỗi khi tải file EPUB:\n\n' + error.message);
    }
}

/**
 * Setup navigation events
 */
function setupNavigation() {
    // Navigation events
    rendition.on('relocated', function(location) {
        currentLocation = location;
        updateProgress();
        updateCurrentSection();
    });

    // Keyboard navigation
    document.addEventListener('keydown', function(e) {
        if (e.target.tagName.toLowerCase() === 'input' ||
            e.target.tagName.toLowerCase() === 'select') return;

        switch(e.key) {
            case 'ArrowLeft':
                e.preventDefault();
                previousPage();
                break;
            case 'ArrowRight':
                e.preventDefault();
                nextPage();
                break;
        }
    });

    // Touch/swipe navigation
    rendition.on('touchstart', function(e) {
        touchStartX = e.changedTouches[0].screenX;
    });

    rendition.on('touchend', function(e) {
        touchEndX = e.changedTouches[0].screenX;
        handleSwipe();
    });
}

function handleSwipe() {
    const swipeThreshold = 50;
    const diff = touchStartX - touchEndX;

    if (Math.abs(diff) > swipeThreshold) {
        if (diff > 0) {
            nextPage();
        } else {
            previousPage();
        }
    }
}

/**
 * Load table of contents
 */
async function loadTableOfContents() {
    try {
        const navigation = await book.loaded.navigation;
        const tocContainer = document.getElementById('tocContainer');
        tocContainer.innerHTML = '';

        navigation.toc.forEach((chapter) => {
            const tocItem = document.createElement('div');
            tocItem.className = 'toc-item';
            tocItem.textContent = chapter.label;
            tocItem.onclick = () => goToChapter(chapter.href);
            tocContainer.appendChild(tocItem);
        });

    } catch (error) {
        console.error('Error loading table of contents:', error);
    }
}

function goToChapter(href) {
    if (rendition) {
        rendition.display(href);
        if (window.innerWidth <= 768) {
            toggleSidebar();
        }
    }
}

function previousPage() {
    if (rendition) {
        rendition.prev();
    }
}

function nextPage() {
    if (rendition) {
        rendition.next();
    }
}

/**
 * Update progress display
 */
function updateProgress() {
    if (!currentLocation) return;

    // Check if locations are generated
    if (book.locations && book.locations.total > 0) {
        const progress = book.locations.percentageFromCfi(currentLocation.start.cfi);
        const progressPercent = Math.round(progress * 100);

        document.getElementById('progressText').textContent = progressPercent + '%';
        document.getElementById('progressBar').style.width = progressPercent + '%';

        // **FIX:** Update Anti-Skimming Tracker với vị trí mới
        if (progressTracker) {
            const cfi = currentLocation.start.cfi;
            const percentage = progress * 100; // Không làm tròn cho chính xác
            progressTracker.updateProgress(cfi, percentage);
            console.log(`📖 EPUB Progress: ${percentage.toFixed(2)}% (CFI: ${cfi.substring(0, 30)}...)`);
        }
    } else {
        // Locations not generated yet, show placeholder
        document.getElementById('progressText').textContent = 'Đang tải...';
        document.getElementById('progressBar').style.width = '0%';
        console.warn('⚠️ Locations not generated yet, cannot calculate percentage');
    }
}

/**
 * Update current section display
 */
function updateCurrentSection() {
    if (!currentLocation || !book.navigation) return;

    book.loaded.navigation.then(navigation => {
        const currentChapter = navigation.toc.find(chapter =>
            currentLocation.start.href.includes(chapter.href.split('#')[0])
        );

        if (currentChapter) {
            document.getElementById('currentSection').textContent = currentChapter.label;

            // Highlight in TOC
            document.querySelectorAll('.toc-item').forEach(item => {
                item.classList.remove('active');
                if (item.textContent === currentChapter.label) {
                    item.classList.add('active');
                }
            });
        }
    });
}

/**
 * Toggle sidebar
 */
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const btn = document.getElementById('toggleSidebarBtn');
    const isMobile = window.innerWidth <= 768;

    if (isMobile) {
        sidebar.classList.toggle('show');
        if (sidebar.classList.contains('show')) {
            btn.innerHTML = '<i class="fas fa-times"></i> Ẩn mục lục';
        } else {
            btn.innerHTML = '<i class="fas fa-bars"></i> Mục lục';
        }
    } else {
        sidebar.classList.toggle('hidden');
        if (sidebar.classList.contains('hidden')) {
            btn.innerHTML = '<i class="fas fa-bars"></i> Hiện mục lục';
        } else {
            btn.innerHTML = '<i class="fas fa-times"></i> Ẩn mục lục';
        }
    }
}

/**
 * Toggle settings panel
 */
function toggleSettings() {
    const panel = document.getElementById('settingsPanel');
    panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
}

// ==================== ZOOM FUNCTIONS ====================

function zoomIn() {
    currentZoom = Math.min(currentZoom + 25, 300);
    document.getElementById('zoomInput').value = currentZoom;
    applyZoom();
}

function zoomOut() {
    currentZoom = Math.max(currentZoom - 25, 50);
    document.getElementById('zoomInput').value = currentZoom;
    applyZoom();
}

function setZoom(zoomPercent) {
    currentZoom = parseInt(zoomPercent);
    currentZoom = Math.max(50, Math.min(currentZoom, 300));
    document.getElementById('zoomInput').value = currentZoom;
    applyZoom();
}

function applyZoom() {
    if (rendition) {
        const fontSize = Math.round(baseFontSize * (currentZoom / 100));
        rendition.themes.fontSize(fontSize + 'px');
        console.log('Zoom:', currentZoom + '%', 'Font size:', fontSize + 'px');
    }
}

function changeFontFamily(family) {
    if (rendition) {
        rendition.themes.font(family);
    }
}

function changePageWidth(width) {
    if (rendition) {
        const container = document.getElementById('epub-viewer');
        const readingContent = document.querySelector('.reading-content');
        const isMobile = window.innerWidth <= 768;

        container.style.cssText = '';

        if (width === '100%') {
            const availableWidth = readingContent ? readingContent.clientWidth : window.innerWidth;
            const contentWidth = Math.max(availableWidth - 120, 300);

            container.style.width = '100%';
            container.style.maxWidth = 'none';
            container.style.margin = '0 auto';
            container.style.padding = '20px 60px';
            container.style.boxSizing = 'border-box';
            container.style.overflowX = 'hidden';

            setTimeout(() => {
                rendition.resize(contentWidth, window.innerHeight - 150);
            }, 100);
        } else {
            const targetWidth = isMobile ? '100%' : width + 'px';

            container.style.width = targetWidth;
            container.style.maxWidth = targetWidth;
            container.style.margin = '0 auto';
            container.style.padding = '20px';
            container.style.boxSizing = 'border-box';
            container.style.overflowX = 'hidden';

            setTimeout(() => {
                rendition.resize();
            }, 100);
        }
    }
}

/**
 * Toggle dark mode
 */
function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');
    const isDark = document.body.classList.contains('dark-mode');
    const btn = document.getElementById('darkModeBtn');

    if (isDark) {
        btn.innerHTML = '<i class="fas fa-sun"></i> Chế độ sáng';
        if (rendition) {
            rendition.themes.default({
                'body': {
                    'background': '#2d3748 !important',
                    'background-color': '#2d3748 !important',
                    'color': '#e2e8f0 !important'
                },
                'html': {
                    'background': '#2d3748 !important',
                    'background-color': '#2d3748 !important'
                },
                'p, div, span, a, li, td, th, h1, h2, h3, h4, h5, h6': {
                    'color': '#e2e8f0 !important'
                },
                'a': {
                    'color': '#90cdf4 !important'
                },
                'a:visited': {
                    'color': '#b794f4 !important'
                },
                '*': {
                    'background-color': 'transparent !important'
                }
            });
        }
    } else {
        btn.innerHTML = '<i class="fas fa-moon"></i> Chế độ tối';
        if (rendition) {
            rendition.themes.default({
                'body': {
                    'background': '#ffffff !important',
                    'background-color': '#ffffff !important',
                    'color': '#333333 !important'
                },
                'html': {
                    'background': '#ffffff !important',
                    'background-color': '#ffffff !important'
                },
                'p, div, span, a, li, td, th, h1, h2, h3, h4, h5, h6': {
                    'color': '#333333 !important'
                },
                'a': {
                    'color': '#2563eb !important'
                },
                'a:visited': {
                    'color': '#7c3aed !important'
                },
                '*': {
                    'background-color': 'transparent !important'
                }
            });
        }
    }

    // Save preference
    fetch('/reading/api/toggle-mode', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `mode=${isDark ? 'dark' : 'light'}`
    });
}

// ==================== SAVE PROGRESS ====================

/**
 * Save reading progress to server
 * FIXED: Lưu CFI vào last_read_location (field location)
 * Works alongside tracker for immediate/forced saves
 */
async function saveProgress() {
    if (isLoading || !currentLocation) return;

    // Nếu tracker đang chạy, để tracker xử lý
    if (progressTracker) {
        console.log('⏭️ Tracker is handling progress sync');
        return;
    }

    try {
        const cfi = currentLocation.start.cfi;

        // Calculate percentage - wait for locations if needed
        let percentage = 0;
        if (book.locations && book.locations.total > 0) {
            percentage = book.locations.percentageFromCfi(cfi) * 100;
        } else {
            console.warn('⚠️ Locations not generated, saving with 0%');
        }

        console.log('=== FALLBACK EPUB PROGRESS SAVE ===');
        console.log('CFI:', cfi);
        console.log('Percentage:', percentage);

        // Sử dụng API mới với anti-skimming
        const requestData = {
            bookId: bookId,
            bookAssetId: bookAssetId,
            currentLocationRaw: cfi,
            progressPercentage: percentage,
            activeTimeDelta: 30, // Estimate 30 seconds (fallback)
            format: 'EPUB'
        };

        const response = await fetch('/api/reading/sync', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            console.error('Failed to save progress:', response.status);
        } else {
                    const data = await response.json();
            console.log('✅ Progress saved (fallback):', data);

            // Hiển thị cảnh báo skimming
            // Check both 'isSkimming' and 'skimming' (Jackson may strip 'is' prefix)
            const isSkimmingDetected = data.isSkimming || data.skimming;
            if (isSkimmingDetected) {
                console.warn('⚠️ Skimming detected:', data.message);
                console.log('>>> ABOUT TO CALL showSkimmingWarning <<<');
                console.log('>>> Function exists?', typeof showSkimmingWarning);

                try {
                    showSkimmingWarning(data.message || 'Bạn đang đọc quá nhanh! Vui lòng đọc chậm lại.');
                    console.log('>>> showSkimmingWarning called successfully');
                } catch (e) {
                    console.error('>>> ERROR calling showSkimmingWarning:', e);
                }
            }
        }
    } catch (error) {
        console.error('Error saving progress:', error);
    }
}

// ==================== BOOKMARKS MANAGEMENT ====================

/**
 * Toggle bookmarks sidebar
 */
function toggleBookmarksSidebar() {
    const sidebar = document.getElementById('bookmarksSidebar');
    sidebar.classList.toggle('show');

    if (sidebar.classList.contains('show')) {
        loadBookmarks();
    }
}

/**
 * Load bookmarks from server
 */
async function loadBookmarks() {
    try {
        const response = await fetch(`/reading/api/bookmarks/${bookId}`, {
            credentials: 'same-origin'
        });
        if (!response.ok) {
            throw new Error('Failed to load bookmarks');
        }

        const bookmarks = await response.json();
        displayBookmarks(bookmarks);
    } catch (error) {
        console.error('Error loading bookmarks:', error);
        document.getElementById('bookmarksList').innerHTML = `
            <p class="text-danger text-center">Lỗi tải bookmarks</p>
        `;
    }
}

/**
 * Display bookmarks list
 */
function displayBookmarks(bookmarks) {
    const container = document.getElementById('bookmarksList');

    if (!bookmarks || bookmarks.length === 0) {
        container.innerHTML = `
            <div class="no-bookmarks">
                <i class="fas fa-bookmark"></i>
                <p>Chưa có bookmark nào</p>
                <small>Nhấn "Thêm Bookmark" để lưu vị trí đọc</small>
            </div>
        `;
        return;
    }

    // Sort by percentage
    bookmarks.sort((a, b) => (a.percentage || 0) - (b.percentage || 0));

    // Store bookmarks data for click handlers
    window.bookmarksData = {};
    bookmarks.forEach(bm => {
        window.bookmarksData[bm.id] = bm.location;
    });

    container.innerHTML = bookmarks.map(bm => `
        <div class="bookmark-item" onclick="jumpToBookmarkById('${bm.id}')">
            <div class="bookmark-header">
                <strong>📖 ${bm.percentage ? bm.percentage.toFixed(1) + '%' : 'Vị trí'}</strong>
                <button class="bookmark-delete-btn"
                        onclick="deleteBookmark('${bm.id}', event)"
                        title="Xóa bookmark">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
            ${bm.note ? `<div class="bookmark-note">"${escapeHtml(bm.note)}"</div>` : ''}
            <div class="bookmark-meta">
                <span>${formatBookmarkDate(bm.createdAt)}</span>
            </div>
        </div>
    `).join('');
}

/**
 * Jump to bookmark by ID
 */
function jumpToBookmarkById(bookmarkId) {
    const cfi = window.bookmarksData[bookmarkId];
    if (cfi) {
        jumpToBookmark(cfi);
    }
}

/**
 * Save manual bookmark
 */
async function saveManualBookmark() {
    if (!currentLocation) {
        alert('❌ Chưa có vị trí đọc hiện tại');
        return;
    }

    const note = prompt('Ghi chú cho bookmark (tùy chọn):');
    if (note === null) return;

    try {
        const cfi = currentLocation.start.cfi;
        const percentage = book.locations ?
            Math.round(book.locations.percentageFromCfi(cfi) * 100) : 0;

        const formData = new FormData();
        formData.append('location', cfi); // Lưu CFI string trực tiếp
        formData.append('percentage', percentage);
        formData.append('note', note || '');

        console.log('=== SAVING EPUB BOOKMARK ===');
        console.log('CFI:', cfi, 'Percentage:', percentage);

        const response = await fetch(`/reading/api/bookmarks/${bookId}`, {
            method: 'POST',
            credentials: 'same-origin',
            body: formData
        });

        if (!response.ok) {
            throw new Error('Failed to save bookmark');
        }

        alert('✅ Bookmark đã lưu!');

        // Reload bookmarks if sidebar is open
        if (document.getElementById('bookmarksSidebar').classList.contains('show')) {
            loadBookmarks();
        }
    } catch (error) {
        console.error('Error saving bookmark:', error);
        alert('❌ Lỗi khi lưu bookmark: ' + error.message);
    }
}

/**
 * Jump to bookmark
 */
async function jumpToBookmark(cfi) {
    try {
        console.log('=== JUMPING TO BOOKMARK ===');
        console.log('CFI:', cfi);

        // cfi là CFI string trực tiếp
        if (cfi && cfi.includes('epubcfi') && rendition) {
            await rendition.display(cfi);
            console.log('✅ Jumped to bookmark successfully');
        } else {
            console.error('Invalid CFI or rendition is null');
            alert('❌ Không thể chuyển đến vị trí bookmark');
        }
    } catch (error) {
        console.error('Error jumping to bookmark:', error);
        alert('❌ Không thể chuyển đến vị trí bookmark');
    }
}

/**
 * Delete bookmark
 */
async function deleteBookmark(bookmarkId, event) {
    event.stopPropagation();

    if (!confirm('Xóa bookmark này?')) return;

    try {
        const response = await fetch(
            `/reading/api/bookmarks/${bookId}/${bookmarkId}`,
            {
                method: 'DELETE',
                credentials: 'same-origin'
            }
        );

        if (!response.ok) {
            throw new Error('Failed to delete bookmark');
        }

        loadBookmarks();
    } catch (error) {
        console.error('Error deleting bookmark:', error);
        alert('❌ Lỗi khi xóa bookmark: ' + error.message);
    }
}

// ==================== HELPER FUNCTIONS ====================

function formatBookmarkDate(dateString) {
    if (!dateString) return '';

    try {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now - date;
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (days === 0) return 'Hôm nay';
        if (days === 1) return 'Hôm qua';
        if (days < 7) return `${days} ngày trước`;

        return date.toLocaleDateString('vi-VN');
    } catch {
        return '';
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.style.display = show ? 'flex' : 'none';
    }
}

function updateLoadingProgress(percent, message) {
    const overlay = document.getElementById('loadingOverlay');
    const progressBar = overlay ? overlay.querySelector('.progress-bar') : null;
    const progressText = overlay ? overlay.querySelector('#loadingPercentage') : null;
    const loadingInfo = overlay ? overlay.querySelector('#loadingInfo') : null;
    const loadingText = overlay ? overlay.querySelector('#loadingText') : null;

    if (overlay) {
        overlay.style.display = 'flex';
    }

    if (progressBar) {
        progressBar.style.width = percent + '%';
    }

    if (progressText) {
        progressText.textContent = percent + '%';
    }

    if (loadingInfo && message) {
        loadingInfo.textContent = message;
    }

    if (loadingText) {
        loadingText.textContent = percent < 100 ? 'Đang tải EPUB...' : 'Sẵn sàng đọc!';
    }
}

// ==================== EVENT LISTENERS ====================

// Initialize when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        console.log('DOM loaded, initializing EPUB viewer...');
        initEPUBViewer();
    });
} else {
    console.log('DOM already loaded, initializing EPUB viewer...');
    initEPUBViewer();
}

// Close settings panel when clicking outside
document.addEventListener('click', function(e) {
    const panel = document.getElementById('settingsPanel');
    const btn = document.getElementById('settingsBtn');

    if (panel && btn && !panel.contains(e.target) && !btn.contains(e.target)) {
        panel.style.display = 'none';
    }
});

// Save progress when leaving page
// Save progress when leaving page
window.addEventListener('beforeunload', () => {
    // Stop tracker first (this will trigger final sync)
    if (progressTracker) {
        progressTracker.stop();
    }

    // Also do manual save as fallback
    if (!progressTracker && currentLocation) {
        saveProgress();
    }
});

// Auto-hide sidebar on mobile
if (window.innerWidth <= 768) {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        sidebar.classList.add('hidden');
        sidebar.classList.remove('show');
    }
}

// Resize rendition when window resizes
let resizeTimeout;
window.addEventListener('resize', function() {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(function() {
        if (rendition) {
            const isMobile = window.innerWidth <= 768;
            const container = document.getElementById('epub-viewer');

            if (isMobile) {
                container.style.maxWidth = '100%';
                container.style.padding = '10px 5px';
            } else {
                const selectedWidth = document.getElementById('pageWidthSelect').value;
                if (selectedWidth !== '100%') {
                    container.style.maxWidth = selectedWidth + 'px';
                }
                container.style.padding = '40px 20px';
            }

            container.style.overflowX = 'hidden';
            rendition.resize();

            const sidebar = document.getElementById('sidebar');
            if (isMobile && sidebar) {
                sidebar.classList.remove('show');
            }
        }
    }, 300);
});

// ==================== TOAST NOTIFICATIONS ====================

/**
 * Hiển thị cảnh báo khi phát hiện skimming
 */
function showSkimmingWarning(message) {
    // Remove existing toast if any
    const existingToast = document.querySelector('.reading-toast.warning');
    if (existingToast) {
        existingToast.remove();
    }

    // Create toast element
    const toast = document.createElement('div');
    toast.className = 'reading-toast warning';
    toast.innerHTML = `
        <i class="fas fa-exclamation-triangle"></i>
        <span>${message}</span>
    `;

    // Append to toast container or body
    const container = document.getElementById('toast-container') || document.body;
    container.appendChild(toast);

    // Animate in
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);

    // Auto dismiss after 5 seconds
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 5000);
}

