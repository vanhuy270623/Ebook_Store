/**
 * Book Download Handler
 * Xử lý tải xuống sách với thông báo lỗi thân thiện
 */

document.addEventListener('DOMContentLoaded', function() {
    // Kiểm tra SweetAlert2 đã load chưa
    if (typeof Swal === 'undefined') {
        console.error('SweetAlert2 chưa được load. Vui lòng thêm SweetAlert2 CDN vào head.');
        return;
    }

    // Tìm tất cả các link download
    const downloadLinks = document.querySelectorAll('a[href*="/books/download/"]');

    downloadLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();

            const downloadUrl = this.getAttribute('href');
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
                    // Lấy filename từ header
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
                    // Lấy lý do từ header
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
                Swal.fire({
                    icon: 'success',
                    title: 'Tải xuống thành công!',
                    text: `"${bookTitle}" đã được tải về máy của bạn.`,
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
        });
    });
});

