/**
 * Admin Categories List Page JavaScript
 * Handles DataTable initialization
 */
$(function () {
    $('#categoriesTable').DataTable({
        "responsive": true,
        "lengthChange": false,
        "searching": false,
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.25/i18n/Vietnamese.json"
        }
    });
});

