/**
 * PDF Reader Script
 * Handles PDF book reading with PDF.js library
 */

// PDF.js variables
let pdfDoc = null;
let pageNum = 1;
let pageCount = 0;
let scale = 1.0;
let canvas = null;
let ctx = null;

// Global bookId
let bookId = null;

// Track loading attempts
let loadAttempts = 0;
const maxAttempts = 50;

/**
 * Parse page number from location string
 */
function parsePageNumber(location) {
    if (!location) return 1;
    // If format is "page-X", extract X
    if (typeof location === 'string' && location.startsWith('page-')) {
        return parseInt(location.split('-')[1]) || 1;
    }
    // If it's already a number
    return parseInt(location) || 1;
}

/**
 * Initialize PDF Viewer
 */
function initPDFViewer() {
    loadAttempts++;

    // Check if pdfjsLib loaded
    if (typeof pdfjsLib === 'undefined') {
        if (loadAttempts >= maxAttempts) {
            console.error('PDF.js failed to load after ' + (maxAttempts * 100) + 'ms');
            showLoading(false);
            alert('Lỗi: Không thể tải thư viện PDF.js từ CDN.\n\n' +
                'Nguyên nhân có thể:\n' +
                '1. Trình duyệt chặn CDN (Tracking Prevention)\n' +
                '2. Không có kết nối internet\n' +
                '3. CDN cloudflare.com bị chặn\n\n' +
                'Giải pháp:\n' +
                '- Tắt Tracking Prevention trong browser\n' +
                '- Kiểm tra kết nối internet\n' +
                '- Thử browser khác (Chrome, Firefox)');
            return;
        }
        console.log('Waiting for PDF.js to load... (attempt ' + loadAttempts + ')');
        setTimeout(initPDFViewer, 100);
        return;
    }

    console.log('✅ PDF.js loaded successfully after ' + (loadAttempts * 100) + 'ms');

    try {
        // Set worker source
        pdfjsLib.GlobalWorkerOptions.workerSrc =
            'https://unpkg.com/pdfjs-dist@3.11.174/build/pdf.worker.min.js';

        canvas = document.getElementById('pdfCanvas');
        ctx = canvas.getContext('2d');

        if (!canvas) {
            throw new Error('Canvas element not found');
        }

        // Get data from HTML
        const dataContainer = document.getElementById('pdf-data');
        if (!dataContainer) {
            throw new Error('PDF data container not found');
        }

        bookId = dataContainer.dataset.bookId;
        const assetPath = dataContainer.dataset.assetPath;
        const assetFileUrl = dataContainer.dataset.assetFileUrl; // Fallback
        const encodedLocation = dataContainer.dataset.encodedLocation;

        console.log('=== INIT DEBUG ===');
        console.log('bookId:', bookId);
        console.log('assetPath (readingUrl):', assetPath);
        console.log('assetFileUrl (fallback):', assetFileUrl);
        console.log('encodedLocation:', encodedLocation);

        // Validate assetPath - use readingUrl if available, otherwise fileUrl
        let finalPath = assetPath;
        if (!assetPath || assetPath.trim() === '' || assetPath === 'null' || assetPath === 'undefined') {
            console.warn('⚠️ readingUrl is null/empty, trying fileUrl as fallback...');
            finalPath = assetFileUrl;
        }

        if (!finalPath || finalPath.trim() === '' || finalPath === 'null' || finalPath === 'undefined') {
            console.error('❌ Both readingUrl and fileUrl are invalid!');
            console.error('assetPath:', assetPath);
            console.error('assetFileUrl:', assetFileUrl);
            throw new Error('Đường dẫn file PDF không hợp lệ. Vui lòng thử lại hoặc liên hệ admin.');
        }

        console.log('✅ Using finalPath:', finalPath);

        // Decode saved location
        let lastReadLocation = 'page-1';
        if (encodedLocation && encodedLocation !== 'null' && encodedLocation.trim() !== '') {
            try {
                lastReadLocation = atob(encodedLocation);
                console.log('Decoded last location:', lastReadLocation);
            } catch (e) {
                console.warn('Could not decode location:', e);
                lastReadLocation = 'page-1';
            }
        }

        pageNum = parsePageNumber(lastReadLocation);

        // Store finalPath globally (using readingUrl or fileUrl fallback)
        window.pdfAssetPath = finalPath;
        console.log('Stored pdfAssetPath:', window.pdfAssetPath);
        console.log('Stored pdfAssetPath:', window.pdfAssetPath);

        loadPDF();

        // Auto-save progress every 30 seconds
        setInterval(saveProgress, 30000);
    } catch (error) {
        console.error('Error initializing PDF viewer:', error);
        showLoading(false);
        alert('Lỗi khởi tạo PDF viewer: ' + error.message);
    }
}

/**
 * Load PDF file
 */
async function loadPDF() {
    try {
        showLoading(true);
        updateLoadingProgress(0, 'Đang kết nối...');

        const pdfPath = window.pdfAssetPath;
        console.log('=== PDF LOADING DEBUG ===');
        console.log('Asset Path:', pdfPath);

        // Validate pdfPath before proceeding
        if (!pdfPath || pdfPath.trim() === '' || pdfPath === 'null' || pdfPath === 'undefined') {
            console.error('❌ Invalid PDF path:', pdfPath);
            throw new Error('Đường dẫn file PDF không hợp lệ');
        }

        console.log('Full URL:', window.location.origin + pdfPath);

        // Test file accessibility
        try {
            const testResponse = await fetch(pdfPath, { method: 'HEAD' });
            console.log('Test HEAD request status:', testResponse.status);

            if (!testResponse.ok) {
                throw new Error(`File không tồn tại hoặc không truy cập được. HTTP ${testResponse.status}`);
            }
        } catch (fetchError) {
            console.error('Fetch test failed:', fetchError);
            throw new Error('Không thể kết nối đến file PDF: ' + fetchError.message);
        }

        console.log('File URL test passed, loading PDF...');

        // Create loading task with progress callback
        const loadingTask = pdfjsLib.getDocument({
            url: pdfPath,
            disableAutoFetch: false,
            disableStream: false,
            httpHeaders: {
                'Cache-Control': 'no-cache'
            }
        });

        // Progress callback
        loadingTask.onProgress = function(progressData) {
            if (progressData.total > 0) {
                const percent = Math.round((progressData.loaded / progressData.total) * 100);
                const loadedMB = (progressData.loaded / (1024 * 1024)).toFixed(2);
                const totalMB = (progressData.total / (1024 * 1024)).toFixed(2);
                updateLoadingProgress(percent, `Đang tải: ${loadedMB}MB / ${totalMB}MB`);
                console.log(`Loading progress: ${percent}% (${loadedMB}MB / ${totalMB}MB)`);
            }
        };

        updateLoadingProgress(10, 'Đang tải file PDF...');

        // Load PDF document
        pdfDoc = await loadingTask.promise;
        pageCount = pdfDoc.numPages;

        updateLoadingProgress(80, `PDF đã tải: ${pageCount} trang`);
        console.log('PDF loaded successfully:', pageCount, 'pages');

        document.getElementById('totalPages').textContent = pageCount;
        document.getElementById('pageInput').max = pageCount;

        updateLoadingProgress(90, 'Đang render trang đầu tiên...');

        // Render first page
        await renderPage(pageNum);

        updateLoadingProgress(100, 'Hoàn thành!');

        setTimeout(() => {
            showLoading(false);
        }, 300);

    } catch (error) {
        console.error('=== PDF LOADING ERROR ===');
        console.error('Error:', error);
        showLoading(false);

        let errorMessage = 'Lỗi khi tải file PDF. ';
        if (error.name === 'MissingPDFException') {
            errorMessage = 'File PDF không tồn tại hoặc đường dẫn không đúng.';
        } else if (error.name === 'UnexpectedResponseException') {
            errorMessage = 'Server không phản hồi hoặc file bị lỗi.';
        } else if (error.name === 'InvalidPDFException') {
            errorMessage = 'File PDF bị hỏng hoặc không hợp lệ.';
        } else {
            errorMessage += error.message;
        }

        alert(errorMessage);
    }
}

/**
 * Update loading progress
 */
function updateLoadingProgress(percent, message) {
    const progressBar = document.getElementById('loadingProgressBar');
    const percentageText = document.getElementById('loadingPercentage');
    const loadingInfo = document.getElementById('loadingInfo');
    const loadingText = document.getElementById('loadingText');

    if (progressBar) {
        progressBar.style.width = percent + '%';
        progressBar.setAttribute('aria-valuenow', percent);
    }
    if (percentageText) {
        percentageText.textContent = percent + '%';
    }
    if (loadingInfo && message) {
        loadingInfo.textContent = message;
    }
    if (loadingText && percent < 100) {
        loadingText.textContent = 'Đang tải PDF...';
    } else if (loadingText) {
        loadingText.textContent = 'Sẵn sàng đọc!';
    }
}

/**
 * Render specific page
 */
async function renderPage(num) {
    if (!pdfDoc) {
        console.error('PDF document not loaded yet');
        return;
    }

    try {
        console.log('Rendering page:', num);
        canvas.style.opacity = '0.5';

        const page = await pdfDoc.getPage(num);

        // Calculate scale
        let renderScale = scale;
        if (scale === 1.0) {
            const container = document.querySelector('.pdf-canvas-container');
            const containerWidth = container.clientWidth - 40;
            const pageViewport = page.getViewport({ scale: 1.0 });
            renderScale = Math.min(containerWidth / pageViewport.width, 2.0);
        }

        const viewport = page.getViewport({ scale: renderScale });

        // Set canvas size with device pixel ratio
        const outputScale = window.devicePixelRatio || 1;
        canvas.width = Math.floor(viewport.width * outputScale);
        canvas.height = Math.floor(viewport.height * outputScale);
        canvas.style.width = Math.floor(viewport.width) + 'px';
        canvas.style.height = Math.floor(viewport.height) + 'px';

        const transform = outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null;

        const renderContext = {
            canvasContext: ctx,
            viewport: viewport,
            transform: transform
        };

        await page.render(renderContext).promise;

        canvas.style.opacity = '1';

        // Update UI
        document.getElementById('pageInput').value = num;
        pageNum = num;
        scale = renderScale;

        // Save progress (debounced)
        if (window.saveProgressTimeout) {
            clearTimeout(window.saveProgressTimeout);
        }
        window.saveProgressTimeout = setTimeout(() => {
            saveProgress();
        }, 2000);

    } catch (error) {
        console.error('Error rendering page:', error);
        canvas.style.opacity = '1';
        alert('Lỗi khi hiển thị trang ' + num + ': ' + error.message);
    }
}

/**
 * Navigation functions
 */
function previousPage() {
    if (pageNum > 1) {
        renderPage(pageNum - 1);
    }
}

function nextPage() {
    if (pageNum < pageCount) {
        renderPage(pageNum + 1);
    }
}

function goToPage(page) {
    const pageNumber = parseInt(page);
    if (pageNumber >= 1 && pageNumber <= pageCount) {
        renderPage(pageNumber);
    } else {
        document.getElementById('pageInput').value = pageNum;
    }
}

/**
 * Zoom functions
 */
function zoomIn() {
    scale = Math.min(scale * 1.25, 3.0);
    document.getElementById('zoomInput').value = Math.round(scale * 100);
    renderPage(pageNum);
}

function zoomOut() {
    scale = Math.max(scale * 0.8, 0.25);
    document.getElementById('zoomInput').value = Math.round(scale * 100);
    renderPage(pageNum);
}

function setZoom(zoomPercent) {
    scale = parseInt(zoomPercent) / 100;
    scale = Math.max(0.25, Math.min(scale, 3.0));
    renderPage(pageNum);
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
    } else {
        btn.innerHTML = '<i class="fas fa-moon"></i> Chế độ tối';
    }

    // Save preference
    fetch('/reading/api/toggle-mode', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `mode=${isDark ? 'dark' : 'light'}`
    }).catch(error => {
        console.warn('Could not save dark mode preference:', error);
    });
}

/**
 * Save reading progress
 */
async function saveProgress() {
    try {
        console.log('=== SAVING PROGRESS ===');
        console.log('Page:', pageNum, '/', pageCount);

        const formData = new FormData();
        formData.append('currentPage', pageNum);
        formData.append('totalPages', pageCount);

        const response = await fetch(`/reading/api/progress/${bookId}`, {
            method: 'POST',
            credentials: 'same-origin',
            body: formData
        });

        if (!response.ok) {
            console.error('Failed to save progress:', response.status);
        } else {
            console.log('✅ Progress saved:', pageNum, '/', pageCount);
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

    // Sort by page number
    bookmarks.sort((a, b) => (a.pageNumber || 0) - (b.pageNumber || 0));

    // Store bookmarks data for click handlers
    window.bookmarksData = {};
    bookmarks.forEach(bm => {
        window.bookmarksData[bm.id] = bm.location;
    });

    container.innerHTML = bookmarks.map(bm => `
        <div class="bookmark-item" onclick="jumpToBookmarkById('${bm.id}')">
            <div class="bookmark-header">
                <strong>📖 Trang ${bm.pageNumber || '?'}</strong>
                <button class="bookmark-delete-btn"
                        onclick="deleteBookmark('${bm.id}', event)"
                        title="Xóa bookmark">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
            ${bm.note ? `<div class="bookmark-note">"${escapeHtml(bm.note)}"</div>` : ''}
            <div class="bookmark-meta">
                <span>${bm.percentage ? bm.percentage.toFixed(1) + '%' : ''}</span>
                <span>${formatBookmarkDate(bm.createdAt)}</span>
            </div>
        </div>
    `).join('');
}

/**
 * Save manual bookmark
 */
async function saveManualBookmark() {
    const note = prompt('Ghi chú cho bookmark (tùy chọn):');
    if (note === null) return;

    try {
        const formData = new FormData();
        formData.append('location', `page-${pageNum}`);
        formData.append('pageNumber', pageNum);
        formData.append('percentage', ((pageNum / pageCount) * 100).toFixed(2));
        formData.append('note', note || '');

        console.log('=== SAVING BOOKMARK ===');
        console.log('Page:', pageNum);

        const response = await fetch(`/reading/api/bookmarks/${bookId}`, {
            method: 'POST',
            credentials: 'same-origin',
            body: formData
        });

        if (!response.ok) {
            throw new Error('Failed to save bookmark');
        }

        alert('✅ Bookmark đã lưu!');

        if (document.getElementById('bookmarksSidebar').classList.contains('show')) {
            loadBookmarks();
        }
    } catch (error) {
        console.error('Error saving bookmark:', error);
        alert('❌ Lỗi khi lưu bookmark: ' + error.message);
    }
}

/**
 * Jump to bookmark by ID
 */
function jumpToBookmarkById(bookmarkId) {
    const location = window.bookmarksData[bookmarkId];
    if (location) {
        jumpToBookmark(location);
    }
}

/**
 * Jump to bookmark
 */
function jumpToBookmark(location) {
    console.log('Jumping to bookmark:', location);
    const pageNumber = parseInt(location.split('-')[1]);
    if (pageNumber && pageNumber >= 1 && pageNumber <= pageCount) {
        renderPage(pageNumber);
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
    if (show) {
        overlay.classList.add('show');
    } else {
        overlay.classList.remove('show');
    }
}

// ==================== EVENT LISTENERS ====================

// Initialize when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        console.log('DOM loaded, starting PDF.js check...');
        initPDFViewer();
    });
} else {
    console.log('DOM already loaded, starting PDF.js check...');
    initPDFViewer();
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    if (e.target.tagName.toLowerCase() === 'input') return;

    switch(e.key) {
        case 'ArrowLeft':
            e.preventDefault();
            previousPage();
            break;
        case 'ArrowRight':
            e.preventDefault();
            nextPage();
            break;
        case 'Home':
            e.preventDefault();
            goToPage(1);
            break;
        case 'End':
            e.preventDefault();
            goToPage(pageCount);
            break;
        case '=':
        case '+':
            e.preventDefault();
            zoomIn();
            break;
        case '-':
            e.preventDefault();
            zoomOut();
            break;
    }
});

// Save progress when leaving page
window.addEventListener('beforeunload', saveProgress);

