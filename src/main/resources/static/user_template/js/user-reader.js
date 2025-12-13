/**
 * User Book Reader JavaScript
 * Handles reader page functions
 */

// Check available formats when page loads
document.addEventListener('DOMContentLoaded', function() {
    checkAvailableFormats();
});

function checkAvailableFormats() {
    const pdfCard = document.getElementById('pdfCard');
    const epubCard = document.getElementById('epubCard');

    if (!pdfCard || !epubCard) return;

    let hasPDF = false;
    let hasEPUB = false;

    // Check asset types from window.readerData
    if (window.readerData && window.readerData.availableAssets && window.readerData.availableAssets.length > 0) {
        window.readerData.availableAssets.forEach(asset => {
            if (asset.assetType === 'PDF') hasPDF = true;
            if (asset.assetType === 'EPUB') hasEPUB = true;
        });
    } else {
        // Default - assume PDF is available
        hasPDF = true;
    }

    if (hasPDF) {
        pdfCard.classList.add('available');
    } else {
        pdfCard.classList.add('unavailable');
        pdfCard.onclick = () => alert('File PDF không khả dụng cho sách này');
    }

    if (hasEPUB) {
        epubCard.classList.add('available');
    } else {
        epubCard.classList.add('unavailable');
        epubCard.onclick = () => alert('File EPUB không khả dụng cho sách này');
    }
}

function openReader(format) {
    const bookId = window.readerData ? window.readerData.bookId : null;
    if (!bookId) {
        alert('Không tìm thấy thông tin sách');
        return;
    }

    // Add loading state
    const formatOptions = document.getElementById('formatOptions');
    if (formatOptions) {
        formatOptions.classList.add('loading');
    }

    let url;
    switch(format) {
        case 'pdf':
            url = `/reading/pdf/${bookId}`;
            break;
        case 'epub':
            url = `/reading/epub/${bookId}`;
            break;
        case 'auto':
        default:
            url = `/reading/book/${bookId}`;
            break;
    }

    // Navigate to reader
    window.location.href = url;
}

function showBookmarks() {
    const bookId = window.readerData ? window.readerData.bookId : null;
    if (!bookId) return;

    const bookmarks = getBookmarks(bookId);
    if (bookmarks.length === 0) {
        alert('Bạn chưa có bookmark nào cho cuốn sách này.');
        return;
    }

    let message = 'Bookmarks của bạn:\n\n';
    bookmarks.forEach((bookmark, index) => {
        const date = new Date(bookmark.timestamp).toLocaleDateString('vi-VN');
        message += `${index + 1}. `;
        if (bookmark.page) {
            message += `Trang ${bookmark.page}`;
        } else if (bookmark.percentage) {
            message += `${bookmark.percentage}%`;
        }
        message += ` - ${date}`;
        if (bookmark.note) {
            message += `\n   "${bookmark.note}"`;
        }
        message += '\n\n';
    });

    alert(message);
}

function getBookmarks(bookId) {
    try {
        const stored = localStorage.getItem(`bookmarks_${bookId}`);
        return stored ? JSON.parse(stored) : [];
    } catch {
        return [];
    }
}

function shareBook() {
    const bookTitle = window.readerData ? window.readerData.bookTitle : 'Tên sách';

    if (navigator.share) {
        navigator.share({
            title: bookTitle,
            text: 'Tôi đang đọc cuốn sách này, rất hay!',
            url: window.location.href
        });
    } else {
        // Fallback for browsers that don't support Web Share API
        const url = window.location.href;
        navigator.clipboard.writeText(url).then(() => {
            alert('Đã copy link sách vào clipboard!');
        }).catch(() => {
            prompt('Copy link này để chia sẻ:', url);
        });
    }
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    if (e.target.tagName.toLowerCase() === 'input') return;

    switch(e.key) {
        case '1':
            openReader('pdf');
            break;
        case '2':
            openReader('epub');
            break;
        case '3':
        case 'Enter':
            openReader('auto');
            break;
        case 'b':
            showBookmarks();
            break;
        case 'Escape':
            window.history.back();
            break;
    }
});

