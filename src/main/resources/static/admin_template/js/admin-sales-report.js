/**
 * Admin Sales Report - JavaScript
 * Xử lý biểu đồ và filters cho trang báo cáo doanh thu
 */

(function() {
    'use strict';

    // Global variables
    var revenueChart;
    var sourceChart;
    var dailyRevenueData = {};
    var revenueByTypeData = {};
    var totalRevenueValue = 0;

    /**
     * Initialize data from Thymeleaf
     */
    function initData(dailyData, typeData, totalRevenue) {
        dailyRevenueData = dailyData || {};
        revenueByTypeData = typeData || {};
        totalRevenueValue = totalRevenue || 0;

        // Debug logs
        console.log('=== Sales Report Data ===');
        console.log('Total Revenue:', totalRevenueValue);
        console.log('Daily Revenue:', dailyRevenueData);
        console.log('Revenue by Type:', revenueByTypeData);
        console.log('========================');
    }

    /**
     * Helper function to parse date from dd/MM/yyyy format
     */
    function parseDate(dateStr) {
        var parts = dateStr.split('/');
        if (parts.length === 3) {
            // dd/MM/yyyy -> yyyy-MM-dd
            var day = parseInt(parts[0], 10);
            var month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
            var year = parseInt(parts[2], 10);
            return new Date(year, month, day);
        }
        return new Date(dateStr); // Fallback
    }

    /**
     * Create revenue over time chart
     */
    function createRevenueChart(period) {
        var labels = [];
        var data = [];

        // Process data based on period
        var entries = Object.entries(dailyRevenueData);

        // Sort entries by date (ascending - from past to present)
        entries.sort(function(a, b) {
            var dateA = parseDate(a[0]);
            var dateB = parseDate(b[0]);
            return dateA - dateB; // Ascending order
        });

        if (period === 'week') {
            // Last 7 days
            entries = entries.slice(-7);
        } else if (period === 'month') {
            // Last 30 days
            entries = entries.slice(-30);
        } else if (period === 'year') {
            // Group by month for year view
            var monthlyData = {};
            var monthOrder = [];

            entries.forEach(function(entry) {
                var date = entry[0];
                var amount = entry[1];
                var month = date.substring(3); // Get MM/yyyy

                if (!monthlyData[month]) {
                    monthlyData[month] = 0;
                    monthOrder.push(month);
                }
                monthlyData[month] += amount;
            });

            // Keep chronological order for months
            entries = monthOrder.map(function(month) {
                return [month, monthlyData[month]];
            });
        }

        entries.forEach(function(entry) {
            labels.push(entry[0]);
            data.push(entry[1]);
        });

        var ctx = document.getElementById('revenueChart');
        if (!ctx) return;

        // Destroy existing chart if exists
        if (revenueChart) {
            revenueChart.destroy();
        }

        revenueChart = new Chart(ctx.getContext('2d'), {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Doanh thu',
                    data: data,
                    borderColor: '#3498db',
                    backgroundColor: 'rgba(52, 152, 219, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 4,
                    pointBackgroundColor: '#3498db'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return context.parsed.y.toLocaleString('vi-VN') + ' ₫';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return (value / 1000000).toFixed(1) + 'M';
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Create revenue source pie chart
     */
    function createSourceChart() {
        var sourceLabels = [];
        var sourceData = [];
        var sourceColors = ['#2ecc71', '#f39c12', '#e74c3c'];
        var usedColors = [];

        var typeEntries = Object.entries(revenueByTypeData);

        if (typeEntries.length === 0) {
            // No data - show placeholder
            sourceLabels = ['Không có dữ liệu'];
            sourceData = [1];
            usedColors = ['#ecf0f1'];
        } else {
            typeEntries.forEach(function(entry, index) {
                var orderType = entry[0];
                var amount = entry[1];

                // Map order type to label
                var label = 'Khác';
                if (orderType === 'BOOK') {
                    label = 'Bán sách';
                } else if (orderType === 'SUBSCRIPTION') {
                    label = 'Đăng ký gói';
                }

                sourceLabels.push(label);
                sourceData.push(amount);
                usedColors.push(sourceColors[index % sourceColors.length]);
            });
        }

        var ctx2 = document.getElementById('sourceChart');
        if (!ctx2) return;

        sourceChart = new Chart(ctx2.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: sourceLabels,
                datasets: [{
                    data: sourceData,
                    backgroundColor: usedColors,
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        enabled: typeEntries.length > 0,
                        callbacks: {
                            label: function(context) {
                                var value = context.parsed;
                                var total = context.dataset.data.reduce((a, b) => a + b, 0);
                                var percentage = ((value / total) * 100).toFixed(1);
                                return context.label + ': ' + value.toLocaleString('vi-VN') + ' ₫ (' + percentage + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Setup chart period tabs
     */
    function setupChartTabs() {
        var tabButtons = document.querySelectorAll('.tab-btn');

        tabButtons.forEach(function(btn) {
            btn.addEventListener('click', function() {
                // Remove active class from all buttons
                tabButtons.forEach(function(b) {
                    b.classList.remove('active');
                });

                // Add active class to clicked button
                this.classList.add('active');

                // Update chart
                var period = this.getAttribute('data-period');
                createRevenueChart(period);
            });
        });
    }

    /**
     * Setup transaction filters
     */
    function setupTransactionFilters() {
        var typeFilter = document.getElementById('transactionTypeFilter');
        var timeFilter = document.getElementById('transactionTimeFilter');

        if (!typeFilter || !timeFilter) return;

        function filterTransactions() {
            var selectedType = typeFilter.value;
            var selectedTime = timeFilter.value;
            var now = new Date();

            var rows = document.querySelectorAll('.transaction-row');
            var visibleCount = 0;

            rows.forEach(function(row) {
                var orderType = row.getAttribute('data-order-type');
                var createdDateStr = row.getAttribute('data-created-date');

                // Parse ISO datetime format from Thymeleaf
                var createdDate = new Date(createdDateStr);

                var typeMatch = !selectedType || orderType === selectedType;
                var timeMatch = true;

                if (selectedTime === 'today') {
                    timeMatch = createdDate.toDateString() === now.toDateString();
                } else if (selectedTime === 'week') {
                    var weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                    timeMatch = createdDate >= weekAgo;
                } else if (selectedTime === 'month') {
                    var monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                    timeMatch = createdDate >= monthAgo;
                }

                if (typeMatch && timeMatch) {
                    row.style.display = '';
                    visibleCount++;
                } else {
                    row.style.display = 'none';
                }
            });

            // Show/hide no data message
            var noDataRow = document.querySelector('.transaction-table tbody tr:last-child');
            if (noDataRow && noDataRow.querySelector('td[colspan]')) {
                noDataRow.style.display = visibleCount === 0 ? '' : 'none';
            }
        }

        typeFilter.addEventListener('change', filterTransactions);
        timeFilter.addEventListener('change', filterTransactions);
    }

    /**
     * Initialize everything
     */
    function init(dailyData, typeData, totalRevenue) {
        // Load Chart.js if not already loaded
        if (typeof Chart === 'undefined') {
            console.error('Chart.js is not loaded!');
            return;
        }

        // Initialize data
        initData(dailyData, typeData, totalRevenue);

        // Create charts
        createRevenueChart('week'); // Default to week view
        createSourceChart();

        // Setup interactions
        setupChartTabs();
        setupTransactionFilters();
    }

    // Export to global scope
    window.SalesReport = {
        init: init
    };

})();

