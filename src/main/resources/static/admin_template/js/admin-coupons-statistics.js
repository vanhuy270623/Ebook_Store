/**
 * Admin Coupons Statistics Page JavaScript
 * Handles Chart.js initialization for coupons statistics
 */
$(document).ready(function() {
    // Get data from global variables set by Thymeleaf
    var activeCoupons = window.couponStats.activeCoupons || 0;
    var expiredCoupons = window.couponStats.expiredCoupons || 0;
    var usedUpCoupons = window.couponStats.usedUpCoupons || 0;

    // Coupon Type Chart
    var ctxType = document.getElementById('couponTypeChart').getContext('2d');
    new Chart(ctxType, {
        type: 'doughnut',
        data: {
            labels: ['Phần trăm (%)', 'Cố định (VNĐ)'],
            datasets: [{
                data: [activeCoupons, expiredCoupons],
                backgroundColor: ['#3c8dbc', '#f39c12']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true
        }
    });

    // Coupon Status Chart
    var ctxStatus = document.getElementById('couponStatusChart').getContext('2d');
    new Chart(ctxStatus, {
        type: 'pie',
        data: {
            labels: ['Đang hoạt động', 'Hết hạn', 'Hết lượt'],
            datasets: [{
                data: [activeCoupons, expiredCoupons, usedUpCoupons],
                backgroundColor: ['#00a65a', '#dd4b39', '#f39c12']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true
        }
    });
});

