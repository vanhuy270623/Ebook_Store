/**
 * Reading Progress Tracker với Anti-Skimming Support
 *
 * Module này giúp:
 * - Track thời gian đọc thực tế (active time)
 * - Chuẩn hóa vị trí đọc từ PDF/EPUB về % chung
 * - Đồng bộ tiến độ lên server định kỳ
 * - Phát hiện và cảnh báo khi user đọc quá nhanh
 *
 * Usage:
 * const tracker = new ReadingProgressTracker({
 *     bookId: 'book_001',
 *     bookAssetId: 'asset_pdf_001',
 *     format: 'PDF',  // hoặc 'EPUB'
 *     syncInterval: 30000  // sync mỗi 30 giây
 * });
 *
 * tracker.updateProgress('page-15', 25.5);  // Cập nhật vị trí
 * tracker.start();  // Bắt đầu tracking
 */

class ReadingProgressTracker {
    constructor(options) {
        this.bookId = options.bookId;
        this.bookAssetId = options.bookAssetId;
        this.format = options.format || 'PDF';
        this.syncInterval = options.syncInterval || 30000; // Mặc định 30 giây

        // Tracking state
        this.currentLocation = null;
        this.currentProgress = 0.0;
        this.lastSyncTime = Date.now();
        this.activeTimeAccumulator = 0; // Tích lũy giây active
        this.isActive = false;
        this.syncTimer = null;

        // Callbacks
        this.onSyncSuccess = options.onSyncSuccess || null;
        this.onSyncError = options.onSyncError || null;
        this.onSkimmingDetected = options.onSkimmingDetected || null;

        // Bind event listeners
        this._setupActivityDetection();
    }

    /**
     * Setup phát hiện user đang active (đọc thật) hay không
     */
    _setupActivityDetection() {
        // Các event cho thấy user đang tương tác
        const activityEvents = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart'];

        let activityTimer = null;

        const markActive = () => {
            this.isActive = true;

            // Reset timer - sau 5 giây không có activity thì coi như inactive
            if (activityTimer) clearTimeout(activityTimer);
            activityTimer = setTimeout(() => {
                this.isActive = false;
            }, 5000);
        };

        activityEvents.forEach(event => {
            document.addEventListener(event, markActive, { passive: true });
        });

        // Tab visibility - pause khi user chuyển tab
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                this.isActive = false;
            }
        });
    }

    /**
     * Bắt đầu tracking và auto-sync
     */
    start() {
        console.log('[ProgressTracker] Started for book:', this.bookId);
        this.lastSyncTime = Date.now();
        this.isActive = true;

        // Accumulate active time mỗi giây
        this.activeTimer = setInterval(() => {
            if (this.isActive) {
                this.activeTimeAccumulator++;
            }
        }, 1000);

        // Auto sync theo interval
        this.syncTimer = setInterval(() => {
            this.syncToServer();
        }, this.syncInterval);
    }

    /**
     * Dừng tracking
     */
    stop() {
        console.log('[ProgressTracker] Stopped');
        if (this.activeTimer) clearInterval(this.activeTimer);
        if (this.syncTimer) clearInterval(this.syncTimer);

        // Sync lần cuối trước khi thoát
        this.syncToServer();
    }

    /**
     * Cập nhật vị trí đọc hiện tại
     *
     * @param {string} location - Vị trí raw (page-15 hoặc CFI)
     * @param {number} progressPercentage - % đã chuẩn hóa (0-100)
     */
    updateProgress(location, progressPercentage) {
        this.currentLocation = location;
        this.currentProgress = progressPercentage;
    }

    /**
     * Đồng bộ tiến độ lên server
     */
    async syncToServer() {
        if (!this.currentLocation || this.activeTimeAccumulator === 0) {
            console.log('[ProgressTracker] Skipping sync - no data to sync');
            return;
        }

        const payload = {
            bookId: this.bookId,
            bookAssetId: this.bookAssetId,
            currentLocationRaw: this.currentLocation,
            progressPercentage: this.currentProgress,
            activeTimeDelta: this.activeTimeAccumulator,
            format: this.format
        };

        console.log('[ProgressTracker] Syncing to server:', payload);

        try {
            const response = await fetch('/api/reading/sync', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (data.success) {
                console.log('[ProgressTracker] Sync successful:', data);

                // Reset accumulator sau khi sync thành công
                this.activeTimeAccumulator = 0;
                this.lastSyncTime = Date.now();

                // Callback
                if (this.onSyncSuccess) {
                    this.onSyncSuccess(data);
                }

                // Cảnh báo nếu phát hiện skimming (time-capping occurred)
                // Check both 'isSkimming' and 'skimming' (Jackson may strip 'is' prefix)
                const isSkimmingDetected = data.isSkimming || data.skimming;
                if (isSkimmingDetected) {
                    console.warn('[ProgressTracker] Time-capping applied! Velocity:', data.readingVelocity);

                    // Show warning toast
                    console.log('[ProgressTracker] About to call _showToast, this:', this);
                    console.log('[ProgressTracker] _showToast exists?', typeof this._showToast);

                    try {
                        this._showToast(
                            'warning',
                            'Tiến độ đã được điều chỉnh',
                            data.message || 'Hãy dành thời gian đọc kỹ để hiểu sâu nội dung.',
                            6000
                        );
                        console.log('[ProgressTracker] _showToast called successfully');
                    } catch (error) {
                        console.error('[ProgressTracker] Error calling _showToast:', error);
                        // Fallback: show alert
                        alert('⚠️ CẢNH BÁO SKIMMING\n\n' + (data.message || 'Đang đọc quá nhanh!'));
                    }

                    if (this.onSkimmingDetected) {
                        this.onSkimmingDetected(data);
                    }
                } else {
                    // Normal sync - show subtle success message
                    console.log('[ProgressTracker] Normal reading - progress saved:', data.currentProgress + '%');
                }

                // Hiển thị thông báo tiến độ đủ điều kiện review
                if (data.canReview && !this._reviewNotificationShown) {
                    this._showReviewEligibilityNotification();
                    this._reviewNotificationShown = true;
                }

                // Hiển thị thông báo hoàn thành sách
                // Check both 'isCompleted' and 'completed' (Jackson may strip 'is' prefix)
                const isCompletedDetected = data.isCompleted || data.completed;
                if (isCompletedDetected && !this._completionNotificationShown) {
                    this._showToast(
                        'success',
                        'Chúc mừng!',
                        'Bạn đã hoàn thành cuốn sách này!',
                        7000
                    );
                    this._completionNotificationShown = true;
                }

            } else {
                console.error('[ProgressTracker] Sync failed:', data.message);

                // Show error toast
                this._showToast(
                    'error',
                    'Lỗi đồng bộ',
                    data.message || 'Không thể lưu tiến độ đọc. Vui lòng thử lại.',
                    5000
                );

                if (this.onSyncError) {
                    this.onSyncError(data);
                }
            }

        } catch (error) {
            console.error('[ProgressTracker] Network error:', error);
            if (this.onSyncError) {
                this.onSyncError(error);
            }
        }
    }

    /**
     * Hiển thị cảnh báo đọc lướt
     */
    _showSkimmingWarning() {
        // Get or create toast container
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        // Create toast element
        const toast = document.createElement('div');
        toast.className = 'reading-toast warning';
        toast.innerHTML = `
            <i class="fas fa-exclamation-triangle"></i>
            <span><strong>Đọc quá nhanh!</strong><br>Tiến độ đã được điều chỉnh. Hãy đọc kỹ để hiểu sâu nội dung.</span>
        `;

        // Add click to dismiss
        toast.onclick = function() {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        };

        container.appendChild(toast);

        // Show toast with animation
        setTimeout(() => {
            toast.classList.add('show');
        }, 100);

        // Auto hide after 6 seconds
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 6000);
    }

    /**
     * Thông báo đủ điều kiện review
     */
    _showReviewEligibilityNotification() {
        // Get or create toast container
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        // Create toast element
        const toast = document.createElement('div');
        toast.className = 'reading-toast success';
        toast.innerHTML = `
            <i class="fas fa-check-circle"></i>
            <span><strong>Chúc mừng!</strong><br>Bạn đã đọc đủ 20% và có thể đánh giá sách này.</span>
        `;

        // Add click to dismiss
        toast.onclick = function() {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        };

        container.appendChild(toast);

        // Show toast with animation
        setTimeout(() => {
            toast.classList.add('show');
        }, 100);

        // Auto hide after 7 seconds
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 7000);
    }

    /**
     * Helper: Hiển thị toast notification chung
     * @param {string} type - 'success' | 'warning' | 'error' | 'info'
     * @param {string} title - Tiêu đề
     * @param {string} message - Nội dung
     * @param {number} duration - Thời gian hiển thị (ms)
     */
    _showToast(type, title, message, duration = 5000) {
        console.log('[Toast] Creating toast:', { type, title, message, duration });

        // Icon mapping
        const icons = {
            success: 'fa-check-circle',
            warning: 'fa-exclamation-triangle',
            error: 'fa-times-circle',
            info: 'fa-info-circle'
        };

        // Get or create toast container
        let container = document.getElementById('toast-container');
        if (!container) {
            console.log('[Toast] Container not found, creating new one');
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        console.log('[Toast] Container:', container);

        // Create toast element
        const toast = document.createElement('div');
        toast.className = `reading-toast ${type}`;
        toast.innerHTML = `
            <i class="fas ${icons[type] || icons.info}"></i>
            <span>${title ? `<strong>${title}</strong><br>` : ''}${message}</span>
        `;
        console.log('[Toast] Created element:', toast);

        // Add click to dismiss
        toast.onclick = function() {
            console.log('[Toast] Clicked to dismiss');
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        };

        container.appendChild(toast);
        console.log('[Toast] Appended to container');

        // Show toast with animation
        setTimeout(() => {
            toast.classList.add('show');
            console.log('[Toast] Added show class');
        }, 100);

        // Auto hide
        setTimeout(() => {
            toast.classList.remove('show');
            console.log('[Toast] Removed show class');
            setTimeout(() => {
                toast.remove();
                console.log('[Toast] Removed from DOM');
            }, 300);
        }, duration);
    }
}

// ========== PDF-specific Helper ==========

class PdfProgressAdapter {
    /**
     * Chuẩn hóa tiến độ PDF về %
     * @param {PDFViewer} pdfViewer - PDF.js viewer instance
     * @returns {Object} { location, percentage }
     */
    static normalize(pdfViewer) {
        const currentPage = pdfViewer.currentPageNumber;
        const totalPages = pdfViewer.pagesCount;
        const percentage = (currentPage / totalPages) * 100;

        return {
            location: `page-${currentPage}`,
            percentage: percentage
        };
    }

    /**
     * Resume từ vị trí đã lưu
     * @param {PDFViewer} pdfViewer
     * @param {string} savedLocation - "page-15"
     */
    static resume(pdfViewer, savedLocation) {
        if (!savedLocation) return;

        const match = savedLocation.match(/page-(\d+)/);
        if (match) {
            const pageNum = parseInt(match[1]);
            pdfViewer.currentPageNumber = pageNum;
        }
    }
}

// ========== EPUB-specific Helper ==========

class EpubProgressAdapter {
    /**
     * Chuẩn hóa tiến độ EPUB về %
     * @param {Book} epubBook - ePub.js book instance
     * @returns {Object} { location, percentage }
     */
    static normalize(epubBook) {
        const currentLocation = epubBook.rendition.currentLocation();
        const currentCfi = currentLocation.start.cfi;
        const percentage = epubBook.locations.percentageFromCfi(currentCfi) * 100;

        return {
            location: currentCfi,
            percentage: percentage
        };
    }

    /**
     * Resume từ vị trí đã lưu
     * @param {Rendition} rendition - ePub.js rendition
     * @param {string} savedLocation - CFI string
     */
    static async resume(rendition, savedLocation) {
        if (!savedLocation) return;

        try {
            await rendition.display(savedLocation);
        } catch (error) {
            console.error('Error resuming EPUB location:', error);
        }
    }
}

// Export cho cả browser và module
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        ReadingProgressTracker,
        PdfProgressAdapter,
        EpubProgressAdapter
    };
}

