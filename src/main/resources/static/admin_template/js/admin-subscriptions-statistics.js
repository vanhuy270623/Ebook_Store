/**
 * Admin Subscriptions Statistics Page JavaScript
 * Handles Chart.js initialization for subscriptions statistics
 */
$(document).ready(function() {
    // Get data from global variables set by Thymeleaf
    var orderCountData = window.subscriptionStats.orderCountData || {};
    var revenueData = window.subscriptionStats.revenueData || {};

    // Order Count Chart
    var ctxOrder = document.getElementById('orderCountChart').getContext('2d');
    new Chart(ctxOrder, {
        type: 'bar',
        data: {
            labels: Object.keys(orderCountData),
            datasets: [{
                label: 'Số đơn hàng',
                data: Object.values(orderCountData),
                backgroundColor: '#3c8dbc'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });

    // Revenue Chart
    var ctxRevenue = document.getElementById('revenueChart').getContext('2d');
    new Chart(ctxRevenue, {
        type: 'bar',
        data: {
            labels: Object.keys(revenueData),
            datasets: [{
                label: 'Doanh thu (VNĐ)',
                data: Object.values(revenueData),
                backgroundColor: '#00a65a'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
});

