/**
 * Admin Books Statistics Page JavaScript
 * Handles Chart.js initialization for books statistics
 */
$(document).ready(function() {
    // Access Type Chart - uses global variables from inline script
    var accessTypeCtx = document.getElementById('accessTypeChart').getContext('2d');
    var accessTypeChart = new Chart(accessTypeCtx, {
        type: 'doughnut',
        data: {
            labels: ['Miễn phí', 'Trả phí', 'Theo gói'],
            datasets: [{
                data: [
                    window.bookStats.freeBooks || 0,
                    window.bookStats.paidBooks || 0,
                    window.bookStats.subscriptionBooks || 0
                ],
                backgroundColor: [
                    '#00a65a',
                    '#f39c12',
                    '#dd4b39'
                ]
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            legend: {
                position: 'bottom'
            }
        }
    });

    // Sample Category Chart (would need actual category data)
    var categoryCtx = document.getElementById('categoryChart').getContext('2d');
    var categoryChart = new Chart(categoryCtx, {
        type: 'bar',
        data: {
            labels: ['Tiểu thuyết', 'Kỹ năng sống', 'Truyện tranh', 'Kinh tế', 'Thiếu nhi'],
            datasets: [{
                label: 'Số lượng sách',
                data: [12, 8, 15, 6, 10], // Sample data - should be dynamic
                backgroundColor: '#3c8dbc'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
});

