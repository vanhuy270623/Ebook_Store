/**
 * Book Download Handler with Format Selection
 * Xử lý tải xuống sách với modal chọn format (PDF/EPUB)
 */

document.addEventListener('DOMContentLoaded', function() {
    // Kiểm tra SweetAlert2 đã load chưa
    if (typeof Swal === 'undefined') {
        console.error('SweetAlert2 chưa được load. Vui lòng thêm SweetAlert2 CDN vào head.');
        return;
    }

    // Lấy danh sách assets từ Thymeleaf
    const bookAssetsData = document.getElementById('bookAssetsData');
    let bookAssets = [];

    if (bookAssetsData) {
        try {
            const jsonText = bookAssetsData.textContent.trim();
            console.log('Raw bookAssetsData:', jsonText);

            if (jsonText) {
                bookAssets = JSON.parse(jsonText);
                console.log('Parsed bookAssets:', bookAssets);
            }
        } catch (e) {
            console.error('Error parsing book assets:', e);
            console.error('Content was:', bookAssetsData.textContent);
        }
    } else {
        console.warn('bookAssetsData element not found');
    }

    // Xử lý các nút download mới (button với class book-download-btn)
    const downloadButtons = document.querySelectorAll('.book-download-btn');

    console.log('Found', downloadButtons.length, 'download buttons');
    console.log('BookAssets available:', bookAssets.length);

    downloadButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();

            const bookId = this.getAttribute('data-book-id');

            console.log('Download clicked for bookId:', bookId);
            console.log('Available assets:', bookAssets);

            if (!bookAssets || bookAssets.length === 0) {
                console.error('No assets available, falling back to direct download');
                // Fallback: tải trực tiếp không cần chọn format
                downloadFile(bookId, null);
                return;
            }

            // Nếu chỉ có 1 file, tải luôn
            if (bookAssets.length === 1) {
                downloadFile(bookId, bookAssets[0].fileType);
            } else {
                // Có nhiều file, hiện modal chọn
                showFormatModal(bookId, bookAssets);
            }
        });
    });

    // Tìm tất cả các link download cũ (legacy support)
    const downloadLinks = document.querySelectorAll('a[href*="/books/download/"]');

    downloadLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const downloadUrl = this.getAttribute('href');
            // Extract bookId from URL
            const matches = downloadUrl.match(/\/books\/download\/([^?]+)/);
            if (matches && matches[1]) {
                const bookId = matches[1];

                if (!bookAssets || bookAssets.length === 0) {
                    // Fallback to direct download
                    directDownload(downloadUrl);
                } else if (bookAssets.length === 1) {
                    downloadFile(bookId, bookAssets[0].fileType);
                } else {
                    showFormatModal(bookId, bookAssets);
                }
            } else {
                directDownload(downloadUrl);
            }
        });
    });
});

/**
 * Hiển thị modal chọn format
 */
function showFormatModal(bookId, assets) {
    const modal = new bootstrap.Modal(document.getElementById('downloadFormatModal'));
    const optionsContainer = document.getElementById('downloadOptions');

    // Clear options
    optionsContainer.innerHTML = '';

    // Tạo button cho mỗi format
    assets.forEach(asset => {
        const button = document.createElement('button');
        button.className = 'btn btn-outline-primary btn-lg';
        button.style.textAlign = 'left';

        const icon = asset.fileType === 'PDF' ? 'fa-file-pdf' : 'fa-book';
        const color = asset.fileType === 'PDF' ? 'text-danger' : 'text-success';
        const size = asset.fileSize ? formatFileSize(asset.fileSize) : 'N/A';

        button.innerHTML = `
            <i class="fas ${icon} ${color} me-2"></i>
            <strong>${asset.fileType}</strong>
            <span class="text-muted ms-2">(${size})</span>
        `;

        button.addEventListener('click', function() {
            modal.hide();
            downloadFile(bookId, asset.fileType);
        });

        optionsContainer.appendChild(button);
    });

    modal.show();
}

/**
 * Tải file với fileType cụ thể
 */
function downloadFile(bookId, fileType) {
    const bookTitle = document.querySelector('h2')?.textContent || 'sách này';

    // Build URL với hoặc không có fileType parameter
    let downloadUrl = `/books/download/${bookId}`;
    if (fileType) {
        downloadUrl += `?fileType=${fileType}`;
    }

    console.log('Downloading from:', downloadUrl);

    // Hiện loading
    Swal.fire({
        title: 'Đang chuẩn bị tải xuống...',
        text: 'Vui lòng đợi trong giây lát',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    // Gọi API download
    fetch(downloadUrl, {
        method: 'GET',
        credentials: 'include'
    })
    .then(response => {
        if (response.ok) {
            // Lấy filename từ header
            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = fileType ? `ebook.${fileType.toLowerCase()}` : 'ebook.pdf';

            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/);
                if (filenameMatch && filenameMatch[1]) {
                    filename = decodeURIComponent(filenameMatch[1]);
                }
            }

            return response.blob().then(blob => ({
                blob: blob,
                filename: filename
            }));
        } else if (response.status === 401) {
            throw new Error('Bạn cần đăng nhập để tải xuống sách.');
        } else if (response.status === 403) {
            const errorReason = response.headers.get('X-Download-Error');
            if (errorReason) {
                throw new Error(decodeURIComponent(errorReason));
            }
            throw new Error('Bạn không có quyền tải xuống sách này.');
        } else if (response.status === 404) {
            const errorReason = response.headers.get('X-Download-Error');
            if (errorReason) {
                throw new Error(decodeURIComponent(errorReason));
            }
            throw new Error('Không tìm thấy file sách.');
        } else {
            throw new Error('Lỗi khi tải xuống sách. Vui lòng thử lại sau.');
        }
    })
    .then(data => {
        // Tải xuống file
        const url = window.URL.createObjectURL(data.blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = data.filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

        // Đóng loading và hiện thông báo thành công
        const formatText = fileType ? ` (${fileType})` : '';
        Swal.fire({
            icon: 'success',
            title: 'Tải xuống thành công!',
            text: `"${bookTitle}"${formatText} đã được tải về máy của bạn.`,
            timer: 3000,
            showConfirmButton: false
        });
    })
    .catch(error => {
        // Hiện thông báo lỗi
        Swal.fire({
            icon: 'error',
            title: 'Không thể tải xuống',
            html: error.message,
            confirmButtonText: 'Đóng',
            confirmButtonColor: '#3085d6'
        });
    });
}

/**
 * Tải file trực tiếp (legacy fallback)
 */
function directDownload(downloadUrl) {
    const bookTitle = document.querySelector('h2')?.textContent || 'sách này';

    // Hiện loading
    Swal.fire({
        title: 'Đang chuẩn bị tải xuống...',
        text: 'Vui lòng đợi trong giây lát',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    // Gọi API download
    fetch(downloadUrl, {
        method: 'GET',
        credentials: 'include'
    })
    .then(response => {
        if (response.ok) {
            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = 'ebook.pdf';

            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/);
                if (filenameMatch && filenameMatch[1]) {
                    filename = decodeURIComponent(filenameMatch[1]);
                }
            }

            return response.blob().then(blob => ({
                blob: blob,
                filename: filename
            }));
        } else if (response.status === 401) {
            throw new Error('Bạn cần đăng nhập để tải xuống sách.');
        } else if (response.status === 403) {
            const errorReason = response.headers.get('X-Download-Error');
            if (errorReason) {
                throw new Error(decodeURIComponent(errorReason));
            }
            throw new Error('Bạn không có quyền tải xuống sách này.');
        } else if (response.status === 404) {
            const errorReason = response.headers.get('X-Download-Error');
            if (errorReason) {
                throw new Error(decodeURIComponent(errorReason));
            }
            throw new Error('Không tìm thấy file sách.');
        } else {
            throw new Error('Lỗi khi tải xuống sách. Vui lòng thử lại sau.');
        }
    })
    .then(data => {
        const url = window.URL.createObjectURL(data.blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = data.filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

        Swal.fire({
            icon: 'success',
            title: 'Tải xuống thành công!',
            text: `"${bookTitle}" đã được tải về máy của bạn.`,
            timer: 3000,
            showConfirmButton: false
        });
    })
    .catch(error => {
        Swal.fire({
            icon: 'error',
            title: 'Không thể tải xuống',
            html: error.message,
            confirmButtonText: 'Đóng',
            confirmButtonColor: '#3085d6'
        });
    });
}

/**
 * Format file size
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
