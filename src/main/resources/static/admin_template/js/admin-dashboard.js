/**
 * Admin Dashboard JavaScript
 * Handles charts and dynamic updates
 */

$(document).ready(function() {
    console.log('Dashboard loaded');

    // Initialize monthly revenue chart if data exists
    if (typeof monthlyRevenueData !== 'undefined' && monthlyRevenueData.length > 0) {
        initMonthlyRevenueChart(monthlyRevenueData);
    }

    // Auto-refresh statistics every 5 minutes (optional)
    // setInterval(refreshStatistics, 300000);

    // Animate numbers on load
    animateNumbers();
});

/**
 * Initialize monthly revenue chart
 */
function initMonthlyRevenueChart(data) {
    const ctx = document.getElementById('monthlyRevenueChart');
    if (!ctx) return;

    const labels = data.map(item => {
        const [year, month] = item[0].split('-');
        return `${month}/${year}`;
    });

    const values = data.map(item => item[1]);

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Doanh thu (đ)',
                data: values,
                backgroundColor: 'rgba(60, 141, 188, 0.1)',
                borderColor: 'rgba(60, 141, 188, 0.8)',
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                pointRadius: 4,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            label += new Intl.NumberFormat('vi-VN').format(context.parsed.y) + ' đ';
                            return label;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return new Intl.NumberFormat('vi-VN', {
                                notation: 'compact',
                                compactDisplay: 'short'
                            }).format(value) + 'đ';
                        }
                    }
                }
            }
        }
    });
}

/**
 * Animate numbers on page load
 */
function animateNumbers() {
    $('.info-box-number, .small-box h3').each(function() {
        const $this = $(this);
        const text = $this.text().trim();

        // Check if it's a number
        const number = parseFloat(text.replace(/[^0-9.-]+/g, ''));
        if (!isNaN(number) && number > 0) {
            $this.prop('Counter', 0).animate({
                Counter: number
            }, {
                duration: 1000,
                easing: 'swing',
                step: function(now) {
                    if (text.includes(',')) {
                        $this.text(Math.ceil(now).toLocaleString('vi-VN'));
                    } else {
                        $this.text(Math.ceil(now));
                    }
                }
            });
        }
    });
}

/**
 * Refresh statistics (optional - for real-time updates)
 */
function refreshStatistics() {
    $.ajax({
        url: '/admin/api/dashboard/stats',
        method: 'GET',
        success: function(data) {
            // Update statistics without page reload
            updateStatisticsDisplay(data);
        },
        error: function(xhr, status, error) {
            console.error('Failed to refresh statistics:', error);
        }
    });
}

/**
 * Update statistics display
 */
function updateStatisticsDisplay(data) {
    // Update each statistic
    if (data.totalOrders !== undefined) {
        $('#totalOrders').text(data.totalOrders.toLocaleString('vi-VN'));
    }
    if (data.totalRevenue !== undefined) {
        $('#totalRevenue').text(data.totalRevenue.toLocaleString('vi-VN') + ' đ');
    }
    if (data.todayRevenue !== undefined) {
        $('#todayRevenue').text(data.todayRevenue.toLocaleString('vi-VN'));
    }
    // Add more as needed
}

/**
 * Format currency
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(amount) + ' đ';
}

/**
 * Format date
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

/**
 * Show notification
 */
function showNotification(message, type = 'info') {
    // Using AdminLTE notification if available
    if (typeof toastr !== 'undefined') {
        toastr[type](message);
    } else {
        alert(message);
    }
}

/**
 * Export statistics to CSV
 */
function exportStatistics() {
    window.location.href = '/admin/dashboard/export';
}

/**
 * Print dashboard
 */
function printDashboard() {
    window.print();
}

