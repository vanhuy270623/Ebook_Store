/**
 * Admin Subscriptions List Page JavaScript
 * Handles DataTable initialization
 */
$(function () {
    $('#subscriptionsTable').DataTable({
        'paging': true,
        'lengthChange': false,
        'searching': true,
        'ordering': true,
        'info': true,
        'autoWidth': false,
        'language': {
            'url': '//cdn.datatables.net/plug-ins/1.10.24/i18n/Vietnamese.json'
        }
    });
});

