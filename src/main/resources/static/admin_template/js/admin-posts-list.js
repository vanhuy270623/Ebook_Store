/**
 * Admin Posts List Page JavaScript
 * Handles DataTable initialization
 */
$(function () {
    $('#postsTable').DataTable({
        "responsive": true,
        "searching": false,
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.25/i18n/Vietnamese.json"
        }
    });
});

