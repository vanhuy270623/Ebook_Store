/**
 * Admin Books Form Page JavaScript
 * Handles Select2 initialization and image preview
 */
$(document).ready(function() {
    // Initialize Select2
    $('.select2').select2({
        placeholder: "Chọn tác giả",
        allowClear: true
    });

    // Image preview
    $('#coverImage').change(function() {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function(e) {
                $('#imagePreview').attr('src', e.target.result).show();
            }
            reader.readAsDataURL(file);
        }
    });
});

