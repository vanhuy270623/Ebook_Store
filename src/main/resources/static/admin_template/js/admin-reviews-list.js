/**
 * Admin Reviews List Page JavaScript
 * Handles DataTable initialization
 */
$(function () {
    $('#reviewsTable').DataTable({
        "responsive": true,
        "searching": false,
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.25/i18n/Vietnamese.json"
        }
    });
});

