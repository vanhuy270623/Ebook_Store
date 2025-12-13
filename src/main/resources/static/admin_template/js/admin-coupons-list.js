/**
 * Admin Coupons List Page JavaScript
 * Handles DataTable initialization
 */
$(function () {
    $('#couponsTable').DataTable({
        'paging': true,
        'lengthChange': true,
        'searching': true,
        'ordering': true,
        'info': true,
        'autoWidth': false,
        'pageLength': 25,
        'language': {
            'url': '//cdn.datatables.net/plug-ins/1.10.24/i18n/Vietnamese.json'
        }
    });
});

