/**
 * Admin Dashboard Page Scripts
 * Chart.js initialization and data visualization
 */

$(document).ready(function() {
    // Sales Chart
    initSalesChart();
});

/**
 * Initialize sales chart with Chart.js
 */
function initSalesChart() {
    var ctx = document.getElementById('salesChart');
    if (!ctx) return;

    ctx = ctx.getContext('2d');
    var salesChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
            datasets: [{
                label: 'Doanh thu (triệu đồng)',
                data: [12, 19, 15, 25, 22, 30, 28, 35, 32, 40, 38, 45],
                borderColor: '#3c8dbc',
                backgroundColor: 'rgba(60, 141, 188, 0.1)',
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}


