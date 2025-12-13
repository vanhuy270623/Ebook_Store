/**
 * User Profile Page JavaScript
 * Handles avatar preview before upload
 */

// Preview avatar before upload
function previewAvatar(input) {
    if (input.files && input.files[0]) {
        // Check file size (5MB max)
        if (input.files[0].size > 5 * 1024 * 1024) {
            alert('Kích thước file không được vượt quá 5MB!');
            input.value = '';
            return;
        }

        // Check file type
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/jpg'];
        if (!allowedTypes.includes(input.files[0].type)) {
            alert('Chỉ chấp nhận file ảnh (JPG, PNG, GIF)!');
            input.value = '';
            return;
        }

        // Preview image
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('avatarPreview');
            const icon = document.getElementById('avatarIcon');

            if (preview) {
                preview.src = e.target.result;
                preview.style.display = 'block';
                if (icon) icon.style.display = 'none';
            } else {
                // Create preview if not exists
                const newPreview = document.createElement('img');
                newPreview.id = 'avatarPreview';
                newPreview.src = e.target.result;
                newPreview.className = 'rounded-circle border';
                newPreview.style.width = '100px';
                newPreview.style.height = '100px';
                newPreview.style.objectFit = 'cover';

                if (icon) {
                    icon.parentNode.replaceChild(newPreview, icon);
                }
            }
        };
        reader.readAsDataURL(input.files[0]);
    }
}

