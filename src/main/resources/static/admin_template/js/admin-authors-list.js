/**
 * Admin Authors List Page JavaScript
 * Handles DataTable initialization
 */
$(function () {
    $('#authorsTable').DataTable({
        "responsive": true,
        "lengthChange": false,
        "autoWidth": false,
        "searching": false,
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.25/i18n/Vietnamese.json"
        }
    });
});

