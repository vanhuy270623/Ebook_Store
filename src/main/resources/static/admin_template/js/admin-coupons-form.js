/**
 * Admin Coupons Form Page JavaScript
 * Handles discount type changes and date validation
 */
$(document).ready(function() {
    // Update discount unit based on type
    $('#discountType').on('change', function() {
        var type = $(this).val();
        if (type === 'PERCENT') {
            $('#discountUnit').text('%');
            $('#discountValue').attr('max', '100');
        } else {
            $('#discountUnit').text('VNĐ');
            $('#discountValue').removeAttr('max');
        }
    });

    // Trigger on load
    $('#discountType').trigger('change');

    // Validate dates
    $('#validTo').on('change', function() {
        var validFrom = new Date($('#validFrom').val());
        var validTo = new Date($(this).val());

        if (validTo <= validFrom) {
            alert('Ngày kết thúc phải sau ngày bắt đầu!');
            $(this).val('');
        }
    });
});

