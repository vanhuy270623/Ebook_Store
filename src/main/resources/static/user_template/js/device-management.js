/**
 * Device Management JavaScript
 * File: device-management.js
 * Purpose: Handle device management interactions
 */

$(document).ready(function() {
    // Variables
    let deviceIdToRemove = null;
    const removeModal = new bootstrap.Modal(document.getElementById('removeDeviceModal'));

    /**
     * Handle remove device button click
     */
    $('.btn-remove-device').click(function() {
        deviceIdToRemove = $(this).data('device-id');
        const deviceName = $(this).data('device-name');
        $('#deviceNameToRemove').text(deviceName);
        removeModal.show();
    });

    /**
     * Confirm remove device
     */
    $('#confirmRemoveDevice').click(function() {
        if (!deviceIdToRemove) {
            console.error('No device ID to remove');
            return;
        }

        // Get CSRF token
        const csrfToken = $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

        $.ajax({
            url: '/user/devices/' + deviceIdToRemove + '/remove',
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            },
            success: function(response) {
                if (response.success) {
                    removeModal.hide();
                    Swal.fire({
                        icon: 'success',
                        title: 'Thành công',
                        text: response.message || 'Đã xóa thiết bị thành công',
                        timer: 2000,
                        showConfirmButton: false
                    }).then(() => {
                        location.reload();
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi',
                        text: response.message || 'Không thể xóa thiết bị'
                    });
                }
            },
            error: function(xhr, status, error) {
                console.error('Error removing device:', error);
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: 'Không thể xóa thiết bị. Vui lòng thử lại.'
                });
            }
        });
    });

    /**
     * Handle flash messages
     */
    const successMsg = $('[data-flash-success]').data('flash-success');
    const errorMsg = $('[data-flash-error]').data('flash-error');

    if (successMsg) {
        Swal.fire({
            icon: 'success',
            title: 'Thành công',
            text: successMsg,
            timer: 3000,
            showConfirmButton: false
        });
    }

    if (errorMsg) {
        Swal.fire({
            icon: 'error',
            title: 'Lỗi',
            text: errorMsg
        });
    }

    /**
     * Add tooltip for disabled buttons
     */
    $('[data-bs-toggle="tooltip"]').tooltip();

    /**
     * Handle modal close
     */
    $('#removeDeviceModal').on('hidden.bs.modal', function () {
        deviceIdToRemove = null;
    });

    /**
     * Animate device cards on load
     */
    $('.device-card').each(function(index) {
        $(this).css({
            'animation-delay': (index * 0.1) + 's'
        });
    });

    /**
     * Update trust score color dynamically
     */
    $('.trust-score-value').each(function() {
        const score = parseInt($(this).text());
        if (score >= 80) {
            $(this).addClass('trust-score-high');
        } else if (score >= 50) {
            $(this).addClass('trust-score-medium');
        } else {
            $(this).addClass('trust-score-low');
        }
    });

    /**
     * Handle device limit warning
     */
    const currentCount = parseInt($('.device-count-current').text());
    const maxDevices = parseInt($('.device-count-max').text());

    if (currentCount >= maxDevices) {
        $('.device-limit-warning').show();
    }

    /**
     * Format last login time
     */
    function formatLastLogin(days) {
        if (days === 0) {
            return 'Hôm nay';
        } else if (days === 1) {
            return 'Hôm qua';
        } else if (days < 7) {
            return days + ' ngày trước';
        } else if (days < 30) {
            return Math.floor(days / 7) + ' tuần trước';
        } else {
            return Math.floor(days / 30) + ' tháng trước';
        }
    }

    /**
     * Log device management actions
     */
    function logAction(action, deviceId) {
        console.log('Device Management Action:', {
            action: action,
            deviceId: deviceId,
            timestamp: new Date().toISOString()
        });
    }
});

