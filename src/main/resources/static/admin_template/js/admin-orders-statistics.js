/**
 * Admin Orders Statistics Page JavaScript
 * Handles Chart.js initialization for orders statistics
 */
$(document).ready(function() {
    // Get data from global variables set by Thymeleaf
    var bookOrders = window.orderStats.bookOrders || 0;
    var subscriptionOrders = window.orderStats.subscriptionOrders || 0;
    var pendingOrders = window.orderStats.pendingOrders || 0;
    var completedOrders = window.orderStats.completedOrders || 0;
    var failedOrders = window.orderStats.failedOrders || 0;
    var cancelledOrders = window.orderStats.cancelledOrders || 0;

    // Order Type Chart
    var ctxType = document.getElementById('orderTypeChart').getContext('2d');
    new Chart(ctxType, {
        type: 'doughnut',
        data: {
            labels: ['Mua sách', 'Gói subscription'],
            datasets: [{
                data: [bookOrders, subscriptionOrders],
                backgroundColor: ['#3c8dbc', '#00c0ef']
            }]
        }
    });

    // Order Status Chart
    var ctxStatus = document.getElementById('orderStatusChart').getContext('2d');
    new Chart(ctxStatus, {
        type: 'pie',
        data: {
            labels: ['Chờ xử lý', 'Hoàn thành', 'Thất bại', 'Đã hủy'],
            datasets: [{
                data: [pendingOrders, completedOrders, failedOrders, cancelledOrders],
                backgroundColor: ['#f39c12', '#00a65a', '#dd4b39', '#d2d6de']
            }]
        }
    });
});

